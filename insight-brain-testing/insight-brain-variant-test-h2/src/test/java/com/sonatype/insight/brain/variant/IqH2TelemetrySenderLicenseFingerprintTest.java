/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.jupiter.api.Test;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Special test for {@link TelemetrySender} to check that the license fingerprint is added as the `X-CLM-Token`
 * header by the HDS client. Kept separate because the HDS server here cannot be mocked out.
 */
@IqH2Test
class IqH2TelemetrySenderLicenseFingerprintTest
{
  private IqTestContext ctx;

  @Test
  void testSend_TokenMatchesFingerprint() {
    TelemetrySender telemetrySender = ctx.lookup(TelemetrySender.class);
    telemetrySender.start();

    TelemetryData telemetryDataSend = new TelemetryData(TelemetryPurpose.DATABASE);
    telemetryDataSend.put("test-key", "test-value");

    telemetrySender.send(telemetryDataSend);

    String licenseFingerprint = ctx.lookup(LicenseFingerprinter.class).calculate();
    // Telemetry is sent async, so we have to await for this assertion to succeed.
    await().atMost(5, SECONDS)
        .untilAsserted(() -> assertThat(ctx.getHdsServer().getCapturedRequestHttpHeaders(TelemetrySender.RESOURCE_PATH))
            .containsEntry("X-CLM-Token", licenseFingerprint));
  }
}
