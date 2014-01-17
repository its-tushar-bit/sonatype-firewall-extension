/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.example

import geb.spock.GebReportingSpec
import org.junit.ClassRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import spock.lang.Shared

class JunitRuleSpec extends GebReportingSpec
{
  @Shared
  @ClassRule
  TestRule classRule = new ConsoleMessageRule("Custom message")
  
  def setup() {
    browser.config.baseUrl = "http://sonatype.com/clm/overview"
  }
  
  def "class rule is run"() {
    expect:
      true
  }

  def ".com site promotes CLM"() {
    when:
      go() // to the base url

    then:
      assert title.startsWith("CLM")
  }

  static class ConsoleMessageRule implements TestRule {
    String message = "Default message"

    ConsoleMessageRule() {
    }

    ConsoleMessageRule(String message) {
      this.message = message
    }

    @Override
    Statement apply(Statement base, Description description) {
      return new Statement() {
        @Override
        void evaluate() {
          println message
          base.evaluate()
        }
      }
    }
  }
}
