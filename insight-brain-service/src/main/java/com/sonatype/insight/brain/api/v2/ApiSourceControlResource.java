/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.sourcecontrol.ApiSourceControlRepositoryUserDTO;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.git.ScmUserMatchingService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.66
 */
@Named
@Timed
@Tag(name = "Source Control",
    description = "Use this REST API to:" +
        "<ul>" +
        "<li>Create, update and delete source control management (SCM) configuration for the root organization, " +
        "sub-organizations and applications.</li>" +
        "<li>Automatically assign the developer role to all contributors of the associated repository, who are " +
        "registered IQ users.</li>" +
        "</ul>")
@Path(value = PublicApiPaths.SOURCE_CONTROL_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
public class ApiSourceControlResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  /* paths are package private for use in tests */
  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  static final String AUTOMATIC_ROLE_ASSIGNMENT_PATH = "/automaticRoleAssignment/{publicId}";

  private final ApiSourceControlService sourceControlService;

  private final ScmUserMatchingService scmUserMatchingService;

  @Inject
  public ApiSourceControlResource(
      final ApiSourceControlService apiSourceControlService,
      final ScmUserMatchingService scmUserMatchingService)
  {
    this.sourceControlService = apiSourceControlService;
    this.scmUserMatchingService = scmUserMatchingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to retrieve the source control configuration settings for an " +
      "organization or an application." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains source control configuration settings for the specified ownerId." +
                  "\n" +
                  "\n" +
                  "<ul>" +
                  "<li><code>id</code> is the owner internal ID.</li>" +
                  "<li><code>repositoryUrl</code> indicates the http(s) and ssh urls for the application " +
                  "specified in the ownerId.</li>" +
                  "<li><code>username</code> is retrieved if available on the SCM system, e.g. for Bitbucket Server " +
                  "and Cloud.</li>" +
                  "<li><code>provider</code> indicates the name of the SCM system.</li>" +
                  "<li><code>baseBranch</code> indicates the name of the last selected branch.</li>" +
                  "<li><code>enablePullRequests</code> has been deprecated in version 124.</li>" +
                  "<li><code>remediationPullRequestsEnabled</code> indicates if the Automated Pull Requests " +
                  "feature is enabled.</li>" +
                  "<li><code>enableStatusChecks</code> has been deprecated in version 124.</li>" +
                  "<li><code>statusChecksEnabled</code> is an internal field.</li>" +
                  "<li><code>pullRequestCommentingEnabled</code> indicates if the Pull Request Commenting feature is " +
                  "enabled.</li>" +
                  "<li><code>sourceControlEvaluationsEnabled</code> indicates if the source control evaluations are " +
                  "enabled for the continuous risk profile feature.</li>" +
                  "<li><code>sourceControlScanTarget</code> indicates the path inside the repository.</li>" +
                  "<li><code>sshEnabled</code> indicates if ssh is enabled.</li>" +
                  "<li><code>commitStatusEnabled</code> indicates if interaction with the commit statuses " +
                  "on the SCM system is enabled.</li>" +
                  "</ul>",
              useReturnTypeSchema = true
          )
      })
  public ApiSourceControlDTO getSourceControl(
      @Parameter(description = "Enter the value for ownerType.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the value for internal ownerId. Use ROOT_ORGANIZATION_ID for the root " +
          "organization")
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    return sourceControlService.getSourceControlByOwner(ownerType, internalOwnerId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_SOURCE_CONTROL)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to create a source control configuration setting." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The Source Control Management (SCM) settings have been created successfully.",
              useReturnTypeSchema = true
          )
      })
  public ApiSourceControlDTO addSourceControl(
      @Parameter(description = "Enter the value for ownerType.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the value for internal ownerId. Use ROOT_ORGANIZATION_ID for root " +
          "organization.", required = true)
      @PathParam("internalOwnerId") String internalOwnerId,
      @RequestBody(
          description = "Specify the SCM settings for the ownerId specified above in the request JSON." +
              "<ul>" +
              "<li><code>id</code> is the internal owner ID.</li>" +
              "<li><code>repositoryUrl</code> is the http(s) and ssh urls for the application specified in the " +
              "ownerId.</li>" +
              "<li><code>username</code> is optional, can be provided for Bitbucket Server and Cloud.</li>" +
              "<li><code>token</code> is optional,if inherited. If provided, this value will override the value " +
              "inherited from the root organization, organization or application level." +
              "<li><code>provider</code> is the name of of the SCM system. Allowed values are <code>azure</code>, " +
              "<code>github</code>, <code>gitlab</code>, and <code>bitbucket</code>.</li>" +
              "<li><code>baseBranch</code> is required for the root organization. Organizations and applications " +
              "inherit from the root unless overridden.</li>" +
              "<li><code>enablePullRequests</code> has been deprecated in version 124.</li>" +
              "<li><code>remediationPullRequestsEnabled</code> is optional. Set it to `true` to enable " +
              "the Automated Pull Requests.</li>" +
              "<li><code>enableStatusChecks</code> has been deprecated in version 124.</li>" +
              "<li><code>statusChecksEnabled</code> is an internal field.</li>" +
              "<li><code>pullRequestCommentingEnabled</code> is optional. Set it to `true` to enable the " +
              " Pull Request Commenting feature.</li>" +
              "<li><code>sourceControlEvaluationsEnabled</code> is set to `true` to enable source control " +
              "evaluations for the continuous risk profile feature.</li>" +
              "<li><code>sourceControlScanTarget</code> is the path inside the repository.</li>" +
              "<li><code>sshEnabled</code> is set to `true` to enable ssh.</li>" +
              "<li><code>commitStatusEnabled</code> is set to `true` if interaction with the commit statuses" +
              " on the SCM is enabled.</li>" +
              "</ul>"
      )
      ApiSourceControlDTO sourceControl)
  {
    return sourceControlService.addSourceControlByOwner(ownerType, internalOwnerId, sourceControl);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_SOURCE_CONTROL)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to update an existing SCM setting." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The SCM settings have been updated successfully. The JSON returned shows the " +
                  "updated values.",
              useReturnTypeSchema = true
          )
      })
  public ApiSourceControlDTO updateSourceControl(
      @Parameter(description = "Enter the value for ownerType.")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the internal ownerId. Use ROOT_ORGANIZATION_ID for the root organization.")
      @PathParam("internalOwnerId") String internalOwnerId,
      @RequestBody(
          description = "Specify the SCM settings for the ownerId specified above in the request JSON." +
              "<ul>" +
              "<li><code>id</code> is the internal owner ID.</li>" +
              "<li><code>repositoryUrl</code> is the http(s) and ssh urls for the application specified in the " +
              "ownerId.</li>" +
              "<li><code>username</code> is optional, can be provided for Bitbucket Server and Cloud.</li>" +
              "<li><code>token</code> is optional if inherited. If provided, this value will override the value " +
              "inherited from the root organization, organization or application level." +
              "<li><code>provider</code> is the name of of the SCM system. Allowed values are <code>azure</code>, " +
              "<code>github</code>, <code>gitlab</code>, and <code>bitbucket</code>.</li>" +
              "<li><code>baseBranch</code> is required for the root organization. Organizations and applications " +
              "inherit from the root unless overridden.</li>" +
              "<li><code>enablePullRequests</code> has been deprecated in version 124.</li>" +
              "<li><code>remediationPullRequestsEnabled</code> is optional. Set it to `true` to enable " +
              "the Automated Pull Requests.</li>" +
              "<li><code>enableStatusChecks</code> has been deprecated in version 124.</li>" +
              "<li><code>statusChecksEnabled</code> is an internal field.</li>" +
              "<li><code>pullRequestCommentingEnabled</code> is optional. Set it to `true` to enable the " +
              " Pull Request Commenting feature.</li>" +
              "<li><code>sourceControlEvaluationsEnabled</code> is set to `true` to enable source control " +
              "evaluations for the continuous risk profile feature.</li>" +
              "<li><code>sourceControlScanTarget</code> is the path inside the repository.</li>" +
              "<li><code>sshEnabled</code> is set to `true` to enable ssh.</li>" +
              "<li><code>commitStatusEnabled</code> is set to `true` if interaction with the commit statuses" +
              " on the SCM is enabled.</li>" +
              "</ul>"
      )
      ApiSourceControlDTO sourceControl)
  {
    return sourceControlService.updateSourceControlByOwner(ownerType, internalOwnerId, sourceControl);
  }

  @DELETE
  @Path(BY_OWNER)
  @Audited(AuditEvent.DELETE_SOURCE_CONTROL)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(description = "Use this method to delete a SCM setting for the specified ownerType/ownerId." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "The SCM setting for the specified ownerType/ownerId has been successfully deleted.",
              useReturnTypeSchema = true)
      }
  )
  public void deleteSourceControl(
      @Parameter(description = "Enter the value for ownerType.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the value for internal ownerId.", required = true)
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    sourceControlService.deleteSourceControlByOwner(ownerType, internalOwnerId);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.AUTO_CREATE_SOURCE_CONTROL)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Hidden
  public ApiSourceControlDTO addOrUpdateSourceControl(
      @QueryParam("publicId") final String publicId,
      @QueryParam("repositoryUrl") final String repositoryUrl,
      final ApiSourceControlRepositoryUserDTO apiSourceControlRepoUserDTO)
  {
    return sourceControlService.addOrUpdateSourceControl(publicId,
        repositoryUrl, apiSourceControlRepoUserDTO);
  }

  @POST
  @Path(AUTOMATIC_ROLE_ASSIGNMENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.GRANT_ROLE_MEMBERSHIP)
  @Operation(description = "Use this method to automatically grant the 'developer' role to all contributors of " +
      "a repository on a given application." +
      "\n" +
      "\n" +
      "Prerequisites for automatic role assignment are:" +
      "<ol>" +
      "<li>SCM configuration for the application and authentication token should exist.</li>" +
      "<li>The contributors to the repository should have a matching username in IQ.</li>" +
      "</ol>" +
      "\n" +
      "\n" +
      "Permissions required: Edit access control on the application.",
      responses = {
          @ApiResponse(responseCode = "204",
              description =
                  "The 'developer' role has automatically been assigned to all contributors of the repository, who " +
                      "have a matching username configured in IQ Server." +
                      "\n" +
                      "\n" +
                      "The response contains all usernames that were successfully granted the 'developer' role " +
                      "on the given application.",
              useReturnTypeSchema = true)
      }
  )
  public Set<String> automaticRoleAssignment(
      @Parameter(description = "Enter the public applicationId for automatic role assignment.", required = true)
      @PathParam("publicId") String publicId)
  {
    return scmUserMatchingService.automaticRoleAssignment(publicId);
  }
}
