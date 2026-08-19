/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import com.sonatype.insight.brain.model.OwnerType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApplicableContextTest
{
  private ApplicableContext applicableContext;

  @BeforeEach
  public void setUp() {
    applicableContext = new ApplicableContext();
  }

  @Test
  public void testSetTypeBad() {
    assertSetTypeBad(OwnerType.GLOBAL);
    assertSetTypeBad(null);
  }

  private void assertSetTypeBad(final OwnerType ownerType) {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> applicableContext.setType(ownerType))
        .withMessage("Unknown context type: " + ownerType);
  }

  @Test
  public void testSetType() {
    applicableContext.setType(OwnerType.APPLICATION);
    applicableContext.setType(OwnerType.ORGANIZATION);
    applicableContext.setType(OwnerType.REPOSITORY);
    applicableContext.setType(OwnerType.REPOSITORY_CONTAINER);
  }
}
