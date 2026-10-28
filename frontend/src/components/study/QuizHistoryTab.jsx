import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { Award, Calendar, CheckCircle2, TrendingUp, RotateCcw, AlertCircle, BarChart3, Clock } from 'lucide-react';

export default function QuizHistoryTab({ material, onTakeQuizClick }) {
  const [attempts, setAttempts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAttempts = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.quizzes.getAttempts(material?.id);
      setAttempts(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load quiz attempts history.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAttempts();
  }, [material?.id]);

  // Derived metrics
  const totalAttempts = attempts.length;
  const averageScore = totalAttempts > 0
    ? (attempts.reduce((sum, a) => sum + (a.percentage || 0), 0) / totalAttempts).toFixed(1)
    : 0;
  const bestScore = totalAttempts > 0
    ? Math.max(...attempts.map(a => a.percentage || 0)).toFixed(1)
    : 0;

  const formatDate = (isoString) => {
    if (!isoString) return 'Just now';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  const getScoreBadgeClass = (pct) => {
    if (pct >= 80) return 'badge-success';
    if (pct >= 50) return 'badge-warning';
    return 'badge-danger';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Overview Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px' }}>
        <div className="card" style={{ padding: '16px', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'rgba(99, 102, 241, 0.15)',
            color: 'var(--primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <BarChart3 size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: '600' }}>Total Attempts</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800' }}>{totalAttempts}</div>
          </div>
        </div>

        <div className="card" style={{ padding: '16px', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'rgba(16, 185, 129, 0.15)',
            color: 'var(--success)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Award size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: '600' }}>Best Score</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800', color: totalAttempts > 0 && bestScore >= 80 ? 'var(--success)' : 'inherit' }}>
              {totalAttempts > 0 ? `${bestScore}%` : '—'}
            </div>
          </div>
        </div>

        <div className="card" style={{ padding: '16px', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'rgba(245, 158, 11, 0.15)',
            color: 'var(--warning)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <TrendingUp size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: '600' }}>Average Score</div>
            <div style={{ fontSize: '1.4rem', fontWeight: '800' }}>
              {totalAttempts > 0 ? `${averageScore}%` : '—'}
            </div>
          </div>
        </div>
      </div>

      {/* Attempts List */}
      <div className="card" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: '800', marginBottom: '4px' }}>Quiz Attempt History</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Past performance and score progression for this study material
            </p>
          </div>
          <button
            className="btn btn-secondary"
            onClick={fetchAttempts}
            disabled={loading}
            style={{ fontSize: '0.85rem', padding: '8px 12px' }}
          >
            <RotateCcw size={15} /> Refresh
          </button>
        </div>

        {error && (
          <div className="alert alert-error" style={{ marginBottom: '16px' }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
            <div className="spinner" style={{ margin: '0 auto 12px' }} />
            <p>Loading attempt history...</p>
          </div>
        ) : attempts.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: '48px 20px',
            backgroundColor: 'var(--bg-elevated)',
            borderRadius: 'var(--radius-md)',
            border: '1px dashed var(--border-color)'
          }}>
            <Award size={36} style={{ color: 'var(--text-muted)', marginBottom: '12px', opacity: 0.7 }} />
            <h4 style={{ fontWeight: '700', marginBottom: '6px' }}>No quiz attempts yet</h4>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', maxWidth: '380px', margin: '0 auto 18px' }}>
              Test your knowledge by generating an AI mock quiz from this material!
            </p>
            {onTakeQuizClick && (
              <button className="btn btn-primary" onClick={onTakeQuizClick} style={{ fontSize: '0.9rem' }}>
                Take a Quiz Now
              </button>
            )}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {attempts.map((attempt, index) => {
              const pct = attempt.percentage != null ? attempt.percentage : 0;
              return (
                <div
                  key={attempt.id || index}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '16px',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--bg-elevated)',
                    border: '1px solid var(--border-color)',
                    flexWrap: 'wrap',
                    gap: '12px'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <div style={{
                      width: '44px',
                      height: '44px',
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: '800',
                      fontSize: '0.9rem',
                      backgroundColor: pct >= 80 ? 'rgba(16, 185, 129, 0.15)' : pct >= 50 ? 'rgba(245, 158, 11, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                      color: pct >= 80 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)'
                    }}>
                      {pct}%
                    </div>
                    <div>
                      <div style={{ fontWeight: '700', fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span>Score: {attempt.score} / {attempt.totalQuestions}</span>
                        <span className={`badge ${getScoreBadgeClass(pct)}`} style={{ fontSize: '0.75rem', padding: '2px 8px' }}>
                          {pct >= 80 ? 'Mastered' : pct >= 50 ? 'Good Progress' : 'Needs Review'}
                        </span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Clock size={13} /> {formatDate(attempt.completedAt)}
                        </span>
                        <span>•</span>
                        <span>Quiz Ref: {attempt.quizId?.substring(0, 8)}...</span>
                      </div>
                    </div>
                  </div>

                  <div style={{ minWidth: '120px' }}>
                    <div style={{
                      height: '8px',
                      width: '100%',
                      backgroundColor: 'rgba(0,0,0,0.1)',
                      borderRadius: '4px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        height: '100%',
                        width: `${Math.min(100, Math.max(0, pct))}%`,
                        backgroundColor: pct >= 80 ? 'var(--success)' : pct >= 50 ? 'var(--warning)' : 'var(--danger)',
                        borderRadius: '4px',
                        transition: 'width 0.4s ease'
                      }} />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
