/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Path(PolicyEvaluateResource.RESOURCE_PATH)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_EVALUATION)
public class PolicyEvaluateResource
{
  public static final String RESOURCE_PATH = "rest/policy/{applicationPublicId}/evaluate";

  private final PolicyEvaluateService policyEvaluateService;

  @Inject
  public PolicyEvaluateResource(PolicyEvaluateService policyEvaluateService) {
    this.policyEvaluateService = policyEvaluateService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  public PolicyEvaluationResult evaluate(@PathParam("applicationPublicId") final String applicationPublicId,
                                         @QueryParam("scanId") final String scanId,
                                         @QueryParam("scanTriggerType") @DefaultValue("Unknown")
                                           ScanTriggerType scanTriggerType,
                                         final Stage stage) throws IOException
  {
    AuditData.get().setScanId(scanId);
    return policyEvaluateService.evaluate(applicationPublicId, scanId, stage, scanTriggerType);
  }
}
