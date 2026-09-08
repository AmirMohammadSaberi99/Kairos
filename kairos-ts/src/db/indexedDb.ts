import { RepeatRule, Task, UserPreferences } from '../types';
import { INITIAL_TASKS } from './initialData';

const DB_NAME = 'kairos_db';
const DB_VERSION = 3;
const STORE_TASKS = 'tasks';
const STORE_PREFS = 'preferences';

export function generateUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

class LocalDatabase {
  private db: IDBDatabase | null = null;
  private initPromise: Promise<IDBDatabase> | null = null;
  private listeners: (() => void)[] = [];

  private async init(): Promise<IDBDatabase> {
    if (this.db) return this.db;
    if (this.initPromise) return this.initPromise;

    this.initPromise = new Promise((resolve, reject) => {
      try {
        const req = indexedDB.open(DB_NAME, DB_VERSION);

        req.onupgradeneeded = (e) => {
          const db = (e.target as IDBOpenDBRequest).result;
          if (!db.objectStoreNames.contains(STORE_TASKS)) {
            const taskStore = db.createObjectStore(STORE_TASKS, { keyPath: 'id', autoIncrement: true });
            taskStore.createIndex('scheduled_date', 'scheduled_date', { unique: false });
            taskStore.createIndex('sync_id', 'sync_id', { unique: true });
            // Add initial starter tasks during upgrade
            INITIAL_TASKS.forEach((t) => taskStore.add(t));
          }
          if (!db.objectStoreNames.contains(STORE_PREFS)) {
            const prefStore = db.createObjectStore(STORE_PREFS, { keyPath: 'id' });
            prefStore.add({
              id: 'user_prefs',
              focus_minutes: 25,
              break_minutes: 5,
              sound_enabled: true,
              quotes_enabled: true,
              week_starts_monday: true,
            });
          }
        };

        req.onsuccess = () => {
          this.db = req.result;
          resolve(this.db);
        };

        req.onerror = () => {
          this.initPromise = null;
          reject(req.error);
        };
      } catch (err) {
        this.initPromise = null;
        reject(err);
      }
    });

    return this.initPromise;
  }

