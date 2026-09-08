import React, { useState } from 'react';
import { Plus, Check, Flame, Calendar, Users2, ShieldAlert, Sparkles, Timer, Trash2, ArrowUpRight } from 'lucide-react';
import { EisenhowerPriority, Task } from '../types';

interface MatrixViewProps {
  tasks: Task[];
  onToggle: (id: number) => void;
  onEdit: (task: Task) => void;
  onDelete: (id: number) => void;
  onFocus?: (task: Task) => void;
  onAddInQuadrant: (priority: EisenhowerPriority) => void;
  onQuickAddInQuadrant?: (title: string, priority: EisenhowerPriority) => void;
}

interface QuadrantMeta {
  priority: EisenhowerPriority;
  qCode: string;
  title: string;
  subtitle: string;
  axisUrgent: string;
  axisImportant: string;
  icon: React.ComponentType<{ className?: string }>;
  accentColor: string;
  borderColor: string;
  headerBadgeBg: string;
  glowBg: string;
  emptyTitle: string;
  emptyDesc: string;
}

const QUADRANTS: QuadrantMeta[] = [
  {
    priority: 1,
    qCode: 'Q1',
    title: 'Do First',
    subtitle: 'Urgent & Important — Execute immediately',
    axisUrgent: 'Urgent',
    axisImportant: 'Important',
    icon: Flame,
    accentColor: 'text-[#FF7D8B]',
    borderColor: 'border-[#FF7D8B]/30 hover:border-[#FF7D8B]/60 focus-within:border-[#FF7D8B]/60',
    headerBadgeBg: 'bg-[#2E181F] text-[#FF7D8B] border-[#FF7D8B]/40',
    glowBg: 'from-[#FF7D8B]/5 to-transparent',
    emptyTitle: 'No urgent crises pending',
    emptyDesc: 'Great job staying proactive. Nothing critical demanding immediate panic.',
  },
  {
    priority: 2,
    qCode: 'Q2',
    title: 'Schedule',
    subtitle: 'Important, Not Urgent — Strategic deep work',
    axisUrgent: 'Not Urgent',
    axisImportant: 'Important',
    icon: Calendar,
    accentColor: 'text-[#8B7CFF]',
    borderColor: 'border-[#8B7CFF]/30 hover:border-[#8B7CFF]/60 focus-within:border-[#8B7CFF]/60',
    headerBadgeBg: 'bg-[#1E1A33] text-[#8B7CFF] border-[#8B7CFF]/40',
    glowBg: 'from-[#8B7CFF]/5 to-transparent',
    emptyTitle: 'High-impact zone is clear',
    emptyDesc: 'Schedule strategic goals, learning, and preventive work here to prevent future crises.',
  },
  {
    priority: 3,
    qCode: 'Q3',
    title: 'Delegate',
    subtitle: 'Urgent, Not Important — Automate, batch, or offload',
    axisUrgent: 'Urgent',
    axisImportant: 'Not Important',
    icon: Users2,
    accentColor: 'text-[#F59E0B]',
    borderColor: 'border-[#F59E0B]/30 hover:border-[#F59E0B]/60 focus-within:border-[#F59E0B]/60',
    headerBadgeBg: 'bg-[#2B2212] text-[#F59E0B] border-[#F59E0B]/40',
    glowBg: 'from-[#F59E0B]/5 to-transparent',
    emptyTitle: 'Zero interruptions',
    emptyDesc: 'No pressing minor requests or administrative busywork demanding attention.',
  },
  {
    priority: 4,
    qCode: 'Q4',
    title: 'Eliminate',
    subtitle: 'Not Urgent & Not Important — Time sinks to discard',
    axisUrgent: 'Not Urgent',
    axisImportant: 'Not Important',
    icon: ShieldAlert,
    accentColor: 'text-[#989BA8]',
    borderColor: 'border-[#6B7280]/30 hover:border-[#6B7280]/60 focus-within:border-[#6B7280]/60',
    headerBadgeBg: 'bg-[#181C24] text-[#989BA8] border-[#6B7280]/40',
    glowBg: 'from-[#6B7280]/5 to-transparent',
    emptyTitle: 'Clean mental canvas',
    emptyDesc: 'Free from trivial distractions, unessential chores, and mental clutter.',
  },
];

