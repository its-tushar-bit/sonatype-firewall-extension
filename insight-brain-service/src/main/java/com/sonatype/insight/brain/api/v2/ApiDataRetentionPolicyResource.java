/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.service.ApiDataRetentionPolicyService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.63
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH)
@Tag(name = "Data Retention Policies",
    description = "Set policies for automatic purging of obsolete application " +
        "and Success Metrics reports. " +
        "<p>" +
        "Note that IQ Server has a preset limit of purging 5000 reports in one execution of its report purging job."
)
@ProductLicenseEnforcementPoint(LicensedFeature.DATA_RETENTION)
public class ApiDataRetentionPolicyResource
{
  static final String ORGANIZATION_PATH = "organizations/{organizationId}";

  static final String PARENT_ORGANIZATION_PATH = "organizations/{organizationId}/parent";

  private final ApiDataRetentionPolicyService dataRetentionService;

  @Inject
  public ApiDataRetentionPolicyResource(ApiDataRetentionPolicyService dataRetentionService) {
    this.dataRetentionService = dataRetentionService;
  }

  @GET
  @Path(ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Data retention policies help to limit the disk space consumption by removing obsolete data. " +
          "Use this method to inspect the retention policies that are in effect for an organization. " +
          "Application reports created by continuous monitoring are not affected by the stage retention policy. " +
          "They appear separately under the key continuous-monitoring in the response JSON" +
          "<p>" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description =
                  "The response JSON contains the policy settings for both applicationReports and successMetrics. " +
                      "Policy settings for application reports are shown for each stage of development. " +
                      "<ul>" +
                      "<li>inheritPolicy IS a boolean flag indicating whether the policy is inherited from a " +
                      "parent organization.</li>" +
                      "<li>enablePurging IS a boolean flag indicating if automatic purging is enabled or disabled.</li>"
                      + "<li>maxCount IS the maximum no. of reports to retain.</li>" +
                      "<li>maxAge IS the maximum age that a report is allowed to reach before it is purged. " +
                      "Possible values are days, weeks, months, years.</li>" +
                      "</ul>" +
                      "The latest application report is always retained, regardless of its age.",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiDataRetentionPoliciesDTO getDataRetentionPolicies(
      @Parameter(description = "The organizationId assigned by IQ Server. Use the organization REST API to " +
          "retrieve the organizationId.", required = true) @PathParam("organizationId") String organizationId)
  {
    return dataRetentionService.getDataRetentionPolicies(organizationId);
  }

  @GET
  @Path(PARENT_ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Data retention policies help to limit the disk space consumption by removing obsolete data. " +
          "Use this method to inspect the retention policies that are in effect for the parent organization of the " +
          "given organization. " +
          "Application reports created by continuous monitoring are not affected by the stage retention policy. " +
          "They appear separately under the key continuous-monitoring." +
          "<p>" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description =
                  "The response JSON contains the policy settings for both applicationReports and successMetrics. " +
                      "Policy settings for application reports are shown for each stage of development. " +
                      "<ul>" +
                      "<li>inheritPolicy IS a boolean flag indicating whether the policy is inherited from a " +
                      "parent organization.</li>" +
                      "<li>enablePurging IS a boolean flag indicating if automatic purging is enabled or disabled.</li>"
                      + "<li>maxCount IS the maximum no. of reports to retain.</li>" +
                      "<li>maxAge IS the maximum age that a report is allowed to reach before it is purged. " +
                      "Possible values are days, weeks, months, years.</li>" +
                      "</ul>" +
                      "The latest application report is always retained, regardless of its age.",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiDataRetentionPoliciesDTO getParentDataRetentionPolicies(
      @Parameter(description = "The organizationId assigned by IQ Server. Use the organization REST API to " +
          "retrieve the parent organizationId",
          required = true) @PathParam("organizationId") String organizationId)
  {
    return dataRetentionService.getParentDataRetentionPolicies(organizationId);
  }

  @PUT
  @Path(ORGANIZATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_DATA_RETENTION)
  @Operation(
      description = "Data retention policies help to limit the disk space consumption by removing obsolete data. " +
          "Use this method to set the retention policies for an organization. " +
          "Application reports created by continuous monitoring are not affected by the stage retention policy. " +
          "They appear separately under the key continuous-monitoring." +
          "<p>" +
          "Permissions required: Edit IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The data retention policy has been set successfully.",
              useReturnTypeSchema = true
          )
      }
  )
  public void setDataRetentionPolicies(
      @Parameter(description =
          "The organizationId for the organization you want to set the data retention policy. Use the " +
              "organization REST API to " +
              "retrieve the organizationId.", required = true) @PathParam("organizationId") String organizationId,
      @RequestBody(
          description =
              "The request JSON should include the retention policy settings for both application reports and" +
                  " success metrics." +
                  "\n" +
                  "\n" +
                  "Policy settings for application reports can be specified for each stage of " +
                  "development represented " +
                  "in the example below by additionalProp1. " +
                  "\n" +
                  "Example values for additionalProp1 are develop, build, stage-release, release, operate &" +
                  " continuous monitoring. " +
                  "For application reports created during continuous monitoring use the key continuous-monitoring " +
                  "instead of the stage name. " +
                  "<ul>" +
                  "<li>inheritPolicy IS a boolean flag indicating whether the policy is inherited from a " +
                  "parent organization.</li>" +
                  "<li>enablePurging IS a boolean flag indicating enabled or disabled status for " +
                  "automatic purging. </li>" +
                  "<li>maxCount IS the maximum no. of reports to retain.</li>" +
                  "<li>maxAge IS the maximum age that a report is allowed to reach before it is purged. " +
                  "Possible values are days, weeks, months, years.</li>" +
                  "</ul>",
          required = true) ApiDataRetentionPoliciesDTO dto)
  {
    AuditData.get().setData("dataRetentionPolicies", dto);
    dataRetentionService.setDataRetentionPolicies(organizationId, dto);
  }
}
