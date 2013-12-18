/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import org.junit.Test;

import static org.junit.Assert.fail;

public class PolicyMonitoringTest
{
  @Test
  public void testInvalidStageTypeId_Constructor() {
    try {
      new PolicyMonitoring("fakeOwnerId", "fakeStageTypeId");
      fail("Expected InvalidStageException");
    }
    catch (InvalidStageException expected) {
      if (!expected.getMessage().equals("Invalid stage id=fakeStageTypeId")) {
        throw expected;
      }
    }
  }

  @Test
  public void testInvalidStageTypeId_Setter() {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring();
    try {
      policyMonitoring.setStageTypeId("fakeStageTypeId");
      fail("Expected InvalidStageException");
    }
    catch (InvalidStageException expected) {
      if (!expected.getMessage().equals("Invalid stage id=fakeStageTypeId")) {
        throw expected;
      }
    }
  }
}
