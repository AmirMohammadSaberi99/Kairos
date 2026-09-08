import React, { useRef } from 'react';
import { Download, Upload, Volume2, Sparkles, Timer, Palette, Sun, Moon } from 'lucide-react';
import { Task, UserPreferences } from '../types';

interface SettingsViewProps {
  preferences: UserPreferences;
  allTasks: Task[];
  onSavePreferences: (prefs: UserPreferences) => void;
  onImportTasks: (tasks: Task[]) => void;
  onClearAllData?: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  preferences,
  allTasks,
  onSavePreferences,
  onImportTasks,
  onClearAllData,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleExport = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(allTasks, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', `kairos-backup-${new Date().toISOString().split('T')[0]}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const imported = JSON.parse(event.target?.result as string);
        if (Array.isArray(imported)) {
          onImportTasks(imported);
          alert(`Successfully imported ${imported.length} tasks!`);
        }
      } catch {
        alert('Invalid JSON file format.');
      }
    };
    reader.readAsText(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className="flex-1 h-screen overflow-y-auto p-6 lg:p-8 max-w-5xl xl:max-w-6xl w-full mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-kairos-text">Settings & Preferences</h1>
        <p className="text-xs text-kairos-muted mt-1">Configure appearance, focus timers, sound, and data portability</p>
      </div>

      {/* Theme & Appearance Section */}
      <div className="p-5 rounded-2xl bg-kairos-surface/80 backdrop-blur-xl border border-kairos-border space-y-4 shadow-sm">
        <div className="flex items-center space-x-2 text-kairos-accent">
          <Palette className="w-4 h-4" />
          <h2 className="text-xs font-bold uppercase tracking-wider">
            Theme & Visual Atmosphere
          </h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
          {/* Dark Midnight Mode Card */}
          <div
            onClick={() => onSavePreferences({ ...preferences, theme: 'dark' })}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all flex items-start space-x-3.5 ${
              (preferences.theme || 'dark') === 'dark'
                ? 'border-kairos-accent bg-kairos-accent/10 shadow-glow-accent'
                : 'border-kairos-border bg-kairos-surface-high/40 hover:border-kairos-border/80'
            }`}
          >
            <div className="p-2.5 rounded-lg bg-[#090B10] text-[#8B7CFF] border border-white/10 shrink-0">
              <Moon className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between">
                <p className="text-xs font-bold text-kairos-text">Midnight Dark</p>
                {(preferences.theme || 'dark') === 'dark' && (
                  <span className="w-2 h-2 rounded-full bg-kairos-accent" />
                )}
              </div>
              <p className="text-[11px] text-kairos-muted mt-1 leading-relaxed">
                Deep obsidian canvas with subtle neon glows, low eye-strain for deep night focus.
              </p>
            </div>
          </div>

          {/* Frosted Light Mode Card */}
          <div
            onClick={() => onSavePreferences({ ...preferences, theme: 'light' })}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all flex items-start space-x-3.5 ${
              preferences.theme === 'light'
                ? 'border-kairos-accent bg-kairos-accent/10 shadow-sm'
                : 'border-kairos-border bg-kairos-surface-high/40 hover:border-kairos-border/80'
            }`}
          >
            <div className="p-2.5 rounded-lg bg-white text-amber-500 border border-black/10 shadow-sm shrink-0">
              <Sun className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between">
                <p className="text-xs font-bold text-kairos-text">Frosted Light Glass</p>
                {preferences.theme === 'light' && (
                  <span className="w-2 h-2 rounded-full bg-kairos-accent" />
                )}
              </div>
              <p className="text-[11px] text-kairos-muted mt-1 leading-relaxed">
                Luminous frosted glass canvas with low opacity translucent cards and clean refraction.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Focus & Audio Configuration */}
      <div className="p-5 rounded-2xl bg-kairos-surface/80 backdrop-blur-xl border border-kairos-border space-y-4 shadow-sm">
        <h2 className="text-xs font-bold uppercase tracking-wider text-kairos-accent">
          Focus & Sound Preferences
        </h2>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="flex items-center text-xs font-semibold text-kairos-muted mb-1.5">
              <Timer className="w-3.5 h-3.5 mr-1" />
              Focus Duration (Minutes)
            </label>
            <input
              type="number"
              min="1"
              max="120"
              value={preferences.focus_minutes}
              onChange={(e) =>
                onSavePreferences({
                  ...preferences,
                  focus_minutes: parseInt(e.target.value) || 25,
                })
              }
              className="w-full px-3 py-2 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-xs focus:outline-none focus:border-kairos-accent"
            />
          </div>

          <div>
            <label className="flex items-center text-xs font-semibold text-kairos-muted mb-1.5">
              <Timer className="w-3.5 h-3.5 mr-1" />
              Break Duration (Minutes)
            </label>
            <input
              type="number"
              min="1"
              max="60"
              value={preferences.break_minutes}
              onChange={(e) =>
                onSavePreferences({
                  ...preferences,
                  break_minutes: parseInt(e.target.value) || 5,
                })
              }
              className="w-full px-3 py-2 rounded-xl bg-kairos-surface-high border border-kairos-border text-kairos-text text-xs focus:outline-none focus:border-kairos-accent"
            />
          </div>
        </div>

        <div className="space-y-3 pt-2">
          <label className="flex items-center space-x-3 cursor-pointer">
            <input
              type="checkbox"
              checked={preferences.sound_enabled}
              onChange={(e) =>
                onSavePreferences({
                  ...preferences,
                  sound_enabled: e.target.checked,
                })
              }
              className="w-4 h-4 rounded text-kairos-accent focus:ring-0 bg-kairos-surface-high border-kairos-border"
            />
            <span className="text-xs text-kairos-text font-medium flex items-center">
              <Volume2 className="w-3.5 h-3.5 mr-1.5 text-kairos-muted" />
              Play procedural harmonic bell chime on session completion
            </span>
          </label>

          <label className="flex items-center space-x-3 cursor-pointer">
            <input
              type="checkbox"
              checked={preferences.quotes_enabled}
              onChange={(e) =>
                onSavePreferences({
                  ...preferences,
                  quotes_enabled: e.target.checked,
                })
              }
              className="w-4 h-4 rounded text-kairos-accent focus:ring-0 bg-kairos-surface-high border-kairos-border"
            />
            <span className="text-xs text-kairos-text font-medium flex items-center">
              <Sparkles className="w-3.5 h-3.5 mr-1.5 text-kairos-muted" />
              Display inspirational stoic & scientific quotes in Today view
            </span>
          </label>
        </div>
      </div>

      {/* Data Backup & Portability */}
      <div className="p-5 rounded-2xl bg-kairos-surface/80 backdrop-blur-xl border border-kairos-border space-y-4 shadow-sm">
        <div>
          <h2 className="text-xs font-bold uppercase tracking-wider text-kairos-accent">
            Data Portability & Android Compatibility
          </h2>
          <p className="text-xs text-kairos-muted mt-1">
            Export all tasks as a JSON file, or restore from an existing Kairos backup.
          </p>
        </div>

        <div className="flex items-center space-x-3 pt-1">
          <button
            onClick={handleExport}
            className="flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-kairos-surface-high hover:bg-kairos-surface-high/80 border border-kairos-border text-kairos-text text-xs font-semibold transition-colors"
          >
            <Download className="w-4 h-4 text-kairos-accent" />
            <span>Export JSON Backup</span>
          </button>

          <button
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-kairos-surface-high hover:bg-kairos-surface-high/80 border border-kairos-border text-kairos-text text-xs font-semibold transition-colors"
          >
            <Upload className="w-4 h-4 text-kairos-mint" />
            <span>Import Backup</span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            onChange={handleFileChange}
            className="hidden"
          />
        </div>

        <div className="pt-3 border-t border-kairos-border/60 flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-kairos-coral">Clear All Local Data</p>
            <p className="text-[11px] text-kairos-muted">Permanently wipe all tasks and start with a clean slate.</p>
          </div>
          <button
            onClick={() => {
              if (window.confirm('Are you sure you want to permanently delete all tasks? This cannot be undone.')) {
                onClearAllData?.();
              }
            }}
            className="px-3.5 py-2 rounded-xl bg-kairos-coral/15 hover:bg-kairos-coral/25 border border-kairos-coral/30 text-kairos-coral text-xs font-semibold transition-colors"
          >
            Reset All Tasks
          </button>
        </div>
      </div>
    </div>
  );
};
