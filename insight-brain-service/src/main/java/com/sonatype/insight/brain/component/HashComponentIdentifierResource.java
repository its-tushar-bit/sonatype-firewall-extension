/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;

import com.codahale.metrics.annotation.Timed;

/**
 * Associates component hash to a component identifier.
 *
 * @since 1.4.1
 */
@Named
@Timed
@Path(HashComponentIdentifierResource.RESOURCE_PATH)
public class HashComponentIdentifierResource
{
  public static final String RESOURCE_PATH = "rest/component/identified";

  private final HashComponentIdentifierService hashComponentIdentifierService;

  @Inject
  public HashComponentIdentifierResource(final HashComponentIdentifierService hashComponentIdentifierService) {
    this.hashComponentIdentifierService = hashComponentIdentifierService;
  }

  /**
   * @since 1.64
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{hash}")
  public HashComponentIdentifierDTO get(@PathParam("hash") final String hash) {
    return hashComponentIdentifierService.get(hash);
  }

  /**
   * @since 1.4.1
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SET_COMPONENT_IDENTITY)
  public HashComponentIdentifierDTO set(final HashComponentIdentifier hashComponentIdentifier) {
    return hashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SET_COMPONENT_IDENTITY)
  public HashComponentIdentifierDTO update(final HashComponentIdentifier hashComponentIdentifier) {
    return hashComponentIdentifierService.update(hashComponentIdentifier);
  }

  @DELETE
  @Path("{hash}")
  @Audited(AuditEvent.UNSET_COMPONENT_IDENTITY)
  public void delete(@PathParam("hash") final String hash) {
    hashComponentIdentifierService.delete(hash);
  }
}
