import React from 'react';
import { FileText, Trash2, Calendar, BookOpen } from 'lucide-react';

export default function MaterialList({ materials, selectedId, onSelect, onDelete, loading }) {
  if (loading && (!materials || materials.length === 0)) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '36px' }}>
        <span className="spinner" style={{ margin: '0 auto 12px auto' }} />
        <p style={{ color: 'var(--text-secondary)' }}>Loading your study library...</p>
      </div>
    );
  }

  if (!materials || materials.length === 0) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '40px 20px' }}>
        <BookOpen size={40} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
        <h4 style={{ fontWeight: '700', marginBottom: '6px' }}>Your library is empty</h4>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Upload your first PDF above to unlock AI summaries, quizzes, and grounded Q&A.
        </p>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <h3 style={{ fontSize: '1rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '4px' }}>
        Your Study Materials ({materials.length})
      </h3>

      {materials.map((m) => {
        const isSelected = selectedId === m.id;
        const formattedDate = m.createdAt
          ? new Date(m.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
          : 'Recently';

        return (
          <div
            key={m.id}
            className="card"
            style={{
              padding: '16px',
              cursor: 'pointer',
              borderLeft: isSelected ? '4px solid var(--primary)' : '1px solid var(--border-subtle)',
              backgroundColor: isSelected ? 'var(--bg-card-hover)' : 'var(--bg-card)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: '12px'
            }}
            onClick={() => onSelect(m)}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', flex: 1, minWidth: 0 }}>
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: isSelected ? 'rgba(99, 102, 241, 0.2)' : 'var(--bg-elevated)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: isSelected ? 'var(--primary)' : 'var(--text-secondary)',
                  flexShrink: 0
                }}
              >
                <FileText size={18} />
              </div>

              <div style={{ minWidth: 0, flex: 1 }}>
                <h4 style={{ fontSize: '0.95rem', fontWeight: '700', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', marginBottom: '2px' }}>
                  {m.title}
                </h4>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  <span style={{ backgroundColor: 'var(--bg-elevated)', padding: '2px 8px', borderRadius: 'var(--radius-sm)', fontWeight: '600' }}>
                    {m.subject || 'General'}
                  </span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Calendar size={12} /> {formattedDate}
                  </span>
                </div>
              </div>
            </div>

            <button
              className="btn-icon"
              style={{ padding: '6px', color: 'var(--danger)', flexShrink: 0 }}
              title="Delete study material"
              onClick={(e) => {
                e.stopPropagation();
                if (window.confirm(`Delete "${m.title}" and its associated quiz/embedding data?`)) {
                  onDelete(m.id);
                }
              }}
            >
              <Trash2 size={16} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
