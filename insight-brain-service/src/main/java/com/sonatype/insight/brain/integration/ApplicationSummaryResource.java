/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application rest resource for integration with other tools such as Sonar
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(ApplicationSummaryResource.RESOURCE_PATH)
public class ApplicationSummaryResource
{
  public static final String RESOURCE_PATH = "rest/integration/applications";

  static final String VERIFY_OR_CREATE_APPLICATION_PATH = "verifyOrCreate/{applicationPublicId}";

  static final String VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH = "/verifyOrCreateForContainerImageFirewall";

  private static final Logger log = LoggerFactory.getLogger(ApplicationSummaryResource.class);

  private final ApplicationSummaryService applicationSummaryService;

  private final RepositoryDAO repositoryDAO;

  private final ApplicationForContainerImageFirewallService applicationForContainerImageFirewallService;

  @Inject
  public ApplicationSummaryResource(
      final ApplicationSummaryService applicationSummaryService,
      final RepositoryDAO repositoryDAO,
      final ApplicationForContainerImageFirewallService applicationForContainerImageFirewallService)
  {
    this.applicationSummaryService = applicationSummaryService;
    this.repositoryDAO = repositoryDAO;
    this.applicationForContainerImageFirewallService = applicationForContainerImageFirewallService;
  }

  /**
   * Gets all applications for which the current user has permissions required for the specified goal, sorted
   * by (case-insensitive) name and filtered by the given organization Id.
   *
   * @param goal The goal for getting the list of applications. Defaults to READ permission for backward compatibility
   *          (Jenkins/Hudson plugin <= 2.12.1, Bamboo plugin <=1.0.0, Eclipse plugin <= 2.8.0, SonarQube plugin <=
   *          1.0.2, Nexus plugins <= 3.0.0).
   * @param organizationId The organization Id for getting the list of applications. If null or empty, no filtering
   *          is applied
   * @param applicationPublicIds The set of public ids to filter on (cannot be null)
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationSummaryList getApplications(
      @QueryParam("goal") Goal goal,
      @QueryParam("organizationId") String organizationId,
      @QueryParam("applicationPublicIds") final Set<String> applicationPublicIds)
  {
    log.debug("Received request to get applications for goal {} and organization id {}", goal, organizationId);
    return applicationSummaryService.getApplications(goal, organizationId, applicationPublicIds);
  }

  /**
   * Verifies if the user can access the application identified by applicationPublicId for the specified goal.
   * If an application with the specified applicationPublicId already exists, then the method checks access for the
   * current user and the specified goal to that application.
   * If such an application does not exist and automatic application creation is enabled, then the method creates the
   * new application and returns true to indicate the application will now be available.
   *
   * @param applicationPublicId public shared id
   * @param goal {@link Goal}
   * @param request {@link HttpServletRequest}
   * @return true if false otherwise.
   */
  @POST
  @Path(VERIFY_OR_CREATE_APPLICATION_PATH)
  @Produces("text/plain")
  public boolean verifyOrCreateApplication(
      @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("goal") Goal goal,
      @QueryParam("organizationId") String organizationId,
      @Context HttpServletRequest request)
  {
    log.debug("Received request to verify access for or create application with public ID {} and goal {}.",
        applicationPublicId, goal);
    return applicationSummaryService.verifyOrCreateApplication(applicationPublicId, organizationId, goal,
        HdsClient.getClientUserAgent(request));
  }

  /**
   * @param apiVerifyOrCreateApplicationForContainerImageFirewallDTO
   *          {@link ApiVerifyOrCreateApplicationForContainerImageFirewallDTO}
   * @return applicationPublicId
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path(VERIFY_OR_CREATE_APP_FOR_CONTAINER_IMAGE_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
  @HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
  @Produces("text/plain")
  public String verifyOrCreateApplicationForContainerImage(
      ApiVerifyOrCreateApplicationForContainerImageFirewallDTO apiVerifyOrCreateApplicationForContainerImageFirewallDTO)
  {
    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(
            apiVerifyOrCreateApplicationForContainerImageFirewallDTO.getRepositoryManagerInstanceId(),
            apiVerifyOrCreateApplicationForContainerImageFirewallDTO.getRepositoryPublicId());

    return applicationForContainerImageFirewallService.verifyOrCreateApplicationForContainerImage(repository,
        apiVerifyOrCreateApplicationForContainerImageFirewallDTO);
  }
}
