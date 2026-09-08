/**
 * Procedural harmonic chime synthesizer using Web Audio API.
 * Produces a soothing ambient bell sound on session finish without needing external audio files.
 */
class SoundEngine {
  private ctx: AudioContext | null = null;

  private initCtx() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.ctx = new AudioCtx();
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  playFocusComplete() {
    try {
      this.initCtx();
      if (!this.ctx) return;

      const now = this.ctx.currentTime;
      // Beautiful harmonic chord: D5 (587.33Hz), F#5 (739.99Hz), A5 (880Hz), D6 (1174.66Hz)
      const freqs = [587.33, 739.99, 880, 1174.66];

      freqs.forEach((freq, idx) => {
        if (!this.ctx) return;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + idx * 0.08);

        // Soft bell envelope
        gain.gain.setValueAtTime(0, now + idx * 0.08);
        gain.gain.linearRampToValueAtTime(0.12, now + idx * 0.08 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + idx * 0.08 + 1.6);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start(now + idx * 0.08);
        osc.stop(now + idx * 0.08 + 1.8);
      });
    } catch {
      // Audio context might be restricted before user gesture
    }
  }

  playBreakComplete() {
    try {
      this.initCtx();
      if (!this.ctx) return;

      const now = this.ctx.currentTime;
      // Gentle ascending 2-note chime: A4 -> E5
      [440, 659.25].forEach((freq, idx) => {
        if (!this.ctx) return;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + idx * 0.15);

        gain.gain.setValueAtTime(0, now + idx * 0.15);
        gain.gain.linearRampToValueAtTime(0.15, now + idx * 0.15 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + idx * 0.15 + 1.2);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start(now + idx * 0.15);
        osc.stop(now + idx * 0.15 + 1.4);
      });
    } catch {
      // Silently catch
    }
  }
}

export const sounds = new SoundEngine();
