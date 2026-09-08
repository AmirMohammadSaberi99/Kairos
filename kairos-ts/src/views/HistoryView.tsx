import React from 'react';
import { CheckCircle2, Timer, Check, Trash2 } from 'lucide-react';
import { Task } from '../types';

interface HistoryViewProps {
  tasks: Task[];
  totalPomodoros: number;
  onToggle: (id: number) => void;
  onDelete: (id: number) => void;
}

export const HistoryView: React.FC<HistoryViewProps> = ({
  tasks,
  totalPomodoros,
  onToggle,
  onDelete,
}) => {
  return (
    <div className="flex-1 h-screen overflow-y-auto p-6 lg:p-8 max-w-5xl xl:max-w-6xl w-full mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-kairos-text">Completed Archive</h1>
        <p className="text-xs text-kairos-muted mt-1">Review accomplishments and focused blocks</p>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 gap-4">
        <div className="p-4 rounded-2xl bg-kairos-surface/80 backdrop-blur-md border border-kairos-border flex items-center space-x-3.5 shadow-sm">
          <div className="p-3 rounded-xl bg-kairos-mint/15 text-kairos-mint border border-kairos-mint/25">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-kairos-muted font-medium">Completed Tasks</p>
            <p className="text-2xl font-bold text-kairos-text mt-0.5">{tasks.length}</p>
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-kairos-surface/80 backdrop-blur-md border border-kairos-border flex items-center space-x-3.5 shadow-sm">
          <div className="p-3 rounded-xl bg-kairos-accent/15 text-kairos-accent border border-kairos-accent/25">
            <Timer className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs text-kairos-muted font-medium">Total Focus Pomodoros</p>
            <p className="text-2xl font-bold text-kairos-text mt-0.5">{totalPomodoros}</p>
          </div>
        </div>
      </div>

      {/* List */}
      <div className="space-y-2 pt-2">
        {tasks.length === 0 ? (
          <div className="py-24 text-center text-xs text-kairos-muted">
            <p className="text-sm font-medium text-kairos-text mb-1">No completed tasks yet</p>
            <p>Complete tasks in Today or Upcoming to build your history.</p>
          </div>
        ) : (
          tasks.map((task) => (
            <div
              key={task.id}
              className="flex items-center justify-between p-3.5 rounded-xl bg-kairos-surface/50 border border-kairos-border/60 hover:border-kairos-border transition-colors group"
            >
              <div className="flex items-center space-x-3 min-w-0 flex-1">
                <button
                  onClick={() => onToggle(task.id)}
                  className="w-5 h-5 rounded-md bg-kairos-mint text-kairos-bg flex items-center justify-center shrink-0"
                  title="Mark incomplete"
                >
                  <Check className="w-3.5 h-3.5 stroke-[3]" />
                </button>
                <div className="truncate">
                  <p className="text-xs font-medium text-kairos-muted line-through truncate">
                    {task.title}
                  </p>
                  <p className="text-[10px] text-kairos-muted/60 mt-0.5">
                    {task.completed_at
                      ? `Completed ${new Date(task.completed_at).toLocaleString()}`
                      : 'Completed'}
                  </p>
                </div>
              </div>

              <button
                onClick={() => onDelete(task.id)}
                className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg text-kairos-muted hover:text-kairos-coral hover:bg-kairos-coral/15 transition-all ml-2"
                title="Delete"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
