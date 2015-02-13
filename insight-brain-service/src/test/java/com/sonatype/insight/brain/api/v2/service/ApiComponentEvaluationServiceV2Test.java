/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.rhc.RepoHealthCheckSecurityVulnerability;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.saas.ComponentDetailsLoader;
import com.sonatype.insight.brain.saas.SaasClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ApiComponentEvaluationServiceV2Test
    extends AbstractComponentTest
{

  private static final long RETRY_INTERVAL = 500;

  private static final int NUM_TRIES = 100;

  private static final int CHUNK_SIZE = 5;

  private ApiComponentEvaluationServiceV2 apiComponentEvaluationService;

  @Inject
  private PolicyEvaluator policyEvaluator;

  @Inject
  private ComponentDetailsLoader componentDetailsLoader;

  @Inject
  private ApiComponentDetailsAdapter componentDetailsAdapter;

  @Inject
  private InsightWork work;

  @Inject
  private ApiComponentIdentifierValidator componentIdentifierValidator;

  @Inject
  private ErrorResponseGenerator errorResponseGenerator;

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  @Mock
  private SaasClient client;

  private ComponentEvaluationV2Helper componentEvaluationV2Helper = new ComponentEvaluationV2Helper();

  private Organization org;

  private Application app;

  @Before
  public void setupApplication() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    apiComponentEvaluationService = new ApiComponentEvaluationServiceV2(applicationDAO, policyEvaluator,
        componentDetailsLoader, componentDetailsAdapter, client, work, componentIdentifierValidator,
        errorResponseGenerator);

    apiComponentEvaluationService.setChunkSize(CHUNK_SIZE);
  }

  @Test
  public void testEvaluateComponents_chunked() throws Exception {
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Arrays.asList(new License("Apache-2.0", "Apache-2.0")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(Arrays.asList(new License("ATT", "ATT")));
    List<RepoHealthCheckSecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper
        .createSecurityVulnerabilities();

    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    int numChunks = 2;
    for (int chunk = 0; chunk < numChunks; chunk++) {
      ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
      componentEvaluationDataList.components = new ArrayList<>();
      for (int i = 0; i < CHUNK_SIZE; i++) {
        ComponentIdentifier componentIdentifier =
            componentEvaluationV2Helper.createMavenComponentIdentifier("g" + i, "a" + i, "v" + i, "e" + i);
        ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, "h" + i);
        request.components.add(component);
        componentEvaluationDataList.components.add(
            componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier, component.hash,
                MatchState.EXACT, i,
                declaredLicenseSet, observedLicenseSet, securityVulnerabilities));
      }
      when(client.post(
              eq(ComponentEvaluationDataList.class),
              eq(ApiComponentEvaluationServiceV2.HDS_EVALUATION_COMPONENTS_PATH),
              anyObject())
      ).thenReturn(componentEvaluationDataList);
    }
    int numComponents = CHUNK_SIZE * 2;


    ApiComponentEvaluationTicketDTOV2 ticket = apiComponentEvaluationService.evaluateComponents(app.getId(), request);
    ApiComponentEvaluationResultDTOV2 details = getComponentEvaluationResult(ticket);

    assertThat(details, notNullValue());
    assertThat(details.isError, is(false));
    assertThat(details.errorMessage, nullValue());
    assertThat(details.applicationId, is(app.getId()));
    assertThat(details.evaluationDate, notNullValue());
    assertThat(details.submittedDate, notNullValue());
    assertThat(details.results, notNullValue());
    assertThat(details.results.size(), is(numComponents));
    int i = 0;
    for (ApiComponentDetailsDTOV2 componentDetailsDTOV2 : details.results) {
      componentEvaluationV2Helper.assertComponentDetails(componentDetailsDTOV2, request.components.get(i),
          MatchState.EXACT.getId(),
          new ArrayList<>(declaredLicenseSet), new ArrayList<>(observedLicenseSet), securityVulnerabilities, policies);
      i++;
    }
  }

  private ApiComponentEvaluationResultDTOV2 getComponentEvaluationResult(
      final ApiComponentEvaluationTicketDTOV2 evaluationTicket)
      throws Exception
  {
    boolean done = false;
    int tryCount = 1;
    while (!done) {
      try {
        return apiComponentEvaluationService
            .getComponentEvaluation(evaluationTicket.applicationId, evaluationTicket.resultId);
      }
      catch (NotFoundException e) {
        tryCount++;
      }
      if (tryCount >= NUM_TRIES) {
        done = true;
      }
      Thread.sleep(RETRY_INTERVAL);
    }

    return null;
  }
}
