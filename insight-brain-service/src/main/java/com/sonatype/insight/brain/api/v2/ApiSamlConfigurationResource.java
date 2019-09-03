/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @since 1.72
 */
@Named
@Timed
@Path(value = PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2)
public class ApiSamlConfigurationResource
{
  private final ApiSamlConfigurationService apiSamlConfigurationService;

  @Inject
  public ApiSamlConfigurationResource(ApiSamlConfigurationService apiSamlConfigurationService) {
    this.apiSamlConfigurationService = apiSamlConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiSamlConfigurationResponseDTO getSamlConfiguration() {
    return apiSamlConfigurationService.getSamlConfiguration();
  }

  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Audited(AuditEvent.CONFIGURE_SAML)
  public void insertOrUpdateSamlConfiguration(
      @FormDataParam("identityProviderXml") String identityProviderXml,
      @FormDataParam("samlConfiguration") ApiSamlConfigurationDTO samlConfiguration)
  {
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(identityProviderXml, samlConfiguration);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_SAML)
  public void deleteSamlConfiguration() {
    apiSamlConfigurationService.deleteSamlConfiguration();
  }
}
