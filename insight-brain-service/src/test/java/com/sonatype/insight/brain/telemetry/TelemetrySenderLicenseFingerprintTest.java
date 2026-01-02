/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

/**
 * Special test for {@link TelemetrySender} to check that the license fingerprint is added as the `X-CLM-Token` header
 * by {@link HdsClient}. Has to be separate from {@link TelemetrySenderTest} as the HdsClient cannot be mocked out.
 */
@Category(SlowTest.class)
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
    // Telemetry is sent async, so we have to await for this assertion to succeed.
    await().atMost(5, SECONDS)
        .untilAsserted(() -> assertThat(hdsMockServer.getCapturedRequestHttpHeaders(TelemetrySender.RESOURCE_PATH))
            .containsEntry("X-CLM-Token", licenseFingerprint));
  }
}
