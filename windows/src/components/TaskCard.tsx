import React from 'react';
import { motion } from 'framer-motion';
import { Check, ChevronUp, ChevronDown, Edit2, Trash2, Timer, Repeat } from 'lucide-react';
import { Task } from '../types';

interface TaskCardProps {
  task: Task;
  onToggle: (id: number) => void;
  onEdit: (task: Task) => void;
  onDelete: (id: number) => void;
  onFocus: (task: Task) => void;
  onMoveUp?: (id: number) => void;
  onMoveDown?: (id: number) => void;
  showReorder?: boolean;
}

const QUADRANT_CONFIG = {
  1: { label: 'Do now', bg: 'bg-kairos-coral/15', border: 'border-kairos-coral/30', text: 'text-kairos-coral' },
  2: { label: 'Schedule', bg: 'bg-kairos-accent/15', border: 'border-kairos-accent/30', text: 'text-kairos-accent' },
  3: { label: 'Delegate', bg: 'bg-kairos-amber/15', border: 'border-kairos-amber/30', text: 'text-kairos-amber' },
  4: { label: 'Eliminate', bg: 'bg-kairos-slate/15', border: 'border-kairos-slate/30', text: 'text-kairos-muted' },
  0: { label: '', bg: '', border: '', text: '' },
};

export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  onToggle,
  onEdit,
  onDelete,
  onFocus,
  onMoveUp,
  onMoveDown,
  showReorder = true,
}) => {
  const quadrant = QUADRANT_CONFIG[task.priority] || QUADRANT_CONFIG[0];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
      transition={{ type: 'spring', stiffness: 450, damping: 30 }}
      className={`group relative flex items-center justify-between p-3.5 rounded-xl border transition-all duration-200 backdrop-blur-md ${
        task.is_completed
          ? 'bg-kairos-surface/40 border-kairos-border/40 opacity-60'
          : 'bg-kairos-surface/75 border-kairos-border hover:border-kairos-accent/40 hover:bg-kairos-surface shadow-sm hover:shadow-md'
      }`}
    >
      <div className="flex items-center space-x-3.5 flex-1 min-w-0">
        {/* Custom Rounded Checkbox */}
        <button
          onClick={() => onToggle(task.id)}
          className={`w-5 h-5 rounded-md flex items-center justify-center border transition-all shrink-0 ${
            task.is_completed
              ? 'bg-kairos-mint border-kairos-mint text-white'
              : 'border-kairos-muted/40 hover:border-kairos-accent text-transparent'
          }`}
          title={task.is_completed ? 'Mark incomplete' : 'Mark complete'}
        >
          <Check className="w-3.5 h-3.5 stroke-[3]" />
        </button>

        {/* Middle Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center space-x-2 flex-wrap gap-y-1">
            <span
              className={`text-sm font-medium transition-all truncate ${
                task.is_completed ? 'line-through text-kairos-muted' : 'text-kairos-text'
              }`}
            >
              {task.title}
            </span>

            {/* Eisenhower Quadrant Pill */}
            {task.priority > 0 && quadrant.label && (
              <span
                className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${quadrant.bg} ${quadrant.border} ${quadrant.text}`}
              >
                {quadrant.label}
              </span>
            )}

            {/* Recurring Tag */}
            {task.repeat_rule !== 'NONE' && (
              <span className="flex items-center text-[10px] px-1.5 py-0.5 rounded-full bg-kairos-accent/15 text-kairos-accent border border-kairos-accent/25">
                <Repeat className="w-2.5 h-2.5 mr-1" />
                {task.repeat_rule.toLowerCase()}
              </span>
            )}
          </div>

          {/* Subtitles: Date, Notes, Pomodoro info */}
          <div className="flex items-center space-x-3 mt-1 text-[11px] text-kairos-muted">
            {task.scheduled_date && <span>📅 {task.scheduled_date}</span>}
            {task.estimated_pomodoros > 0 && (
              <span className="flex items-center text-kairos-accent font-medium">
                <Timer className="w-3 h-3 mr-1" />
                {task.completed_pomodoros}/{task.estimated_pomodoros}
              </span>
            )}
            {task.notes && <span className="truncate max-w-[240px]">{task.notes}</span>}
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center space-x-1 opacity-90 group-hover:opacity-100 transition-opacity ml-3">
        {/* Reordering */}
        {showReorder && !task.is_completed && onMoveUp && onMoveDown && (
          <div className="flex items-center space-x-0.5 mr-1">
            <button
              onClick={() => onMoveUp(task.id)}
              className="p-1 rounded-md bg-kairos-surface-high border border-kairos-border text-kairos-muted hover:text-kairos-text"
              title="Move up"
            >
              <ChevronUp className="w-3 h-3" />
            </button>
            <button
              onClick={() => onMoveDown(task.id)}
              className="p-1 rounded-md bg-kairos-surface-high border border-kairos-border text-kairos-muted hover:text-kairos-text"
              title="Move down"
            >
              <ChevronDown className="w-3 h-3" />
            </button>
          </div>
        )}

        {/* Quick Focus Button */}
        {!task.is_completed && (
          <button
            onClick={() => onFocus(task)}
            className="flex items-center space-x-1 text-xs px-2.5 py-1 rounded-md bg-kairos-accent/15 border border-kairos-accent/30 text-kairos-accent hover:bg-kairos-accent hover:text-white font-medium transition-colors"
            title="Start Pomodoro focus"
          >
            <Timer className="w-3 h-3 mr-0.5" />
            <span>Focus</span>
          </button>
        )}

        {/* Edit */}
        <button
          onClick={() => onEdit(task)}
          className="p-1.5 rounded-md hover:bg-kairos-surface-high text-kairos-muted hover:text-kairos-text transition-colors"
          title="Edit"
        >
          <Edit2 className="w-3.5 h-3.5" />
        </button>

        {/* Delete */}
        <button
          onClick={() => onDelete(task.id)}
          className="p-1.5 rounded-md hover:bg-kairos-coral/15 text-kairos-muted hover:text-kairos-coral transition-colors"
          title="Delete"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      </div>
    </motion.div>
  );
};
