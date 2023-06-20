/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
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
