/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.test;

import com.sonatype.insight.brain.service.MultiTenantBrainServiceTestHelper;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IntegrationTestRunListener allows running IQ server integration tests against MTIQ server
 */
public class IntegrationTestRunListener extends RunListener
{
  private static final Logger log = LoggerFactory.getLogger(IntegrationTestRunListener.class);

  @Override
  public void testRunStarted(Description description) throws Exception {
    log.info("Running MultiTenantBrainServiceTestHelper setup");
    MultiTenantBrainServiceTestHelper.setup();
    super.testRunStarted(description);
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    log.info("Running MultiTenantBrainServiceTestHelper stop");
    MultiTenantBrainServiceTestHelper.stop();
    super.testRunFinished(result);
  }
}
