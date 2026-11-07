export type Seniority = 'JUNIOR' | 'MID' | 'SENIOR' | 'LEAD' | 'EXECUTIVE';

export type Experience = {
  role: string;
  company: string;
  location: string;
  startDate: string;
  endDate: string;
  current: boolean;
  description: string;
  achievements: string[];
  technologies: string[];
};

export type Education = {
  degree: string;
  field: string;
  institution: string;
  startDate: string;
  endDate: string;
  grade: string;
};

export type LanguageSkill = { language: string; level: string };

export type CandidateProfile = {
  fullName: string;
  headline: string;
  location: string;
  summary: string;
  seniority: Seniority;
  yearsOfExperience: number;
  experiences: Experience[];
  education: Education[];
  technicalSkills: string[];
  softSkills: string[];
  languages: LanguageSkill[];
  certifications: string[];
  cvLanguage: string;
};

export type ProfileView = {
  status: 'DRAFT' | 'CONFIRMED';
  sourceCvId: string | null;
  updatedAt: string;
  confirmedAt: string | null;
  profile: CandidateProfile;
};

export type CvSummary = {
  id: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  textLength: number;
  createdAt: string;
};

export type Criterion = 'CLARITY' | 'IMPACT' | 'STRUCTURE' | 'ATS_COMPATIBILITY' | 'JOB_FIT';
export type Priority = 'HIGH' | 'MEDIUM' | 'LOW';

export type CvReview = {
  overallScore: number;
  summary: string;
  criteria: { criterion: Criterion; score: number; comment: string }[];
  strengths: string[];
  suggestions: { priority: Priority; section: string; issue: string; recommendation: string; example: string }[];
};

export type ReviewView = {
  id: string;
  cvId: string;
  runId: string | null;
  jobDescription: string | null;
  review: CvReview;
  createdAt: string;
};

export type WorkMode = 'REMOTE' | 'HYBRID' | 'ON_SITE';
export type ApplicationLanguage = 'IT' | 'EN';

export type JobPreferences = {
  targetRoles: string[];
  locations: string[];
  workModes: WorkMode[];
  minAnnualSalaryEur: number | null;
  seniority: Seniority | null;
  excludedIndustries: string[];
  applicationLanguages: ApplicationLanguage[];
  willingToRelocate: boolean;
};

export type RunStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export type RunView = {
  id: string;
  workflow: 'IMPORT_CV' | 'REVIEW_CV';
  status: RunStatus;
  error: string | null;
  output: Record<string, unknown> | null;
  inputTokens: number;
  outputTokens: number;
  createdAt: string;
  finishedAt: string | null;
  steps: {
    agent: string;
    model: string | null;
    status: RunStatus;
    error: string | null;
    inputTokens: number;
    outputTokens: number;
    startedAt: string;
    finishedAt: string | null;
  }[];
};
