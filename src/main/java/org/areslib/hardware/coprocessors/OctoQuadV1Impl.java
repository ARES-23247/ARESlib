package org.areslib.hardware.coprocessors;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.hardware.digitalchickenlabs.OctoQuad;

import java.lang.reflect.Method;

public class OctoQuadV1Impl implements AresOctoQuadDriver {
    
    private final OctoQuad octoQuad;
    
    public OctoQuadV1Impl(HardwareMap hardwareMap, String deviceName) {
        this.octoQuad = hardwareMap.get(OctoQuad.class, deviceName);
    }

    @Override
    public void clearReadCache() {
        try {
            Method clearMethod = octoQuad.getClass().getMethod("clearReadCache");
            clearMethod.invoke(octoQuad);
        } catch (Exception e) {
            try {
                Method refreshMethod = octoQuad.getClass().getMethod("refreshCache");
                refreshMethod.invoke(octoQuad);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public double readPosition(int channel) {
        return octoQuad.readSinglePosition(channel);
    }

    @Override
    public double readPulseWidth(int channel) {
        try {
            // Pulse width reading on V1 natively uses getSingleChannelPulseWidthParams
            Object params = octoQuad.getSingleChannelPulseWidthParams(channel);
            // Just use reflection to pull the length
            return ((Number) params.getClass().getField("pulseWidth").get(params)).doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public void setChannelBankConfig(int channel, OctoMode mode) {
        // Assume mapping logic for bank config if necessary.
        // Usually modifying a single channel config sets it to pulse/encoder.
        try {
            // Placeholder execution to map to OctoQuadBase$ChannelBankConfig
        } catch (Exception ignored) {}
    }
}
