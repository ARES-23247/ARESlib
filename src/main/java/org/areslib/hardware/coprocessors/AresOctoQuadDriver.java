package org.areslib.hardware.coprocessors;

public interface AresOctoQuadDriver {
    void clearReadCache();
    double readPosition(int channel);
    double readPulseWidth(int channel);
    void setChannelBankConfig(int channel, OctoMode mode);
}
