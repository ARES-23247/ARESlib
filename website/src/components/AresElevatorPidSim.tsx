import React, { useEffect, useRef, useState } from 'react';

export default function AresElevatorPidSim() {
  const eCanvasRef = useRef<HTMLCanvasElement>(null);
  const gCanvasRef = useRef<HTMLCanvasElement>(null);
  
  const [kp, setKp] = useState(0.2);
  const [ki, setKi] = useState(0.0);
  const [kd, setKd] = useState(0.05);
  const [kg, setKg] = useState(0.0);
  const [setpoint, setSetpoint] = useState(0.8);

  const stateRef = useRef({ kp, ki, kd, kg, setpoint });
  useEffect(() => { stateRef.current = { kp, ki, kd, kg, setpoint }; }, [kp, ki, kd, kg, setpoint]);

  useEffect(() => {
    const eCanvas = eCanvasRef.current;
    const gCanvas = gCanvasRef.current;
    if (!eCanvas || !gCanvas) return;

    const eCtx = eCanvas.getContext('2d');
    const gCtx = gCanvas.getContext('2d');
    if (!eCtx || !gCtx) return;

    const resize = () => {
      if (gCanvas.parentElement) {
        gCanvas.width = gCanvas.parentElement.clientWidth;
      }
    };
    window.addEventListener('resize', resize);
    resize();

    let position = 0.2; 
    let velocity = 0;
    let lastError = 0;
    let integral = 0;
    
    const history: {p: number, s: number}[] = [];
    const dt = 0.02; // 50hz
    
    let intervalId: number;
    let frameId: number;

    function simulate() {
      const { kp: kP, ki: kI, kd: kD, kg: kG, setpoint: s } = stateRef.current;
      
      const error = s - position;
      const errorRate = (error - lastError) / dt;
      lastError = error;
      
      integral += error * dt;
      if (integral > 2) integral = 2;
      if (integral < -2) integral = -2;
      
      let voltage = (kP * error * 50) + (kI * integral * 20) + (kD * errorRate * 1.5);
      voltage += kG;
      
      const GRAVITY_FORCE = -0.6; // Slightly stronger gravity for FTC
      const MOTOR_FORCE = (voltage * 0.12); 
      const FORCE = MOTOR_FORCE + GRAVITY_FORCE; 
      
      velocity += FORCE * dt; 
      velocity *= 0.88; // Drag
      
      position += velocity * dt;
      
      if(position <= 0) { position = 0; velocity = 0; integral = 0; }
      if(position >= 1) { position = 1; velocity = 0; integral = 0; }
      
      history.push({p: position, s});
      if(history.length > 300) history.shift();
    }

    function draw() {
      // Draw Elevator
      eCtx!.clearRect(0,0,eCanvas.width,eCanvas.height);
      const eH = eCanvas.height;
      
      eCtx!.fillStyle = '#111';
      eCtx!.fillRect(35, 10, 10, eH-20);
      
      const trackH = eH - 40;
      const yPx = (1 - position) * trackH + 10;
      const { setpoint: s } = stateRef.current;
      const syPx = (1 - s) * trackH + 10;
      
      // Setpoint Indicator
      eCtx!.strokeStyle = '#00d0ff'; // Blue
      eCtx!.lineWidth = 2;
      eCtx!.beginPath(); eCtx!.moveTo(10, syPx+10); eCtx!.lineTo(70, syPx+10); eCtx!.stroke();
      
      // Carriage
      eCtx!.fillStyle = '#B32416'; // ARES Red
      eCtx!.fillRect(20, yPx, 40, 20);
      eCtx!.fillStyle = '#ff4d4d';
      eCtx!.fillRect(25, yPx+5, 30, 10);
      
      // Draw Graph
      gCtx!.clearRect(0,0,gCanvas.width,gCanvas.height);
      const gW = gCanvas.width;
      const gH = gCanvas.height;
      
      gCtx!.strokeStyle = '#222';
      gCtx!.lineWidth = 1;
      for(let i=0; i<=4; i++){ gCtx!.beginPath(); gCtx!.moveTo(0, i*(gH/4)); gCtx!.lineTo(gW, i*(gH/4)); gCtx!.stroke(); }
      
      if(history.length === 0) { frameId = requestAnimationFrame(draw); return; }
      
      const slice = gW / 300;
      
      // Setpoint line
      gCtx!.beginPath();
      gCtx!.strokeStyle = '#00d0ff';
      gCtx!.lineWidth = 2;
      for(let i=0; i<history.length; i++) {
          const x = i * slice;
          const y = (1 - history[i].s) * gH;
          if(i===0) gCtx!.moveTo(x,y); else gCtx!.lineTo(x,y);
      }
      gCtx!.stroke();
      
      // Position line
      gCtx!.beginPath();
      gCtx!.strokeStyle = '#CD7F32'; // Bronze
      gCtx!.lineWidth = 2.5;
      for(let i=0; i<history.length; i++) {
          const x = i * slice;
          const y = (1 - history[i].p) * gH;
          if(i===0) gCtx!.moveTo(x,y); else gCtx!.lineTo(x,y);
      }
      gCtx!.stroke();
      
      frameId = requestAnimationFrame(draw);
    }
    
    intervalId = window.setInterval(simulate, 20);
    draw();

    return () => {
      window.removeEventListener('resize', resize);
      window.clearInterval(intervalId);
      cancelAnimationFrame(frameId);
    };
  }, []);

  return (
    <div style={{ backgroundColor: '#0a0a0a', border: '1px solid #CD7F32', borderRadius: '12px', overflow: 'hidden', display: 'flex', flexDirection: 'column', color: '#e8e8e8', boxShadow: '0 10px 30px rgba(0,0,0,0.5)', margin: '30px 0' }}>
      <div style={{ padding: '20px', borderBottom: '1px solid rgba(205, 127, 50, 0.2)', display: 'flex', gap: '20px', background: 'rgba(205, 127, 50, 0.05)', flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: '130px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: '"Orbitron", sans-serif', fontSize: '10px', color: '#888', marginBottom: '8px' }}>
                <span>PROPORTIONAL (kP)</span><span>{kp.toFixed(2)}</span>
            </div>
            <input type="range" min="0" max="25" step="0.01" value={kp} onChange={e => setKp(parseFloat(e.target.value))} style={{ width: '100%', accentColor: '#CD7F32' }} />
        </div>
        <div style={{ flex: 1, minWidth: '130px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: '"Orbitron", sans-serif', fontSize: '10px', color: '#888', marginBottom: '8px' }}>
                <span>INTEGRAL (kI)</span><span>{ki.toFixed(2)}</span>
            </div>
            <input type="range" min="0" max="10" step="0.01" value={ki} onChange={e => setKi(parseFloat(e.target.value))} style={{ width: '100%', accentColor: '#CD7F32' }} />
        </div>
        <div style={{ flex: 1, minWidth: '130px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: '"Orbitron", sans-serif', fontSize: '10px', color: '#888', marginBottom: '8px' }}>
                <span>DERIVATIVE (kD)</span><span>{kd.toFixed(2)}</span>
            </div>
            <input type="range" min="0" max="10" step="0.01" value={kd} onChange={e => setKd(parseFloat(e.target.value))} style={{ width: '100%', accentColor: '#CD7F32' }} />
        </div>
        <div style={{ flex: 1, minWidth: '130px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: '"Orbitron", sans-serif', fontSize: '10px', color: '#888', marginBottom: '8px' }}>
                <span>GRAVITY (kG)</span><span>{kg.toFixed(1)}</span>
            </div>
            <input type="range" min="0" max="15" step="0.1" value={kg} onChange={e => setKg(parseFloat(e.target.value))} style={{ width: '100%', accentColor: '#B32416' }} />
        </div>
        <div style={{ display: 'flex', alignItems: 'center' }}>
            <button 
                onClick={() => setSetpoint(setpoint > 0.5 ? 0.2 : 0.8)} 
                style={{ background: '#CD7F32', color: '#fff', border: 'none', padding: '10px 18px', borderRadius: '6px', cursor: 'pointer', fontFamily: '"Orbitron", sans-serif', fontWeight: 800, fontSize: '10px' }}>
                FLIP SETPOINT
            </button>
        </div>
      </div>
      <div style={{ display: 'flex', padding: '24px', gap: '24px', height: '320px' }}>
        <div>
          <canvas ref={eCanvasRef} width="80" height="270" style={{ background: '#050505', borderRadius: '6px', border: '1px solid #222' }} />
        </div>
        <div style={{ flex: 1, position: 'relative' }}>
          <canvas ref={gCanvasRef} width="100" height="270" style={{ display: 'block', width: '100%', background: '#050505', borderRadius: '6px', border: '1px solid #222' }} />
          <div style={{ position: 'absolute', top: '12px', right: '15px', display: 'flex', gap: '20px', fontFamily: '"Orbitron", sans-serif', fontSize: '10px', fontWeight: 700 }}>
            <span style={{ color: '#00d0ff' }}>■ TARGET</span>
            <span style={{ color: '#CD7F32' }}>■ ACTUAL</span>
          </div>
        </div>
      </div>
    </div>
  );
}
