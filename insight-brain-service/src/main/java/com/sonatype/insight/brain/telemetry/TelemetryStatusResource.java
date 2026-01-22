/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.collections4.CollectionUtils;

@Named
@Timed
@Path(TelemetryStatusResource.TELEMETRY_STATUS_PATH)
public class TelemetryStatusResource
{
  public static final String TELEMETRY_STATUS_PATH = "rest/telemetry/status";

  private final ApplicationDAO applicationDAO;

  private final ApplicationService applicationService;

  private final Configuration configuration;

  private final ProductLicense productLicense;

  private final TelemetryId telemetryId;

  @Inject
  public TelemetryStatusResource(
      ApplicationDAO applicationDAO,
      ApplicationService applicationService,
      Configuration configuration,
      ProductLicense productLicense,
      TelemetryId telemetryId)
  {
    this.applicationDAO = applicationDAO;
    this.applicationService = applicationService;
    this.configuration = configuration;
    this.productLicense = productLicense;
    this.telemetryId = telemetryId;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TelemetryStatusDTO getTelemetryStatus() {
    final var telemetryIdStr = telemetryId.getId();
    final var clusterId = telemetryId.getClusterId();
    final var advancedReportingEnabled = configuration.getAdvanceReportingInsightsEnabled();
    final var enterpriseReportingFeatureExists =
        productLicense.hasFeature(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    final var userApps = applicationService.getApplications();
    final var userAppCount = CollectionUtils.isNotEmpty(userApps) ? userApps.size() : 0;
    final var totalAppCount = applicationDAO.getCount();

    return new TelemetryStatusDTO(
        telemetryIdStr,
        clusterId,
        advancedReportingEnabled,
        enterpriseReportingFeatureExists,
        userAppCount,
        totalAppCount);
  }

  public record TelemetryStatusDTO(
      String telemetryId,
      String clusterId,
      boolean advancedReportingEnabled,
      boolean enterpriseReportingFeatureExists,
      int userApplicationCount,
      long totalApplicationCount)
  {
  }
}
