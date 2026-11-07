import { ApiError, apiRequest } from './client';
import type { CandidateProfile, CvSummary, JobPreferences, ProfileView, ReviewView } from './types';

// --- CV ---
export const listCvs = () => apiRequest<CvSummary[]>('/api/cvs');

export function uploadCv(file: File): Promise<CvSummary> {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest<CvSummary>('/api/cvs', { method: 'POST', formData });
}

export const deleteCv = (id: string) => apiRequest<void>(`/api/cvs/${id}`, { method: 'DELETE' });
export const cvFileUrl = (id: string) => `/api/cvs/${id}/file`;
export const listReviews = (cvId: string) => apiRequest<ReviewView[]>(`/api/cvs/${cvId}/reviews`);

// --- Workflow (agenti) ---
type RunStarted = { runId: string };

export const startImportCv = (cvId: string) =>
  apiRequest<RunStarted>('/api/workflows/import-cv', { method: 'POST', json: { cvId } });

export const startReviewCv = (cvId: string, jobDescription: string) =>
  apiRequest<RunStarted>('/api/workflows/review-cv', {
    method: 'POST',
    json: { cvId, jobDescription: jobDescription.trim() || null },
  });

// --- Profilo ---
/** null finché l'utente non ha importato un CV. */
export async function fetchProfile(): Promise<ProfileView | null> {
  try {
    return await apiRequest<ProfileView>('/api/profile');
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

export const saveProfile = (profile: CandidateProfile, confirm: boolean) =>
  apiRequest<ProfileView>('/api/profile', { method: 'PUT', json: { profile, confirm } });

// --- Preferenze ---
export const fetchPreferences = () => apiRequest<JobPreferences>('/api/preferences');
export const savePreferences = (preferences: JobPreferences) =>
  apiRequest<JobPreferences>('/api/preferences', { method: 'PUT', json: preferences });
