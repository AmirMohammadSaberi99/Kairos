import React from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, Grid2X2, Calendar, Timer, History, Settings, Plus, Search, Sun, Moon } from 'lucide-react';
import { ViewDestination } from '../types';

interface SidebarProps {
  activeDestination: ViewDestination;
  onNavigate: (dest: ViewDestination) => void;
  onNewTask: () => void;
  onOpenCommand: () => void;
  todayCount?: number;
  theme?: 'dark' | 'light';
  onToggleTheme?: () => void;
}

const NAV_ITEMS: { id: ViewDestination; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'today', label: 'Today', icon: CheckCircle2 },
  { id: 'matrix', label: 'Eisenhower (2x2)', icon: Grid2X2 },
  { id: 'focus', label: 'Focus & Timer', icon: Timer },
  { id: 'upcoming', label: 'Upcoming', icon: Calendar },
  { id: 'history', label: 'History', icon: History },
  { id: 'settings', label: 'Settings', icon: Settings },
];

export const Sidebar: React.FC<SidebarProps> = ({
  activeDestination,
  onNavigate,
  onNewTask,
  onOpenCommand,
  todayCount = 0,
  theme = 'dark',
  onToggleTheme,
}) => {
  return (
    <aside className="w-64 h-screen bg-kairos-surface/75 backdrop-blur-2xl border-r border-kairos-border flex flex-col justify-between p-4 select-none z-20 shadow-glass-sm">
      {/* Top Header & Navigation */}
      <div className="space-y-4">
        {/* Brand */}
        <div className="flex items-center justify-between px-2 py-1">
          <div className="flex items-center space-x-2.5">
            <img src="./kairos_logo.png" alt="Kairos Logo" className="w-6 h-6 rounded-lg shadow-sm" />
            <span className="text-base font-bold tracking-tight text-kairos-text">Kairos</span>
          </div>
          <span className="text-[10px] font-mono font-medium px-1.5 py-0.5 rounded bg-kairos-surface-high border border-kairos-border text-kairos-muted">
            TS v0.4.1
          </span>
        </div>

        {/* Quick Actions (New Task & Search) */}
        <div className="space-y-2 pt-1">
          <button
            onClick={onNewTask}
            className="w-full flex items-center justify-center space-x-2 py-2.5 rounded-xl bg-kairos-accent hover:bg-kairos-accent-hover text-white font-bold text-xs shadow-glow-accent transition-all duration-200"
          >
            <Plus className="w-4 h-4" />
            <span>New Task</span>
          </button>

          {/* Command Palette Trigger */}
          <button
            onClick={onOpenCommand}
            className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-kairos-surface-high/60 border border-kairos-border text-kairos-muted hover:text-kairos-text text-xs transition-colors"
          >
            <div className="flex items-center space-x-2">
              <Search className="w-3.5 h-3.5" />
              <span>Search / Jump to...</span>
            </div>
            <kbd className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-kairos-surface border border-kairos-border text-kairos-muted">
              Ctrl+K
            </kbd>
          </button>
        </div>

        {/* Nav Links */}
        <nav className="space-y-1 pt-2">
          {NAV_ITEMS.map((item) => {
            const isActive = activeDestination === item.id;
            const Icon = item.icon;

            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={`relative w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium transition-all ${
                  isActive
                    ? 'text-kairos-accent font-semibold'
                    : 'text-kairos-muted hover:text-kairos-text hover:bg-kairos-surface-high/40'
                }`}
              >
                {/* Active Indicator Background */}
                {isActive && (
                  <motion.div
                    layoutId="sidebarActivePill"
                    className="absolute inset-0 rounded-xl bg-kairos-accent/15 border border-kairos-accent/30"
                    transition={{ type: 'spring', stiffness: 500, damping: 35 }}
                  />
                )}

                <div className="relative z-10 flex items-center space-x-3">
                  <Icon className={`w-4 h-4 ${isActive ? 'text-kairos-accent' : 'text-kairos-muted'}`} />
                  <span>{item.label}</span>
                </div>

                {item.id === 'today' && todayCount > 0 && (
                  <span className="relative z-10 text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-kairos-surface-high border border-kairos-border text-kairos-muted">
                    {todayCount}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Footer Controls & Theme Toggle */}
      <div className="space-y-3 pt-3 border-t border-kairos-border/50">
        {onToggleTheme && (
          <button
            onClick={onToggleTheme}
            className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-kairos-surface-high/70 hover:bg-kairos-surface-high border border-kairos-border text-kairos-text text-xs transition-all shadow-sm group"
            title={`Switch to ${theme === 'dark' ? 'Frosted Light' : 'Midnight Dark'} theme`}
          >
            <div className="flex items-center space-x-2.5">
              {theme === 'dark' ? (
                <Moon className="w-3.5 h-3.5 text-kairos-accent transition-transform group-hover:-rotate-12" />
              ) : (
                <Sun className="w-3.5 h-3.5 text-amber-500 transition-transform group-hover:rotate-45" />
              )}
              <span className="font-medium">
                {theme === 'dark' ? 'Midnight Dark' : 'Frosted Light'}
              </span>
            </div>
            <span className="text-[10px] font-mono uppercase px-1.5 py-0.5 rounded bg-kairos-surface border border-kairos-border text-kairos-muted">
              {theme === 'dark' ? 'Dark' : 'Light'}
            </span>
          </button>
        )}

        <div className="px-1 text-[11px] text-kairos-muted/80 flex items-center justify-between">
          <span className="font-medium text-kairos-muted">Offline-First</span>
          <span className="text-[10px] text-kairos-muted/60">IndexedDB</span>
        </div>
      </div>
    </aside>
  );
};
