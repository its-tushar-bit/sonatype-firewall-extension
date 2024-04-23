/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Special test for {@link TelemetrySender} to check that the license fingerprint is added as the `X-CLM-Token` header
 * by {@link HdsClient}. Has to be separate from {@link TelemetrySenderTest} as the HdsClient cannot be mocked out.
 */
public class TelemetrySenderLicenseFingerprintTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testSend_TokenMatchesFingerprint() {
    TelemetrySender telemetrySender = getCLMServer().getInstance(TelemetrySender.class);
    telemetrySender.start();

    TelemetryData telemetryDataSend = new TelemetryData(TelemetryPurpose.DATABASE);
    telemetryDataSend.put("test-key", "test-value");

    telemetrySender.send(telemetryDataSend);

    String licenseFingerprint = getLicenseFingerprint();
    Map<String, String> headers = hdsMockServer.getCapturedRequestHttpHeaders(TelemetrySender.RESOURCE_PATH);
    assertThat(headers).containsEntry("X-CLM-Token", licenseFingerprint);
  }
}
