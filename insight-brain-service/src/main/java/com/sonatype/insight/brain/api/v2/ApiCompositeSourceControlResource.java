/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.79
 */
@Named
@Timed
@Path(value = PublicApiPaths.COMPOSITE_SOURCE_CONTROL_PATH_V2)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
@Tag(name = "Composite Source Control",
    description = "Use this REST API to access the composite source control management configuration (SCM) for " +
        "an application or organization." +
        "\n" +
        "\n" +
        "Composite source control configuration is defined as the configuration that is " +
        "inherited from the parent organization or is directly assigned.")
public class ApiCompositeSourceControlResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  /* paths are package private for use in tests */
  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  private final ApiCompositeSourceControlService apiCompositeSourceControlService;

  @Inject
  public ApiCompositeSourceControlResource(
      final ApiCompositeSourceControlService apiCompositeSourceControlService)
  {
    this.apiCompositeSourceControlService = apiCompositeSourceControlService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @Operation(description = "Use this method to retrieve the composite source control management (SCM) configuration " +
      "settings." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The response contains values for the SCM configuration. For each value, the " +
                "corresponding parent value will be shown, if applicable." +
                "<ul>" +
                "<li><code>id</code> is the internal identifier for the SCM configuration.</li>" +
                "<li><code>ownerId</code> is the identifier for the ownerType specified.</li>" +
                "<li><code>repositoryUrl</code> indicates the URL of application/organization. " +
                "Will indicate 'null' for organizations.</li>" +
                "<li><code>provider</code> is the name of the source code host for the parent. Values can be " +
                "Azure, GitHub, GitLab and Bitbucket.</li>" +
                "<li><code>username</code> is returned if found for the specific provider. Currently, the values " +
                "are available for Bitbucket Server and Bitbucket Cloud.</li>" +
                "<li><code>token</code> is obfuscated and indicates the composite configuration for the " +
                "source control host." +
                "<li><code>baseBranch</code> shows the base branch name." +
                "<li><code>remediationPullRequestsEnabled</code> indicates if the Automated Pull Request feature " +
                "is enabled.</li>" +
                "<li><code>statusChecksEnabled</code> indicates if the status checks for the source code are " +
                "enabled.</li>" +
                "<li><code>pullRequestCommentingEnabled</code> indicates if PR commenting is enabled for " +
                "this application/organization.</li>" +
                "<li><code>sourceControlEvaluationsEnabled</code> indicates if the evaluations triggered by the IQ " +
                "Server are enabled, for the Continuous Risk Profile feature.</li>" +
                "<li><code>sshEnabled</code> indicates if ssh settings are enabled.</li>" +
                "<li><code>commitStatusEnabled</code> indicates if commit status check is enabled.</li>",
            useReturnTypeSchema = true)
      })
  public ApiCompositeSourceControlDTO getCompositeSourceControlByOwner(
      @Parameter(description = "Select the ownerType of the entity (organization or application) for which you want " +
          "to retrieve the composite source control configuration settings.",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id of the application or organization for which you want to retrieve the " +
          "composite source control configuration settings",
          required = true) @PathParam("internalOwnerId") String internalOwnerId)
  {
    return apiCompositeSourceControlService.getCompositeSourceControlByOwner(ownerType, internalOwnerId);
  }
}
