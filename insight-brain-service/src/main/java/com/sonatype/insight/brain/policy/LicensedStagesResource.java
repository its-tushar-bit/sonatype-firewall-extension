/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.StageType;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.13
 */
@Named
@Timed
@Path(LicensedStagesResource.RESOURCE_PATH)
public class LicensedStagesResource
{
  public static final String RESOURCE_PATH = "rest/policy/stages";

  private static final Logger log = LoggerFactory.getLogger(LicensedStagesResource.class);

  private final StageTypeService stageTypeService;

  @Inject
  public LicensedStagesResource(final StageTypeService stageTypeService) {
    this.stageTypeService = stageTypeService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<Stage> getStageTypes(
      @QueryParam("context") @DefaultValue(StageTypeService.CLI_CONTEXT) final String context)
  {
    log.debug("Received request to get licensed stages for context {}", context);

    Collection<Stage> stages = new ArrayList<>();
    Collection<StageType> stageTypes = stageTypeService.getLicensedStageTypes(context);
    for (StageType stageType : stageTypes) {
      stages.add(new Stage(stageType.getId(), stageType.getName()));
    }
    return stages;
  }
}
