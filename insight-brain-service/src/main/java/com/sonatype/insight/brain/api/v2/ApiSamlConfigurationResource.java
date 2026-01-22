/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @since 1.72
 */
@Named
@Timed
@Path(value = PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config SAML",
    description = "Use this REST API to manage the SAML configuration for IQ Server.")
public class ApiSamlConfigurationResource
{
  public static final String METADATA = "metadata";

  private final ApiSamlConfigurationService apiSamlConfigurationService;

  @Inject
  public ApiSamlConfigurationResource(ApiSamlConfigurationService apiSamlConfigurationService) {
    this.apiSamlConfigurationService = apiSamlConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to inspect the SAML configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "404",
              description = "SAML is not configured."
          ),
          @ApiResponse(
              responseCode = "200",
              description = "The response contains:" +
                  "<ul>" +
                  "<li>`identityProviderName` the name of the Identity Provider that is displayed on the login page " +
                  "when SAML is configured.</li>" +
                  "<li>`entityId` is the URI that IQ Server uses to identify itself in requests to the SSO" +
                  "service.</li>" +
                  "<li>`firstNameAttribute` is the SAML attribute that IQ Server extracts from the login " +
                  "response of the identity provider and uses as the user's first name.</li>" +
                  "<li>`lastNameAttribute` is the SAML attribute that IQ Server extracts from the login " +
                  "response of the identity provider and uses as the user's last name.</li>" +
                  "<li>`emailAttributeName` is the SAML attribute that IQ Server extracts from the login " +
                  "response of the identity provider to determine the user's email address.</li>" +
                  "<li>`usernameAttributeName` is the SAML attribute that IQ Server extracts from the login " +
                  "response of the identity provider to determine the username or id.</li>" +
                  "<li>`groupAttributeName` is the SAML attribute that IQ Server extracts from the login " +
                  "response of the identity provider to determine the groups the user belongs to.</li>" +
                  "<li>`validateResponseSignature` indicates whether the SAML responses from the identity provider  " +
                  "are cryptographically signed. A `null` value indicates that this setting is derived from the SAML " +
                  "metadata from the identity provider performing signature validation if a signing key " +
                  "(`KeyDescriptor`) is included." +
                  "<li>`validateAssertionSignature` indicates whether the SAML assertions from the identity provider " +
                  " are cryptographically signed. A `null` value indicates that this setting is derived from  " +
                  "the SAML metadata from the identity provider performing signature validation if a signing key " +
                  "(`KeyDescriptor`) is included.</li>" +
                  "<li>`identityProviderMetadataXml` is the metadata of the identity provider.</li>" +
                  "</ul>",
              useReturnTypeSchema = true)
      }
  )
  public ApiSamlConfigurationResponseDTO getSamlConfiguration() {
    return apiSamlConfigurationService.getSamlConfiguration();
  }

  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Audited(AuditEvent.CONFIGURE_SAML)
  @Operation(description = "Use this method to enable SSO using SAML. This request uses the content type " +
      "multipart/form-data to transmit the configuration to IQ Server." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(
              responseCode = "400",
              description = "Invalid configuration."
          ),
          @ApiResponse(
              responseCode = "204",
              description = "Configuration successful."
          )
      }
  )
  public void insertOrUpdateSamlConfiguration(
      @Parameter(description = "Enter the SAML metadata XML of your IdP. Refer to the IdP documentation to obtain " +
          "this metadata.", schema = @Schema(type = "string", format = "binary"), required = true)
      @FormDataParam("identityProviderXml") String identityProviderXml,
      @Parameter(required = true)
      @FormDataParam("samlConfiguration") ApiSamlConfigurationDTO samlConfiguration)
  {
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(identityProviderXml, samlConfiguration);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_SAML)
  @Operation(description = "Use this method to delete the SAML configuration." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users.",
      responses = {
          @ApiResponse(
              responseCode = "204",
              description = "Successfully deleted the SAML configuration."
          ),
          @ApiResponse(
              responseCode = "404",
              description = "No SAML configuration found."
          )
      })
  public void deleteSamlConfiguration() {
    apiSamlConfigurationService.deleteSamlConfiguration();
  }

  @GET
  @Path(METADATA)
  @Produces(MediaType.APPLICATION_XML)
  @Operation(description = "Use this method to retrieve IQ Server's metadata service provider descriptor." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(
              responseCode = "404",
              description = "SAML is not configured."
          ),
          @ApiResponse(
              responseCode = "200",
              description = "The IQ Server's metadata service provider descriptor in XML format.",
              useReturnTypeSchema = true
          )
      }
  )
  public String getMetadata() {
    return apiSamlConfigurationService.getMetadata();
  }
}