export const MatrixView: React.FC<MatrixViewProps> = ({
  tasks,
  onToggle,
  onEdit,
  onDelete,
  onFocus,
  onAddInQuadrant,
  onQuickAddInQuadrant,
}) => {
  const [quickInputs, setQuickInputs] = useState<{ [key: number]: string }>({});

  const handleInputChange = (priority: EisenhowerPriority, text: string) => {
    setQuickInputs((prev) => ({ ...prev, [priority]: text }));
  };

  const handleQuickSubmit = (e: React.FormEvent, priority: EisenhowerPriority) => {
    e.preventDefault();
    const val = quickInputs[priority]?.trim();
    if (!val) return;
    if (onQuickAddInQuadrant) {
      onQuickAddInQuadrant(val, priority);
    } else {
      onAddInQuadrant(priority);
    }
    setQuickInputs((prev) => ({ ...prev, [priority]: '' }));
  };

  const totalActive = tasks.filter((t) => !t.is_completed).length;

  return (
    <div className="flex-1 h-screen flex flex-col p-5 lg:p-7 min-h-0 overflow-hidden w-full select-none bg-kairos-bg">
      {/* Top Header & Executive Summary Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-kairos-border/60 shrink-0">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-xl lg:text-2xl font-bold tracking-tight text-kairos-text">
              Eisenhower Matrix (2×2)
            </h1>
            <span className="text-xs font-mono font-semibold px-2 py-0.5 rounded-full bg-kairos-surface-high border border-kairos-border text-kairos-muted">
              {totalActive} active tasks
            </span>
          </div>
          <p className="text-xs text-kairos-muted mt-0.5">
            Decide what to do, schedule, delegate, or discard to master your mental bandwidth
          </p>
        </div>

        {/* Quadrant Quick Counts Legend */}
        <div className="flex items-center space-x-2 shrink-0">
          {QUADRANTS.map((q) => {
            const count = tasks.filter((t) => t.priority === q.priority && !t.is_completed).length;
            return (
              <div
                key={q.priority}
                onClick={() => onAddInQuadrant(q.priority)}
                className={`flex items-center space-x-1.5 px-2.5 py-1 rounded-lg border text-xs cursor-pointer hover:opacity-80 transition-all ${q.headerBadgeBg}`}
                title={`Click to add in ${q.title}`}
              >
                <span className="font-mono font-bold text-[11px]">{q.qCode}</span>
                <span className="font-semibold">{count}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Axis Guide Row (Urgent vs Not Urgent) */}
      <div className="hidden md:grid grid-cols-2 gap-4 lg:gap-5 pt-2 shrink-0">
        <div className="flex items-center justify-between px-2 text-[11px] font-mono uppercase tracking-widest text-kairos-muted/80">
          <span className="flex items-center space-x-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-[#FF7D8B]" />
            <span>◄ Urgent</span>
          </span>
          <span className="text-[10px] text-kairos-muted/50">Immediate Attention</span>
        </div>
        <div className="flex items-center justify-between px-2 text-[11px] font-mono uppercase tracking-widest text-kairos-muted/80">
          <span className="flex items-center space-x-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-[#8B7CFF]" />
            <span>Not Urgent ►</span>
          </span>
          <span className="text-[10px] text-kairos-muted/50">Planned & Proactive</span>
        </div>
      </div>

      {/* 2x2 Matrix Fluid Workspace Grid */}
      <div className="flex-1 grid grid-cols-1 md:grid-cols-2 grid-rows-2 gap-4 lg:gap-5 min-h-0 w-full mt-2">
        {QUADRANTS.map((q) => {
          const qTasks = tasks.filter((t) => t.priority === q.priority);
          const activeTasks = qTasks.filter((t) => !t.is_completed);
          const completedTasks = qTasks.filter((t) => t.is_completed);
          const Icon = q.icon;

          return (
            <div
              key={q.priority}
              className={`flex flex-col h-full min-h-0 rounded-2xl bg-kairos-surface/85 border p-4 lg:p-5 transition-all shadow-md relative overflow-hidden backdrop-blur-sm group/quadrant ${q.borderColor}`}
            >
              {/* Subtle Atmospheric Corner Glow */}
              <div
                className={`absolute -top-16 -right-16 w-36 h-36 rounded-full bg-gradient-to-br ${q.glowBg} pointer-events-none blur-2xl opacity-60`}
              />

              {/* Quadrant Header */}
              <div className="flex items-center justify-between pb-3 border-b border-kairos-border/60 shrink-0 relative z-10">
                <div className="flex items-center space-x-2.5 min-w-0">
                  <div className={`p-1.5 rounded-xl border ${q.headerBadgeBg}`}>
                    <Icon className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center space-x-2">
                      <span className={`text-sm font-bold tracking-tight truncate ${q.accentColor}`}>
                        {q.title}
                      </span>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded-md bg-kairos-surface-high text-kairos-muted font-bold border border-kairos-border/60">
                        {activeTasks.length}
                      </span>
                    </div>
                    <p className="text-[11px] text-kairos-muted mt-0.5 truncate hidden sm:block">
                      {q.subtitle}
                    </p>
                  </div>
                </div>

                <div className="flex items-center space-x-1.5 shrink-0 ml-2">
                  <button
                    onClick={() => onAddInQuadrant(q.priority)}
                    className="p-1.5 rounded-lg bg-kairos-surface-high hover:bg-kairos-accent hover:text-white border border-kairos-border text-kairos-muted transition-all shadow-sm"
                    title={`Open task modal for ${q.title}`}
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Scrollable Tasks List inside Quadrant */}
              <div className="flex-1 overflow-y-auto min-h-0 py-2.5 space-y-2 pr-1 custom-scrollbar relative z-10">
                {qTasks.length === 0 ? (
                  <div className="h-full flex flex-col items-center justify-center text-center p-4 border border-dashed border-kairos-border/50 rounded-xl bg-kairos-surface-high/15">
                    <div className={`p-2.5 rounded-xl mb-2 opacity-50 ${q.headerBadgeBg}`}>
                      <Icon className="w-5 h-5" />
                    </div>
                    <p className="text-xs font-semibold text-kairos-text/90">{q.emptyTitle}</p>
                    <p className="text-[11px] text-kairos-muted max-w-xs mt-1 leading-relaxed">
                      {q.emptyDesc}
                    </p>
                    <button
                      onClick={() => onAddInQuadrant(q.priority)}
                      className="mt-3 text-[11px] font-medium text-kairos-accent hover:underline flex items-center space-x-1"
                    >
                      <Plus className="w-3 h-3" />
                      <span>Add item to {q.title}</span>
                    </button>
                  </div>
                ) : (
                  <>
                    {/* Active Tasks */}
                    {activeTasks.map((task) => (
                      <div
                        key={task.id}
                        className="group flex items-center justify-between p-2.5 lg:p-3 rounded-xl bg-kairos-surface-high/60 border border-kairos-border/80 hover:border-kairos-muted/50 hover:bg-kairos-surface-high transition-all shadow-sm"
                      >
                        <div className="flex items-center space-x-2.5 min-w-0 flex-1">
                          <button
                            onClick={() => onToggle(task.id)}
                            className="w-4 h-4 rounded-md border border-kairos-muted/40 hover:border-kairos-accent flex items-center justify-center text-transparent hover:text-kairos-accent transition-all shrink-0"
                            title="Complete task"
                          >
                            <Check className="w-3 h-3 stroke-[3]" />
                          </button>
                          <div className="min-w-0 flex-1 cursor-pointer" onClick={() => onEdit(task)}>
                            <p className="text-xs font-medium text-kairos-text truncate">
                              {task.title}
                            </p>
                            {task.notes && (
                              <p className="text-[10px] text-kairos-muted truncate mt-0.5">
                                {task.notes}
                              </p>
                            )}
                          </div>
                        </div>

                        {/* Hover Actions */}
                        <div className="flex items-center space-x-1 shrink-0 ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          {onFocus && (
                            <button
                              onClick={() => onFocus(task)}
                              className="p-1 rounded-md text-kairos-muted hover:text-kairos-accent hover:bg-kairos-surface transition-colors"
                              title="Focus with Pomodoro"
                            >
                              <Timer className="w-3.5 h-3.5" />
                            </button>
                          )}
                          <button
                            onClick={() => onEdit(task)}
                            className="p-1 rounded-md text-kairos-muted hover:text-kairos-text hover:bg-kairos-surface transition-colors"
                            title="Edit"
                          >
                            <ArrowUpRight className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => onDelete(task.id)}
                            className="p-1 rounded-md text-kairos-muted hover:text-kairos-coral hover:bg-[#2E181F] transition-colors"
                            title="Delete"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    ))}

                    {/* Completed Tasks in Quadrant */}
                    {completedTasks.length > 0 && (
                      <div className="pt-2 space-y-1.5 opacity-60">
                        {completedTasks.map((task) => (
                          <div
                            key={task.id}
                            className="flex items-center justify-between p-2 rounded-lg bg-kairos-surface-high/30 border border-kairos-border/40 text-xs"
                          >
                            <div className="flex items-center space-x-2.5 min-w-0 flex-1">
                              <button
                                onClick={() => onToggle(task.id)}
                                className="w-4 h-4 rounded-md bg-kairos-mint text-kairos-bg flex items-center justify-center shrink-0"
                                title="Mark incomplete"
                              >
                                <Check className="w-3 h-3 stroke-[3]" />
                              </button>
                              <span
                                onClick={() => onEdit(task)}
                                className="text-xs text-kairos-muted line-through truncate cursor-pointer"
                              >
                                {task.title}
                              </span>
                            </div>
                            <button
                              onClick={() => onDelete(task.id)}
                              className="text-[10px] text-kairos-muted hover:text-kairos-coral px-1"
                            >
                              ✕
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>

              {/* Inline Quick Add Footer */}
              <form
                onSubmit={(e) => handleQuickSubmit(e, q.priority)}
                className="pt-2.5 border-t border-kairos-border/50 shrink-0 flex items-center space-x-2 relative z-10"
              >
                <input
                  type="text"
                  value={quickInputs[q.priority] || ''}
                  onChange={(e) => handleInputChange(q.priority, e.target.value)}
                  placeholder={`+ Quick add to ${q.title}...`}
                  className="flex-1 px-3 py-1.5 rounded-lg bg-kairos-surface-high/70 border border-kairos-border/70 text-kairos-text placeholder:text-kairos-muted/60 text-xs focus:outline-none focus:border-kairos-accent transition-colors"
                />
                <button
                  type="submit"
                  disabled={!quickInputs[q.priority]?.trim()}
                  className="px-2.5 py-1.5 rounded-lg bg-kairos-surface-high border border-kairos-border text-kairos-muted hover:text-white hover:bg-kairos-accent text-xs font-semibold disabled:opacity-40 disabled:hover:bg-kairos-surface-high disabled:hover:text-kairos-muted transition-all"
                >
                  Add
                </button>
              </form>
            </div>
          );
        })}
      </div>
    </div>
  );
};
