/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegalFileOverrideTest
{
  @Test
  public void testIsUserCreated_True() {
    assertThat(new LegalFileOverride(null, null, null, null, null).isUserCreated()).isTrue();
    assertThat(new LegalFileOverride(null, "", null, null, null).isUserCreated()).isTrue();
    assertThat(new LegalFileOverride(null, " \t", null, null, null).isUserCreated()).isTrue();
  }

  @Test
  public void testIsUserCreated_False() {
    LegalFileOverride legalFileOverride = new LegalFileOverride("originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, "componentCopyrightId");

    assertThat(legalFileOverride.isUserCreated()).isFalse();
  }
}
