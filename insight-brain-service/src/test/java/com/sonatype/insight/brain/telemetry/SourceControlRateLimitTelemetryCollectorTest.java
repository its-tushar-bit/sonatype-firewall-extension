/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.client.utils.RateLimitRecorder;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SourceControlRateLimitTelemetryCollectorTest
{
  @Test
  public void testCollectAllData() {
    // given: rate limit metrics for multiple users and SCMs
    RateLimitRecorder.recordApiRateLimitRemaining("some-scm", "user123", 123);

    RateLimitRecorder.recordApiRateLimitRemaining("some-scm", "user456", 457);
    RateLimitRecorder.recordApiRateLimitRemaining("some-scm", "user456", 456);
    RateLimitRecorder.recordApiRateLimitExceeded("some-scm", "user456");

    RateLimitRecorder.recordApiRateLimitRemaining("other-scm", "user789", 791);
    RateLimitRecorder.recordApiRateLimitRemaining("other-scm", "user789", 790);
    RateLimitRecorder.recordApiRateLimitRemaining("other-scm", "user789", 789);
    RateLimitRecorder.recordApiRateLimitExceeded("other-scm", "user789");
    RateLimitRecorder.recordApiRateLimitExceeded("other-scm", "user789");

    // when: fetch daily rate limit info
    List<TelemetryData> telemetryDataList = new SourceControlRateLimitTelemetryCollector().collectAllData();

    // then: the number of telemetry objects we'd expect were created
    assertThat(telemetryDataList).hasSize(3);

    // and then: the telemetry data is what we expect
    telemetryDataList.forEach(data -> {
      Map attributeMap = data.getAttributes();
      SourceControlRateLimitTelemetry rateLimitTelemetry =
          (SourceControlRateLimitTelemetry) attributeMap
              .get(SourceControlRateLimitTelemetry.SOURCE_CONTROL_RATE_LIMITS);

      switch (rateLimitTelemetry.minRemaining) {
        case 123:
          assertThat(rateLimitTelemetry.userHash).isEqualTo(DigestUtils.sha512Hex("user123"));
          assertThat(rateLimitTelemetry.timesExceeded).isZero();
          assertThat(rateLimitTelemetry.scm).isEqualTo("some-scm");
          assertThat(rateLimitTelemetry.calls).isEqualTo(1);
          break;

        case 456:
          assertThat(rateLimitTelemetry.userHash).isEqualTo(DigestUtils.sha512Hex("user456"));
          assertThat(rateLimitTelemetry.timesExceeded).isEqualTo(1);
          assertThat(rateLimitTelemetry.scm).isEqualTo("some-scm");
          assertThat(rateLimitTelemetry.calls).isEqualTo(2);
          break;

        case 789:
          assertThat(rateLimitTelemetry.userHash).isEqualTo(DigestUtils.sha512Hex("user789"));
          assertThat(rateLimitTelemetry.timesExceeded).isEqualTo(2);
          assertThat(rateLimitTelemetry.scm).isEqualTo("other-scm");
          assertThat(rateLimitTelemetry.calls).isEqualTo(3);
          break;

        default:
          fail("unexpected telemetry value");
      }
    });
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(new SourceControlRateLimitTelemetryCollector().isClusterTelemetry()).isFalse();
  }
}
