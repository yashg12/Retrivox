import React, { useState } from 'react';
import { api } from '../../services/api';
import { FileText, Trash2, Calendar, BookOpen, Search, Filter, Edit2, Check, X } from 'lucide-react';

export default function MaterialList({ materials, selectedId, onSelect, onDelete, onUpdate, loading }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSubject, setSelectedSubject] = useState('ALL');
  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [editSubject, setEditSubject] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

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
          Upload your notes above (.pdf, .txt, .md) to unlock AI summaries, quizzes, and grounded Q&A.
        </p>
      </div>
    );
  }

  const subjects = ['ALL', ...Array.from(new Set(materials.map(m => m.subject || 'General')))];

  const filteredMaterials = materials.filter(m => {
    const matchesSearch = (m.title || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
                          (m.subject || '').toLowerCase().includes(searchQuery.toLowerCase());
    const matchesSubject = selectedSubject === 'ALL' || (m.subject || 'General') === selectedSubject;
    return matchesSearch && matchesSubject;
  });

  const startEdit = (m, e) => {
    e.stopPropagation();
    setEditingId(m.id);
    setEditTitle(m.title);
    setEditSubject(m.subject || '');
  };

  const cancelEdit = (e) => {
    e?.stopPropagation();
    setEditingId(null);
    setEditTitle('');
    setEditSubject('');
  };

  const saveEdit = async (id, e) => {
    e.stopPropagation();
    if (!editTitle.trim()) return;
    setSavingEdit(true);
    try {
      const updated = await api.materials.update(id, editTitle.trim(), editSubject.trim());
      if (onUpdate) {
        onUpdate(updated);
      }
      setEditingId(null);
    } catch (err) {
      alert(err.message || 'Failed to update material');
    } finally {
      setSavingEdit(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
        <h3 style={{ fontSize: '0.9rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Your Study Materials ({filteredMaterials.length})
        </h3>
      </div>

      {/* Search & Filter Controls */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '4px' }}>
        <div style={{ position: 'relative', flex: 1 }}>
          <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search notes by title or subject..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="form-input"
            style={{ paddingLeft: '32px', paddingRight: '12px', fontSize: '0.825rem', height: '34px' }}
          />
        </div>
        {subjects.length > 2 && (
          <select
            value={selectedSubject}
            onChange={(e) => setSelectedSubject(e.target.value)}
            className="select-input"
            style={{ width: 'auto', fontSize: '0.8rem', height: '34px', padding: '4px 10px' }}
          >
            {subjects.map(sub => (
              <option key={sub} value={sub}>{sub === 'ALL' ? 'All Subjects' : sub}</option>
            ))}
          </select>
        )}
      </div>

      {filteredMaterials.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '24px 12px', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
          No materials matching "{searchQuery}"
        </div>
      ) : (
        filteredMaterials.map((m) => {
          const isSelected = selectedId === m.id;
          const isEditing = editingId === m.id;
          const formattedDate = m.createdAt
            ? new Date(m.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
            : 'Recently';

          return (
            <div
              key={m.id}
              className="card"
              style={{
                padding: '16px',
                cursor: isEditing ? 'default' : 'pointer',
                borderLeft: isSelected ? '4px solid var(--primary)' : '1px solid var(--border-subtle)',
                backgroundColor: isSelected ? 'var(--bg-card-hover)' : 'var(--bg-card)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '12px'
              }}
              onClick={() => !isEditing && onSelect(m)}
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
                  {isEditing ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }} onClick={(e) => e.stopPropagation()}>
                      <input
                        type="text"
                        value={editTitle}
                        onChange={(e) => setEditTitle(e.target.value)}
                        placeholder="Material title"
                        className="form-input"
                        style={{ fontSize: '0.85rem', padding: '4px 8px' }}
                        autoFocus
                      />
                      <input
                        type="text"
                        value={editSubject}
                        onChange={(e) => setEditSubject(e.target.value)}
                        placeholder="Subject (e.g. Physics)"
                        className="form-input"
                        style={{ fontSize: '0.8rem', padding: '4px 8px' }}
                      />
                    </div>
                  ) : (
                    <>
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
                    </>
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
                {isEditing ? (
                  <>
                    <button
                      className="btn-icon"
                      style={{ padding: '6px', color: 'var(--success)' }}
                      title="Save changes"
                      disabled={savingEdit}
                      onClick={(e) => saveEdit(m.id, e)}
                    >
                      <Check size={16} />
                    </button>
                    <button
                      className="btn-icon"
                      style={{ padding: '6px', color: 'var(--text-muted)' }}
                      title="Cancel edit"
                      disabled={savingEdit}
                      onClick={cancelEdit}
                    >
                      <X size={16} />
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      className="btn-icon"
                      style={{ padding: '6px', color: 'var(--text-muted)' }}
                      title="Rename material"
                      onClick={(e) => startEdit(m, e)}
                    >
                      <Edit2 size={15} />
                    </button>
                    <button
                      className="btn-icon"
                      style={{ padding: '6px', color: 'var(--danger)' }}
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
                  </>
                )}
              </div>
            </div>
          );
        })
      )}
    </div>
  );
}


