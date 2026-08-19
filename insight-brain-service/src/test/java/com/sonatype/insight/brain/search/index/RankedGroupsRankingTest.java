/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.apache.lucene.util.BytesRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RankedGroupsRankingTest
{
  @Test
  public void compareUtf8_matchesUnsignedByteOrderAndLuceneBytesRef() {
    // Leading byte 0xC3 (>= 0x80): signed Arrays.compare would invert vs Lucene BytesRef.
    String latinSmallAWithGrave = "\u00E0"; // UTF-8 C3 A0
    String asciiTilde = "~"; // UTF-8 7E

    byte[] accented = latinSmallAWithGrave.getBytes(StandardCharsets.UTF_8);
    byte[] tilde = asciiTilde.getBytes(StandardCharsets.UTF_8);
    assertThat(accented[0] & 0xFF).isGreaterThan(0x7F);

    int unsigned = Arrays.compareUnsigned(tilde, accented);
    int signed = Arrays.compare(tilde, accented);
    assertThat(signed).isGreaterThan(0); // signed wrongly puts 0xC3 before 0x7E
    assertThat(unsigned).isLessThan(0);

    assertThat(RankedGroupsRanking.compareUtf8(asciiTilde, latinSmallAWithGrave)).isEqualTo(unsigned);
    assertThat(RankedGroupsRanking.compareUtf8(asciiTilde, latinSmallAWithGrave))
        .isEqualTo(new BytesRef(asciiTilde).compareTo(new BytesRef(latinSmallAWithGrave)));
  }

  @Test
  public void compareMetricThenKey_tieBreakUsesUnsignedUtf8() {
    int cmp = RankedGroupsRanking.compareMetricThenKey(
        7.0f, "~", 7.0f, "\u00E0", false);
    assertThat(cmp).isLessThan(0);
  }

  @Test
  public void bandFor_halfOpenIncludesLowerExcludesUpper() {
    Map<String, float[]> bands = Map.of(
        "medium", new float[]{4.0f, 7.0f},
        "high", new float[]{7.0f, 9.0f});
    assertThat(RankedGroupsRanking.bandFor(bands, 7.0f)).isEqualTo("high");
    assertThat(RankedGroupsRanking.bandFor(bands, 6.9f)).isEqualTo("medium");
    assertThat(RankedGroupsRanking.bandFor(bands, Float.NaN)).isNull();
  }
}
