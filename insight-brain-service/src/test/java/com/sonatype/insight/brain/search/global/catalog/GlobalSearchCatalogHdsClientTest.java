/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit coverage for the pure-static helpers on {@link GlobalSearchCatalogHdsClient}: the pool-size
 * env-var resolution and the redacted DEBUG logging. No HDS transport is exercised.
 */
public class GlobalSearchCatalogHdsClientTest
{
  @Test
  public void resolvePoolSize_nullBlankNonNumericOrOutOfRange_fallsBackToDefault() {
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize(null))
        .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize("   "))
        .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize("abc"))
        .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize("0"))
        .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize("-1"))
        .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
    // MAX + 1 is out of range and clamps to the default.
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize(
        String.valueOf(GlobalSearchCatalogHdsClient.MAX_POOL_SIZE + 1)))
            .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
  }

  @Test
  public void resolvePoolSize_defaultValueEcho_returnsDefault() {
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize(
        String.valueOf(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE)))
            .isEqualTo(GlobalSearchCatalogHdsClient.DEFAULT_POOL_SIZE);
  }

  @Test
  public void resolvePoolSize_validNonDefaultOverride_isHonored() {
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize("30")).isEqualTo(30);
    // Boundary values are accepted.
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize("1")).isEqualTo(1);
    assertThat(GlobalSearchCatalogHdsClient.resolvePoolSize(
        String.valueOf(GlobalSearchCatalogHdsClient.MAX_POOL_SIZE)))
            .isEqualTo(GlobalSearchCatalogHdsClient.MAX_POOL_SIZE);
  }

  @Test
  public void logRedactedRequest_replacesRawQueryWithCharCount_masksToken_passesOthersThrough() {
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalSearchCatalogHdsClient.class);
    Level original = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.DEBUG);
    logger.addAppender(appender);

    final String rawQuery = "log4j secret-project";
    Multimap<String, String> params = ArrayListMultimap.create();
    params.put("query", rawQuery);
    params.put("limit", "25");

    try {
      GlobalSearchCatalogHdsClient.logRedactedRequest("rest/search/global", params);
    }
    finally {
      logger.detachAppender(appender);
      logger.setLevel(original);
    }

    String formatted = appender.list.stream()
        .filter(e -> e.getLevel() == Level.DEBUG)
        .map(ILoggingEvent::getFormattedMessage)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Expected a DEBUG log line"));

    // The raw query text must never reach the log; only its character count.
    assertThat(formatted).doesNotContain(rawQuery);
    assertThat(formatted).contains("query=<" + rawQuery.length() + " chars>");
    // The X-CLM-Token header is always masked with the fixed marker (it is never sent as a
    // query param, so it does not travel the per-param echo path).
    assertThat(formatted).contains("token=****");
    // Non-sensitive params pass through unredacted.
    assertThat(formatted).contains("limit=25");
    assertThat(formatted).contains("rest/search/global");
  }

  @Test
  public void logRedactedRequest_debugDisabled_emitsNothing() {
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalSearchCatalogHdsClient.class);
    Level original = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.INFO);
    logger.addAppender(appender);

    Multimap<String, String> params = ArrayListMultimap.create();
    params.put("query", "anything");

    try {
      GlobalSearchCatalogHdsClient.logRedactedRequest("rest/search/global", params);
    }
    finally {
      logger.detachAppender(appender);
      logger.setLevel(original);
    }

    assertThat(appender.list).isEmpty();
  }
}
