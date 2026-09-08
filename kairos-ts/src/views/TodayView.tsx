import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Sparkles, Plus, CheckCircle2 } from 'lucide-react';
import { EisenhowerPriority, Task } from '../types';
import { TaskCard } from '../components/TaskCard';
import { getTodayQuote } from '../db/initialData';

interface TodayViewProps {
  tasks: Task[];
  onToggle: (id: number) => void;
  onEdit: (task: Task) => void;
  onDelete: (id: number) => void;
  onFocus: (task: Task) => void;
  onMoveUp: (id: number) => void;
  onMoveDown: (id: number) => void;
  onQuickAdd: (title: string, priority: EisenhowerPriority) => void;
}

const FILTER_CHIPS: { value: EisenhowerPriority; label: string }[] = [
  { value: 0, label: 'All' },
  { value: 1, label: 'Do now (Q1)' },
  { value: 2, label: 'Schedule (Q2)' },
  { value: 3, label: 'Delegate (Q3)' },
  { value: 4, label: 'Eliminate (Q4)' },
];

export const TodayView: React.FC<TodayViewProps> = ({
  tasks,
  onToggle,
  onEdit,
  onDelete,
  onFocus,
  onMoveUp,
  onMoveDown,
  onQuickAdd,
}) => {
  const [selectedFilter, setSelectedFilter] = useState<EisenhowerPriority>(0);
  const [quickInput, setQuickInput] = useState('');

  const quote = getTodayQuote();

  const totalTasks = tasks.length;
  const completedTasks = tasks.filter((t) => t.is_completed).length;
  const progressPercent = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  const filteredTasks = tasks.filter((t) => {
    if (selectedFilter === 0) return true;
    return t.priority === selectedFilter;
  });

  const handleQuickAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!quickInput.trim()) return;
    onQuickAdd(quickInput.trim(), selectedFilter);
    setQuickInput('');
  };

  const todayFormatted = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="flex-1 h-screen overflow-y-auto p-6 lg:p-8 max-w-5xl xl:max-w-6xl w-full mx-auto space-y-6">
      {/* Date Title */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-kairos-text">{todayFormatted}</h1>
        <p className="text-xs text-kairos-muted mt-1">Focus on what matters most today</p>
      </div>

      {/* Daily Progress Card */}
      <div className="p-4 rounded-2xl bg-kairos-surface border border-kairos-border shadow-sm">
        <div className="flex items-center justify-between text-xs mb-2.5">
          <span className="font-semibold text-kairos-text flex items-center">
            <CheckCircle2 className="w-4 h-4 mr-1.5 text-kairos-mint" />
            Today's Progress
          </span>
          <span className="text-kairos-muted font-mono font-medium">
            {completedTasks}/{totalTasks} completed ({progressPercent}%)
          </span>
        </div>
        <div className="w-full h-2 rounded-full bg-kairos-surface-high overflow-hidden">
          <motion.div
            initial={{ width: 0 }}
            animate={{ width: `${progressPercent}%` }}
            transition={{ duration: 0.5, ease: 'easeOut' }}
            className="h-full bg-kairos-mint rounded-full shadow-glow-mint"
          />
        </div>
      </div>

      {/* Stoic Quote Banner */}
      <div className="p-4 rounded-xl bg-gradient-to-r from-kairos-surface/80 to-kairos-surface-high/60 backdrop-blur-md border-l-4 border-kairos-accent border border-kairos-border/70 text-xs shadow-sm">
        <div className="flex items-start space-x-2.5">
          <Sparkles className="w-4 h-4 text-kairos-accent shrink-0 mt-0.5" />
          <div>
            <p className="italic text-kairos-text font-serif text-[13px]">“{quote.text}”</p>
            <p className="text-[11px] text-kairos-muted mt-1 font-sans">— {quote.source}</p>
          </div>
        </div>
      </div>

      {/* Filter Chips & Quick Add */}
      <div className="space-y-3">
        {/* Quadrant Chips */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1">
          {FILTER_CHIPS.map((chip) => {
            const isSelected = selectedFilter === chip.value;
            return (
              <button
                key={chip.value}
                onClick={() => setSelectedFilter(chip.value)}
                className={`px-3 py-1.5 rounded-xl text-xs font-medium transition-all ${
                  isSelected
                    ? 'bg-kairos-accent text-white font-bold shadow-glow-accent'
                    : 'bg-kairos-surface border border-kairos-border text-kairos-muted hover:border-kairos-surface-high hover:text-kairos-text'
                }`}
              >
                {chip.label}
              </button>
            );
          })}
        </div>

        {/* Quick Add Bar */}
        <form onSubmit={handleQuickAddSubmit} className="flex items-center space-x-2">
          <input
            type="text"
            value={quickInput}
            onChange={(e) => setQuickInput(e.target.value)}
            placeholder="+ Quick add task for today (Press Enter)..."
            className="flex-1 px-4 py-2.5 rounded-xl bg-kairos-surface border border-kairos-border text-kairos-text placeholder:text-kairos-muted text-xs focus:outline-none focus:border-kairos-accent transition-colors shadow-sm"
          />
          <button
            type="submit"
            className="px-4 py-2.5 rounded-xl bg-kairos-surface-high hover:bg-kairos-accent hover:text-white border border-kairos-border text-kairos-text text-xs font-semibold transition-colors flex items-center space-x-1.5"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Add</span>
          </button>
        </form>
      </div>

      {/* Tasks List */}
      <div className="space-y-2.5 pt-2">
        {filteredTasks.length === 0 ? (
          <div className="py-16 text-center text-xs text-kairos-muted">
            <p className="text-sm font-medium text-kairos-text mb-1">All clear!</p>
            <p>No tasks match this filter. Take a breath or add a new goal.</p>
          </div>
        ) : (
          filteredTasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onToggle={onToggle}
              onEdit={onEdit}
              onDelete={onDelete}
              onFocus={onFocus}
              onMoveUp={onMoveUp}
              onMoveDown={onMoveDown}
              showReorder={selectedFilter === 0}
            />
          ))
        )}
      </div>
    </div>
  );
};
