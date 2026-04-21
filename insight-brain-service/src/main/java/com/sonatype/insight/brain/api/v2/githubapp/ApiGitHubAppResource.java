/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.githubapp;

import java.io.IOException;
import java.net.URI;

import com.sonatype.insight.brain.api.v2.dto.githubapp.ApiGitHubAppManifestDTO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.githubapp.ApiGitHubAppService;
import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Timed
@Path(PublicApiPaths.GITHUB_APP_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "GitHub App Configuration", description = "GitHub App configuration operations")
public class ApiGitHubAppResource
{
  private static final Logger log = LoggerFactory.getLogger(ApiGitHubAppResource.class);

  private final ApiGitHubAppService apiGitHubAppService;

  private final BaseUrl baseUrl;

  private final OwnerDAO ownerDAO;

  @Inject
  public ApiGitHubAppResource(
      final ApiGitHubAppService apiGitHubAppService,
      final BaseUrl baseUrl,
      final OwnerDAO ownerDAO)
  {
    this.apiGitHubAppService = apiGitHubAppService;
    this.baseUrl = baseUrl;
    this.ownerDAO = ownerDAO;
  }

  @POST
  @Path("manifest")
  @Operation(
      summary = "Generate GitHub App manifest",
      description = "Generate a GitHub App manifest for registration. " +
          "Returns manifest JSON with a state token for CSRF protection. " +
          "The state token is cryptographically secure, single-use, and expires after 10 minutes. " +
          "Submit the manifest to GitHub's app creation flow, which will redirect back to IQ Server " +
          "with the state token for validation. " +
          "\n\n" +
          "**Permissions Required:** Configure System Configuration and Users",
      tags = {"GitHub App"})
  @ApiResponse(responseCode = "200", description = "Manifest generated successfully")
  @ApiResponse(responseCode = "400", description = "Missing owner id or organization name")
  @ApiResponse(responseCode = "500", description = "Base URL not configured or invalid request")
  public ApiGitHubAppManifestDTO generateManifest(
      @Parameter(description = "Owner (organization/application) ID",
          required = true) @QueryParam("ownerId") @NotBlank final String ownerId,
      @Parameter(description = "GitHub organization name",
          required = false) @QueryParam("organizationName") final String organizationName)
  {
    return apiGitHubAppService.generateManifest(ownerId, organizationName);
  }

  @GET
  @Path("redirect")
  @Operation(
      summary = "GitHub App registration redirect callback",
      description = "Handles redirect from GitHub after manifest submission. " +
          "Exchanges temporary code for permanent app credentials. " +
          "The user's browser is automatically redirected here by GitHub - not intended for direct use. " +
          "**Permissions Required:** Configure System Configuration and Users",
      tags = {"GitHub App"})
  @ApiResponse(responseCode = "303", description = "Successfully registered GitHub App, redirecting to settings page")
  @ApiResponse(responseCode = "400", description = "Invalid or expired code")
  @ApiResponse(responseCode = "401", description = "Authentication required")
  @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  public Response handleRedirect(
      @Parameter(description = "Temporary manifest conversion code from GitHub",
          required = true) @QueryParam("code") final String code,
      @Parameter(
          description = "OAuth state token for CSRF protection") @QueryParam("state") final String state) throws Exception
  {
    final String installUrl = apiGitHubAppService.handleManifestConversionAndRegistration(code, state);
    return Response.seeOther(URI.create(installUrl)).build();
  }

  @GET
  @Path(PublicApiPaths.GITHUB_APP_SETUP_INSTALLATION_PATH)
  @ApiResponse(responseCode = "302", description = "Redirect to source control configuration page")
  @ApiResponse(responseCode = "400", description = "Invalid or missing parameters")
  @ApiResponse(responseCode = "500", description = "Failed to configure GitHub App installation")
  @Operation(
      summary = "Handle GitHub App installation setup callback with OAuth + PKCE",
      description = "Process the redirect from GitHub after OAuth authorization, " +
          "validate state token, exchange OAuth code with PKCE verification, verify user ownership, " +
          "configure the installation for the specified organization/application, " +
          "and redirect to the configuration page")
  public Response handleInstallationSetup(
      @Parameter(description = "GitHub App installation ID",
          required = true) @QueryParam("installation_id") @NotNull @Min(1) final Long installationId,

      @Parameter(description = "State token for CSRF protection",
          required = true) @QueryParam("state") @NotBlank final String state,

      @Parameter(description = "OAuth authorization code",
          required = true) @QueryParam("code") @NotBlank final String oauthCode) throws IOException
  {
    GitHubApp gitHubApp = apiGitHubAppService.handleInstallationSetupCallback(installationId, state, oauthCode);

    String ownerId = gitHubApp.getOwnerId();
    Owner owner = ownerDAO.getByIdNotNull(ownerId);

    String ownerIdForUrl = owner instanceof Application
        ? ((Application) owner).getPublicId()
        : owner.getId();

    URI uri = baseUrl.redirect()
        .path(UserInterfaceLinksHelper.RESOURCE_PATH)
        .path(UserInterfaceLinksHelper.SOURCE_CONTROL_MANAGEMENT_PATH)
        .queryParam("githubAppId", gitHubApp.getId())
        .build(owner.getType().toString().toLowerCase(), ownerIdForUrl);

    log.info("Redirecting to source control configuration after GitHub App installation: " +
        "ownerId={}, ownerType={}, githubAppId={}", ownerIdForUrl, owner.getType(), gitHubApp.getId());

    return Response.temporaryRedirect(uri).build();
  }
}
