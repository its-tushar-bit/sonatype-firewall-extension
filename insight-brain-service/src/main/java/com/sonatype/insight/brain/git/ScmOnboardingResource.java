/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.OnboardingOrganization;
import com.sonatype.insight.brain.git.dto.SCMRepositories;
import com.sonatype.insight.brain.git.dto.ValidationResponse;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;

/**
 * This resource supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
@Named
@Timed
@Path(ScmOnboardingResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
public class ScmOnboardingResource
{
  static final String RESOURCE_PATH = "rest/onboarding";

  static final String LOAD_REPO_PATH = "loadRepositories";

  static final String IMPORT_REPO_PATH = "importRepositories/{orgId}";

  static final String DEFAULT_HOST_URL = "defaultHostUrl";

  static final String VALIDATE_SCM_HOST_URL = "validate/{scmProvider}";

  static final String ORGANIZATIONS = "organizations";

  private final ScmOnboardingService scmOnboardingService;

  @Inject
  public ScmOnboardingResource(final ScmOnboardingService scmOnboardingService) {
    this.scmOnboardingService = scmOnboardingService;
  }

  @Path(LOAD_REPO_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public SCMRepositories loadRepositories(
      @QueryParam("orgId") String orgId,
      @QueryParam("defaultHostUrl") String defaultHostUrl)
      throws IOException
  {
    return scmOnboardingService.loadRepositories(orgId, defaultHostUrl);
  }

  @Path(DEFAULT_HOST_URL)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public Map<String, String> getDefaultHostUrl(
      @QueryParam("provider") String provider,
      @QueryParam("orgId") String orgId)
  {
    return ImmutableMap.of("defaultHostUrl", scmOnboardingService.getDefaultHostUrl(provider, orgId));
  }

  @Path(IMPORT_REPO_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ImportResults importRepositories(
      @PathParam("orgId") String orgId,
      final ImportRepositoriesRequest importReposRequest)
  {
    return scmOnboardingService.importRepositories(orgId, importReposRequest);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(VALIDATE_SCM_HOST_URL)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ValidationResponse validateScmHostUrl(
      @PathParam("scmProvider") String scmProvider,
      @QueryParam("scmHostUrl") String scmHostUrl)
  {
    return scmOnboardingService.validateScmHostUrl(scmProvider, scmHostUrl);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(ORGANIZATIONS)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public List<OnboardingOrganization> getOrgsForOnboarding() {
    return scmOnboardingService.getOrgsForOnboarding();
  }
}
