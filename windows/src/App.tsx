import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { TaskModal } from './components/TaskModal';
import { CommandPalette } from './components/CommandPalette';
import { TodayView } from './views/TodayView';
import { MatrixView } from './views/MatrixView';
import { FocusView } from './views/FocusView';
import { UpcomingView } from './views/UpcomingView';
import { HistoryView } from './views/HistoryView';
import { SettingsView } from './views/SettingsView';
import { db, generateUuid } from './db/indexedDb';
import { EisenhowerPriority, Task, UserPreferences, ViewDestination } from './types';

export const App: React.FC = () => {
  const [activeDestination, setActiveDestination] = useState<ViewDestination>('today');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [allTasks, setAllTasks] = useState<Task[]>([]);
  const [preferences, setPreferences] = useState<UserPreferences>({
    focus_minutes: 25,
    break_minutes: 5,
    sound_enabled: true,
    quotes_enabled: true,
    week_starts_monday: true,
    theme: 'dark',
  });

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [modalInitialPriority, setModalInitialPriority] = useState<EisenhowerPriority>(0);
  const [isCommandOpen, setIsCommandOpen] = useState(false);
  const [linkedFocusTask, setLinkedFocusTask] = useState<Task | null>(null);

  const loadData = async () => {
    const all = await db.getAllTasks();
    setAllTasks(all);

    if (activeDestination === 'today' || activeDestination === 'matrix') {
      const today = await db.getTodayTasks();
      setTasks(today);
    } else if (activeDestination === 'upcoming') {
      const upcoming = await db.getUpcomingTasks();
      setTasks(upcoming);
    } else if (activeDestination === 'history') {
      const history = await db.getHistoryTasks();
      setTasks(history);
    } else {
      setTasks(all.filter((t) => !t.is_deleted));
    }

    const prefs = await db.getPreferences();
    setPreferences(prefs);
  };

  useEffect(() => {
    db.purgeSampleTasks().then(() => loadData());
    const unsubscribe = db.subscribe(loadData);
    return () => unsubscribe();
  }, [activeDestination]);

  // Synchronize Dark / Light Theme class on root document
  useEffect(() => {
    const isDark = (preferences.theme || 'dark') === 'dark';
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [preferences.theme]);

  // Global Keyboard Shortcuts (Ctrl+K, Ctrl+N)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setIsCommandOpen((prev) => !prev);
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        setEditingTask(null);
        setModalInitialPriority(0);
        setIsModalOpen(true);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleToggleTheme = async () => {
    const nextTheme = (preferences.theme || 'dark') === 'dark' ? 'light' : 'dark';
    const updated: UserPreferences = { ...preferences, theme: nextTheme };
    setPreferences(updated);
    await db.savePreferences(updated);
  };

  const handleToggle = async (id: number) => {
    await db.toggleComplete(id);
  };

  const handleDelete = async (id: number) => {
    await db.deleteTask(id);
  };

  const handleEdit = (task: Task) => {
    setEditingTask(task);
    setIsModalOpen(true);
  };

  const handleFocus = (task: Task) => {
    setLinkedFocusTask(task);
    setActiveDestination('focus');
  };

  const handleMoveUp = async (id: number) => {
    await db.reorderTask(id, 'up');
  };

  const handleMoveDown = async (id: number) => {
    await db.reorderTask(id, 'down');
  };

  const handleQuickAdd = async (title: string, priority: EisenhowerPriority) => {
    const todayStr = new Date().toISOString().split('T')[0];
    await db.addTask({
      sync_id: generateUuid(),
      title,
      notes: '',
      scheduled_date: todayStr,
      reminder_minutes: null,
      priority,
      repeat_rule: 'NONE',
      sort_position: Date.now(),
      estimated_pomodoros: 0,
      completed_pomodoros: 0,
      is_completed: false,
      completed_at: null,
      created_at: Date.now(),
      updated_at: Date.now(),
      is_deleted: false,
    });
  };

  const handleSaveModal = async (taskData: Partial<Task>) => {
    if (editingTask) {
      await db.updateTask({
        ...editingTask,
        ...taskData,
      } as Task);
    } else {
      await db.addTask({
        sync_id: generateUuid(),
        title: taskData.title || '',
        notes: taskData.notes || '',
        scheduled_date: taskData.scheduled_date || new Date().toISOString().split('T')[0],
        reminder_minutes: null,
        priority: taskData.priority || 0,
        repeat_rule: taskData.repeat_rule || 'NONE',
        sort_position: Date.now(),
        estimated_pomodoros: taskData.estimated_pomodoros || 0,
        completed_pomodoros: 0,
        is_completed: false,
        completed_at: null,
        created_at: Date.now(),
        updated_at: Date.now(),
        is_deleted: false,
      });
    }
  };

  const handleAddInQuadrant = (priority: EisenhowerPriority) => {
    setEditingTask(null);
    setModalInitialPriority(priority);
    setIsModalOpen(true);
  };

  const handlePomodoroComplete = async (taskId?: number) => {
    if (taskId) {
      await db.incrementPomodoro(taskId);
    }
  };

  const handleImportTasks = async (importedTasks: Task[]) => {
    for (const t of importedTasks) {
      const existing = allTasks.find((x) => x.sync_id === t.sync_id);
      if (!existing) {
        await db.addTask(t);
      } else if (t.updated_at > existing.updated_at) {
        await db.updateTask({ ...t, id: existing.id });
      }
    }
  };

  const handleClearAllData = async () => {
    await db.clearAllData();
    await loadData();
  };

  const totalPomodoros = allTasks
    .filter((t) => !t.is_deleted)
    .reduce((acc, curr) => acc + (curr.completed_pomodoros || 0), 0);

  return (
    <div className="flex h-screen bg-kairos-bg overflow-hidden font-sans relative transition-colors duration-300">
      {/* Decorative Ambient Frosted Glass Refraction Lights */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0 select-none">
        <div className="absolute -top-32 -left-32 w-[34rem] h-[34rem] rounded-full bg-kairos-accent/15 blur-3xl transition-all duration-700" />
        <div className="absolute top-1/3 -right-32 w-[32rem] h-[32rem] rounded-full bg-kairos-mint/12 blur-3xl transition-all duration-700" />
        <div className="absolute -bottom-32 left-1/3 w-[36rem] h-[36rem] rounded-full bg-kairos-coral/10 blur-3xl transition-all duration-700" />
      </div>

      {/* Frosted Glass Sidebar */}
      <Sidebar
        activeDestination={activeDestination}
        onNavigate={setActiveDestination}
        onNewTask={() => {
          setEditingTask(null);
          setModalInitialPriority(0);
          setIsModalOpen(true);
        }}
        onOpenCommand={() => setIsCommandOpen(true)}
        todayCount={allTasks.filter((t) => !t.is_completed && !t.is_deleted).length}
        theme={preferences.theme || 'dark'}
        onToggleTheme={handleToggleTheme}
      />

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col overflow-hidden relative z-10">
        {activeDestination === 'today' && (
          <TodayView
            tasks={tasks}
            onToggle={handleToggle}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onFocus={handleFocus}
            onMoveUp={handleMoveUp}
            onMoveDown={handleMoveDown}
            onQuickAdd={handleQuickAdd}
          />
        )}

        {activeDestination === 'matrix' && (
          <MatrixView
            tasks={allTasks.filter((t) => !t.is_deleted)}
            onToggle={handleToggle}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onFocus={handleFocus}
            onAddInQuadrant={handleAddInQuadrant}
            onQuickAddInQuadrant={handleQuickAdd}
          />
        )}

        {activeDestination === 'focus' && (
          <FocusView
            tasks={allTasks}
            linkedTask={linkedFocusTask}
            preferences={preferences}
            onPomodoroComplete={handlePomodoroComplete}
          />
        )}

        {activeDestination === 'upcoming' && (
          <UpcomingView
            tasks={tasks}
            onToggle={handleToggle}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onFocus={handleFocus}
          />
        )}

        {activeDestination === 'history' && (
          <HistoryView
            tasks={tasks}
            totalPomodoros={totalPomodoros}
            onToggle={handleToggle}
            onDelete={handleDelete}
          />
        )}

        {activeDestination === 'settings' && (
          <SettingsView
            preferences={preferences}
            allTasks={allTasks}
            onSavePreferences={(p) => {
              setPreferences(p);
              db.savePreferences(p);
            }}
            onImportTasks={handleImportTasks}
            onClearAllData={handleClearAllData}
          />
        )}
      </main>

      {/* Modals & Command Palette */}
      <TaskModal
        isOpen={isModalOpen}
        task={editingTask}
        initialPriority={modalInitialPriority}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveModal}
      />

      <CommandPalette
        isOpen={isCommandOpen}
        tasks={allTasks}
        onClose={() => setIsCommandOpen(false)}
        onNavigate={setActiveDestination}
        onNewTask={() => {
          setEditingTask(null);
          setIsModalOpen(true);
        }}
        onSelectTask={(task) => {
          handleFocus(task);
        }}
        onToggleTheme={handleToggleTheme}
      />
    </div>
  );
};
