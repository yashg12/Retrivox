import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { Sparkles, RefreshCw, AlertCircle, Clock } from 'lucide-react';

export default function SummaryTab({ material }) {
  const [summaryData, setSummaryData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!material?.id) return;
    setLoading(true);
    setError(null);

    // Check if summary already exists on material or via endpoint
    api.summaries.get(material.id)
      .then((res) => {
        if (res && res.summary) {
          setSummaryData(res);
        } else {
          setSummaryData(null);
        }
      })
      .catch((err) => {
        // 404 means no summary generated yet
        if (err.status !== 404) {
          setError(err.message);
        }
        setSummaryData(null);
      })
      .finally(() => setLoading(false));
  }, [material?.id]);

  const handleGenerate = async () => {
    setGenerating(true);
    setError(null);
    try {
      const res = await api.summaries.generate(material.id);
      setSummaryData(res);
    } catch (err) {
      setError(err.message || 'Failed to generate summary.');
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Sparkles size={20} color="var(--primary)" /> AI Executive Summary
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Grounded key concepts and takeaways extracted directly from {material.title}
          </p>
        </div>

        <button
          className="btn btn-primary"
          onClick={handleGenerate}
          disabled={generating || loading}
        >
          {generating ? (
            <>
              <span className="spinner" /> Analyzing Document...
            </>
          ) : (
            <>
              <Sparkles size={16} /> {summaryData ? 'Regenerate Summary' : 'Generate Summary'}
            </>
          )}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="card" style={{ textAlign: 'center', padding: '40px' }}>
          <span className="spinner" style={{ margin: '0 auto 12px auto' }} />
          <p style={{ color: 'var(--text-secondary)' }}>Checking for existing summary...</p>
        </div>
      ) : summaryData ? (
        <div className="card" style={{ padding: '28px', backgroundColor: 'var(--bg-elevated)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.8rem', marginBottom: '16px' }}>
            <Clock size={14} /> Generated on {summaryData.createdAt ? new Date(summaryData.createdAt).toLocaleString() : 'recently'}
          </div>

          <div
            style={{
              fontSize: '0.975rem',
              lineHeight: '1.8',
              whiteSpace: 'pre-wrap',
              color: 'var(--text-primary)'
            }}
          >
            {summaryData.summary}
          </div>
        </div>
      ) : (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <Sparkles size={40} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
          <h4 style={{ fontWeight: '700', marginBottom: '6px' }}>No summary generated yet</h4>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '440px', margin: '0 auto 18px auto' }}>
            Click "Generate Summary" to create a grounded AI synthesis of your uploaded document notes.
          </p>
          <button className="btn btn-secondary" onClick={handleGenerate} disabled={generating}>
            <Sparkles size={16} color="var(--primary)" /> Generate First Summary
          </button>
        </div>
      )}
    </div>
  );
}
