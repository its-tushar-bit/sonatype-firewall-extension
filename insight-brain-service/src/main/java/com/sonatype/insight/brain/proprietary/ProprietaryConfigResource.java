/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Named("proprietaryConfigResource")
@Timed
@Path(ProprietaryConfigResource.RESOURCE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProprietaryConfigResource
{
  public static final String RESOURCE_PATH = "rest/proprietary/{ownerType: application|organization}/{ownerId}";

  public static final String ADD_FILE_PATH_REGEX = "add";

  private final ProprietaryConfigService proprietaryConfigService;

  @Inject
  public ProprietaryConfigResource(ProprietaryConfigService proprietaryConfigService) {
    this.proprietaryConfigService = proprietaryConfigService;
  }

  @GET
  public ProprietaryConfigHierarchy getProprietaryConfigHierarchy(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    return proprietaryConfigService.getProprietaryConfigHierarchy(ownerType, ownerId);
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS)
  public ProprietaryConfig upsertProprietaryConfig(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      ProprietaryConfig proprietaryConfig)
  {
    return proprietaryConfigService.upsertProprietaryConfig(ownerType, ownerId, proprietaryConfig);
  }

  @POST
  @Path(ADD_FILE_PATH_REGEX)
  @Audited(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS)
  public ProprietaryConfig addFilePathRegexToProprietaryConfig(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      final FilePathRegex filePathRegex)
  {
    return proprietaryConfigService.addFilePathRegexToProprietaryConfig(ownerType, ownerId, filePathRegex);
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

    public ProprietaryConfigByOwner(
        String ownerId,
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
