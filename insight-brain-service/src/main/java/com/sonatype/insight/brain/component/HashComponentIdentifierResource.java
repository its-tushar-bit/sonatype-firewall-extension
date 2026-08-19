/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
