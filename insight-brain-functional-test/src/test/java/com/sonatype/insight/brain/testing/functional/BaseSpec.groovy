/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.service.InsightConfig
import com.sonatype.insight.brain.service.TestInsightBrainService

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared

abstract class BaseSpec extends GebReportingSpec {
  static {
    System.setProperty("javax.net.ssl.trustStore", "src/test/resources/ssl/server-store");
  }

  @Shared
  @ClassRule
  TestRule serviceRule = new DropwizardServiceRule<InsightConfig>(TestInsightBrainService.class,
  Resources.getResource('config-test.yml').getPath())
  
  def setupSpec() {
    // Use port as reported by service under test since it's not known until runtime.
    System.setProperty("geb.build.baseUrl", "http://localhost:" + serviceRule.getLocalPort() + "/")
  }
}