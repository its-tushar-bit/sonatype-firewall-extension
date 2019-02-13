/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
  @SuppressWarnings("checkstyle:LineLength")
  public Collection<Stage> getStageTypes(@QueryParam("context") @DefaultValue(StageTypeService.CLI_CONTEXT) final String context) {
    log.debug("Received request to get licensed stages for context {}", context);

    Collection<Stage> stages = new ArrayList<>();
    Collection<StageType> stageTypes = stageTypeService.getLicensedStageTypes(context);
    for (StageType stageType : stageTypes) {
      stages.add(new Stage(stageType.getId(), stageType.getName()));
    }
    return stages;
  }
}
