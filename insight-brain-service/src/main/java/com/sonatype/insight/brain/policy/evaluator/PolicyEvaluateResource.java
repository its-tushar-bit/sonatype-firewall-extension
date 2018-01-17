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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(PolicyEvaluateResource.RESOURCE_PATH)
@Named
public class PolicyEvaluateResource
{
  public static final String RESOURCE_PATH = "rest/policy/{applicationPublicId}/evaluate";

  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluateResource.class);

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public PolicyEvaluateResource(final ScanPolicyEvaluator scanPolicyEvaluator, PolicyAlertNotifier policyAlertNotifier)
  {
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_APPLICATION, anonymousAllowed = true)
  @Timed
  public PolicyEvaluationResult evaluate(@PathParam("applicationPublicId") @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
                                         @QueryParam("scanId") final String scanId,
                                         final Stage stage) throws IOException
  {
    log.debug("Received request to evaluate policy for app id {}, scan id {}, stageTypeId {}", applicationPublicId,
        scanId, stage.getStageTypeId());

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(applicationPublicId, scanId, stage);
    PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator
        .createPolicyEvaluationResult(results.evaluation, results.activeViolations, true);

    if (!results.evaluation.isReevaluation()) {
      policyAlertNotifier.sendNotifications(application, results.evaluation, results.notifiableViolations);
    }

    return policyEvaluationResult;
  }

}
