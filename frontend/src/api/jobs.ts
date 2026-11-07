import { apiRequest } from './client';
import type { ContactInfo, CoverLetter, DocumentType, GeneratedDocument, JobPosting, Language, Tone } from './types';

type RunStarted = { runId: string };

// --- Annunci ---
export const listJobs = () => apiRequest<JobPosting[]>('/api/jobs');
export const getJob = (id: string) => apiRequest<JobPosting>(`/api/jobs/${id}`);
export const deleteJob = (id: string) => apiRequest<void>(`/api/jobs/${id}`, { method: 'DELETE' });

export const createJob = (description: string, url: string) =>
  apiRequest<JobPosting>('/api/jobs', { method: 'POST', json: { description, url: url.trim() || null } });

export const startAnalyzeJob = (jobId: string) =>
  apiRequest<RunStarted>('/api/workflows/analyze-job', { method: 'POST', json: { jobId } });

export type PrepareRequest = {
  jobId: string;
  documents: DocumentType[];
  language: Language | null;
  tone: Tone;
  notes: string;
};

export const startPrepareApplication = (request: PrepareRequest) =>
  apiRequest<RunStarted>('/api/workflows/prepare-application', {
    method: 'POST',
    json: { ...request, notes: request.notes.trim() || null },
  });

// --- Documenti ---
export const listDocuments = (jobId: string) => apiRequest<GeneratedDocument[]>(`/api/jobs/${jobId}/documents`);

export const reviseLetter = (documentId: string, letter: CoverLetter) =>
  apiRequest<GeneratedDocument>(`/api/documents/${documentId}/revisions`, { method: 'POST', json: letter });

export const exportUrl = (documentId: string, format: 'pdf' | 'docx') => `/api/documents/${documentId}/export?format=${format}`;

// --- Contatti (solo per l'intestazione dei documenti, mai inviati all'AI) ---
export const getContact = () => apiRequest<ContactInfo>('/api/profile/contact');
export const saveContact = (contact: ContactInfo) =>
  apiRequest<ContactInfo>('/api/profile/contact', { method: 'PUT', json: contact });
