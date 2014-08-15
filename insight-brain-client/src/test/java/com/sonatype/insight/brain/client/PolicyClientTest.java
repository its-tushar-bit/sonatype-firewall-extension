/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class PolicyClientTest
    extends AbstractBrainServiceTest
{
  @Test
  public void testLinkToManagement() throws Exception {
    String appId = "app id";
    PolicyClient policyClient = new PolicyClient(getCLMServer().getClientConfiguration(), appId);
    UriBuilder uriBuilder = UriBuilder.fromPath(getCLMServer().getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksResource.SERVICE_PATH).path(UserInterfaceLinksResource.MANAGEMENT_PATH);
    Assert.assertEquals(policyClient.linkToManagement(), uriBuilder.build(IdUtils.TYPE_APPLICATION, appId).toString());
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = "test-scanid";

    Application application = tempEntity.newApplicationWithParent("test-app");

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    PolicyClient policyClient = new PolicyClient(config, application.getId());

    PolicyEvaluationSummary policyEvaluationSummary = policyClient
        .getPolicyEvaluationSummary(new Stage(Stage.ID_BUILD));
    assertThat(policyEvaluationSummary, is(nullValue()));

    PolicyEvaluation policyEvaluation = tempEntity
        .newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application.getId(), "test-policy");
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    policyEvaluationSummary = policyClient.getPolicyEvaluationSummary(new Stage(Stage.ID_BUILD));

    assertThat(policyEvaluationSummary, notNullValue());
    assertThat(policyEvaluationSummary.getReportUrl(),
        is("ui/links/application/" + application.getPublicId() + "/report/" + scanId));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(1));
  }
}
