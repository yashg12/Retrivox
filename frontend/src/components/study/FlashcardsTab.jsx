import React, { useState } from 'react';
import { api } from '../../services/api';
import {
  Layers,
  Sparkles,
  RotateCw,
  ChevronLeft,
  ChevronRight,
  Shuffle,
  CheckCircle2,
  XCircle,
  RotateCcw,
  AlertCircle
} from 'lucide-react';

export default function FlashcardsTab({ material }) {
  const [flashcards, setFlashcards] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [cardStatus, setCardStatus] = useState({}); // { [id]: 'mastered' | 'review' }
  const [cardCount, setCardCount] = useState(10);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setIsFlipped(false);
    setCurrentIndex(0);
    setCardStatus({});

    try {
      const data = await api.flashcards.generate(material.id, cardCount);
      setFlashcards(data?.flashcards || []);
    } catch (err) {
      setError(err.message || 'Failed to generate flashcards. Please check that text is uploaded.');
    } finally {
      setLoading(false);
    }
  };

  const handleFlip = () => {
    setIsFlipped(!isFlipped);
  };

  const handleNext = () => {
    if (currentIndex < flashcards.length - 1) {
      setIsFlipped(false);
      setCurrentIndex(currentIndex + 1);
    }
  };

  const handlePrev = () => {
    if (currentIndex > 0) {
      setIsFlipped(false);
      setCurrentIndex(currentIndex - 1);
    }
  };

  const handleShuffle = () => {
    if (flashcards.length <= 1) return;
    const shuffled = [...flashcards].sort(() => Math.random() - 0.5);
    setFlashcards(shuffled);
    setCurrentIndex(0);
    setIsFlipped(false);
  };

  const markStatus = (status) => {
    const currentCard = flashcards[currentIndex];
    if (!currentCard) return;
    setCardStatus((prev) => ({
      ...prev,
      [currentCard.id]: status
    }));
    // Auto advance if not last card
    if (currentIndex < flashcards.length - 1) {
      setTimeout(() => {
        handleNext();
      }, 250);
    }
  };

  const currentCard = flashcards[currentIndex];
  const masteredCount = Object.values(cardStatus).filter((s) => s === 'mastered').length;
  const reviewCount = Object.values(cardStatus).filter((s) => s === 'review').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Control bar */}
      <div
        className="card"
        style={{
          padding: '18px 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'rgba(99, 102, 241, 0.15)',
              color: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Layers size={22} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: '800', marginBottom: '2px' }}>
              Concept Flashcards
            </h3>
            <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Active recall revision grounded in this study material
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <label style={{ fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-secondary)' }}>
              Count:
            </label>
            <select
              value={cardCount}
              onChange={(e) => setCardCount(Number(e.target.value))}
              disabled={loading}
              className="select-input"
              style={{ padding: '6px 12px', fontSize: '0.85rem', width: 'auto' }}
            >
              <option value={5}>5 Cards</option>
              <option value={10}>10 Cards</option>
              <option value={15}>15 Cards</option>
              <option value={20}>20 Cards</option>
            </select>
          </div>

          <button
            className="btn btn-primary"
            onClick={handleGenerate}
            disabled={loading}
            style={{ fontSize: '0.875rem' }}
          >
            {loading ? (
              <>
                <div className="spinner" style={{ width: '16px', height: '16px' }} /> Generating...
              </>
            ) : (
              <>
                <Sparkles size={16} /> Generate Flashcards
              </>
            )}
          </button>
        </div>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Main Flashcard Arena */}
      {flashcards.length === 0 && !loading && (
        <div
          className="card"
          style={{
            padding: '50px 20px',
            textAlign: 'center',
            backgroundColor: 'var(--bg-elevated)',
            borderRadius: 'var(--radius-lg)'
          }}
        >
          <Layers size={42} style={{ color: 'var(--text-muted)', marginBottom: '14px', opacity: 0.6 }} />
          <h4 style={{ fontWeight: '700', marginBottom: '8px', fontSize: '1.1rem' }}>
            No flashcards generated yet
          </h4>
          <p
            style={{
              fontSize: '0.875rem',
              color: 'var(--text-muted)',
              maxWidth: '420px',
              margin: '0 auto 20px'
            }}
          >
            Click "Generate Flashcards" above to extract high-yield concept terms and practice active recall.
          </p>
          <button className="btn btn-primary" onClick={handleGenerate}>
            <Sparkles size={16} /> Generate Now
          </button>
        </div>
      )}

      {loading && (
        <div className="card" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <div className="spinner" style={{ margin: '0 auto 16px', width: '32px', height: '32px' }} />
          <h4 style={{ fontWeight: '700', marginBottom: '6px' }}>Synthesizing Flashcards...</h4>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Extracting core terms, formulas, and concepts from your notes.
          </p>
        </div>
      )}

      {flashcards.length > 0 && !loading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Progress & Stats */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.875rem',
              color: 'var(--text-muted)',
              flexWrap: 'wrap',
              gap: '12px'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
              <span style={{ fontWeight: '700', color: 'var(--text-primary)' }}>
                Card {currentIndex + 1} of {flashcards.length}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--success)', fontWeight: '600' }}>
                <CheckCircle2 size={15} /> {masteredCount} Mastered
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--danger)', fontWeight: '600' }}>
                <XCircle size={15} /> {reviewCount} Review
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                className="btn btn-secondary"
                onClick={handleShuffle}
                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                title="Shuffle cards"
              >
                <Shuffle size={14} /> Shuffle
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setCardStatus({});
                  setCurrentIndex(0);
                  setIsFlipped(false);
                }}
                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                title="Reset progress"
              >
                <RotateCcw size={14} /> Reset
              </button>
            </div>
          </div>

          {/* Interactive Flip Card */}
          <div
            onClick={handleFlip}
            style={{
              minHeight: '280px',
              perspective: '1000px',
              cursor: 'pointer'
            }}
          >
            <div
              className="card"
              style={{
                minHeight: '280px',
                padding: '40px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderRadius: 'var(--radius-lg)',
                boxShadow: '0 8px 30px rgba(0,0,0,0.12)',
                backgroundColor: isFlipped ? 'var(--bg-elevated)' : 'var(--card-bg)',
                border: isFlipped ? '2px solid var(--primary)' : '1px solid var(--border-color)',
                transition: 'transform 0.3s ease, background-color 0.3s ease',
                position: 'relative'
              }}
            >
              {/* Card Label */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  borderBottom: '1px solid var(--border-color)',
                  paddingBottom: '12px'
                }}
              >
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: '800',
                    letterSpacing: '0.05em',
                    textTransform: 'uppercase',
                    color: isFlipped ? 'var(--primary)' : 'var(--text-muted)'
                  }}
                >
                  {isFlipped ? 'Answer / Explanation (Back)' : 'Question / Concept (Front)'}
                </span>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontSize: '0.75rem',
                    color: 'var(--text-muted)'
                  }}
                >
                  <RotateCw size={13} /> Click card to flip
                </div>
              </div>

              {/* Card Content Body */}
              <div
                style={{
                  margin: 'auto 0',
                  padding: '24px 0',
                  textAlign: 'center'
                }}
              >
                <h3
                  style={{
                    fontSize: isFlipped ? '1.15rem' : '1.35rem',
                    fontWeight: isFlipped ? '500' : '700',
                    lineHeight: '1.6',
                    color: 'var(--text-primary)'
                  }}
                >
                  {isFlipped ? currentCard?.back : currentCard?.front}
                </h3>
              </div>

              {/* Card Footer status indicator */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  paddingTop: '12px',
                  borderTop: '1px solid var(--border-color)',
                  fontSize: '0.8rem',
                  color: 'var(--text-muted)'
                }}
              >
                {cardStatus[currentCard?.id] === 'mastered' && (
                  <span style={{ color: 'var(--success)', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <CheckCircle2 size={15} /> Marked as Mastered
                  </span>
                )}
                {cardStatus[currentCard?.id] === 'review' && (
                  <span style={{ color: 'var(--danger)', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <XCircle size={15} /> Marked for Review
                  </span>
                )}
                {!cardStatus[currentCard?.id] && (
                  <span>Rate your recall below</span>
                )}
              </div>
            </div>
          </div>

          {/* Navigation & Grading Bar */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '14px',
              padding: '12px 0'
            }}
          >
            <button
              className="btn btn-secondary"
              onClick={handlePrev}
              disabled={currentIndex === 0}
              style={{ padding: '10px 16px' }}
            >
              <ChevronLeft size={18} /> Previous
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <button
                className="btn btn-secondary"
                onClick={() => markStatus('review')}
                style={{
                  color: 'var(--danger)',
                  borderColor: 'rgba(239, 68, 68, 0.3)',
                  backgroundColor: 'rgba(239, 68, 68, 0.08)',
                  padding: '10px 18px',
                  fontWeight: '700'
                }}
              >
                <XCircle size={18} /> Still Learning
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => markStatus('mastered')}
                style={{
                  color: 'var(--success)',
                  borderColor: 'rgba(16, 185, 129, 0.3)',
                  backgroundColor: 'rgba(16, 185, 129, 0.08)',
                  padding: '10px 18px',
                  fontWeight: '700'
                }}
              >
                <CheckCircle2 size={18} /> Got It!
              </button>
            </div>

            <button
              className="btn btn-secondary"
              onClick={handleNext}
              disabled={currentIndex === flashcards.length - 1}
              style={{ padding: '10px 16px' }}
            >
              Next <ChevronRight size={18} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
