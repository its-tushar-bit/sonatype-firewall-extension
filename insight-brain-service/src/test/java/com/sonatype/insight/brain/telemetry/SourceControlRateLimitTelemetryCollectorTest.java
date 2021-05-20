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
      String scm = (String)attributeMap.get(SourceControlRateLimitTelemetryCollector.SCM);
      String userHash = (String)attributeMap.get(SourceControlRateLimitTelemetryCollector.USER_HASH);
      Integer calls = (Integer)attributeMap.get(SourceControlRateLimitTelemetryCollector.CALLS);
      Integer minRemaining = (Integer)attributeMap.get(SourceControlRateLimitTelemetryCollector.MIN_REMAINING);
      Integer timesExceeded = (Integer)attributeMap.get(SourceControlRateLimitTelemetryCollector.TIMES_EXCEEDED);

      switch (minRemaining) {
        case 123:
          assertThat(userHash).isEqualTo(DigestUtils.sha512Hex("user123"));
          assertThat(timesExceeded).isZero();
          assertThat(scm).isEqualTo("some-scm");
          assertThat(calls).isEqualTo(1);
          break;

        case 456:
          assertThat(userHash).isEqualTo(DigestUtils.sha512Hex("user456"));
          assertThat(timesExceeded).isEqualTo(1);
          assertThat(scm).isEqualTo("some-scm");
          assertThat(calls).isEqualTo(2);
          break;

        case 789:
          assertThat(userHash).isEqualTo(DigestUtils.sha512Hex("user789"));
          assertThat(timesExceeded).isEqualTo(2);
          assertThat(scm).isEqualTo("other-scm");
          assertThat(calls).isEqualTo(3);
          break;

        default:
          fail("unexpected telemetry value");
      }
    });
  }
}
