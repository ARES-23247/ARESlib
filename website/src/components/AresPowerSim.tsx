import React, { useState, useEffect } from 'react';

export default function AresPowerSim() {
  const [loads, setLoads] = useState({
    mecanum: false,
    vipers: false,
    intake: false,
    servos: false,
  });

  const [voltage, setVoltage] = useState(12.0);
  const [isShedding, setIsShedding] = useState(false);
  const [isBrownout, setIsBrownout] = useState(false);

  const internalResistance = 0.045; // FTC Batteries have higher internal resistance (~45mOhm)
  const nominalVoltage = 12.4; // Fresh charge

  // FTC Load values in Amps (Aggressive estimates for stress testing)
  const loadValues = {
    mecanum: 75, // 4-motor stall/accel
    vipers: 25,  // Heavy climbing or slide extension
    intake: 15,
    servos: 8,   // High-torque axon/gobilda servos
  };

  useEffect(() => {
    let totalCurrent = 0;
    
    // Tier 1: Mecanum (Main Propulsion - Never sheds)
    if (loads.mecanum) totalCurrent += loadValues.mecanum;
    
    // Tier 2: Viper Slides (Critical for scoring)
    if (loads.vipers) totalCurrent += loadValues.vipers;

    // Initial check for potential voltage sag with T1/T2
    let potentialVoltage = nominalVoltage - (totalCurrent * internalResistance);
    
    // ARESLib dynamic shedding threshold (7.5V to avoid 7.0V Control Hub cutoff)
    const sheddingActive = potentialVoltage < 8.5;
    setIsShedding(sheddingActive);

    // Tier 3: Intake / Servos (Sheddable)
    if (loads.intake) {
      totalCurrent += sheddingActive ? (loadValues.intake * 0.15) : loadValues.intake;
    }
    if (loads.servos) {
      totalCurrent += sheddingActive ? (loadValues.servos * 0.15) : loadValues.servos;
    }

    const finalVoltage = Math.max(0, nominalVoltage - (totalCurrent * internalResistance));
    setVoltage(finalVoltage);
    setIsBrownout(finalVoltage < 7.0); // Control Hub cutoff risk
  }, [loads]);

  const toggleLoad = (load: keyof typeof loads) => {
    setLoads(prev => ({ ...prev, [load]: !prev[load] }));
  };

  const getVoltageColor = () => {
    if (isBrownout) return '#ff4d4d'; // Red
    if (isShedding) return '#CD7F32'; // Bronze
    return '#00d0ff'; // Blue
  };

  return (
    <div style={{
      background: '#0a0a0a',
      border: '1px solid #CD7F32',
      borderRadius: '12px',
      margin: '30px 0',
      display: 'flex',
      flexDirection: 'column',
      overflow: 'hidden',
      color: '#fff',
      fontFamily: '"Orbitron", sans-serif',
      boxShadow: '0 10px 30px rgba(0,0,0,0.5)'
    }}>
      <div style={{
        padding: '16px 20px',
        background: 'rgba(205, 127, 50, 0.05)',
        borderBottom: '1px solid rgba(205, 127, 50, 0.2)',
        fontSize: '12px',
        fontWeight: 800,
        color: '#CD7F32',
        letterSpacing: '1px'
      }}>
        ARESLIB HARDWARE RESILIENCE SIMULATOR
      </div>

      <div style={{
        display: 'flex',
        padding: '24px',
        gap: '30px',
        flexWrap: 'wrap',
        justifyContent: 'center'
      }}>
        {/* LOAD TOGGLES */}
        <div style={{
          flex: '1 1 300px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px'
        }}>
          <div style={{ fontSize: '10px', color: '#888', marginBottom: '4px', fontFamily: 'monospace' }}>CONTROL_HUB_LOADS</div>
          
          <button 
            onClick={() => toggleLoad('mecanum')}
            style={{
              padding: '12px 16px',
              borderRadius: '6px',
              border: '1px solid #444',
              background: loads.mecanum ? '#B32416' : '#111',
              color: '#fff',
              cursor: 'pointer',
              textAlign: 'left',
              display: 'flex',
              justifyContent: 'space-between',
              transition: '0.2s',
              fontFamily: '"Orbitron", sans-serif'
            }}>
            <span>[T1] MECANUM DRIVE</span>
            <span style={{ fontSize: '12px', opacity: 0.8 }}>~75A</span>
          </button>

          <button 
            onClick={() => toggleLoad('vipers')}
            style={{
              padding: '12px 16px',
              borderRadius: '6px',
              border: '1px solid #444',
              background: loads.vipers ? '#CD7F32' : '#111',
              color: '#fff',
              cursor: 'pointer',
              textAlign: 'left',
              display: 'flex',
              justifyContent: 'space-between',
              transition: '0.2s',
              fontFamily: '"Orbitron", sans-serif'
            }}>
            <span>[T2] VIPER SLIDES</span>
            <span style={{ fontSize: '12px', opacity: 0.8 }}>~25A</span>
          </button>

          <button 
            onClick={() => toggleLoad('intake')}
            style={{
              padding: '12px 16px',
              borderRadius: '6px',
              border: '1px solid #444',
              background: loads.intake ? (isShedding ? '#333' : '#CD7F32') : '#111',
              color: '#fff',
              cursor: 'pointer',
              textAlign: 'left',
              display: 'flex',
              justifyContent: 'space-between',
              transition: '0.2s',
              opacity: loads.intake && isShedding ? 0.6 : 1,
              fontFamily: '"Orbitron", sans-serif'
            }}>
            <span>[T3] INTAKE MOTORS</span>
            <span style={{ fontSize: '12px', opacity: 0.8 }}>
              {loads.intake && isShedding ? '2A (SHED)' : '15A'}
            </span>
          </button>

          <button 
            onClick={() => toggleLoad('servos')}
            style={{
              padding: '12px 16px',
              borderRadius: '6px',
              border: '1px solid #444',
              background: loads.servos ? (isShedding ? '#333' : '#CD7F32') : '#111',
              color: '#fff',
              cursor: 'pointer',
              textAlign: 'left',
              display: 'flex',
              justifyContent: 'space-between',
              transition: '0.2s',
              opacity: loads.servos && isShedding ? 0.6 : 1,
              fontFamily: '"Orbitron", sans-serif'
            }}>
            <span>[T3] H-TORQUE SERVOS</span>
            <span style={{ fontSize: '12px', opacity: 0.8 }}>
              {loads.servos && isShedding ? '1A (SHED)' : '8A'}
            </span>
          </button>
        </div>

        {/* DASHBOARD */}
        <div style={{
          flex: '1 1 250px',
          background: '#050505',
          padding: '24px',
          borderRadius: '8px',
          border: '1px solid #222',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '15px',
          position: 'relative',
          overflow: 'hidden'
        }}>
          <div style={{
            fontSize: '42px',
            fontWeight: 900,
            color: getVoltageColor(),
            textShadow: `0 0 20px ${getVoltageColor()}44`
          }}>
            {voltage.toFixed(1)}V
          </div>
          
          <div style={{
            fontSize: '10px',
            fontWeight: 800,
            color: isBrownout ? '#ff4d4d' : (isShedding ? '#CD7F32' : '#00d0ff'),
            letterSpacing: '0.2em'
          }}>
            {isBrownout ? 'BROWNOUT RISK' : (isShedding ? 'SHEDDING_ACTIVE' : 'VOLTAGE_NOMINAL')}
          </div>

          <div style={{
            width: '100%',
            height: '8px',
            background: '#111',
            borderRadius: '4px',
            overflow: 'hidden',
            border: '1px solid #222'
          }}>
            <div style={{
              width: `${(voltage / 12.4) * 100}%`,
              height: '100%',
              background: getVoltageColor(),
              transition: '0.3s cubic-bezier(0.4, 0, 0.2, 1)'
            }}></div>
          </div>

          {isShedding && (
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              background: 'rgba(205, 127, 50, 0.05)',
              pointerEvents: 'none',
              animation: 'ares-pulse 2s infinite'
            }}></div>
          )}

          {isBrownout && (
            <div style={{
              padding: '6px 10px',
              background: '#B32416',
              color: '#fff',
              fontWeight: 900,
              fontSize: '9px',
              borderRadius: '4px',
              marginTop: '10px',
              textTransform: 'uppercase'
            }}>
              CONTROL HUB CRITICAL
            </div>
          )}
        </div>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        @keyframes ares-pulse {
          0% { opacity: 0.1; }
          50% { opacity: 0.4; }
          100% { opacity: 0.1; }
        }
      `}} />
    </div>
  );
}
