import React, { useState } from 'react';
import SummaryTab from './SummaryTab';
import QuizTab from './QuizTab';
import RagChatTab from './RagChatTab';
import QuizHistoryTab from './QuizHistoryTab';
import FlashcardsTab from './FlashcardsTab';
import ExtractedTextTab from './ExtractedTextTab';
import { Sparkles, BrainCircuit, MessageSquare, FileText, ArrowLeft, BarChart3, Layers, AlignLeft } from 'lucide-react';

export default function StudyHub({ material, onBack, onUpdate }) {
  const [activeTab, setActiveTab] = useState('summary'); // 'summary' | 'flashcards' | 'quiz' | 'history' | 'rag' | 'text'

  if (!material) return null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header with Title and Subject */}
      <div className="card" style={{ padding: '20px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          {onBack && (
            <button
              className="btn btn-secondary"
              onClick={onBack}
              style={{ padding: '8px 12px', fontSize: '0.85rem' }}
              title="Return to library"
            >
              <ArrowLeft size={16} /> Back
            </button>
          )}

          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'rgba(99, 102, 241, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)'
            }}
          >
            <FileText size={22} />
          </div>

          <div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: '800', marginBottom: '2px' }}>
              {material.title}
            </h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              <span style={{ backgroundColor: 'var(--bg-elevated)', padding: '2px 8px', borderRadius: 'var(--radius-sm)', fontWeight: '600' }}>
                {material.subject || 'General'}
              </span>
              <span>•</span>
              <span>File: {material.fileName}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Workspace Tabs Navigation */}
      <div className="card" style={{ padding: '24px' }}>
        <div className="tabs-header">
          <button
            className={`tab-btn ${activeTab === 'summary' ? 'active' : ''}`}
            onClick={() => setActiveTab('summary')}
          >
            <Sparkles size={18} /> Executive Summary
          </button>
          <button
            className={`tab-btn ${activeTab === 'flashcards' ? 'active' : ''}`}
            onClick={() => setActiveTab('flashcards')}
          >
            <Layers size={18} /> Flashcards
          </button>
          <button
            className={`tab-btn ${activeTab === 'quiz' ? 'active' : ''}`}
            onClick={() => setActiveTab('quiz')}
          >
            <BrainCircuit size={18} /> AI Quiz Arena
          </button>
          <button
            className={`tab-btn ${activeTab === 'history' ? 'active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            <BarChart3 size={18} /> Quiz History
          </button>
          <button
            className={`tab-btn ${activeTab === 'rag' ? 'active' : ''}`}
            onClick={() => setActiveTab('rag')}
          >
            <MessageSquare size={18} /> Document Q&A (RAG)
          </button>
          <button
            className={`tab-btn ${activeTab === 'text' ? 'active' : ''}`}
            onClick={() => setActiveTab('text')}
          >
            <AlignLeft size={18} /> Source Text
          </button>
        </div>

        {/* Tab Content Panes */}
        <div>
          {activeTab === 'summary' && <SummaryTab material={material} />}
          {activeTab === 'flashcards' && <FlashcardsTab material={material} />}
          {activeTab === 'quiz' && (
            <QuizTab
              material={material}
              onViewHistoryClick={() => setActiveTab('history')}
            />
          )}
          {activeTab === 'history' && (
            <QuizHistoryTab
              material={material}
              onTakeQuizClick={() => setActiveTab('quiz')}
            />
          )}
          {activeTab === 'rag' && <RagChatTab material={material} />}
          {activeTab === 'text' && <ExtractedTextTab material={material} />}
        </div>
      </div>
    </div>
  );
}



