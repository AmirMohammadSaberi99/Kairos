import React from 'react';
import ReactDOM from 'react-dom/client';
import { App } from './App';
import './index.css';

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends React.Component<{ children: React.ReactNode }, ErrorBoundaryState> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 40, background: '#090B10', color: '#FF7D8B', fontFamily: 'monospace', minHeight: '100vh' }}>
          <h2 style={{ fontSize: 20, marginBottom: 12 }}>⚠️ Kairos Application Error:</h2>
          <div style={{ background: '#1A1E29', padding: 16, borderRadius: 8, color: '#F2F1F6', marginBottom: 16 }}>
            {this.state.error?.toString()}
          </div>
          <button
            onClick={() => window.location.reload()}
            style={{ padding: '8px 16px', background: '#8B7CFF', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 'bold' }}
          >
            Reload Kairos
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);
