import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Calendar, Repeat as RepeatIcon, Timer, AlertCircle } from 'lucide-react';
import { EisenhowerPriority, RepeatRule, Task } from '../types';

interface TaskModalProps {
  isOpen: boolean;
  task?: Task | null;
  initialPriority?: EisenhowerPriority;
  onClose: () => void;
  onSave: (taskData: Partial<Task>) => void;
}

const QUADRANTS: { value: EisenhowerPriority; label: string; hint: string; border: string; activeBg: string; activeText: string }[] = [
  { value: 0, label: 'Unsorted', hint: 'No box', border: 'border-kairos-border', activeBg: 'bg-kairos-accent', activeText: 'text-white' },
  { value: 1, label: 'Do now', hint: 'Urgent + Important', border: 'border-kairos-coral', activeBg: 'bg-kairos-coral/20', activeText: 'text-kairos-coral' },
  { value: 2, label: 'Schedule', hint: 'Important', border: 'border-kairos-accent', activeBg: 'bg-kairos-accent/20', activeText: 'text-kairos-accent' },
  { value: 3, label: 'Delegate', hint: 'Urgent', border: 'border-kairos-amber', activeBg: 'bg-kairos-amber/20', activeText: 'text-kairos-amber' },
  { value: 4, label: 'Eliminate', hint: 'Low Priority', border: 'border-kairos-slate', activeBg: 'bg-kairos-slate/20', activeText: 'text-kairos-muted' },
];

export const TaskModal: React.FC<TaskModalProps> = ({
  isOpen,
  task,
  initialPriority = 0,
  onClose,
  onSave,
}) => {
  const [title, setTitle] = useState('');
  const [notes, setNotes] = useState('');
  const [priority, setPriority] = useState<EisenhowerPriority>(initialPriority);
  const [scheduledDate, setScheduledDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [repeatRule, setRepeatRule] = useState<RepeatRule>('NONE');
  const [estimatedPomodoros, setEstimatedPomodoros] = useState(0);

  useEffect(() => {
    if (task) {
      setTitle(task.title);
      setNotes(task.notes || '');
      setPriority(task.priority);
      setScheduledDate(task.scheduled_date || new Date().toISOString().split('T')[0]);
      setRepeatRule(task.repeat_rule || 'NONE');
      setEstimatedPomodoros(task.estimated_pomodoros || 0);
    } else {
      setTitle('');
      setNotes('');
      setPriority(initialPriority);
      setScheduledDate(new Date().toISOString().split('T')[0]);
      setRepeatRule('NONE');
      setEstimatedPomodoros(0);
    }
  }, [task, initialPriority, isOpen]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    onSave({
      title: title.trim(),
      notes: notes.trim(),
      priority,
      scheduled_date: scheduledDate,
      repeat_rule: repeatRule,
      estimated_pomodoros: estimatedPomodoros,
    });
    onClose();
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-black/70 backdrop-blur-sm"
        />

        {/* Modal Dialog */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          transition={{ type: 'spring', duration: 0.3 }}
          className="relative w-full max-w-lg bg-kairos-surface border border-kairos-border rounded-2xl shadow-2xl p-6 z-10"
        >
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-semibold text-kairos-text">
              {task ? 'Edit Task' : 'Create New Task'}
            </h2>
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-kairos-muted hover:text-kairos-text hover:bg-kairos-surface-high transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Title */}
            <div>
              <label className="block text-xs font-semibold text-kairos-muted mb-1.5 uppercase tracking-wider">
                Title
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="What needs to be done?"
                autoFocus
                className="w-full px-3.5 py-2.5 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-sm focus:outline-none focus:border-kairos-accent transition-colors"
              />
            </div>

            {/* Notes */}
            <div>
              <label className="block text-xs font-semibold text-kairos-muted mb-1.5 uppercase tracking-wider">
                Notes
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add context, sub-tasks, or details..."
                rows={2}
                className="w-full px-3.5 py-2 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-sm focus:outline-none focus:border-kairos-accent transition-colors resize-none"
              />
            </div>

            {/* Eisenhower Matrix Priority Selector */}
            <div>
              <label className="block text-xs font-semibold text-kairos-muted mb-1.5 uppercase tracking-wider">
                Eisenhower Priority
              </label>
              <div className="grid grid-cols-5 gap-1.5">
                {QUADRANTS.map((q) => {
                  const isActive = priority === q.value;
                  return (
                    <button
                      key={q.value}
                      type="button"
                      onClick={() => setPriority(q.value)}
                      title={q.hint}
                      className={`px-2 py-2 rounded-lg border text-xs font-medium transition-all text-center ${
                        isActive
                          ? `${q.activeBg} ${q.activeText} ${q.border} font-bold shadow-sm`
                          : 'bg-kairos-surface-high border-kairos-border text-kairos-muted hover:border-kairos-muted/50'
                      }`}
                    >
                      {q.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Date & Repeat Row */}
            <div className="grid grid-cols-3 gap-3 pt-1">
              {/* Date */}
              <div>
                <label className="flex items-center text-xs font-semibold text-kairos-muted mb-1.5 uppercase tracking-wider">
                  <Calendar className="w-3.5 h-3.5 mr-1" />
                  Date
                </label>
                <input
                  type="date"
                  value={scheduledDate}
                  onChange={(e) => setScheduledDate(e.target.value)}
                  className="w-full px-2.5 py-2 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-xs focus:outline-none focus:border-kairos-accent"
                />
              </div>

              {/* Repeat */}
              <div>
                <label className="flex items-center text-xs font-semibold text-kairos-muted mb-1.5 uppercase tracking-wider">
                  <RepeatIcon className="w-3.5 h-3.5 mr-1" />
                  Repeat
                </label>
                <select
                  value={repeatRule}
                  onChange={(e) => setRepeatRule(e.target.value as RepeatRule)}
                  className="w-full px-2.5 py-2 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-xs focus:outline-none focus:border-kairos-accent"
                >
                  <option value="NONE">Never</option>
                  <option value="DAILY">Daily</option>
                  <option value="WEEKDAYS">Weekdays</option>
                  <option value="WEEKLY">Weekly</option>
                </select>
              </div>

              {/* Pomodoros */}
              <div>
                <label className="flex items-center text-xs font-semibold text-kairos-muted mb-1.5 uppercase tracking-wider">
                  <Timer className="w-3.5 h-3.5 mr-1" />
                  Pomodoro
                </label>
                <input
                  type="number"
                  min="0"
                  max="20"
                  value={estimatedPomodoros}
                  onChange={(e) => setEstimatedPomodoros(parseInt(e.target.value) || 0)}
                  className="w-full px-2.5 py-2 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-xs focus:outline-none focus:border-kairos-accent"
                />
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end space-x-2.5 pt-4 border-t border-kairos-border mt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-medium text-kairos-muted hover:text-kairos-text hover:bg-kairos-surface-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-xl text-xs font-bold bg-kairos-accent hover:bg-kairos-accent-hover text-white shadow-glow-accent transition-all"
              >
                {task ? 'Save Changes' : 'Create Task'}
              </button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
