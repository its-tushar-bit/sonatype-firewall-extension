/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.component.HashComponentIdentifier;

/**
 * Associates component hash to a component identifier.
 *
 * @since 1.4.1
 */
@Named
@Path(HashComponentIdentifierResource.SERVICE_PATH)
public class HashComponentIdentifierResource
{
  public static final String SERVICE_PATH = "rest/component/identified";

  private final HashComponentIdentifierService hashComponentIdentifierService;

  @Inject
  public HashComponentIdentifierResource(final HashComponentIdentifierService hashComponentIdentifierService)
  {
    this.hashComponentIdentifierService = hashComponentIdentifierService;
  }

  /**
   * @since 1.4.1
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashComponentIdentifierDTO set(final HashComponentIdentifier hashComponentIdentifier) throws IOException {
    return hashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HashComponentIdentifierDTO update(final HashComponentIdentifier hashComponentIdentifier) throws IOException {
    return hashComponentIdentifierService.update(hashComponentIdentifier);
  }

  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{hash}")
  public void delete(@PathParam("hash") final String hash) {
    hashComponentIdentifierService.delete(hash);
  }
}
