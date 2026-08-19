/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.repository.ContainerImageSummaryDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(ContainerImageReportResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
@HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
public class ContainerImageReportResource
{
  static final String SUMMARY = "/containerImageReportSummary";

  static final String RESOURCE_PATH = "rest/firewall/container-images/repositories/{repositoryId}/report";

  private final ContainerImageReportService containerImageReportService;

  @Inject
  public ContainerImageReportResource(final ContainerImageReportService containerImageReportService) {
    this.containerImageReportService = containerImageReportService;
  }

  @GET
  @Path(SUMMARY)
  @Produces(MediaType.APPLICATION_JSON)
  public ContainerImageSummaryDTO getContainerImagesSummary(@PathParam("repositoryId") String repositoryId) {
    return containerImageReportService.getContainerImagesSummary(repositoryId);
  }
}
