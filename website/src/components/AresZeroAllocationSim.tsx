import React, { useEffect, useRef, useState } from 'react';

export default function AresZeroAllocationSim() {
  const loopCanvasRef = useRef<HTMLCanvasElement>(null);
  const heapCanvasRef = useRef<HTMLCanvasElement>(null);
  
  const [mode, setMode] = useState<"STD" | "ARES">("ARES");
  const [loopTxt, setLoopTxt] = useState<{ text: string, color: string }>({ text: "20.0ms", color: "#00d0ff" });

  const loopTimesRef = useRef<{ ts: number, gc: boolean }[]>([]);
  const heapFillRef = useRef(0);
  const modeRef = useRef<"STD" | "ARES">("ARES");

  useEffect(() => {
    modeRef.current = mode;
  }, [mode]);

  useEffect(() => {
    const lCanvas = loopCanvasRef.current;
    const hCanvas = heapCanvasRef.current;
    if (!lCanvas || !hCanvas) return;

    const lCtx = lCanvas.getContext('2d');
    const hCtx = hCanvas.getContext('2d');
    if (!lCtx || !hCtx) return;

    let timeoutId: NodeJS.Timeout;

    const resize = () => {
        if (lCanvas.parentElement) {
            lCanvas.width = lCanvas.parentElement.clientWidth;
            lCanvas.height = lCanvas.parentElement.clientHeight;
        }
        if (hCanvas.parentElement) {
            hCanvas.width = hCanvas.parentElement.clientWidth;
            hCanvas.height = hCanvas.parentElement.clientHeight - 20;
        }
    };
    
    window.addEventListener('resize', resize);
    resize(); // initial sizing

    const draw = () => {
        const loopTimes = loopTimesRef.current;
        const heapFill = heapFillRef.current;

        lCtx.clearRect(0, 0, lCanvas.width, lCanvas.height);
        const lw = lCanvas.width, lh = lCanvas.height;
        const targetY = lh - (20 / 80) * lh;
        
        lCtx.strokeStyle = 'rgba(205, 127, 50, 0.3)'; // Bronze
        lCtx.setLineDash([5, 5]);
        lCtx.beginPath(); lCtx.moveTo(0, targetY); lCtx.lineTo(lw, targetY); lCtx.stroke();
        lCtx.setLineDash([]);
        
        const slice = lw / 250;
        lCtx.beginPath(); lCtx.moveTo(0, lh);
        for (let i = 0; i < loopTimes.length; i++) {
            let y = lh - (loopTimes[i].ts / 80) * lh;
            lCtx.lineTo(i * slice, Math.max(0, y));
        }
        lCtx.lineTo((loopTimes.length > 0 ? loopTimes.length - 1 : 0) * slice, lh);
        lCtx.fillStyle = 'rgba(255,255,255,0.05)';
        lCtx.fill();
        
        lCtx.beginPath();
        for (let i = 0; i < loopTimes.length; i++) {
            let y = lh - (loopTimes[i].ts / 80) * lh;
            if (i === 0) lCtx.moveTo(i * slice, Math.max(0, y));
            else lCtx.lineTo(i * slice, Math.max(0, y));
        }
        lCtx.strokeStyle = '#CD7F32'; // Bronze
        lCtx.lineWidth = 2; lCtx.stroke();
        
        for (let i = 0; i < loopTimes.length; i++) {
            if (loopTimes[i].gc) {
                let y = lh - (loopTimes[i].ts / 80) * lh;
                lCtx.beginPath(); lCtx.arc(i * slice, Math.max(0, y), 5, 0, Math.PI * 2);
                lCtx.fillStyle = '#ff4d4d';
                lCtx.fill();
                lCtx.strokeStyle = 'white';
                lCtx.lineWidth = 1;
                lCtx.stroke();
            }
        }
        
        hCtx.clearRect(0, 0, hCanvas.width, hCanvas.height);
        const hw = hCanvas.width, hh = hCanvas.height;
        const fillH = heapFill * hh;
        
        hCtx.fillStyle = '#111'; hCtx.fillRect(0, 0, hw, hh);
        hCtx.fillStyle = heapFill > 0.9 ? '#ff4d4d' : '#CD7F32';
        hCtx.fillRect(0, hh - fillH, hw, fillH);
        
        if (heapFill > 0.9) {
            hCtx.fillStyle = '#fff'; hCtx.font = 'bold 12px "Orbitron", sans-serif'; 
            hCtx.fillText("GC!", hw / 2 - 12, 20); 
        }
    };

    const simulate = () => {
        const currentMode = modeRef.current;
        let loopMs = 20.0 + (Math.random() * 0.8 - 0.4);
        let isGC = false;
        
        if (currentMode === "STD") {
            heapFillRef.current += 0.012; // Higher allocation rate for FTC Control Hub
            if (heapFillRef.current >= 1.0) { 
                heapFillRef.current = 0; 
                isGC = true; 
                loopMs = 55.0 + Math.random() * 25.0; // Typical FTC GC spike
            }
        } else { 
            heapFillRef.current = 0.15; 
        }
        
        loopTimesRef.current.push({ ts: loopMs, gc: isGC });
        if (loopTimesRef.current.length > 250) loopTimesRef.current.shift();
        
        draw();
        
        if (isGC) {
            setLoopTxt({ text: loopMs.toFixed(1) + "ms (GC LATCH)", color: "#ff4d4d" });
        } else {
            setLoopTxt({ text: loopMs.toFixed(1) + "ms", color: "#00d0ff" });
        }
        
        if (document.hasFocus() || window.document.visibilityState === 'visible') {
             timeoutId = setTimeout(simulate, loopMs);
        } else {
             timeoutId = setTimeout(simulate, Math.max(100, loopMs)); 
        }
    };

    simulate();

    return () => {
        clearTimeout(timeoutId);
        window.removeEventListener('resize', resize);
    };
  }, []);

  return (
    <div style={{ background: '#0a0a0a', border: '1px solid #CD7F32', borderRadius: '12px', margin: '30px 0', display: 'flex', flexDirection: 'column', overflow: 'hidden', boxShadow: '0 10px 30px rgba(0,0,0,0.5)' }}>
      <div style={{ display: 'flex', gap: '20px', padding: '16px 20px', background: 'rgba(205, 127, 50, 0.05)', borderBottom: '1px solid rgba(205, 127, 50, 0.2)', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', gap: '10px', width: '100%' }}>
          <button 
            onClick={() => setMode("STD")}
            style={{ flex: 1, background: mode === "STD" ? '#CD7F32' : 'transparent', color: mode === "STD" ? 'white' : '#888', border: '1px solid #CD7F32', padding: '12px', borderRadius: '6px', cursor: 'pointer', fontFamily: '"Orbitron", sans-serif', fontWeight: 700, transition: '0.2s', fontSize: '0.8rem' }}>
            STANDARD FTC (Allocating)
          </button>
          <button 
            onClick={() => setMode("ARES")}
            style={{ flex: 1, background: mode === "ARES" ? '#CD7F32' : 'transparent', color: mode === "ARES" ? 'white' : '#888', border: '1px solid #CD7F32', padding: '12px', borderRadius: '6px', cursor: 'pointer', fontFamily: '"Orbitron", sans-serif', fontWeight: 700, transition: '0.2s', fontSize: '0.8rem' }}>
            ARESLIB (Zero-Allocation)
          </button>
        </div>
      </div>
      <div style={{ display: 'flex', padding: '20px', gap: '20px', alignItems: 'stretch', height: '350px' }}>
        <div style={{ flex: 1, position: 'relative' }}>
          <canvas ref={loopCanvasRef} style={{ width: '100%', height: '100%', display: 'block', border: '1px solid #222', background: '#050505', borderRadius: '6px' }}></canvas>
          <div style={{ position: 'absolute', top: '10px', left: '15px', fontFamily: '"JetBrains Mono", monospace', fontSize: '11px', color: '#888' }}>
            LOOP_CYCLE: <span style={{ color: loopTxt.color }}>{loopTxt.text}</span>
          </div>
        </div>
        <div style={{ flex: '0 0 80px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
          <canvas ref={heapCanvasRef} style={{ flex: 1, display: 'block', border: '1px solid #222', background: '#050505', borderRadius: '6px' }}></canvas>
          <div style={{ textAlign: 'center', fontFamily: '"Orbitron", sans-serif', fontSize: '9px', color: '#888', marginTop: '8px' }}>VM HEAP</div>
        </div>
      </div>
    </div>
  );
}
