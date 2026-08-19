/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CopyrightOverrideTest
{
  @Test
  public void testIsUserCreated_True() {
    CopyrightOverride copyrightOverride = new CopyrightOverride();
    assertThat(copyrightOverride.getOriginalContentHash()).isNull();

    assertThat(copyrightOverride.isUserCreated()).isTrue();
  }

  @Test
  public void testIsUserCreated_False() {
    CopyrightOverride copyrightOverride = new CopyrightOverride("originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, "componentCopyrightId");

    assertThat(copyrightOverride.isUserCreated()).isFalse();
  }
}
