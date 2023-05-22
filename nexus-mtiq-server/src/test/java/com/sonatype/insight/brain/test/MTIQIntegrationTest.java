/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.test;

import com.sonatype.insight.brain.api.v2.ApiFirewallResourceTest;
import com.sonatype.insight.brain.api.v2.ApiRoleResourceTest;
import com.sonatype.insight.brain.service.MultiTenantBrainServiceTestHelper;
import com.sonatype.insight.brain.service.MultiTenantBrainServiceTestService;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for manually running the insight-brain-service integration tests with MTIQ server.
 * To test an insight-brain-server integration test with mtiq add the class into the run list
 * bellow if all tests are passing as expected the test class can be added into the
 * nexus-mtiq-server failsafe includes block.
 */
public class MTIQIntegrationTest
{
  private static final Logger log = LoggerFactory.getLogger(MTIQIntegrationTest.class);

  public static void main(String[] args) {
    JUnitCore junit = new JUnitCore();
    junit.addListener(new TextListener(System.out));

    MultiTenantBrainServiceTestHelper.setup();

    Result result = junit.run(
        // Include insight-brain-server integration tests here to test with MTIQ.
        ApiFirewallResourceTest.class,
        ApiRoleResourceTest.class
    );

    MultiTenantBrainServiceTestService.stop();

    log.info("Total number of tests {}", result.getRunCount());
    log.info("Total number of tests failed {}", result.getFailureCount());
    for (Failure failure : result.getFailures())
    {
      log.error("Test Failed: {} {}", failure.getDescription(), failure.getMessage());
    }
    log.info("Success: {}", result.wasSuccessful());
    System.exit(0);
  }
}
