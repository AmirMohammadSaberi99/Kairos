import React from 'react';
import { Calendar as CalendarIcon } from 'lucide-react';
import { Task } from '../types';
import { TaskCard } from '../components/TaskCard';

interface UpcomingViewProps {
  tasks: Task[];
  onToggle: (id: number) => void;
  onEdit: (task: Task) => void;
  onDelete: (id: number) => void;
  onFocus: (task: Task) => void;
}

export const UpcomingView: React.FC<UpcomingViewProps> = ({
  tasks,
  onToggle,
  onEdit,
  onDelete,
  onFocus,
}) => {
  // Group by date
  const groupedTasks: { [dateStr: string]: Task[] } = {};
  tasks.forEach((t) => {
    const key = t.scheduled_date || 'No Date';
    if (!groupedTasks[key]) groupedTasks[key] = [];
    groupedTasks[key].push(t);
  });

  const sortedDates = Object.keys(groupedTasks).sort();

  return (
    <div className="flex-1 h-screen overflow-y-auto p-6 lg:p-8 max-w-5xl xl:max-w-6xl w-full mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-kairos-text">Upcoming Schedule</h1>
        <p className="text-xs text-kairos-muted mt-1">Plan and organize future priorities</p>
      </div>

      {sortedDates.length === 0 ? (
        <div className="py-24 text-center text-xs text-kairos-muted">
          <CalendarIcon className="w-8 h-8 mx-auto mb-3 opacity-40" />
          <p className="text-sm font-medium text-kairos-text mb-1">No upcoming tasks scheduled</p>
          <p>Create a task with a future date to plan ahead.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {sortedDates.map((dateStr) => {
            const dateObj = new Date(dateStr + 'T00:00:00');
            const displayDate = isNaN(dateObj.getTime())
              ? dateStr
              : dateObj.toLocaleDateString('en-US', {
                  weekday: 'long',
                  month: 'short',
                  day: 'numeric',
                });

            return (
              <div key={dateStr} className="space-y-2.5">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-kairos-accent">
                    {displayDate}
                  </span>
                  <div className="flex-1 h-px bg-kairos-border/60" />
                </div>

                <div className="space-y-2">
                  {groupedTasks[dateStr].map((task) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      onToggle={onToggle}
                      onEdit={onEdit}
                      onDelete={onDelete}
                      onFocus={onFocus}
                      showReorder={false}
                    />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
