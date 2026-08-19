/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryIdentifiedComponentService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * @since 1.137
 */
@Named
@Timed
@Path(value = PublicApiPaths.REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.INNER_SOURCE_REPOSITORIES)
public class ApiRepositoryIdentifiedComponentResource
{
  private final ApiRepositoryIdentifiedComponentService repositoryIdentifiedComponentService;

  @Inject
  public ApiRepositoryIdentifiedComponentResource(
      ApiRepositoryIdentifiedComponentService repositoryIdentifiedComponentService)
  {
    this.repositoryIdentifiedComponentService = repositoryIdentifiedComponentService;
  }

  @DELETE
  @Audited(AuditEvent.DELETE_REPOSITORY_IDENTIFIED_COMPONENT)
  @Hidden
  public void deleteRepositoryIdentifiedComponent(
      @QueryParam("hash") String hash,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") String packageUrl)
  {
    checkBuiltFromSourceEnabled();
    repositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(hash, componentIdentifier, packageUrl);
  }

  /**
   * Since 156
   */
  @DELETE
  @Path("/clear")
  @Audited(AuditEvent.PURGE_REPOSITORY_IDENTIFIED_COMPONENTS)
  @Hidden
  public void deleteAllRepositoryIdentifiedComponents() {
    checkBuiltFromSourceEnabled();
    repositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();
  }

  private void checkBuiltFromSourceEnabled() {
    if (!SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      throw new NotAuthorizedException(SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.getId()
          + " feature is disabled");
    }
  }
}
