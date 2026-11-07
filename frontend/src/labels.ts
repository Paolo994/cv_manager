import type { Criterion, Priority, Seniority, WorkMode } from './api/types';

export const SENIORITY_LABELS: Record<Seniority, string> = {
  JUNIOR: 'Junior',
  MID: 'Mid',
  SENIOR: 'Senior',
  LEAD: 'Lead',
  EXECUTIVE: 'Executive',
};

export const CRITERION_LABELS: Record<Criterion, string> = {
  CLARITY: 'Chiarezza',
  IMPACT: 'Impatto',
  STRUCTURE: 'Struttura',
  ATS_COMPATIBILITY: 'Compatibilità ATS',
  JOB_FIT: "Aderenza all'annuncio",
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  HIGH: 'Priorità alta',
  MEDIUM: 'Priorità media',
  LOW: 'Priorità bassa',
};

export const WORK_MODE_LABELS: Record<WorkMode, string> = {
  REMOTE: 'Da remoto',
  HYBRID: 'Ibrido',
  ON_SITE: 'In sede',
};

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('it-IT', { dateStyle: 'medium', timeStyle: 'short' });
}

export function formatSize(bytes: number): string {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/** Fascia di colore per un punteggio 0-100. */
export function scoreTone(score: number): 'good' | 'warning' | 'critical' {
  return score >= 75 ? 'good' : score >= 55 ? 'warning' : 'critical';
}
