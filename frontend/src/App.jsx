import React, { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import { api } from './services/api';
import Navbar from './components/common/Navbar';
import AuthCard from './components/auth/AuthCard';
import MaterialUpload from './components/materials/MaterialUpload';
import MaterialList from './components/materials/MaterialList';
import StudyHub from './components/study/StudyHub';
import { BookOpen, RefreshCw, AlertCircle } from 'lucide-react';

export default function App() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const [materials, setMaterials] = useState([]);
  const [selectedMaterial, setSelectedMaterial] = useState(null);
  const [loadingMaterials, setLoadingMaterials] = useState(false);
  const [error, setError] = useState(null);

  const fetchMaterials = async () => {
    if (!isAuthenticated) return;
    setLoadingMaterials(true);
    setError(null);
    try {
      const list = await api.materials.list();
      setMaterials(list || []);

      // If current selected material was deleted or not set, update selection
      if (list && list.length > 0) {
        if (!selectedMaterial || !list.some((m) => m.id === selectedMaterial.id)) {
          setSelectedMaterial(list[0]);
        }
      } else {
        setSelectedMaterial(null);
      }
    } catch (err) {
      setError(err.message || 'Failed to fetch study materials.');
    } finally {
      setLoadingMaterials(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchMaterials();
    } else {
      setMaterials([]);
      setSelectedMaterial(null);
    }
  }, [isAuthenticated]);

  const handleMaterialCreated = (newMaterial) => {
    fetchMaterials();
    setSelectedMaterial(newMaterial);
  };

  const handleMaterialDelete = async (id) => {
    try {
      await api.materials.delete(id);
      fetchMaterials();
    } catch (err) {
      setError(err.message || 'Failed to delete study material.');
    }
  };

  const handleMaterialUpdate = (updatedMaterial) => {
    setMaterials(prev => prev.map(m => m.id === updatedMaterial.id ? updatedMaterial : m));
    if (selectedMaterial?.id === updatedMaterial.id) {
      setSelectedMaterial(updatedMaterial);
    }
  };

  if (authLoading) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: '16px' }}>
        <span className="spinner" style={{ width: '32px', height: '32px', borderTopColor: 'var(--primary)' }} />
        <p style={{ color: 'var(--text-secondary)', fontWeight: '600' }}>Initializing Retrivox...</p>
      </div>
    );
  }

  return (
    <div className="app-container">
      <Navbar />

      <main className="main-content">
        {!isAuthenticated ? (
          <AuthCard />
        ) : (
          <div>
            {error && (
              <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <AlertCircle size={18} />
                  <span>{error}</span>
                </div>
                <button
                  className="btn-icon"
                  style={{ border: 'none', background: 'transparent' }}
                  onClick={() => setError(null)}
                >
                  ✕
                </button>
              </div>
            )}

            <div className="grid-2col">
              {/* Left Column: Material Upload & Library List */}
              <div>
                <MaterialUpload onMaterialCreated={handleMaterialCreated} />
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '10px' }}>
                  <button
                    className="btn btn-secondary"
                    style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                    onClick={fetchMaterials}
                    disabled={loadingMaterials}
                  >
                    <RefreshCw size={14} className={loadingMaterials ? 'spinner' : ''} /> Refresh Library
                  </button>
                </div>
                <MaterialList
                  materials={materials}
                  selectedId={selectedMaterial?.id}
                  onSelect={(m) => setSelectedMaterial(m)}
                  onDelete={handleMaterialDelete}
                  onUpdate={handleMaterialUpdate}
                  loading={loadingMaterials}
                />
              </div>

              {/* Right Column: Active Study Hub Workspace */}
              <div>
                {selectedMaterial ? (
                  <StudyHub
                    material={selectedMaterial}
                    onBack={() => setSelectedMaterial(null)}
                    onUpdate={handleMaterialUpdate}
                  />
                ) : (
                  <div className="card" style={{ textAlign: 'center', padding: '60px 24px' }}>
                    <BookOpen size={48} color="var(--primary)" style={{ margin: '0 auto 16px auto', opacity: 0.8 }} />
                    <h3 style={{ fontSize: '1.25rem', fontWeight: '800', marginBottom: '8px' }}>
                      Select a Study Material
                    </h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', maxWidth: '440px', margin: '0 auto' }}>
                      Choose a document from the left library or upload a new PDF to access AI summaries, generate practice quizzes, and chat with your document.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
