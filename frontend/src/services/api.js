/**
 * Centralized API service for Retrivox
 * Handles baseURL configuration, JWT injection, and unified error parsing.
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const TOKEN_KEY = 'studysync_jwt_token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  remove: () => localStorage.removeItem(TOKEN_KEY)
};

class ApiError extends Error {
  constructor(status, message, data = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const headers = { ...options.headers };

  const token = tokenStorage.get();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // If body is NOT FormData, set JSON content-type
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  }

  const response = await fetch(url, {
    ...options,
    headers
  });

  // Handle 204 No Content
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  let data;
  if (contentType.includes('application/json')) {
    data = await response.json().catch(() => null);
  } else {
    data = await response.text().catch(() => '');
  }

  if (!response.ok) {
    let errorMessage = 'An unexpected error occurred';
    if (typeof data === 'string' && data.trim()) {
      errorMessage = data;
    } else if (data && typeof data === 'object') {
      errorMessage = data.message || data.error || data.detail || `Request failed with status ${response.status}`;
    } else if (response.status === 401) {
      errorMessage = 'Session expired or unauthenticated. Please sign in again.';
    } else if (response.status === 403) {
      errorMessage = 'Access denied: You do not have permission for this resource.';
    } else if (response.status === 404) {
      errorMessage = 'Requested resource not found.';
    } else if (response.status === 413) {
      errorMessage = 'File or payload too large (max 5 MB limit).';
    }

    // Trigger logout callback on 401 if needed
    if (response.status === 401 && tokenStorage.get()) {
      tokenStorage.remove();
      window.dispatchEvent(new CustomEvent('studysync:unauthorized'));
    }

    throw new ApiError(response.status, errorMessage, data);
  }

  return data;
}

export const api = {
  // Authentication
  auth: {
    register: (email, password, fullName) =>
      request('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, password, fullName })
      }),
    login: (email, password) =>
      request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      }),
    me: () => request('/api/auth/me', { method: 'GET' })
  },

  // Study Materials
  materials: {
    list: () => request('/api/study-materials', { method: 'GET' }),
    getById: (id) => request(`/api/study-materials/${id}`, { method: 'GET' }),
    create: (title, subject, fileName) =>
      request('/api/study-materials', {
        method: 'POST',
        body: JSON.stringify({ title, subject, fileName })
      }),
    update: (id, title, subject) =>
      request(`/api/study-materials/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ title, subject })
      }),
    delete: (id) => request(`/api/study-materials/${id}`, { method: 'DELETE' }),
    uploadPdf: (id, file) => {
      const formData = new FormData();
      formData.append('file', file);
      return request(`/api/study-materials/${id}/upload`, {
        method: 'POST',
        body: formData
      });
    }
  },

  // Summaries
  summaries: {
    get: (materialId) => request(`/api/study-materials/${materialId}/summary`, { method: 'GET' }),
    generate: (materialId) => request(`/api/study-materials/${materialId}/summary`, { method: 'POST' })
  },

  // Quizzes
  quizzes: {
    generate: (materialId, questionCount = 5, difficulty = 'Medium') =>
      request(`/api/study-materials/${materialId}/quizzes`, {
        method: 'POST',
        body: JSON.stringify({ questionCount, difficulty })
      }),
    getById: (quizId) => request(`/api/quizzes/${quizId}`, { method: 'GET' }),
    submit: (quizId, answers) =>
      request(`/api/quizzes/${quizId}/submit`, {
        method: 'POST',
        body: JSON.stringify({ answers })
      }),
    getAttempts: (materialId) => {
      const url = materialId ? `/api/quizzes/attempts?studyMaterialId=${materialId}` : '/api/quizzes/attempts';
      return request(url, { method: 'GET' });
    }
  },

  // Document Q&A / RAG
  rag: {
    ask: (materialId, question) =>
      request(`/api/study-materials/${materialId}/ask`, {
        method: 'POST',
        body: JSON.stringify({ question })
      })
  },

  // Flashcards
  flashcards: {
    generate: (materialId, count = 10) =>
      request(`/api/study-materials/${materialId}/flashcards?count=${count}`, {
        method: 'POST'
      })
  }
};

