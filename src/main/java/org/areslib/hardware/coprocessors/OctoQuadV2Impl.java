package org.areslib.hardware.coprocessors;

import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * OctoQuadV2Impl standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * OctoQuadV2Impl}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class OctoQuadV2Impl implements AresOctoQuadDriver {

  private final OctoQuadFWv3 octoQuadV2;

  public OctoQuadV2Impl(HardwareMap hardwareMap, String deviceName) {
    this.octoQuadV2 = hardwareMap.get(OctoQuadFWv3.class, deviceName);
  }

  @Override
  public void clearReadCache() {
    // OctoQuadFWv3 reads bulk data on-demand over I2C and does not expose a public clearCache
    // method directly.
  }

  @Override
  public double readPosition(int channel) {
    if (octoQuadV2 != null) {
      OctoQuadFWv3.EncoderDataBlock block = octoQuadV2.readAllEncoderData();
      if (block.isDataValid()) {
        return block.positions[channel];
      }
    }
    return 0.0;
  }

  @Override
  public double readPulseWidth(int channel) {
    // FWv3 returns pulse widths in the position block if configured for PWM mode
    return readPosition(channel);
  }

  @Override
  public void setChannelBankConfig(int channel, OctoMode mode) {
    if (octoQuadV2 != null) {
      OctoQuadFWv3.ChannelBankConfig cfg = octoQuadV2.getChannelBankConfig();

      // Just assume we want ALL_PULSE_WIDTH if any channel needs absolute, this is a simplified
      // wrapper
      // FWv3 configures banks, not individual channels.
      if (mode == OctoMode.PULSE_WIDTH || mode == OctoMode.ABSOLUTE) {
        if (channel < 4) {
          if (cfg == OctoQuadFWv3.ChannelBankConfig.ALL_QUADRATURE) {
            octoQuadV2.setChannelBankConfig(
                OctoQuadFWv3.ChannelBankConfig.BANK1_PULSE_WIDTH_BANK2_QUADRATURE);
          }
        } else {
          if (cfg == OctoQuadFWv3.ChannelBankConfig.ALL_QUADRATURE) {
            octoQuadV2.setChannelBankConfig(
                OctoQuadFWv3.ChannelBankConfig.BANK1_QUADRATURE_BANK2_PULSE_WIDTH);
          }
        }
      }
    }
  }
}
