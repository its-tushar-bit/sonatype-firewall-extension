/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.enterprise.reporting;

public class EnterpriseReportingDashboardFilterDTO
{
  public String id;

  public String name;

  public String filter; //JSON string returned by Looker

  public Boolean isDefault;

  public EnterpriseReportingDashboardFilterDTO() {
    //for jackson;
  }

  public EnterpriseReportingDashboardFilterDTO(final String id,
                            final String name,
                            final String filter,
                            final Boolean isDefault)
  {
    this.id = id;
    this.name = name;
    this.filter = filter;
    this.isDefault = isDefault;
  }
}
