/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositoryComponentTest
{
  @Test
  public void testIsQuarantined() throws Exception {
    final RepositoryComponent component = new RepositoryComponent();
    assertFalse(component.isQuarantined());

    final Date now = new Date();
    component.setQuarantineTime(now);
    assertTrue("Only 'QuarantineTime' == quarantined.", component.isQuarantined());

    component.setUnquarantineTime(now);
    assertFalse("Both 'Un/QuarantineTime' != quarantined.", component.isQuarantined());
  }
}
