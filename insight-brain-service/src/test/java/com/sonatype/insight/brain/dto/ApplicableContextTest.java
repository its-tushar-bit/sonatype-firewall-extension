/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import com.sonatype.insight.brain.model.OwnerType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ApplicableContextTest
{
  private ApplicableContext applicableContext;

  @Before
  public void setUp() {
     applicableContext = new ApplicableContext();
  }

  @Test
  public void testSetTypeBad() throws Exception {
    assertSetTypeBad(OwnerType.GLOBAL);
    assertSetTypeBad(null);
  }

  private void assertSetTypeBad(final OwnerType ownerType) {
    try {
      applicableContext.setType(ownerType);
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("Unknown context type: " + ownerType, e.getMessage());
    }
  }

  @Test
  public void testSetType() throws Exception {
    applicableContext.setType(OwnerType.APPLICATION);
    applicableContext.setType(OwnerType.ORGANIZATION);
    applicableContext.setType(OwnerType.REPOSITORY);
    applicableContext.setType(OwnerType.REPOSITORY_CONTAINER);
  }
}
