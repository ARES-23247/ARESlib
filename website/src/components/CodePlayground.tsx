import React, { useState } from 'react';
import { Check, Copy, Play, RotateCcw } from 'lucide-react';

interface CodePlaygroundProps {
  initialCode: string;
  language?: string;
  output?: string;
  title?: string;
  onRun?: (code: string) => string;
  readOnly?: boolean;
}

export default function CodePlayground({
  initialCode,
  language = 'java',
  output = '',
  title = 'Interactive Code Example',
  onRun,
  readOnly = false,
}: CodePlaygroundProps) {
  const [code, setCode] = useState(initialCode);
  const [currentOutput, setCurrentOutput] = useState(output);
  const [copied, setCopied] = useState(false);
  const [running, setRunning] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRun = async () => {
    if (onRun) {
      setRunning(true);
      // Simulate processing time
      await new Promise(resolve => setTimeout(resolve, 500));
      const result = onRun(code);
      setCurrentOutput(result);
      setRunning(false);
    }
  };

  const handleReset = () => {
    setCode(initialCode);
    setCurrentOutput(output);
  };

  return (
    <div className="code-playground">
      <div className="playground-header">
        <h4>{title}</h4>
        <div className="playground-actions">
          {!readOnly && (
            <>
              <button
                onClick={handleRun}
                disabled={running}
                className="run-button"
                title="Run code"
              >
                {running ? (
                  <span className="spinner" />
                ) : (
                  <Play size={14} />
                )}
                {running ? 'Running...' : 'Run'}
              </button>
              <button
                onClick={handleReset}
                className="reset-button"
                title="Reset code"
              >
                <RotateCcw size={14} />
                Reset
              </button>
            </>
          )}
          <button
            onClick={handleCopy}
            className="copy-button"
            title={copied ? 'Copied!' : 'Copy code'}
          >
            {copied ? (
              <Check size={14} />
            ) : (
              <Copy size={14} />
            )}
          </button>
        </div>
      </div>

      <div className="playground-content">
        <div className="code-editor">
          <pre className={`language-${language}`}>
            <code>{code}</code>
          </pre>
        </div>

        {currentOutput && (
          <div className="output-panel">
            <div className="output-header">Output</div>
            <pre className="output-content">{currentOutput}</pre>
          </div>
        )}
      </div>
    </div>
  );
}