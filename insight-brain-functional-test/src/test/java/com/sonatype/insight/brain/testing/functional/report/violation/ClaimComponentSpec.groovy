/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.report.violation

import spock.lang.Ignore
import spock.lang.Stepwise

/**
 * @since 1.11
 */
@Stepwise
@Ignore //https://sonatype.atlassian.net/browse/CLM-30530
class ClaimComponentSpec
extends AbstractClaimComponentSpec {
  @Override
  String getReportId() {
    // The reportId must match the reportId value recorded inside the test report.zip used for this test
    return '4e11026275d0446eb94aef0174756976'
  }

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
