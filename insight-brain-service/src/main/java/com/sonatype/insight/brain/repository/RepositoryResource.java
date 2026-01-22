/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.AbstractResourceWithIcon;
import com.sonatype.insight.brain.organization.RobotImageService;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @since 1.18.0
 */
@Named
@Timed
@Path(RepositoryResource.RESOURCE_PATH)
public class RepositoryResource
    extends AbstractResourceWithIcon
{
  public static final String RESOURCE_PATH = "rest/repositories";

  static final String REPOSITORY_PATH = "{repositoryId}";

  static final String EVALUATE_PATH = REPOSITORY_PATH + "/evaluate";

  static final String UNQUARANTINE_PATH = REPOSITORY_PATH + "/unquarantine/{pathname: .+}";

  static final String EVALUATE_COMPONENT_PATH = EVALUATE_PATH + "/{hash}";

  static final String POLICY_EVALUATION_TIMESTAMPS_PATH = REPOSITORY_PATH + "/policyEvaluationTimestamps";

  static final String POLICY_VIOLATIONS_PATH = REPOSITORY_PATH + "/policyViolations/{pathname: .+}";

  static final String POLICY_VIOLATION_PATH = REPOSITORY_PATH + "/policyViolation/{repositoryPolicyViolationId}";

  static final String PROPRIETARY_COMPONENT_NAME_PATTERN_UPDATE_PATH = "proprietaryComponentNamePatterns/update";

  static final String UNCONFIGURED_REPOSITORY_MANAGERS_PATH = "repositoryManager/unconfigured";

  static final String REPOSITORIES_PATH = "repositoryManager/{repositoryManagerId}/repositories";

  static final String CONFIGURE_REPOSITORIES_PATH = "repositoryManager/{repositoryManagerId}/configureRepositories";

  static final String CONFIGURE_FIREWALL_ONBOARDING_PATH = "configureFirewallOnboarding";

  static final String UPDATE_REPOSITORY_MANAGER_NAME_PATH = "repositoryManager/{repositoryManagerId}/{name}";

  static final String PROPRIETARY_COMPONENT_NAME_PATTERN_BY_OWNER_PATH =
      "{ownerType: repository_container|repository_manager|repository}/{ownerId}/proprietaryComponentNamePatterns";

  public static final String REPOSITORY_MANAGER_ICON_PATH = ICON_PATH + "/repositoryManager/{repositoryManagerId}";

  private final InsightWork work;

  private RepositoryService repositoryService;

  @Inject
  public RepositoryResource(
      final RepositoryService repositoryService,
      final BaseUrl baseUrl,
      final RobotImageService robotImageService,
      final NgUploadResponseGenerator ngUploadResponseGenerator,
      final InsightWork work)
  {
    super(baseUrl, ngUploadResponseGenerator, robotImageService);
    this.repositoryService = repositoryService;
    this.work = work;
  }

  /**
   * @since 1.19.0
   */
  @POST
  @Path(UNQUARANTINE_PATH)
  @Audited(AuditEvent.RELEASE_QUARANTINE)
  public void unquarantineComponent(@PathParam("repositoryId") final String repositoryId,
                                    @PathParam("pathname") final String pathname,
                                    @Context final HttpServletRequest request)
  {
    repositoryService.unquarantineComponent(repositoryId, pathname, HdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.19.0
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public RepositoriesDTO getRepositories() {
    return repositoryService.getRepositories();
  }

  @GET
  @Path(REPOSITORY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public RepositoryDTO getRepository(@PathParam("repositoryId") String repositoryId) {
    return repositoryService.getRepositoryById(repositoryId);
  }

  /**
   * @since 1.19.0
   */
  @DELETE
  @Path(REPOSITORY_PATH)
  @Audited(AuditEvent.REMOVE_REPOSITORY)
  public void deleteRepository(@PathParam("repositoryId") String repositoryId) {
    repositoryService.deleteRepository(repositoryId);
  }

  @POST
  @Path(EVALUATE_PATH)
  @Audited(AuditEvent.INITIATE_EVALUATE_REPOSITORY)
  public void reevaluateRepository(@PathParam("repositoryId") String repositoryId) {
    repositoryService.reevaluateRepository(repositoryId);
  }

  @POST
  @Path(EVALUATE_COMPONENT_PATH)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public void reevaluateComponent(@PathParam("repositoryId") String repositoryId,
                                  @PathParam("hash") String componentHash,
                                  @Context final HttpServletRequest request)
  {
    repositoryService.reevaluateComponent(repositoryId, componentHash, HdsClient.getClientUserAgent(request));
  }

  /**
   * Used by the web UI to display various timestamps related to policy evaluations.
   * The UI calls this method for component versions for which it only has a component identifier (no hash or pathname).
   * 
   * @since 1.139
   */
  @GET
  @Path(POLICY_EVALUATION_TIMESTAMPS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyEvaluationTimestampsDTO getPolicyEvaluationTimestamps(
      @PathParam("repositoryId") String repositoryId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier)
  {
    return repositoryService.getPolicyEvaluationTimestamps(repositoryId, componentIdentifier);
  }

  /**
   * @since 1.143
   */
  @GET
  @Path(POLICY_VIOLATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public List<RepositoryPolicyViolationDTO> getPolicyViolations(
      @PathParam("repositoryId") String repositoryId,
      @PathParam("pathname") String pathname)
  {
    return repositoryService.getPolicyViolations(repositoryId, pathname);
  }

  /**
   * @since 1.146
   */
  @GET
  @Path(POLICY_VIOLATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public RepositoryPolicyViolationDTO getPolicyViolation(
      @PathParam("repositoryId") String repositoryId,
      @PathParam("repositoryPolicyViolationId") String repositoryPolicyViolationId)
  {
    return repositoryService.getPolicyViolation(repositoryId, repositoryPolicyViolationId);
  }

  /**
   * @since 1.157
   */
  @POST
  @Path(PROPRIETARY_COMPONENT_NAME_PATTERN_UPDATE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public void updateProprietaryComponentNamePattern(
      ProprietaryComponentNamePatternDTO proprietaryComponentNamePatternDTO)
  {
    repositoryService.updateProprietaryComponentNamePattern(proprietaryComponentNamePatternDTO);
  }

  /**
   * @since 1.160
   */
  @GET
  @Path(UNCONFIGURED_REPOSITORY_MANAGERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public List<RepositoryManager> getUnconfiguredRepositoryManagers() {
    return repositoryService.getUnconfiguredRepositoryManagers();
  }

  /**
   * @since 1.160
   */
  @GET
  @Path(REPOSITORIES_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public RepositoriesDTO getRepositoriesByRepositoryManagerId(
      @PathParam("repositoryManagerId") String repositoryManagerId)
  {
    return repositoryService.getRepositoriesByRepositoryManagerId(repositoryManagerId);
  }

  /**
   * @since 1.161
   */
  @PUT
  @Path(CONFIGURE_REPOSITORIES_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY)
  @Timed
  public void configureRepositories(
      @PathParam("repositoryManagerId") String repositoryManagerId,
      List<Repository> repositories)
  {
    repositoryService.configureRepositories(repositoryManagerId, repositories);
  }

  /**
   * @since 1.164
   */
  @PUT
  @Path(CONFIGURE_FIREWALL_ONBOARDING_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY)
  @Timed
  public void configureFirewallOnboarding(
      FirewallOnboardingOptionsDTO firewallOnboardingOptionsDTO)
  {
    repositoryService.configureFirewallOnboarding(firewallOnboardingOptionsDTO);
  }

  @PUT
  @Path(UPDATE_REPOSITORY_MANAGER_NAME_PATH)
  @Audited(AuditEvent.UPDATE_REPOSITORY_MANAGER)
  public void updateName(
          @PathParam("repositoryManagerId") String repositoryManagerId,
          @PathParam("name") String name)
  {
    repositoryService.updateName(repositoryManagerId, name);
  }

  /**
   * @since 1.170
   */
  @POST
  @Path(PROPRIETARY_COMPONENT_NAME_PATTERN_BY_OWNER_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ProprietaryComponentNamePatternsPage getProprietaryComponentNamePatternsByOwner(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      ProprietaryComponentNamePatternRequest request)
  {
    return repositoryService.getProprietaryComponentNamePatternsByOwner(ownerType, ownerId, request);
  }

  /**
   * @since 1.174
   */
  @Override
  @GET
  @Path(GENERATE_ICON_PATH)
  @Produces("image/png")
  public Response generateIcon(@PathParam("hashcode") final String hashcode) {
    return super.generateIcon(hashcode);
  }

  /**
   * @since 1.174
   */
  @GET
  @Path(REPOSITORY_MANAGER_ICON_PATH)
  @Produces("image/png")
  @Authorize(permission = Permission.READ)
  public Response getIcon(
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) @PathParam("repositoryManagerId")
          String repositoryManagerId) throws IOException
  {
    return super.getIcon(repositoryManagerId, work.getRepositoryManagerIconDir());
  }

  /**
   * @since 1.174
   */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
  @Path(REPOSITORY_MANAGER_ICON_PATH)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY_MANAGER_ICON)
  public Response setIcon(
      @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
      @Context HttpHeaders headers,
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) @PathParam("repositoryManagerId") String repositoryManagerId,
      @FormDataParam("hasRobotSource") boolean hasRobotSource,
      @FormDataParam("hashcode") String hashcode,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @QueryParam("noFormData") boolean noFormData) throws Exception
  {
    return super.setIcon(repositoryManagerId, work.getRepositoryManagerIconDir(), hasRobotSource, hashcode,
        uploadedInputStream,
        fileDetail, csrfToken, headers, noFormData);
  }

  @Override
  protected String getDefaultIconFilename(String ownerId) {
    return "defaulticon_repository_manager.png";
  }
}
