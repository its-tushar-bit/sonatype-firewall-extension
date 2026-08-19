/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentObligationAttributionTest
{
  @Test
  public void testHasRelatedObligation_False() {
    ComponentObligationAttribution componentObligationAttribution = new ComponentObligationAttribution();
    assertThat(componentObligationAttribution.getObligationName()).isNull();

    assertThat(componentObligationAttribution.hasRelatedObligation()).isFalse();
  }

  @Test
  public void testHasRelatedObligation_True() {
    ComponentObligationAttribution componentObligationAttribution = new ComponentObligationAttribution();
    componentObligationAttribution.setObligationName("name");

    assertThat(componentObligationAttribution.hasRelatedObligation()).isTrue();
  }
}
