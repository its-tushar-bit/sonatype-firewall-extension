/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Policy evaluation summary rest resource for integration with other tools such as Sonar
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(PolicyEvaluationSummaryResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
public class PolicyEvaluationSummaryResource
{
  public static final String RESOURCE_PATH = "rest/quality/evaluations/{applicationId}/{stageTypeId}";

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationSummaryResource.class);

  private final PolicyEvaluationSummaryService policyEvaluationSummaryService;

  @Inject
  public PolicyEvaluationSummaryResource(final PolicyEvaluationSummaryService policyEvaluationSummaryService) {
    this.policyEvaluationSummaryService = policyEvaluationSummaryService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyEvaluationSummary getCurrentPolicyEvaluation(@PathParam("applicationId") final String applicationId,
                                                            @PathParam("stageTypeId") final String stageTypeId)
  {
    log.debug("Received request to get policy evaluation summary for app id {}, stageTypeId {}", applicationId,
        stageTypeId);

    StageType stageType = StageTypes.getById(stageTypeId);
    if (stageType == null) {
      throw new BadRequestException("Invalid parameter stageTypeId=" + stageTypeId + ".");
    }
    Stage stage = new Stage(stageTypeId);

    return policyEvaluationSummaryService.getEvaluationSummaryByApplicationId(applicationId, stage);
  }
}
