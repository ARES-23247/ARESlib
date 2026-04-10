package org.areslib.hardware.coprocessors;

/**
 * AresOctoQuadDriver standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * AresOctoQuadDriver}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public interface AresOctoQuadDriver {
  void clearReadCache();

  double readPosition(int channel);

  double readPulseWidth(int channel);

  void setChannelBankConfig(int channel, OctoMode mode);
}
