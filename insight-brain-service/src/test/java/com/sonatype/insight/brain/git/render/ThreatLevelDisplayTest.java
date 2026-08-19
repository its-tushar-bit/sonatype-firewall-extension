/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import com.sonatype.insight.brain.utils.ThreatLevel;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class ThreatLevelDisplayTest
{
  private static final String RED_LINK = "https://cdn.sonatype.com/iq-for-scm/1.0/red-bar.png";

  private static final String ORANGE_LINK = "https://cdn.sonatype.com/iq-for-scm/1.0/orange-bar.png";

  private static final String YELLOW_LINK = "https://cdn.sonatype.com/iq-for-scm/1.0/yellow-bar.png";

  private static final String DARK_BLUE_LINK = "https://cdn.sonatype.com/iq-for-scm/1.0/dark-blue-bar.png";

  @Test
  public void testFromValue_critical() {
    runFromValueTest(CRITICAL, RED_LINK, 8, 9, 10);
  }

  @Test
  public void testFromValue_severe() {
    runFromValueTest(SEVERE, ORANGE_LINK, 4, 5, 6, 7);
  }

  @Test
  public void testFromValue_moderate() {
    runFromValueTest(MODERATE, YELLOW_LINK, 2, 3);
  }

  @Test
  public void testFromValue_low() {
    runFromValueTest(LOW, DARK_BLUE_LINK, 0, 1);
  }

  private static void runFromValueTest(
      final ThreatLevel expectedThreatLevel,
      final String expectedColorLink,
      final int... threatLevelNumbers)
  {
    stream(threatLevelNumbers)
        .forEach(threatLevelNum -> {
          final ThreatLevelDisplay tld = ThreatLevelDisplay.fromValue(threatLevelNum);
          assertThat(tld.getImage().getAlt()).isEqualTo(expectedThreatLevel.getDisplayName());
          assertThat(tld.getValue()).isEqualTo(threatLevelNum);
          assertThat(tld.getImage().getSrc()).isEqualTo(expectedColorLink);
        });
  }
}
