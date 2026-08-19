/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(MtiqUserResource.PATH)
public class MtiqUserResource
{
  public static final String PATH = "/rest/mtiqUser";

  private final MtiqUserService userService;

  private final MTIQFeatureService featureService;

  @Inject
  public MtiqUserResource(final MtiqUserService userService, final MTIQFeatureService featureService) {
    this.userService = userService;
    this.featureService = featureService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<MtiqUserDTO> getAllMtiqUsers() {
    ensureManaged();
    return userService.getAllUsers();
  }

  @POST
  public void inviteUser(MtiqUserDTO user) {
    ensureManaged();
    userService.inviteUser(user);
  }

  @DELETE
  @Path("/{username}")
  public void deleteUser(@PathParam("username") final String username) {
    ensureManaged();
    userService.deleteByUsername(username);
  }

  private void ensureManaged() {
    if (!featureService.isEnabled(SystemConfigurationPropertyFeature.SSO_IDP_MANAGED_BY_SONATYPE)) {
      throw new BadRequestException("Invalid request for managed idp");
    }
  }
}
