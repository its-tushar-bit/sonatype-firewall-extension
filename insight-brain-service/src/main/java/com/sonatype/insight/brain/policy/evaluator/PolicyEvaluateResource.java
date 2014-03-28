/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(PolicyEvaluateResource.SERVICE_PATH)
@Named
public class PolicyEvaluateResource
{
  public static final String SERVICE_PATH = "rest/policy/{applicationPublicId}/evaluate";

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluateResource.class);

  private final PolicyEvaluationUtils policyEvaluationUtils;

  private final PolicyAlertNotifier policyAlertNotifier;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public PolicyEvaluateResource(final PolicyEvaluationUtils policyEvaluationUtils,
      PolicyAlertNotifier policyAlertNotifier)
  {
    this.policyEvaluationUtils = policyEvaluationUtils;
    this.policyAlertNotifier = policyAlertNotifier;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public PolicyEvaluationResult evaluate(@PathParam("applicationPublicId") final String applicationPublicId,
      @QueryParam("scanId") final String scanId, final Stage stage, @HeaderParam("user-agent") final String userAgent)
      throws IOException
  {
    log.debug("Received request to evaluate policy for app id {}, scan id {}, stageTypeId {}", applicationPublicId,
        scanId, stage.getStageTypeId());

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(appId,
        stage.getStageTypeId());

    PolicyEvaluation policyEvaluation = policyEvaluationUtils.evaluate(applicationPublicId, scanId, stage);
    PolicyEvaluationResult policyEvaluationResult = policyEvaluationUtils
        .createPolicyEvaluationResult(policyEvaluation);

    if (!policyEvaluationResult.isReevaluation()) {
      List<PolicyAlert> newAlerts = policyEvaluationResult.getAlerts();
      List<PolicyAlert> oldAlerts = PolicyAlertUtil.createPolicyAlerts(lastPrimaryPolicyEvaluation);
      policyAlertNotifier.sendNotifications(applicationPublicId, appId, scanId, stage, newAlerts, oldAlerts);
    }

    return policyEvaluationResult;
  }

}
