/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.55
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(SuccessMetricsResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SUCCESS_METRICS)
public class SuccessMetricsResource
{
  public static final String RESOURCE_PATH = "rest/successMetrics";

  private final SuccessMetricsService successMetricsService;

  @Inject
  public SuccessMetricsResource(SuccessMetricsService successMetricsService) {
    this.successMetricsService = successMetricsService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SuccessMetricsConfigurationDTO get() {
    return successMetricsService.get();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_SUCCESS_METRICS)
  public SuccessMetricsConfigurationDTO update(SuccessMetricsConfigurationDTO configuration) {
    return successMetricsService.update(configuration);
  }
}
