package com.sonatype.insight.brain.testing.example

import com.sonatype.insight.brain.service.InsightBrainService
import com.sonatype.insight.brain.service.InsightConfig

import com.google.common.io.Resources
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule
import org.junit.ClassRule
import org.junit.rules.TestRule
import spock.lang.Shared
import spock.lang.Specification


class ClmServerRunnerSpec extends Specification
{
  // Locates config in target/test-classes/config-test.yml .  May need to execute maven to copy it before running in IDE.
  // TODO Uses default port.  Will want that to be configurable to find unused port when running in CI.
  @Shared
  @ClassRule
  TestRule testRule = new DropwizardServiceRule<InsightConfig>(InsightBrainService.class,
    Resources.getResource('config-test.yml').getPath())

  // OR: 
  //   - reuse the work for setting up a service for testing in insight-brain-service
  //   - invoke the service from CLI as a customer would
  def "service is started"() {
    expect:
      true
  }
}
