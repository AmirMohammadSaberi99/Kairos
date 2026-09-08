import { DailyQuote, Task } from '../types';

export const INITIAL_QUOTES: DailyQuote[] = [
  {
    text: "If I have seen further, it is by standing on the shoulders of giants.",
    source: "Isaac Newton · letter to Robert Hooke, 1675"
  },
  {
    text: "Chance favors only the prepared mind.",
    source: "Louis Pasteur · University of Lille lecture, 1854"
  },
  {
    text: "Nothing is too wonderful to be true, if it be consistent with the laws of nature.",
    source: "Michael Faraday · laboratory diary, 1849"
  },
  {
    text: "A person who dares to waste one hour of time has not discovered the value of life.",
    source: "Charles Darwin · Life and Letters, 1887"
  },
  {
    text: "The first principle is that you must not fool yourself—and you are the easiest person to fool.",
    source: "Richard Feynman · Caltech address, 1974"
  },
  {
    text: "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
    source: "Will Durant · The Story of Philosophy, 1926"
  }
];

export function getTodayQuote(): DailyQuote {
  const day = Math.floor(Date.now() / (1000 * 60 * 60 * 24));
  return INITIAL_QUOTES[day % INITIAL_QUOTES.length];
}

export const INITIAL_TASKS: Task[] = [];

