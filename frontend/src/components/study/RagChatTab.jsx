import React, { useState } from 'react';
import { api } from '../../services/api';
import { MessageSquare, Send, Quote, AlertCircle, Sparkles, BookOpen } from 'lucide-react';

export default function RagChatTab({ material }) {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: `Hello! I am your study assistant grounded strictly in "${material.title}". Ask me any question and I will answer using the verified text extracted from your document with cited sources.`
    }
  ]);
  const [question, setQuestion] = useState('');
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState(null);

  const handleSend = async (e) => {
    e.preventDefault();
    const query = question.trim();
    if (!query || asking) return;

    setError(null);
    setQuestion('');

    const newMessages = [...messages, { role: 'user', content: query }];
    setMessages(newMessages);
    setAsking(true);

    try {
      const response = await api.rag.ask(material.id, query);
      setMessages([
        ...newMessages,
        {
          role: 'assistant',
          content: response.answer,
          sources: response.sources || []
        }
      ]);
    } catch (err) {
      setError(err.message || 'Failed to retrieve answer from document.');
      setMessages([
        ...newMessages,
        {
          role: 'assistant',
          isError: true,
          content: `Error: ${err.message || 'Unable to retrieve answer.'}`
        }
      ]);
    } finally {
      setAsking(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '620px' }}>
      <div style={{ marginBottom: '14px' }}>
        <h3 style={{ fontSize: '1.2rem', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MessageSquare size={20} color="var(--primary)" /> Document-Grounded Q&A (RAG)
        </h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
          Ask questions answered by semantic search over chunked text in {material.title}
        </p>
      </div>

      {error && (
        <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Chat Messages Log */}
      <div
        className="card"
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
          marginBottom: '16px',
          backgroundColor: 'var(--bg-elevated)'
        }}
      >
        {messages.map((msg, idx) => {
          const isUser = msg.role === 'user';
          return (
            <div
              key={idx}
              style={{
                alignSelf: isUser ? 'flex-end' : 'flex-start',
                maxWidth: '82%',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px'
              }}
            >
              <div
                style={{
                  padding: '14px 18px',
                  borderRadius: 'var(--radius-lg)',
                  borderTopRightRadius: isUser ? '4px' : 'var(--radius-lg)',
                  borderTopLeftRadius: !isUser ? '4px' : 'var(--radius-lg)',
                  backgroundColor: isUser ? 'var(--primary)' : 'var(--bg-card)',
                  color: isUser ? 'white' : 'var(--text-primary)',
                  fontSize: '0.925rem',
                  lineHeight: '1.6',
                  border: isUser ? 'none' : '1px solid var(--border-subtle)',
                  boxShadow: 'var(--shadow-sm)',
                  whiteSpace: 'pre-wrap'
                }}
              >
                {msg.content}
              </div>

              {/* Retrieved Source Snippets */}
              {!isUser && msg.sources && msg.sources.length > 0 && (
                <div
                  style={{
                    backgroundColor: 'rgba(6, 182, 212, 0.08)',
                    border: '1px solid rgba(6, 182, 212, 0.25)',
                    borderRadius: 'var(--radius-md)',
                    padding: '12px 14px',
                    fontSize: '0.8rem',
                    color: 'var(--text-secondary)'
                  }}
                >
                  <div style={{ fontWeight: '700', color: 'var(--secondary)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                    <BookOpen size={14} /> Retrieved Evidence Sources ({msg.sources.length})
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {msg.sources.map((src, sIdx) => (
                      <div key={sIdx} style={{ paddingLeft: '8px', borderLeft: '2px solid var(--secondary)' }}>
                        <div style={{ fontStyle: 'italic', color: 'var(--text-primary)', marginBottom: '2px' }}>
                          "{src.snippet}"
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          Chunk #{src.chunkIndex} • Similarity: {(src.similarity * 100).toFixed(1)}%
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {asking && (
          <div style={{ alignSelf: 'flex-start', display: 'flex', alignItems: 'center', gap: '8px', padding: '12px 16px', backgroundColor: 'var(--bg-card)', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            <span className="spinner" style={{ width: '16px', height: '16px', borderTopColor: 'var(--primary)' }} /> Searching document chunks and synthesizing answer...
          </div>
        )}
      </div>

      {/* Input Field */}
      <form onSubmit={handleSend} style={{ display: 'flex', gap: '10px' }}>
        <input
          type="text"
          className="form-input"
          placeholder={`Ask anything about "${material.title}"...`}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          disabled={asking}
          style={{ flex: 1, padding: '14px 18px' }}
        />
        <button
          type="submit"
          className="btn btn-primary"
          disabled={asking || !question.trim()}
          style={{ padding: '0 24px' }}
        >
          {asking ? <span className="spinner" /> : <Send size={18} />}
        </button>
      </form>
    </div>
  );
}
