/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collection;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Path(StageTypeResource.SERVICE_PATH)
public class StageTypeResource
{
  public static final String SERVICE_PATH = "rest/policy/stageType";

  private static final Logger log = LoggerFactory.getLogger(StageTypeResource.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<StageType> getStageTypes() {
    log.debug("Received request to get all stage types");

    return StageTypes.getAll();
  }
}
