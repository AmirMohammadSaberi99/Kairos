import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Play, Pause, RotateCcw, Coffee, Zap } from 'lucide-react';
import { Task, UserPreferences } from '../types';
import { sounds } from '../audio/chimes';

interface FocusViewProps {
  tasks: Task[];
  linkedTask?: Task | null;
  preferences: UserPreferences;
  onPomodoroComplete: (taskId?: number) => void;
}

export const FocusView: React.FC<FocusViewProps> = ({
  tasks,
  linkedTask = null,
  preferences,
  onPomodoroComplete,
}) => {
  const [isBreak, setIsBreak] = useState(false);
  const [isRunning, setIsRunning] = useState(false);
  const [currentTaskId, setCurrentTaskId] = useState<number | undefined>(linkedTask?.id);

  const focusSeconds = (preferences.focus_minutes || 25) * 60;
  const breakSeconds = (preferences.break_minutes || 5) * 60;

  const [totalSeconds, setTotalSeconds] = useState(focusSeconds);
  const [remainingSeconds, setRemainingSeconds] = useState(focusSeconds);

  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    if (linkedTask) {
      setCurrentTaskId(linkedTask.id);
    }
  }, [linkedTask]);

  useEffect(() => {
    if (isRunning) {
      timerRef.current = window.setInterval(() => {
        setRemainingSeconds((prev) => {
          if (prev <= 1) {
            handleComplete();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isRunning, isBreak, currentTaskId]);

  const handleComplete = () => {
    setIsRunning(false);
    if (preferences.sound_enabled) {
      if (isBreak) {
        sounds.playBreakComplete();
      } else {
        sounds.playFocusComplete();
      }
    }

    if (!isBreak) {
      onPomodoroComplete(currentTaskId);
      // Auto-suggest break
      setIsBreak(true);
      setTotalSeconds(breakSeconds);
      setRemainingSeconds(breakSeconds);
    } else {
      // Auto-suggest focus
      setIsBreak(false);
      setTotalSeconds(focusSeconds);
      setRemainingSeconds(focusSeconds);
    }
  };

  const toggleTimer = () => setIsRunning((prev) => !prev);

  const resetTimer = () => {
    setIsRunning(false);
    const secs = isBreak ? breakSeconds : focusSeconds;
    setTotalSeconds(secs);
    setRemainingSeconds(secs);
  };

  const switchMode = (toBreak: boolean) => {
    setIsRunning(false);
    setIsBreak(toBreak);
    const secs = toBreak ? breakSeconds : focusSeconds;
    setTotalSeconds(secs);
    setRemainingSeconds(secs);
  };

  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = remainingSeconds % 60;
  const timeFormatted = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const progress = totalSeconds > 0 ? (totalSeconds - remainingSeconds) / totalSeconds : 0;
  const radius = 105;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - progress * circumference;

  const activeColor = isBreak ? '#70E1B0' : '#8B7CFF';

  return (
    <div className="flex-1 h-screen overflow-y-auto p-8 flex flex-col items-center justify-center select-none">
      <div className="w-full max-w-md flex flex-col items-center space-y-7">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-2xl font-bold tracking-tight text-kairos-text">Focus & Flow</h1>
          <p className="text-xs text-kairos-muted mt-1">Single-tasking for mental clarity</p>
        </div>

        {/* Task Binding Selector */}
        <div className="w-full p-2 rounded-xl bg-kairos-surface border border-kairos-border flex items-center space-x-2 text-xs">
          <span className="text-kairos-muted font-medium ml-2">Task:</span>
          <select
            value={currentTaskId || ''}
            onChange={(e) => setCurrentTaskId(e.target.value ? parseInt(e.target.value) : undefined)}
            className="flex-1 bg-kairos-surface-high border border-kairos-border text-kairos-text rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-kairos-accent text-xs truncate"
          >
            <option value="">None (General Deep Work)</option>
            {tasks
              .filter((t) => !t.is_completed && !t.is_deleted)
              .map((t) => (
                <option key={t.id} value={t.id}>
                  {t.title}
                </option>
              ))}
          </select>
        </div>

        {/* SVG Circular Ring Timer */}
        <div className="relative w-64 h-64 flex items-center justify-center">
          <svg className="w-full h-full -rotate-90">
            {/* Background Track */}
            <circle
              cx="128"
              cy="128"
              r={radius}
              className="stroke-kairos-surface-high fill-none"
              strokeWidth="8"
            />
            {/* Animated Glow Track */}
            <motion.circle
              cx="128"
              cy="128"
              r={radius}
              fill="none"
              stroke={activeColor}
              strokeWidth="8"
              strokeLinecap="round"
              strokeDasharray={circumference}
              animate={{ strokeDashoffset }}
              transition={{ duration: 0.5, ease: 'linear' }}
              style={{
                filter: isRunning
                  ? isBreak
                    ? 'drop-shadow(0 0 10px rgba(112, 225, 176, 0.4))'
                    : 'drop-shadow(0 0 10px rgba(139, 124, 255, 0.4))'
                  : 'none',
              }}
            />
          </svg>

          {/* Center Info */}
          <div className="absolute flex flex-col items-center">
            <span className="text-5xl font-mono font-bold tracking-tighter text-kairos-text">
              {timeFormatted}
            </span>
            <span
              className={`text-[11px] font-bold tracking-wider uppercase mt-2 px-2.5 py-0.5 rounded-full border ${
                isBreak
                  ? 'bg-kairos-mint/15 text-kairos-mint border-kairos-mint/30'
                  : 'bg-kairos-accent/15 text-kairos-accent border-kairos-accent/30'
              }`}
            >
              {isBreak ? 'Short Break' : 'Deep Focus'}
            </span>
          </div>
        </div>

        {/* Mode Buttons */}
        <div className="flex items-center space-x-2 bg-kairos-surface/80 backdrop-blur-md border border-kairos-border p-1 rounded-xl shadow-sm">
          <button
            onClick={() => switchMode(false)}
            className={`flex items-center space-x-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              !isBreak
                ? 'bg-kairos-accent/15 text-kairos-accent border border-kairos-accent/40 shadow-sm'
                : 'text-kairos-muted hover:text-kairos-text'
            }`}
          >
            <Zap className="w-3.5 h-3.5" />
            <span>25m Focus</span>
          </button>
          <button
            onClick={() => switchMode(true)}
            className={`flex items-center space-x-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              isBreak
                ? 'bg-kairos-mint/15 text-kairos-mint border border-kairos-mint/40 shadow-sm'
                : 'text-kairos-muted hover:text-kairos-text'
            }`}
          >
            <Coffee className="w-3.5 h-3.5" />
            <span>5m Break</span>
          </button>
        </div>

        {/* Controls */}
        <div className="flex items-center space-x-3">
          <button
            onClick={toggleTimer}
            className={`flex items-center justify-center space-x-2 px-8 py-3 rounded-2xl font-bold text-sm shadow-md transition-all duration-200 ${
              isRunning
                ? 'bg-kairos-amber text-kairos-bg hover:opacity-90'
                : isBreak
                ? 'bg-kairos-mint hover:bg-kairos-mint-hover text-kairos-bg shadow-glow-mint'
                : 'bg-kairos-accent hover:bg-kairos-accent-hover text-white shadow-glow-accent'
            }`}
          >
            {isRunning ? (
              <>
                <Pause className="w-4 h-4 fill-current" />
                <span>Pause</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4 fill-current" />
                <span>Start {isBreak ? 'Break' : 'Focus'}</span>
              </>
            )}
          </button>

          <button
            onClick={resetTimer}
            className="p-3 rounded-2xl bg-kairos-surface-high border border-kairos-border text-kairos-muted hover:text-kairos-text hover:border-kairos-surface-high transition-colors"
            title="Reset Timer"
          >
            <RotateCcw className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
