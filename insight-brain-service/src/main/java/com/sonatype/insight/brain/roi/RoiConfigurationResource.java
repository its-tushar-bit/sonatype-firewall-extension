/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.roi.dtos.RoiConfigurationDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(RoiConfigurationResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.ROI_CONFIGURATION)
public class RoiConfigurationResource
{
  public static final String RESOURCE_PATH = "rest/roiConfiguration";

  public static final String ROI_CONFIGURATION_CURRENCY_PATH = "/currencyType/{currencyType}";

  public static final String ROI_CONFIGURATION_DEFAULT_VALUES_PATH = "/defaultValues/currencyType/{currencyType}";

  private RoiConfigurationService roiConfigurationService;

  @Inject
  public RoiConfigurationResource(RoiConfigurationService roiConfigurationService) {
    this.roiConfigurationService = roiConfigurationService;
  }

  @GET
  @Path(ROI_CONFIGURATION_CURRENCY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public RoiConfigurationCurrentAndMinimumValuesDTO getCurrentAndMinimumValuesByCurrencyType(
      @PathParam("currencyType") String currencyType)
  {
    return roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType(currencyType);
  }

  @POST
  @Path(ROI_CONFIGURATION_DEFAULT_VALUES_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.ROI_CONFIG_CREATE)
  public RoiConfigurationCurrentAndMinimumValuesDTO restoreToDefaultValuesByCurrencyType(
      @PathParam("currencyType") String currencyType)
  {
    return roiConfigurationService.restoreToDefaultValuesByCurrencyType(currencyType);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.ROI_CONFIG_CREATE)
  public RoiConfigurationCurrentAndMinimumValuesDTO saveRoiConfiguration(RoiConfigurationDTO roiConfigurationDTO) {
    return roiConfigurationService.saveRoiConfiguration(roiConfigurationDTO);
  }
}
