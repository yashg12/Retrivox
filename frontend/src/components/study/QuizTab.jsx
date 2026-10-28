import React, { useState } from 'react';
import { api } from '../../services/api';
import { BrainCircuit, Award, CheckCircle2, XCircle, RotateCcw, AlertCircle, HelpCircle, BarChart3 } from 'lucide-react';

export default function QuizTab({ material, onViewHistoryClick }) {
  const [questionCount, setQuestionCount] = useState(5);
  const [difficulty, setDifficulty] = useState('Medium');
  const [quiz, setQuiz] = useState(null);
  const [answers, setAnswers] = useState({});
  const [result, setResult] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleGenerateQuiz = async () => {
    setGenerating(true);
    setError(null);
    setQuiz(null);
    setAnswers({});
    setResult(null);

    try {
      const newQuiz = await api.quizzes.generate(material.id, questionCount, difficulty);
      setQuiz(newQuiz);
    } catch (err) {
      setError(err.message || 'Failed to generate quiz. Ensure the document has extracted text.');
    } finally {
      setGenerating(false);
    }
  };

  const handleSelectOption = (questionId, option) => {
    if (result) return; // Prevent changing after submission
    setAnswers((prev) => ({
      ...prev,
      [questionId]: option
    }));
  };

  const handleSubmitQuiz = async () => {
    if (!quiz || !quiz.questions) return;

    // Check if at least some questions answered
    const answeredCount = Object.keys(answers).length;
    if (answeredCount < quiz.questions.length) {
      if (!window.confirm(`You answered ${answeredCount} of ${quiz.questions.length} questions. Submit anyway?`)) {
        return;
      }
    }

    setSubmitting(true);
    setError(null);

    try {
      const res = await api.quizzes.submit(quiz.quizId, answers);
      setResult(res);
    } catch (err) {
      setError(err.message || 'Failed to submit quiz.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h3 style={{ fontSize: '1.2rem', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BrainCircuit size={20} color="var(--primary)" /> AI Quiz & Practice Arena
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
            Test your comprehension with grounded multiple-choice questions
          </p>
        </div>

        {!quiz && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <select
              className="form-select"
              style={{ width: 'auto', padding: '8px 12px', fontSize: '0.85rem' }}
              value={questionCount}
              onChange={(e) => setQuestionCount(Number(e.target.value))}
              disabled={generating}
            >
              <option value={3}>3 Questions</option>
              <option value={5}>5 Questions</option>
              <option value={10}>10 Questions</option>
            </select>

            <select
              className="form-select"
              style={{ width: 'auto', padding: '8px 12px', fontSize: '0.85rem' }}
              value={difficulty}
              onChange={(e) => setDifficulty(e.target.value)}
              disabled={generating}
            >
              <option value="Easy">Easy</option>
              <option value="Medium">Medium</option>
              <option value="Hard">Hard</option>
            </select>

            <button
              className="btn btn-primary"
              onClick={handleGenerateQuiz}
              disabled={generating}
            >
              {generating ? (
                <>
                  <span className="spinner" /> Crafting Quiz...
                </>
              ) : (
                <>
                  <BrainCircuit size={16} /> Generate Quiz
                </>
              )}
            </button>
          </div>
        )}

        {quiz && (
          <button
            className="btn btn-secondary"
            onClick={() => {
              setQuiz(null);
              setResult(null);
              setAnswers({});
            }}
          >
            <RotateCcw size={16} /> New Quiz
          </button>
        )}
      </div>

      {error && (
        <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Quiz Results Score Banner */}
      {result && (
        <div
          className="card"
          style={{
            padding: '24px',
            marginBottom: '24px',
            backgroundColor: result.percentage >= 70 ? 'rgba(16, 185, 129, 0.08)' : 'rgba(99, 102, 241, 0.08)',
            borderColor: result.percentage >= 70 ? 'var(--success-border)' : 'var(--border-active)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '54px',
                height: '54px',
                borderRadius: '50%',
                backgroundColor: result.percentage >= 70 ? 'var(--success)' : 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white'
              }}
            >
              <Award size={28} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.3rem', fontWeight: '800' }}>
                Score: {result.score} / {result.totalQuestions} ({Math.round(result.percentage)}%)
              </h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                {result.percentage >= 80 ? 'Outstanding performance! You know this material well.' : result.percentage >= 60 ? 'Good work! Review the explanations below to improve.' : 'Keep studying and review the explanations below.'}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {onViewHistoryClick && (
              <button className="btn btn-secondary" onClick={onViewHistoryClick}>
                <BarChart3 size={16} /> View History
              </button>
            )}
            <button className="btn btn-primary" onClick={handleGenerateQuiz}>
              <RotateCcw size={16} /> Retake / New Quiz
            </button>
          </div>
        </div>
      )}

      {/* Quiz Question Cards */}
      {quiz && quiz.questions && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {quiz.questions.map((q, idx) => {
            const resultDetail = result?.details?.find((d) => d.questionId === q.id);

            return (
              <div key={q.id || idx} className="card" style={{ padding: '22px' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '12px', marginBottom: '14px' }}>
                  <h4 style={{ fontSize: '1rem', fontWeight: '700', lineHeight: '1.5' }}>
                    <span style={{ color: 'var(--primary)', marginRight: '6px' }}>#{idx + 1}.</span> {q.question}
                  </h4>

                  {resultDetail && (
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        padding: '4px 10px',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '0.8rem',
                        fontWeight: '700',
                        backgroundColor: resultDetail.correct ? 'var(--success-bg)' : 'var(--danger-bg)',
                        color: resultDetail.correct ? 'var(--success)' : 'var(--danger)'
                      }}
                    >
                      {resultDetail.correct ? <CheckCircle2 size={14} /> : <XCircle size={14} />}
                      {resultDetail.correct ? 'Correct' : 'Incorrect'}
                    </span>
                  )}
                </div>

                {/* Options List */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {q.options.map((opt, optIdx) => {
                    const isSelected = answers[q.id] === opt;
                    let optionStyle = {
                      padding: '12px 16px',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-subtle)',
                      backgroundColor: 'var(--bg-input)',
                      cursor: result ? 'default' : 'pointer',
                      fontSize: '0.9rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      transition: 'var(--transition)'
                    };

                    if (resultDetail) {
                      if (opt === resultDetail.correctAnswer) {
                        optionStyle.borderColor = 'var(--success)';
                        optionStyle.backgroundColor = 'var(--success-bg)';
                        optionStyle.fontWeight = '700';
                      } else if (isSelected && !resultDetail.correct) {
                        optionStyle.borderColor = 'var(--danger)';
                        optionStyle.backgroundColor = 'var(--danger-bg)';
                      }
                    } else if (isSelected) {
                      optionStyle.borderColor = 'var(--primary)';
                      optionStyle.backgroundColor = 'rgba(99, 102, 241, 0.15)';
                      optionStyle.fontWeight = '600';
                    }

                    return (
                      <div
                        key={optIdx}
                        style={optionStyle}
                        onClick={() => handleSelectOption(q.id, opt)}
                      >
                        <div
                          style={{
                            width: '24px',
                            height: '24px',
                            borderRadius: '50%',
                            border: '1px solid',
                            borderColor: isSelected ? 'var(--primary)' : 'var(--border-subtle)',
                            backgroundColor: isSelected ? 'var(--primary)' : 'transparent',
                            color: isSelected ? 'white' : 'var(--text-muted)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '0.75rem',
                            fontWeight: '700',
                            flexShrink: 0
                          }}
                        >
                          {String.fromCharCode(65 + optIdx)}
                        </div>
                        <span style={{ flex: 1 }}>{opt}</span>
                      </div>
                    );
                  })}
                </div>

                {/* Explanation review after quiz submission */}
                {resultDetail && resultDetail.explanation && (
                  <div
                    style={{
                      marginTop: '14px',
                      padding: '12px 16px',
                      backgroundColor: 'var(--bg-elevated)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.85rem',
                      lineHeight: '1.6',
                      borderLeft: '3px solid var(--secondary)'
                    }}
                  >
                    <span style={{ fontWeight: '700', color: 'var(--secondary)' }}>Explanation: </span>
                    {resultDetail.explanation}
                  </div>
                )}
              </div>
            );
          })}

          {!result && (
            <div style={{ textAlign: 'right', marginTop: '12px' }}>
              <button
                className="btn btn-primary"
                style={{ padding: '12px 28px' }}
                onClick={handleSubmitQuiz}
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <span className="spinner" /> Evaluating Answers...
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={18} /> Submit Quiz for Grading
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Empty State */}
      {!quiz && (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <BrainCircuit size={40} color="var(--text-muted)" style={{ margin: '0 auto 12px auto' }} />
          <h4 style={{ fontWeight: '700', marginBottom: '6px' }}>Ready to test yourself?</h4>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '420px', margin: '0 auto 18px auto' }}>
            Generate a set of multiple-choice questions configured to your preferred question count and difficulty level.
          </p>
          <button className="btn btn-primary" onClick={handleGenerateQuiz} disabled={generating}>
            <BrainCircuit size={16} /> Generate {questionCount} {difficulty} Questions
          </button>
        </div>
      )}
    </div>
  );
}