  subscribe(callback: () => void): () => void {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== callback);
    };
  }

  private notify() {
    this.listeners.forEach((l) => {
      try {
        l();
      } catch (e) {
        console.error('Listener notification error:', e);
      }
    });
  }

  async getAllTasks(): Promise<Task[]> {
    try {
      const db = await this.init();
      return new Promise((resolve) => {
        const tx = db.transaction(STORE_TASKS, 'readonly');
        const store = tx.objectStore(STORE_TASKS);
        const req = store.getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => resolve([]);
      });
    } catch {
      return INITIAL_TASKS;
    }
  }

  async getTodayTasks(): Promise<Task[]> {
    const all = await this.getAllTasks();
    const today = new Date().toISOString().split('T')[0];
    return all
      .filter((t) => !t.is_deleted && ((!t.is_completed && (!t.scheduled_date || t.scheduled_date <= today)) || (t.is_completed && t.scheduled_date === today)))
      .sort((a, b) => (a.is_completed === b.is_completed ? a.sort_position - b.sort_position : a.is_completed ? 1 : -1));
  }

  async getUpcomingTasks(): Promise<Task[]> {
    const all = await this.getAllTasks();
    const today = new Date().toISOString().split('T')[0];
    return all
      .filter((t) => !t.is_deleted && !t.is_completed && t.scheduled_date && t.scheduled_date > today)
      .sort((a, b) => (a.scheduled_date || '').localeCompare(b.scheduled_date || '') || a.sort_position - b.sort_position);
  }

  async getHistoryTasks(): Promise<Task[]> {
    const all = await this.getAllTasks();
    return all
      .filter((t) => !t.is_deleted && t.is_completed)
      .sort((a, b) => (b.completed_at || 0) - (a.completed_at || 0));
  }

  async addTask(task: Omit<Task, 'id'> | Task): Promise<number> {
    const db = await this.init();
    const now = Date.now();
    const taskToSave = {
      ...task,
      sync_id: task.sync_id || generateUuid(),
      created_at: task.created_at || now,
      updated_at: now,
      sort_position: task.sort_position || now,
      is_deleted: false,
    };

    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_TASKS, 'readwrite');
      const store = tx.objectStore(STORE_TASKS);
      const req = store.add(taskToSave);
      req.onsuccess = () => {
        this.notify();
        resolve(req.result as number);
      };
      req.onerror = () => reject(req.error);
    });
  }

  async updateTask(task: Task): Promise<void> {
    const db = await this.init();
    task.updated_at = Date.now();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_TASKS, 'readwrite');
      const store = tx.objectStore(STORE_TASKS);
      const req = store.put(task);
      req.onsuccess = () => {
        this.notify();
        resolve();
      };
      req.onerror = () => reject(req.error);
    });
  }

  async deleteTask(id: number): Promise<void> {
    const all = await this.getAllTasks();
    const task = all.find((t) => t.id === id);
    if (!task) return;
    task.is_deleted = true;
    await this.updateTask(task);
  }

  async toggleComplete(id: number): Promise<void> {
    const all = await this.getAllTasks();
    const task = all.find((t) => t.id === id);
    if (!task) return;

    const newStatus = !task.is_completed;
    task.is_completed = newStatus;
    task.completed_at = newStatus ? Date.now() : null;
    await this.updateTask(task);

    // Auto-create next recurring occurrence if completing
    if (newStatus && task.repeat_rule !== 'NONE') {
      const nextDate = this.calculateNextOccurrence(task.scheduled_date, task.repeat_rule);
      if (nextDate) {
        const nextTask: Omit<Task, 'id'> = {
          ...task,
          sync_id: generateUuid(),
          scheduled_date: nextDate,
          is_completed: false,
          completed_at: null,
          completed_pomodoros: 0,
          created_at: Date.now(),
          updated_at: Date.now(),
          sort_position: Date.now(),
        };
        await this.addTask(nextTask);
      }
    }
  }

  async incrementPomodoro(id: number): Promise<void> {
    const all = await this.getAllTasks();
    const task = all.find((t) => t.id === id);
    if (!task) return;
    task.completed_pomodoros = (task.completed_pomodoros || 0) + 1;
    await this.updateTask(task);
  }

  async reorderTask(taskId: number, direction: 'up' | 'down'): Promise<void> {
    const today = await this.getTodayTasks();
    const idx = today.findIndex((t) => t.id === taskId);
    if (idx === -1) return;
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= today.length) return;

    const t1 = today[idx];
    const t2 = today[targetIdx];
    const temp = t1.sort_position;
    t1.sort_position = t2.sort_position === temp ? temp + (direction === 'up' ? -1000 : 1000) : t2.sort_position;
    t2.sort_position = temp;

    await this.updateTask(t1);
    await this.updateTask(t2);
  }

  private calculateNextOccurrence(currentDateStr: string | null, rule: RepeatRule): string | null {
    const current = currentDateStr ? new Date(currentDateStr) : new Date();
    if (rule === 'DAILY') {
      current.setDate(current.getDate() + 1);
    } else if (rule === 'WEEKLY') {
      current.setDate(current.getDate() + 7);
    } else if (rule === 'WEEKDAYS') {
      do {
        current.setDate(current.getDate() + 1);
      } while (current.getDay() === 0 || current.getDay() === 6);
    } else {
      return null;
    }
    return current.toISOString().split('T')[0];
  }

  async getPreferences(): Promise<UserPreferences> {
    try {
      const db = await this.init();
      return new Promise((resolve) => {
        const tx = db.transaction(STORE_PREFS, 'readonly');
        const req = tx.objectStore(STORE_PREFS).get('user_prefs');
        req.onsuccess = () => {
          const res = req.result;
          resolve(
            res
              ? { theme: 'dark', ...res }
              : {
                  focus_minutes: 25,
                  break_minutes: 5,
                  sound_enabled: true,
                  quotes_enabled: true,
                  week_starts_monday: true,
                  theme: 'dark',
                }
          );
        };
        req.onerror = () => {
          resolve({
            focus_minutes: 25,
            break_minutes: 5,
            sound_enabled: true,
            quotes_enabled: true,
            week_starts_monday: true,
            theme: 'dark',
          });
        };
      });
    } catch {
      return {
        focus_minutes: 25,
        break_minutes: 5,
        sound_enabled: true,
        quotes_enabled: true,
        week_starts_monday: true,
        theme: 'dark',
      };
    }
  }

  async savePreferences(prefs: UserPreferences): Promise<void> {
    try {
      const db = await this.init();
      return new Promise((resolve, reject) => {
        const tx = db.transaction(STORE_PREFS, 'readwrite');
        const req = tx.objectStore(STORE_PREFS).put({ id: 'user_prefs', ...prefs });
        req.onsuccess = () => {
          this.notify();
          resolve();
        };
        req.onerror = () => reject(req.error);
      });
    } catch {
      // ignore
    }
  }

  async purgeSampleTasks(): Promise<void> {
    try {
      const all = await this.getAllTasks();
      const sampleTasks = all.filter((t) => t.sync_id && t.sync_id.startsWith('sample-'));
      for (const t of sampleTasks) {
        await this.deleteTask(t.id);
      }
    } catch {
      // ignore
    }
  }

  async clearAllData(): Promise<void> {
    try {
      const db = await this.init();
      return new Promise((resolve, reject) => {
        const tx = db.transaction([STORE_TASKS], 'readwrite');
        const req = tx.objectStore(STORE_TASKS).clear();
        req.onsuccess = () => {
          this.notify();
          resolve();
        };
        req.onerror = () => reject(req.error);
      });
    } catch {
      // ignore
    }
  }
}

export const db = new LocalDatabase();
