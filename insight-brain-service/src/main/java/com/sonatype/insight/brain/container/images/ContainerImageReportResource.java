/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

  static final String RESOURCE_PATH = "rest/malware-defense/container-images/repositories/{repositoryId}/report";

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
