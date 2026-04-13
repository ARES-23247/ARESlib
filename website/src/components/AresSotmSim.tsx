import React, { useState, useEffect, useRef } from 'react';

export default function AresSotmSim() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [velocity, setVelocity] = useState({ x: 1.5, y: -0.5 });
  
  const HUB = { x: 250, y: 70 };
  const ROBOT = { x: 250, y: 300 };
  const SCALE = 60;

  const drawArrow = (ctx, fromx, fromy, tox, toy, color, width) => {
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.beginPath();
    ctx.moveTo(fromx, fromy);
    ctx.lineTo(tox, toy);
    ctx.stroke();

    const headSize = 10;
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
    
    // Draw Center Stage / Goal
    ctx.fillStyle = "#CD7F32";
    ctx.beginPath(); ctx.arc(HUB.x, HUB.y, 20, 0, Math.PI*2); ctx.fill();
    ctx.strokeStyle = "white"; ctx.lineWidth = 2; ctx.stroke();
    
    // Draw Robot
    ctx.fillStyle = "#111";
    ctx.fillRect(ROBOT.x - 25, ROBOT.y - 25, 50, 50);
    ctx.strokeStyle = "#B32416"; ctx.lineWidth = 3;
    ctx.strokeRect(ROBOT.x - 25, ROBOT.y - 25, 50, 50);

    const shotVel = 6.0; // m/s
    const vRobotX = velocity.x;
    const vRobotY = velocity.y;
    
    const dx = HUB.x - ROBOT.x;
    const dy = HUB.y - ROBOT.y;
    const dist = Math.sqrt(dx*dx + dy*dy) / SCALE;

    // Iterative solver for TOF
    let tof = dist / shotVel;
    for(let i=0; i<3; i++) {
        const virtualX = HUB.x - (vRobotX * tof * SCALE);
        const virtualY = HUB.y - (vRobotY * tof * SCALE);
        const virtualDist = Math.sqrt(Math.pow(virtualX - ROBOT.x, 2) + Math.pow(virtualY - ROBOT.y, 2)) / SCALE;
        tof = virtualDist / shotVel;
    }

    const aimX = HUB.x - vRobotX * tof * SCALE;
    const aimY = HUB.y - vRobotY * tof * SCALE;

    // Virtual Target Line
    ctx.setLineDash([5, 5]);
    ctx.strokeStyle = "rgba(255, 255, 255, 0.2)";
    ctx.beginPath(); ctx.moveTo(HUB.x, HUB.y); ctx.lineTo(aimX, aimY); ctx.stroke();
    ctx.setLineDash([]);

    // Robot Velocity (Blue)
    drawArrow(ctx, ROBOT.x, ROBOT.y, ROBOT.x + vRobotX * SCALE, ROBOT.y + vRobotY * SCALE, "#4169E1", 2);
    
    // Actual Aim Direction (Bronze)
    drawArrow(ctx, ROBOT.x, ROBOT.y, aimX, aimY, "#CD7F32", 3);
    
    // Resultant Shot Path (Red)
    ctx.setLineDash([2, 5]);
    ctx.strokeStyle = "#B32416";
    ctx.beginPath(); ctx.moveTo(ROBOT.x, ROBOT.y); ctx.lineTo(HUB.x, HUB.y); ctx.stroke();
    ctx.setLineDash([]);

    // Metrics
    ctx.font = 'bold 11px "Orbitron", sans-serif';
    ctx.fillStyle = "#888"; 
    ctx.fillText(`SHOT_TOF: ${tof.toFixed(3)}s`, 20, 30);
    ctx.fillText(`DRIFT_COMP: ${(Math.sqrt(vRobotX*vRobotX + vRobotY*vRobotY) * tof).toFixed(2)}m`, 20, 50);

  }, [velocity]);

  return (
    <div style={{ background: '#0a0a0a', border: '1px solid #CD7F32', borderRadius: '12px', margin: '30px 0', padding: '24px', display: 'flex', flexWrap: 'wrap', gap: '30px', boxShadow: '0 10px 30px rgba(0,0,0,0.5)' }}>
      <div style={{ flex: '1 1 400px' }}>
         <canvas ref={canvasRef} width="500" height="400" style={{ width: '100%', height: 'auto', background: '#050505', borderRadius: '8px', border: '1px solid #222' }} />
      </div>
      <div style={{ flex: '1 1 200px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <h4 style={{ margin: 0, color: '#CD7F32', fontFamily: '"Orbitron", sans-serif', fontSize: '14px' }}>SOTM VECTOR SOLVER</h4>
        
        <div>
          <label style={{ color: '#888', fontSize: '11px', display: 'block', marginBottom: '8px' }}>ROBOT VX (m/s)</label>
          <input type="range" min="-2.5" max="2.5" step="0.1" value={velocity.x} onChange={(e) => setVelocity({...velocity, x: parseFloat(e.target.value)})} style={{ width: '100%', accentColor: '#4169E1' }} />
        </div>

        <div>
          <label style={{ color: '#888', fontSize: '11px', display: 'block', marginBottom: '8px' }}>ROBOT VY (m/s)</label>
          <input type="range" min="-2.5" max="2.5" step="0.1" value={velocity.y} onChange={(e) => setVelocity({...velocity, y: parseFloat(e.target.value)})} style={{ width: '100%', accentColor: '#4169E1' }} />
        </div>

        <div style={{ background: 'rgba(255,255,255,0.03)', padding: '15px', borderRadius: '8px', border: '1px solid #333' }}>
            <p style={{ margin: 0, fontSize: '10px', color: '#666', lineHeight: 1.6, fontStyle: 'italic' }}>
                Because the projectile inherits the robot's velocity, we must aim at a <strong>Virtual Target</strong> shifted opposite to our movement vector.
            </p>
        </div>
      </div>
    </div>
  );
}
