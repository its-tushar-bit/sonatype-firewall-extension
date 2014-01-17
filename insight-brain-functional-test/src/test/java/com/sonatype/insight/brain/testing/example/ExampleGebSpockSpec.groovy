/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.example;

import geb.spock.GebReportingSpec

class ExampleGebSpockSpec extends GebReportingSpec {
  def setup() {
    browser.config.baseUrl = "http://sonatype.com/clm/overview"
  }

  def ".com site promotes CLM"() {
    when:
      go() // to the base url

    then:
      assert title.startsWith("CLM")
  }
}
