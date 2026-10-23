import React, { useState } from 'react';
import { api } from '../../services/api';
import { UploadCloud, FileText, CheckCircle2, AlertCircle } from 'lucide-react';

export default function MaterialUpload({ onMaterialCreated }) {
  const [title, setTitle] = useState('');
  const [subject, setSubject] = useState('');
  const [file, setFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const validateAndSetFile = (selectedFile) => {
    setError(null);
    if (!selectedFile) return;

    if (selectedFile.type !== 'application/pdf' && !selectedFile.name.toLowerCase().endsWith('.pdf')) {
      setError('Only PDF documents are supported.');
      return;
    }

    const MAX_SIZE = 5 * 1024 * 1024; // 5 MB
    if (selectedFile.size > MAX_SIZE) {
      setError('File size exceeds the 5 MB limit.');
      return;
    }

    setFile(selectedFile);
    if (!title) {
      const cleanName = selectedFile.name.replace(/\.pdf$/i, '').replace(/[-_]/g, ' ');
      setTitle(cleanName.charAt(0).toUpperCase() + cleanName.slice(1));
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!title.trim()) {
      setError('Please provide a document title.');
      return;
    }

    if (!file) {
      setError('Please select a PDF file to upload.');
      return;
    }

    setLoading(true);
    try {
      // 1. Create study material record
      const material = await api.materials.create(title.trim(), subject.trim() || 'General', file.name);

      // 2. Upload PDF text content
      await api.materials.uploadPdf(material.id, file);

      setSuccess(`"${title}" uploaded and extracted successfully!`);
      setTitle('');
      setSubject('');
      setFile(null);

      if (onMaterialCreated) {
        onMaterialCreated(material);
      }
    } catch (err) {
      setError(err.message || 'Failed to upload and process PDF.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ marginBottom: '24px' }}>
      <h3 style={{ fontSize: '1.15rem', fontWeight: '700', marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
        <UploadCloud size={20} color="var(--primary)" /> Add Study Material
      </h3>

      {error && (
        <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="alert alert-success" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle2 size={18} />
          <span>{success}</span>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
          style={{
            border: `2px dashed ${dragActive ? 'var(--primary)' : 'var(--border-subtle)'}`,
            borderRadius: 'var(--radius-md)',
            padding: '24px 16px',
            textAlign: 'center',
            backgroundColor: dragActive ? 'rgba(99, 102, 241, 0.08)' : 'var(--bg-input)',
            cursor: 'pointer',
            transition: 'var(--transition)',
            marginBottom: '16px'
          }}
          onClick={() => document.getElementById('pdf-file-input').click()}
        >
          <input
            id="pdf-file-input"
            type="file"
            accept="application/pdf,.pdf"
            style={{ display: 'none' }}
            onChange={(e) => e.target.files && validateAndSetFile(e.target.files[0])}
            disabled={loading}
          />
          {file ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
              <FileText size={24} color="var(--primary)" />
              <div style={{ textAlign: 'left' }}>
                <div style={{ fontWeight: '600', fontSize: '0.9rem' }}>{file.name}</div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                  {(file.size / (1024 * 1024)).toFixed(2)} MB • Ready to upload
                </div>
              </div>
            </div>
          ) : (
            <div>
              <UploadCloud size={32} color="var(--text-muted)" style={{ margin: '0 auto 8px auto' }} />
              <div style={{ fontWeight: '600', fontSize: '0.9rem', marginBottom: '2px' }}>
                Click to browse or drop your PDF here
              </div>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>
                PDF format only, maximum 5 MB
              </div>
            </div>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '14px' }}>
          <div>
            <label className="form-label">Material Title</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. Distributed Systems Ch. 4"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="form-label">Subject / Topic</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. Computer Science"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              disabled={loading}
            />
          </div>
        </div>

        <button
          type="submit"
          className="btn btn-primary"
          style={{ width: '100%', padding: '11px' }}
          disabled={loading || !file}
        >
          {loading ? (
            <>
              <span className="spinner" /> Uploading & Extracting Text...
            </>
          ) : (
            <>
              <UploadCloud size={18} /> Save & Process Material
            </>
          )}
        </button>
      </form>
    </div>
  );
}
