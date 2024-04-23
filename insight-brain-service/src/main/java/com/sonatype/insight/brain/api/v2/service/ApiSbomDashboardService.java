/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;

@Named
public class ApiSbomDashboardService
{
  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ProductLicense productLicense;

  @Inject
  public ApiSbomDashboardService(ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO, ProductLicense productLicense) {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.productLicense = productLicense;
  }

  public SbomsAnalyzedMetricsDTO getSbomsAnalyzedMetrics() {
    return new SbomsAnalyzedMetricsDTO(thirdPartySbomMetadataDAO.getActiveSbomCount(), productLicense.getMaxSboms());
  }

  public ApiSbomApplicationsHistoryMetricDTO getApplicationsHistoryMetric() {
    return thirdPartySbomMetadataDAO.getSbomsHistoryMetrics();
  }
}
