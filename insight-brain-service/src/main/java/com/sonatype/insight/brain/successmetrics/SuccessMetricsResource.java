/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
