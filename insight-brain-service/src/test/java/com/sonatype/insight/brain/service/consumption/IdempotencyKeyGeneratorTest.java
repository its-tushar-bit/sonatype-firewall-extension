/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.List;

import com.sonatype.insight.brain.model.consumption.ActivityType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class IdempotencyKeyGeneratorTest
{
  private ConsumptionContext ctx;

  @Before
  public void setUp() {
    ConsumptionContext.set("org-1", "pro", "ui");
    ctx = ConsumptionContext.get();
    ctx.setUserId("42");
    ctx.setAppId("A1");
    ctx.setScanId("SX");
    ctx.setSessionId("sess-abc123");
  }

  @After
  public void tearDown() {
    ConsumptionContext.clear();
  }

  @Test
  public void componentDetails_buildsFiveSegmentKeyWithHashedSession() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, "C1");
    assertThat(key).isNotNull();
    assertThat(key).startsWith("42:COMPONENT_DETAILS:C1:SX:");
    String[] segments = key.split(":");
    assertThat(segments).hasSize(5);
    assertThat(segments[4]).hasSize(16);
    assertThat(segments[4]).matches("[0-9a-f]{16}");
    assertThat(key).doesNotContain("sess-abc123");
  }

  @Test
  public void versionRecommendation_mirrorsComponentDetailsShape() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.VERSION_RECOMMENDATION, ctx, "C1");
    assertThat(key).startsWith("42:VERSION_RECOMMENDATION:C1:SX:");
  }

  @Test
  public void reachability_mirrorsComponentDetailsShape() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.REACHABILITY, ctx, "C1");
    assertThat(key).startsWith("42:REACHABILITY:C1:SX:");
  }

  @Test
  public void developerPriorities_usesAppIdNotScanId() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.DEVELOPER_PRIORITIES, ctx, "CVE-2024-1|pkg:maven/foo");
    assertThat(key).isNotNull();
    assertThat(key).startsWith("42:DEVELOPER_PRIORITIES:CVE-2024-1|pkg:maven/foo:A1:");
  }

  @Test
  public void appScan_isSessionless() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.APP_SCAN, ctx, null);
    assertThat(key).isEqualTo("42:APP_SCAN:SX");
  }

  @Test
  public void reEvaluate_isSessionless() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.RE_EVALUATE, ctx, null);
    assertThat(key).isEqualTo("42:RE_EVALUATE:SX");
  }

  @Test
  public void continuousMonitoring_isSessionless() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.CONTINUOUS_MONITORING, ctx, null);
    assertThat(key).isEqualTo("42:CONTINUOUS_MONITORING:SX");
  }

  @Test
  public void api_usesEntityIdAsRequestId() {
    String key = IdempotencyKeyGenerator.generate(ActivityType.API, ctx, "req-abc");
    assertThat(key).isEqualTo("42:API:req-abc");
  }

  @Test
  public void others_alwaysReturnsNull() {
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.OTHERS, ctx, "anything")).isNull();
  }

  @Test
  public void componentDetails_returnsNull_whenScanIdMissing() {
    ctx.setScanId(null);
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, "C1")).isNull();
  }

  @Test
  public void componentDetails_returnsNull_whenSessionIdMissing() {
    ctx.setSessionId(null);
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, "C1")).isNull();
  }

  @Test
  public void componentDetails_returnsNull_whenEntityIdMissing() {
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, null)).isNull();
  }

  @Test
  public void componentDetails_returnsNull_whenUserIdMissing() {
    ctx.setUserId(null);
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, "C1")).isNull();
  }

  @Test
  public void developerPriorities_returnsNull_whenAppIdMissing() {
    ctx.setAppId(null);
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.DEVELOPER_PRIORITIES, ctx, "CVE-1|purl")).isNull();
  }

  @Test
  public void appScan_returnsNull_whenScanIdMissing() {
    ctx.setScanId(null);
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.APP_SCAN, ctx, null)).isNull();
  }

  @Test
  public void deterministicAcrossInvocations() {
    String first = IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, "C1");
    String second = IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, "C1");
    assertThat(first).isEqualTo(second);
  }

  @Test
  public void returnsNull_whenGeneratedKeyExceedsColumnWidth() {
    // Pathological username pushes the joined key over 255 chars — fall back to null
    // (unkeyed insert) rather than letting the INSERT fail the column-size constraint.
    ctx.setUserId("u".repeat(300));
    String entityId = "12f334a1dc9c6d2854c4";
    assertThat(IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, entityId)).isNull();
  }

  /**
   * Pins the warn-once-per-bucket sampling contract of {@code logOverflowSampled}:
   * 5 overflows with the same userId-length bucket must produce exactly 1 WARN log entry.
   *
   * <p>
   * {@code overflowsSeen} is a static field, so prior test runs in the same JVM may have
   * left a bucket entry. We use a unique userId length (512 chars → bucket 16) that no
   * other test exercises, ensuring the bucket starts empty without reflective reset.
   */
  @Test
  public void logOverflowSampled_logsWarnOnFirstOccurrence_suppressesSubsequent() throws Exception {
    // Attach a ListAppender to the IdempotencyKeyGenerator logger
    Logger idkLogger = (Logger) LoggerFactory.getLogger(IdempotencyKeyGenerator.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    idkLogger.addAppender(appender);
    try {
      // 512-char userId → bucket 16 (512/32). No other test uses a 512-char userId,
      // so this bucket is guaranteed clean at the start of this test.
      String longUserId = "u".repeat(512);
      ctx.setUserId(longUserId);
      String entityId = "12f334a1dc9c6d2854c4";

      // Trigger overflow 5 times with the same (type, userId-length) bucket
      for (int i = 0; i < 5; i++) {
        assertThat(IdempotencyKeyGenerator.generate(ActivityType.COMPONENT_DETAILS, ctx, entityId)).isNull();
      }

      // Exactly 1 WARN should have been emitted; subsequent occurrences are suppressed
      List<ILoggingEvent> warns = appender.list.stream()
          .filter(e -> e.getLevel() == Level.WARN)
          .collect(java.util.stream.Collectors.toList());
      assertThat(warns).hasSize(1);
      assertThat(warns.get(0).getFormattedMessage()).contains("exceeds");
    }
    finally {
      idkLogger.detachAppender(appender);
    }
  }
}
