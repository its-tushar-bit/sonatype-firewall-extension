/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

/**
 * @since 1.11
 */
class DashboardNavigationSpec
    extends BaseSpec
{

  public def "Can navigate directly to page with url"() {
    when:
      loginAsAdminVia(DashboardOverviewPage, tableName)

    then:
      waitFor { noDataAvailable.displayed }

    where:
      tableName << ['newest-risk', 'components', 'applications']
  }

}