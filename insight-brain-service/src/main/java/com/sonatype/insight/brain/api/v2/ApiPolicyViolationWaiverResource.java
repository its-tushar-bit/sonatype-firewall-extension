/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;

/**
 * @since 1.70
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_VIOLATION_WAIVER_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_WAIVERS)
public class ApiPolicyViolationWaiverResource
{
  private ApiPolicyWaiverService apiPolicyWaiverService;

  @Inject
  public ApiPolicyViolationWaiverResource(final ApiPolicyWaiverService apiPolicyWaiverService) {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
  }

  /**
   * This is currently used in "request waiver"
   *
   * @deprecated Use
   *             {@link ApiPolicyWaiverResource#addPolicyWaiverByPolicyViolationId(OwnerType, String, String, ApiWaiverOptionsDTO)}
   */
  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.CREATE_WAIVER)
  @Deprecated
  @Operation(hidden = true)
  public void addPolicyWaiver(
      @PathParam("policyViolationId") String policyViolationId,
      @PathParam("ownerType") OwnerType ownerType,
      String comment)
  {
    apiPolicyWaiverService.addPolicyWaiver(policyViolationId, ownerType, comment);
  }
}
