import React, { useState } from 'react';
import { FileText, Copy, Check, Download, AlertCircle } from 'lucide-react';

export default function ExtractedTextTab({ material }) {
  const [copied, setCopied] = useState(false);

  const text = material?.extractedText || '';

  const handleCopy = () => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownload = () => {
    if (!text) return;
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${material.title.toLowerCase().replace(/[^a-z0-9]/g, '_')}_extracted_text.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const charCount = text.length;
  const wordCount = text.trim() ? text.trim().split(/\s+/).length : 0;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {/* Header and stats */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h3 style={{ fontSize: '1.15rem', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText size={18} color="var(--primary)" /> Extracted Document Text
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Raw text parsed from {material.fileName || 'the source document'} used for AI grounding and embeddings.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            className="btn btn-secondary"
            onClick={handleCopy}
            disabled={!text}
            style={{ fontSize: '0.85rem', padding: '6px 12px' }}
            title="Copy text to clipboard"
          >
            {copied ? (
              <>
                <Check size={14} color="var(--success)" /> Copied
              </>
            ) : (
              <>
                <Copy size={14} /> Copy Text
              </>
            )}
          </button>
          <button
            className="btn btn-secondary"
            onClick={handleDownload}
            disabled={!text}
            style={{ fontSize: '0.85rem', padding: '6px 12px' }}
            title="Download text file"
          >
            <Download size={14} /> Download (.txt)
          </button>
        </div>
      </div>

      {/* Stats metrics */}
      <div style={{ display: 'flex', gap: '16px', fontSize: '0.825rem', color: 'var(--text-muted)' }}>
        <span>Characters: <strong style={{ color: 'var(--text-primary)' }}>{charCount.toLocaleString()}</strong></span>
        <span>•</span>
        <span>Words: <strong style={{ color: 'var(--text-primary)' }}>{wordCount.toLocaleString()}</strong></span>
      </div>

      {/* Text Container */}
      {!text ? (
        <div className="card" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
          <AlertCircle size={32} style={{ margin: '0 auto 8px auto', opacity: 0.7 }} />
          <p>No extracted text available for this document.</p>
        </div>
      ) : (
        <div
          className="card"
          style={{
            padding: '20px',
            backgroundColor: 'var(--bg-elevated)',
            maxHeight: '480px',
            overflowY: 'auto',
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
            fontSize: '0.875rem',
            lineHeight: '1.7',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            color: 'var(--text-primary)'
          }}
        >
          {text}
        </div>
      )}
    </div>
  );
}
