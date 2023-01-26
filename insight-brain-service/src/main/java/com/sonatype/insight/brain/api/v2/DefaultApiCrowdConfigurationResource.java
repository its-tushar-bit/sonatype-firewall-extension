/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCrowdConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(value = PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Crowd")
public class DefaultApiCrowdConfigurationResource
    implements ApiCrowdConfigurationResourceV2
{
  public static final String TEST_PATH = "test";

  private final ApiCrowdConfigurationService apiCrowdConfigurationService;

  @Inject
  public DefaultApiCrowdConfigurationResource(ApiCrowdConfigurationService apiCrowdConfigurationService) {
    this.apiCrowdConfigurationService = apiCrowdConfigurationService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiCrowdConfigurationDTO getCrowdConfiguration() {
    checkCrowdEnabled();
    return apiCrowdConfigurationService.getCrowdConfiguration();
  }

  @Override
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_CROWD)
  public void insertOrUpdateCrowdConfiguration(ApiCrowdConfigurationDTO crowdConfiguration) {
    checkCrowdEnabled();
    apiCrowdConfigurationService.insertOrUpdateCrowdConfiguration(crowdConfiguration);
  }

  @Override
  @DELETE
  @Audited(AuditEvent.DELETE_CROWD)
  public void deleteCrowdConfiguration() {
    checkCrowdEnabled();
    apiCrowdConfigurationService.deleteCrowdConfiguration();
  }

  @Override
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(TEST_PATH)
  public ApiStatusDTO testCrowdConfiguration(ApiCrowdConfigurationDTO dto) {
    checkCrowdEnabled();
    return apiCrowdConfigurationService.testCrowdConfiguration(dto);
  }

  private void checkCrowdEnabled() {
    if (!SystemConfigurationPropertyFeature.CROWD_INTEGRATION.isEnabled()) {
      throw new NotAuthorizedException(
          SystemConfigurationPropertyFeature.CROWD_INTEGRATION.getId() + " feature is disabled.");
    }
  }
}
