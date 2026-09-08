/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        kairos: {
          bg: 'rgb(var(--kairos-bg) / <alpha-value>)',
          surface: 'rgb(var(--kairos-surface) / <alpha-value>)',
          'surface-high': 'rgb(var(--kairos-surface-high) / <alpha-value>)',
          border: 'rgb(var(--kairos-border) / <alpha-value>)',
          accent: 'rgb(var(--kairos-accent) / <alpha-value>)',
          'accent-hover': 'rgb(var(--kairos-accent-hover) / <alpha-value>)',
          mint: 'rgb(var(--kairos-mint) / <alpha-value>)',
          'mint-hover': 'rgb(var(--kairos-mint-hover) / <alpha-value>)',
          text: 'rgb(var(--kairos-text) / <alpha-value>)',
          muted: 'rgb(var(--kairos-muted) / <alpha-value>)',
          coral: 'rgb(var(--kairos-coral) / <alpha-value>)',
          amber: 'rgb(var(--kairos-amber) / <alpha-value>)',
          slate: 'rgb(var(--kairos-slate) / <alpha-value>)',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
      },
      boxShadow: {
        'glow-accent': '0 0 20px -5px rgba(139, 124, 255, 0.35)',
        'glow-mint': '0 0 20px -5px rgba(112, 225, 176, 0.35)',
        'glow-coral': '0 0 20px -5px rgba(255, 125, 139, 0.35)',
        'glass-sm': '0 4px 20px -2px rgba(0, 0, 0, 0.05), 0 2px 6px -1px rgba(0, 0, 0, 0.03)',
        'glass-md': '0 12px 32px -4px rgba(0, 0, 0, 0.08), 0 4px 12px -2px rgba(0, 0, 0, 0.04)',
      }
    },
  },
  plugins: [],
}
