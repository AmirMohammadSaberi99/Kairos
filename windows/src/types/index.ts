export type EisenhowerPriority = 0 | 1 | 2 | 3 | 4;

export type RepeatRule = 'NONE' | 'DAILY' | 'WEEKDAYS' | 'WEEKLY';

export interface Task {
  id: number;
  sync_id: string;
  title: string;
  notes: string;
  scheduled_date: string | null; // YYYY-MM-DD
  reminder_minutes: number | null;
  priority: EisenhowerPriority; // 0: None, 1: Do now, 2: Schedule, 3: Delegate, 4: Eliminate
  repeat_rule: RepeatRule;
  sort_position: number;
  estimated_pomodoros: number;
  completed_pomodoros: number;
  is_completed: boolean;
  completed_at: number | null;
  created_at: number;
  updated_at: number;
  is_deleted: boolean;
}

export type ViewDestination = 'today' | 'matrix' | 'upcoming' | 'focus' | 'history' | 'settings';

export interface DailyQuote {
  text: string;
  source: string;
}

export interface UserPreferences {
  focus_minutes: number;
  break_minutes: number;
  sound_enabled: boolean;
  quotes_enabled: boolean;
  week_starts_monday: boolean;
  theme?: 'dark' | 'light';
}
