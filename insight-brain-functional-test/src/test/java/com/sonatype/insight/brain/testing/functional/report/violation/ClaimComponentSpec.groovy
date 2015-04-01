/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
class ClaimComponentSpec
extends AbstractClaimComponentSpec {

  @Override
  String getReportPath() {
    return '/canned-reports/report-with-unknown.zip'
  }

  @Override
  String getExpectedDisplayNameString() {
    return 'testG : testA : testV'
  }

  @Override
  String getExpectedUpdatedDisplayNameString() {
    return 'testG : testA : testV-NEW'
  }

  @Override
  String getExpectedHash() {
    return 'b60f7aea3e3bf7247e14'
  }
}
