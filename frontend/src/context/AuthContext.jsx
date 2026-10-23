import React, { createContext, useContext, useState, useEffect } from 'react';
import { api, tokenStorage } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Check if token exists and validate with /api/auth/me
    const token = tokenStorage.get();
    if (!token) {
      setLoading(false);
      return;
    }

    api.auth.me()
      .then((userData) => {
        setUser(userData);
      })
      .catch(() => {
        // Stale or invalid token
        tokenStorage.remove();
        setUser(null);
      })
      .finally(() => {
        setLoading(false);
      });

    // Listen for 401 unauthorized events dispatched from api.js
    const handleUnauthorized = () => {
      setUser(null);
    };

    window.addEventListener('studysync:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('studysync:unauthorized', handleUnauthorized);
    };
  }, []);

  const login = async (email, password) => {
    setError(null);
    try {
      const response = await api.auth.login(email, password);
      tokenStorage.set(response.token);
      setUser(response.user);
      return response.user;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const register = async (email, password, fullName) => {
    setError(null);
    try {
      const response = await api.auth.register(email, password, fullName);
      tokenStorage.set(response.token);
      setUser(response.user);
      return response.user;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const logout = () => {
    tokenStorage.remove();
    setUser(null);
  };

  const value = {
    user,
    token: tokenStorage.get(),
    isAuthenticated: !!user,
    loading,
    error,
    login,
    register,
    logout
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
