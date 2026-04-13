import React, { useState, useEffect, useRef } from 'react';

export default function AresSwerveSim() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [inputs, setInputs] = useState({ vx: 1.0, vy: 0.0, omega: 0.0 });

  const CENTER = { x: 175, y: 175 };
  const BOT_SIZE = 180;
  const SCALE = 40;
  
  const MODULES = [
    { x: -BOT_SIZE/2.5, y: -BOT_SIZE/2.5, name: "FL" },
    { x: BOT_SIZE/2.5, y: -BOT_SIZE/2.5, name: "FR" },
    { x: -BOT_SIZE/2.5, y: BOT_SIZE/2.5, name: "RL" },
    { x: BOT_SIZE/2.5, y: BOT_SIZE/2.5, name: "RR" }
  ];

  const drawArrow = (ctx, fromx, fromy, tox, toy, color, width, headSize) => {
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.beginPath();
    ctx.moveTo(fromx, fromy);
    ctx.lineTo(tox, toy);
    ctx.stroke();

    const angle = Math.atan2(toy - fromy, tox - fromx);
    ctx.beginPath();
    ctx.moveTo(tox, toy);
    ctx.lineTo(tox - headSize * Math.cos(angle - Math.PI / 6), toy - headSize * Math.sin(angle - Math.PI / 6));
    ctx.lineTo(tox - headSize * Math.cos(angle + Math.PI / 6), toy - headSize * Math.sin(angle + Math.PI / 6));
    ctx.closePath();
    ctx.fillStyle = color;
    ctx.fill();
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw Chassis
    ctx.strokeStyle = "rgba(205, 127, 50, 0.2)"; // Bronze
    ctx.lineWidth = 2;
    ctx.strokeRect(CENTER.x - BOT_SIZE/2, CENTER.y - BOT_SIZE/2, BOT_SIZE, BOT_SIZE);
    
    MODULES.forEach(mod => {
        const mX = CENTER.x + mod.x;
        const mY = CENTER.y + mod.y;
        
        // Module Body
        ctx.fillStyle = "#111";
        ctx.fillRect(mX - 12, mY - 12, 24, 24);
        ctx.strokeStyle = "rgba(255,255,255,0.1)";
        ctx.strokeRect(mX - 12, mY - 12, 24, 24);

        // Vector 1: Translation (Blue)
        const tVecX = inputs.vx * SCALE;
        const tVecY = -inputs.vy * SCALE; // Canvas coords negative Y is up
        drawArrow(ctx, mX, mY, mX + tVecX, mY + tVecY, "#4169E1", 1, 5);

        // Vector 2: Rotation (Red)
        const rx = mod.x;
        const ry = mod.y;
        const rVecX = -ry * inputs.omega * (SCALE/1.5);
        const rVecY = rx * inputs.omega * (SCALE/1.5);
        drawArrow(ctx, mX, mY, mX + rVecX, mY + rVecY, "#B32416", 1, 5);

        // Resultant Vector (Bronze)
        const resX = tVecX + rVecX;
        const resY = tVecY + rVecY;
        drawArrow(ctx, mX, mY, mX + resX, mY + resY, "#CD7F32", 3, 10);
    });

    // Legends
    ctx.font = 'bold 11px "Orbitron", sans-serif';
    ctx.fillStyle = "#4169E1"; ctx.fillText("TRANSLATION (m/s)", 20, 310);
    ctx.fillStyle = "#B32416"; ctx.fillText("ROTATION (rad/s)", 20, 330);
    ctx.fillStyle = "#CD7F32"; ctx.fillText("MODULE STATE RESULT", 20, 350);

  }, [inputs]);

  return (
    <div style={{ background: '#0a0a0a', border: '1px solid #CD7F32', borderRadius: '12px', margin: '30px 0', padding: '24px', display: 'flex', flexWrap: 'wrap', gap: '30px', boxShadow: '0 10px 30px rgba(0,0,0,0.5)' }}>
      <div style={{ flex: '1 1 350px' }}>
         <canvas ref={canvasRef} width="350" height="370" style={{ width: '100%', height: 'auto', background: '#050505', borderRadius: '8px', border: '1px solid #222' }} />
      </div>
      <div style={{ flex: '1 1 250px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <h4 style={{ margin: 0, color: '#CD7F32', fontFamily: '"Orbitron", sans-serif', fontSize: '14px' }}>KINEMATICS CONTROLLER</h4>
        
        <div>
          <label style={{ color: '#888', fontSize: '11px', display: 'block', marginBottom: '8px' }}>VX (FORWARD/BACK)</label>
          <input type="range" min="-2" max="2" step="0.1" value={inputs.vx} onChange={(e) => setInputs({...inputs, vx: parseFloat(e.target.value)})} style={{ width: '100%', accentColor: '#CD7F32' }} />
        </div>

        <div>
          <label style={{ color: '#888', fontSize: '11px', display: 'block', marginBottom: '8px' }}>VY (STRAFE)</label>
          <input type="range" min="-2" max="2" step="0.1" value={inputs.vy} onChange={(e) => setInputs({...inputs, vy: parseFloat(e.target.value)})} style={{ width: '100%', accentColor: '#4169E1' }} />
        </div>

        <div>
          <label style={{ color: '#888', fontSize: '11px', display: 'block', marginBottom: '8px' }}>OMEGA (SPIN)</label>
          <input type="range" min="-1" max="1" step="0.1" value={inputs.omega} onChange={(e) => setInputs({...inputs, omega: parseFloat(e.target.value)})} style={{ width: '100%', accentColor: '#B32416' }} />
        </div>

        <div style={{ background: 'rgba(255,255,255,0.03)', padding: '15px', borderRadius: '8px', border: '1px solid #333' }}>
            <p style={{ margin: 0, fontSize: '10px', color: '#666', lineHeight: 1.6, fontStyle: 'italic' }}>
                Module Velocity ($V_m$) is calculated as the vector sum of translate and tangent rotation components:
                <br/>
                $V_m = V_{chassis} + \omega \times r_{module}$
            </p>
        </div>
      </div>
    </div>
  );
}
