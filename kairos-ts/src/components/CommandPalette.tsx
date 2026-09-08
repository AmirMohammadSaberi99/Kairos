import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Plus, Calendar, Grid2X2, Timer, CheckCircle, Settings, Check, SunMoon } from 'lucide-react';
import { Task, ViewDestination } from '../types';

interface CommandPaletteProps {
  isOpen: boolean;
  tasks: Task[];
  onClose: () => void;
  onNavigate: (dest: ViewDestination) => void;
  onNewTask: () => void;
  onSelectTask: (task: Task) => void;
  onToggleTheme?: () => void;
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({
  isOpen,
  tasks,
  onClose,
  onNavigate,
  onNewTask,
  onSelectTask,
  onToggleTheme,
}) => {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  // Static Navigation Commands
  const navCommands = [
    { id: 'new', label: 'Create New Task...', icon: Plus, action: onNewTask },
    { id: 'today', label: 'Go to Today', icon: CheckCircle, action: () => onNavigate('today') },
    { id: 'matrix', label: 'Go to Eisenhower Matrix (2x2)', icon: Grid2X2, action: () => onNavigate('matrix') },
    { id: 'focus', label: 'Go to Focus Timer', icon: Timer, action: () => onNavigate('focus') },
    { id: 'upcoming', label: 'Go to Upcoming Schedule', icon: Calendar, action: () => onNavigate('upcoming') },
    { id: 'history', label: 'Go to Completed History', icon: Check, action: () => onNavigate('history') },
    { id: 'settings', label: 'Go to Settings & Backup', icon: Settings, action: () => onNavigate('settings') },
    ...(onToggleTheme ? [{ id: 'theme', label: 'Toggle Light / Dark Theme', icon: SunMoon, action: onToggleTheme }] : []),
  ];

  const filteredNav = navCommands.filter((c) =>
    c.label.toLowerCase().includes(query.toLowerCase())
  );

  const filteredTasks = tasks.filter((t) =>
    !t.is_deleted && t.title.toLowerCase().includes(query.toLowerCase())
  ).slice(0, 5);

  const allItems = [
    ...filteredNav.map((n) => ({ type: 'nav', data: n })),
    ...filteredTasks.map((t) => ({ type: 'task', data: t })),
  ];

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % allItems.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev - 1 + allItems.length) % allItems.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const current = allItems[selectedIndex];
      if (current) {
        if (current.type === 'nav') {
          (current.data as (typeof navCommands)[0]).action();
        } else {
          onSelectTask(current.data as Task);
        }
        onClose();
      }
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-24 px-4">
          {/* Backdrop Blur Overlay */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/40 backdrop-blur-md"
            onClick={onClose}
          />

          {/* Frosted Glass Command Palette Window */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -10 }}
            transition={{ type: 'spring', duration: 0.25 }}
            className="relative w-full max-w-xl bg-kairos-surface/90 backdrop-blur-2xl border border-kairos-border rounded-2xl shadow-2xl overflow-hidden z-10"
          >
            {/* Search Header */}
            <div className="flex items-center px-4 py-3.5 border-b border-kairos-border">
              <Search className="w-5 h-5 text-kairos-muted mr-3" />
              <input
                ref={inputRef}
                type="text"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setSelectedIndex(0);
                }}
                onKeyDown={handleKeyDown}
                placeholder="Type a command or search tasks..."
                className="w-full bg-transparent text-kairos-text placeholder:text-kairos-muted/60 text-sm focus:outline-none"
              />
              <kbd className="px-2 py-0.5 text-[10px] font-mono bg-kairos-surface-high border border-kairos-border rounded text-kairos-muted">
                ESC
              </kbd>
            </div>

            {/* List Results */}
            <div className="max-h-80 overflow-y-auto p-2 space-y-1">
              {allItems.length === 0 ? (
                <div className="py-8 text-center text-xs text-kairos-muted">
                  No commands or tasks matching "{query}"
                </div>
              ) : (
                allItems.map((item, idx) => {
                  const isSelected = idx === selectedIndex;
                  if (item.type === 'nav') {
                    const cmd = item.data as (typeof navCommands)[0];
                    const Icon = cmd.icon;
                    return (
                      <div
                        key={`nav-${cmd.id}`}
                        onClick={() => {
                          cmd.action();
                          onClose();
                        }}
                        onMouseEnter={() => setSelectedIndex(idx)}
                        className={`flex items-center justify-between px-3 py-2.5 rounded-xl cursor-pointer text-xs transition-colors ${
                          isSelected
                            ? 'bg-kairos-accent text-white font-medium shadow-glow-accent'
                            : 'text-kairos-text hover:bg-kairos-surface-high/60'
                        }`}
                      >
                        <div className="flex items-center space-x-2.5">
                          <Icon className="w-4 h-4" />
                          <span>{cmd.label}</span>
                        </div>
                        <span className={`text-[10px] ${isSelected ? 'text-white/80' : 'text-kairos-muted'}`}>
                          Action
                        </span>
                      </div>
                    );
                  } else {
                    const task = item.data as Task;
                    return (
                      <div
                        key={`task-${task.id}`}
                        onClick={() => {
                          onSelectTask(task);
                          onClose();
                        }}
                        onMouseEnter={() => setSelectedIndex(idx)}
                        className={`flex items-center justify-between px-3 py-2.5 rounded-xl cursor-pointer text-xs transition-colors ${
                          isSelected
                            ? 'bg-kairos-accent text-white font-medium shadow-glow-accent'
                            : 'text-kairos-text hover:bg-kairos-surface-high/60'
                        }`}
                      >
                        <div className="flex items-center space-x-2.5 truncate">
                          <Timer className="w-4 h-4 shrink-0" />
                          <span className="truncate">{task.title}</span>
                        </div>
                        <span className={`text-[10px] shrink-0 ml-2 ${isSelected ? 'text-white/80' : 'text-kairos-muted'}`}>
                          Focus Task
                        </span>
                      </div>
                    );
                  }
                })
              )}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};
