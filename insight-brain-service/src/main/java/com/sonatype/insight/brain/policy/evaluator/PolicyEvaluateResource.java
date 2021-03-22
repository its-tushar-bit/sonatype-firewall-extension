/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;

import com.codahale.metrics.annotation.Timed;

@Path(PolicyEvaluateResource.RESOURCE_PATH)
@Named
@Timed
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
                                         final Stage stage) throws IOException
  {
    AuditData.get().setScanId(scanId);
    return policyEvaluateService.evaluate(applicationPublicId, scanId, stage, ScanTriggerType.UNKNOWN);
  }
}
