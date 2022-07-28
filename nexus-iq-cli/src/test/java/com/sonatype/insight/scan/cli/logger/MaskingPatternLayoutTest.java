/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli.logger;

import java.util.regex.PatternSyntaxException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MaskingPatternLayoutTest
{
  private MaskingPatternLayout dataMaskingLayout;

  private LoggerContext loggerContext = new LoggerContext();

  private Logger logger = loggerContext.getLogger(MaskingPatternLayoutTest.class);

  private static final String MASK_PATTERN_1 = "Authorization: (.*)";

  private static final String MASK_PATTERN_2 = "Second Pattern: (.*,)";

  private static final String MASK_PATTERN_3 = "Third Pattern: (.*)";

  private static final String MULTIPLE_MASK_PATTERNS = "Second Pattern: (.*), Third Pattern: (.*)";

  private static final String BAD_MASK_PATTERN = "Bad Pattern: .*)";

  @Before
  public void setUp() {
    dataMaskingLayout = new MaskingPatternLayout();
    dataMaskingLayout.setContext(loggerContext);
    dataMaskingLayout.setPattern("[%level] %m%n");
    dataMaskingLayout.start();
  }

  @Test
  public void testMaskDataUsingSingleMaskPattern() {
    // Given
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_1);
    ILoggingEvent event = createEvent("Authorization: Basic am9uLWRvZTpteS1wYXNzd29yZA==", Level.DEBUG);

    // When
    String result = dataMaskingLayout.doLayout(event);

    // Then
    assertMessageIsTheExpected(result, "[DEBUG] Authorization: ************");
  }

  @Test
  public void testMaskDataUsingMultipleMaskPatterns() {
    // Given
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_1);
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_2);
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_3);
    ILoggingEvent event1 = createEvent("Authorization: Basic am9uLWRvZTpteS1wYXNzd29yZA==", Level.DEBUG);
    ILoggingEvent event2 = createEvent("Second Pattern: sensitive-data,", Level.INFO);
    ILoggingEvent event3 = createEvent("Third Pattern: more-sensitive-data", Level.WARN);

    // When
    String result1 = dataMaskingLayout.doLayout(event1);
    String result2 = dataMaskingLayout.doLayout(event2);
    String result3 = dataMaskingLayout.doLayout(event3);

    // Then
    assertMessageIsTheExpected(result1, "[DEBUG] Authorization: ************");
    assertMessageIsTheExpected(result2, "[INFO] Second Pattern: ************");
    assertMessageIsTheExpected(result3, "[WARN] Third Pattern: ************");
  }

  @Test
  public void testMaskDataUsingSingleComposedMaskPattern() {
    // Given
    dataMaskingLayout.addMaskPattern(MULTIPLE_MASK_PATTERNS);
    ILoggingEvent event = createEvent("Second Pattern: sensitive-data, Third Pattern: sensitive-data", Level.INFO);

    // When
    String result = dataMaskingLayout.doLayout(event);

    // Then
    assertMessageIsTheExpected(result, "[INFO] Second Pattern: ************, Third Pattern: ************");
  }

  @Test
  public void testMaskDataUsingMultipleMaskPatternsOnSameLine() {
    // Given
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_3);
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_2);
    ILoggingEvent event = createEvent("Second Pattern: sensitive-data, Third Pattern: sensitive-data", Level.INFO);

    // When
    String result = dataMaskingLayout.doLayout(event);

    // Then
    assertMessageIsTheExpected(result, "[INFO] Second Pattern: ************ Third Pattern: ************");
  }

  @Test
  public void testMaskingMessageWithJustOnePatternWhenTwoMayApply() {
    // Given  a pattern that masks the whole message, and a second pattern that still may match
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_1);
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_3);
    ILoggingEvent event = createEvent("Authorization: Basic sensitive-data, Third Pattern: sensitive-data", Level.INFO);

    // When message is masked
    String result = dataMaskingLayout.doLayout(event);

    // Then we expect first pattern masking all message, and second pattern not being used
    assertMessageIsTheExpected(result, "[INFO] Authorization: ************");
  }

  @Test
  public void testNoMaskingDataIfPatternDoNotMatch() {
    // Given
    dataMaskingLayout.addMaskPattern(MASK_PATTERN_1);

    ILoggingEvent event = createEvent("Other: Basic am9uLWRvZTpteS1wYXNzd29yZA==", Level.DEBUG);

    // When
    String result = dataMaskingLayout.doLayout(event);

    // Then
    assertMessageIsTheExpected(result, "[DEBUG] Other: Basic am9uLWRvZTpteS1wYXNzd29yZA==");
  }

  @Test(expected = PatternSyntaxException.class)
  public void testWithBadMaskPattern() {
    // Given
    dataMaskingLayout.addMaskPattern(BAD_MASK_PATTERN);
    ILoggingEvent event = createEvent("Bad Pattern: sensitive-data", Level.DEBUG);

    // When
    dataMaskingLayout.doLayout(event);
  }

  private void assertMessageIsTheExpected(String result, String expected) {
    assertThat(result).isEqualTo(expected + System.lineSeparator());
  }

  private ILoggingEvent createEvent(String message, Level level) {
    return new LoggingEvent(
        Logger.FQCN,
        logger,
        level,
        message,
        null,
        null
    );
  }
}
