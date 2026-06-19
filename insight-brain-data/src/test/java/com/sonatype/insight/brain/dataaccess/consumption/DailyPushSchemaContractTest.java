/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static contract guard for CLM-40771 BDD-048: the daily-push aggregation queries
 * on {@link ConsumptionEventDAO} must NOT reference the {@code idempotency_key}
 * column. That column is internal to dedup; it must remain invisible to the
 * Cloud → Platform daily push, which aggregates only on
 * {@code (billing_month, org_id, activity_type, component_count)}.
 *
 * <p>
 * If this test fails, a recent edit to {@code ConsumptionEventDAO} added an
 * {@code IDEMPOTENCY_KEY} reference outside the {@code recordEvent(...)} insert
 * method. Remove the reference; the column is for INSERT and ON CONFLICT only.
 *
 * @since 1.205 (CLM-40771)
 */
public class DailyPushSchemaContractTest
{
  private static final Path DAO_SOURCE = Path.of(
      System.getProperty("user.dir"),
      "src/main/java/com/sonatype/insight/brain/dataaccess/consumption/ConsumptionEventDAO.java");

  @Test
  public void aggregationMethods_doNotReferenceIdempotencyKey() throws IOException {
    assertThat(DAO_SOURCE)
        .as("DAO source must be readable from CWD '%s' (run from module root)",
            System.getProperty("user.dir"))
        .exists();
    String source = Files.readString(DAO_SOURCE);

    // Find the bounds of the recordEvent method — IDEMPOTENCY_KEY is allowed there.
    Pattern recordEventStart = Pattern.compile(
        "public void recordEvent\\(final ConsumptionEvent event\\)\\s*\\{");
    Matcher startMatcher = recordEventStart.matcher(source);
    assertThat(startMatcher.find())
        .as("recordEvent(final ConsumptionEvent event) method must exist")
        .isTrue();
    int methodStart = startMatcher.start();

    // recordEvent ends at the matching closing brace (brace-count from the opening brace).
    int methodEnd = findMethodEnd(source, startMatcher.end());

    // Also exclude the Javadoc block immediately preceding recordEvent (if any),
    // since it legitimately references idempotency_key to explain the dedup design.
    int exclusionStart = findPrecedingJavadocStart(source, methodStart);

    // Concatenate everything BEFORE recordEvent (including its Javadoc) and AFTER recordEvent.
    String aggregationCode = source.substring(0, exclusionStart) + source.substring(methodEnd);

    assertThat(aggregationCode)
        .as("Daily-push aggregation methods must NOT reference idempotency_key (CLM-40771 BDD-048). " +
            "The column is internal to dedup; the Cloud → Platform daily push aggregates only on " +
            "(billing_month, org_id, activity_type, component_count). If this assertion fails, a " +
            "recent edit added IDEMPOTENCY_KEY in an aggregation method — remove the reference.")
        .doesNotContain("IDEMPOTENCY_KEY")
        .doesNotContain("idempotency_key");
  }

  /**
   * If there is a Javadoc block ({@code /** ... * /}) immediately preceding {@code methodPos}
   * (possibly separated only by whitespace), return the start offset of that block.
   * Otherwise return {@code methodPos} unchanged.
   */
  private static int findPrecedingJavadocStart(final String source, final int methodPos) {
    // Walk backwards past any whitespace between the method and a potential closing "* /"
    int i = methodPos - 1;
    while (i >= 0 && Character.isWhitespace(source.charAt(i))) {
      i--;
    }
    // Check for closing */ of a Javadoc block
    if (i >= 1 && source.charAt(i) == '/' && source.charAt(i - 1) == '*') {
      int closeSlash = i;
      // Find the matching opening /**
      int open = source.lastIndexOf("/**", closeSlash);
      if (open >= 0) {
        // Walk back past any whitespace before /**
        int j = open - 1;
        while (j >= 0 && Character.isWhitespace(source.charAt(j))) {
          j--;
        }
        return j + 1;
      }
    }
    return methodPos;
  }

  /**
   * Find the byte offset of the closing brace of the method that starts at {@code bodyStart}.
   *
   * <p>
   * Naive brace counter: it does not skip braces inside string literals, char literals,
   * or comments. Relies on a property of the current ConsumptionEventDAO source — its log
   * messages happen to contain balanced braces (e.g. {@code "key {}"} contributes
   * {@code +1/-1} which cancel) and the method body has no string literals with mismatched
   * braces. If a future edit introduces a string like {@code "open: {"} inside
   * {@code recordEvent}, the counter will misalign and this test will need a real
   * tokenizer (or the contract should be re-stated as a regex check on the file).
   */
  private static int findMethodEnd(final String source, final int bodyStart) {
    int depth = 1;
    for (int i = bodyStart; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '{') {
        depth++;
      }
      else if (c == '}') {
        depth--;
        if (depth == 0) {
          return i + 1;
        }
      }
    }
    throw new IllegalStateException("Method body not closed in DAO source");
  }
}
