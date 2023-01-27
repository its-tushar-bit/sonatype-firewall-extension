/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlConfigValidatorService;
import com.sonatype.insight.brain.git.ConfigurationValidationResult;

import com.codahale.metrics.annotation.Timed;

/**
 * Provides an endpoint for the SCM Validator Service to perform basic validation on a given configuration
 *
 * @since 1.96
 */
@Named
@Timed
@Path(value = PublicApiPaths.COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2)
public class DefaultApiCompositeSourceControlConfigValidatorResource implements
    ApiCompositeSourceControlConfigValidatorResource
{
  private final ApiCompositeSourceControlConfigValidatorService service;

  @Inject
  public DefaultApiCompositeSourceControlConfigValidatorResource(
      ApiCompositeSourceControlConfigValidatorService service)
  {
    this.service = service;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ConfigurationValidationResult validateSourceControlConfig(
      @PathParam("applicationId") String applicationId)
  {
    return service.validateSourceControlConfig(applicationId);
  }
}
