/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.OwnerType;

@Named
@Path(ProprietaryConfigResource.RESOURCE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProprietaryConfigResource
{
  public static final String RESOURCE_PATH = "rest/proprietary/{ownerType: application|organization}/{publicOwnerId}";

  public static final String ADD_FILE_PATH_REGEX = "add";

  private final ProprietaryConfigService proprietaryConfigService;

  @Inject
  public ProprietaryConfigResource(ProprietaryConfigService proprietaryConfigService) {
    this.proprietaryConfigService = proprietaryConfigService;
  }

  @GET
  public ProprietaryConfigHierarchy getProprietaryConfigHierarchy(@PathParam("ownerType") final OwnerType ownerType,
                                                                  @PathParam("publicOwnerId")
                                                                  final String publicOwnerId)
  {
    return proprietaryConfigService.getProprietaryConfigHierarchy(ownerType, publicOwnerId);
  }

  @PUT
  public ProprietaryConfig upsertProprietaryConfig(@PathParam("ownerType") final OwnerType ownerType,
                                                   @PathParam("publicOwnerId") final String publicOwnerId,
                                                   ProprietaryConfig proprietaryConfig)
  {
    return proprietaryConfigService.upsertProprietaryConfig(ownerType, publicOwnerId, proprietaryConfig);
  }

  @POST
  @Path(ADD_FILE_PATH_REGEX)
  public ProprietaryConfig addFilePathRegexToProprietaryConfig(@PathParam("ownerType") final OwnerType ownerType,
                                                               @PathParam("publicOwnerId") final String publicOwnerId,
                                                               final FilePathRegex filePathRegex)
  {
    return proprietaryConfigService.addFilePathRegexToProprietaryConfig(ownerType, publicOwnerId, filePathRegex);
  }

  public static class FilePathRegex
  {
    public List<String> paths;

    public String regex;
  }

  public static class ProprietaryConfigHierarchy
  {
    public List<ProprietaryConfigByOwner> proprietaryConfigByOwners = new ArrayList<>();
  }

  public static class ProprietaryConfigByOwner
  {
    public String ownerId;

    public String ownerName;

    public OwnerType ownerType;

    public ProprietaryConfig proprietaryConfig;

    public ProprietaryConfigByOwner() {
    }

    public ProprietaryConfigByOwner(String ownerId,
                                    String ownerName,
                                    OwnerType ownerType,
                                    ProprietaryConfig proprietaryConfig)
    {
      this.ownerId = ownerId;
      this.ownerName = ownerName;
      this.ownerType = ownerType;
      this.proprietaryConfig = proprietaryConfig;
    }
  }
}
