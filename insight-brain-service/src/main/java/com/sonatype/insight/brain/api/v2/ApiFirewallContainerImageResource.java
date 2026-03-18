/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationServiceV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.ContainerImageInQuarantineData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Singleton
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
@HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
@Path(PublicApiPaths.FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH)
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallContainerImageResource
{
  static final String QUARANTINED_PATH = "policyViolations/quarantined";

  private final ApiPolicyViolationServiceV2 policyViolationService;

  @Inject
  public ApiFirewallContainerImageResource(final ApiPolicyViolationServiceV2 policyViolationService) {
    this.policyViolationService = policyViolationService;
  }

  private class ContainerImageInQuarantineDataResult
      extends ApiPageResult<ContainerImageInQuarantineData>
  {
  }

  @GET
  @Path(QUARANTINED_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to find all container images currently in quarantine." +
      "\n" +
      "\n" +
      "Permissions required: Read",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Container images in quarantine.",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = ContainerImageInQuarantineDataResult.class)),
            headers = {
              @Header(name = "Link",
                  description = "Pagination links (first, last, next, prev)",
                  schema = @Schema(type = "string"))
            })
      })
  public Response getContainerImagesInQuarantine(
      @Context UriInfo uriInfo,
      @QueryParam("page") @Min(1) int page,
      @QueryParam("pageSize") @Min(1) @Max(100) int pageSize)
  {
    ApiPageResult<ContainerImageInQuarantineData> containerImagesInQuarantine =
        policyViolationService.getContainerImagesInQuarantine(page, pageSize);
    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize,
        containerImagesInQuarantine)
            .build();
  }
}
