/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collection;

import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.policy.ActionType;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Timed
@Path(ActionTypeResource.RESOURCE_PATH)
public class ActionTypeResource
{
  public static final String RESOURCE_PATH = "rest/policy/actionType";

  private static final Logger log = LoggerFactory.getLogger(ActionTypeResource.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<ActionType> getActionTypes() {
    log.debug("Received request to get all action types");

    return ActionTypes.getAll();
  }
}
