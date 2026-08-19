/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.ArrayList;
import java.util.Collections;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritiesUtilsService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClient.StateType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScmStatusHelperTest
{
  private static final String SUCCESS_STATE = "succeeded";

  private static final String FAILURE_STATE = "failed";

  private static final String DEFAULT_GITHUB_TARGET_URL =
      "http://localhost:8070/ui/links/application/appPublicId/report/scanId?source=github";

  private static final String PRIORITIES_TARGET_URL =
      "http://localhost:8070/ui/links/developer/priorities/appPublicId/scanId";

  private static final String SUCCESS_DESCRIPTION = "Components: Critical: 0, Severe: 0, Moderate: 0";

  private static final String FAILURE_DESCRIPTION = "Components: Critical: 5, Severe: 5, Moderate: 5";

  private static final String APP_ID = "appId";

  private static final String SCAN_ID = "scanId";

  private static final String APP_PUBLIC_ID = "appPublicId";

  private static final String BASE_URL = "http://localhost:8070";

  @Mock
  private GitApiClient mockGitApiClient;

  @Mock
  private BaseUrl mockBaseUrl;

  @Mock
  private ApplicationDAO mockApplicationDAO;

  @Mock
  private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  @InjectMocks
  private ScmStatusHelper scmStatusHelper;

  public ScmStatusHelperTest() {
  }

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // mocking getState for GitApiClient
    doReturn(SUCCESS_STATE).when(mockGitApiClient).getState(StateType.SUCCESS);
    doReturn(FAILURE_STATE).when(mockGitApiClient).getState(StateType.FAILURE);

    // mocking ApplicationDAO
    Application application = new Application();
    application.setId(APP_ID);
    application.setPublicId(APP_PUBLIC_ID);
    doReturn(application).when(mockApplicationDAO).getByIdNotNull(APP_ID);

    // mocking BaseUrl
    doReturn(BASE_URL).when(mockBaseUrl).get();
  }

  @Test
  public void testCreateStatusRequestFromPolicyEvaluation_withSuccessState() {
    // given: a policy evaluation and a policy evaluation result
    PolicyEvaluation policyEvaluation = buildPolicyEvaluation(APP_ID, SCAN_ID);
    PolicyEvaluationResult policyEvaluationResult = buildPolicyEvaluationResult(0, 0, 0);
    addActionToPolicyResult(policyEvaluationResult, Action.newWarnAction());

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromPolicyEvaluation(policyEvaluation,
        policyEvaluationResult, mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(SUCCESS_STATE, SUCCESS_DESCRIPTION, DEFAULT_GITHUB_TARGET_URL);
  }

  @Test
  public void testCreateStatusRequestFromPolicyEvaluation_withNoAlerts() {
    // given: a policy evaluation and a policy evaluation result
    PolicyEvaluation policyEvaluation = buildPolicyEvaluation(APP_ID, SCAN_ID);
    PolicyEvaluationResult policyEvaluationResult = buildPolicyEvaluationResult(0, 0, 0);

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromPolicyEvaluation(policyEvaluation,
        policyEvaluationResult, mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(SUCCESS_STATE, SUCCESS_DESCRIPTION, DEFAULT_GITHUB_TARGET_URL);
  }

  @Test
  public void testCreateStatusRequestFromPolicyEvaluation_withFailureState() {
    // given: a policy evaluation and a policy evaluation result
    PolicyEvaluation policyEvaluation = buildPolicyEvaluation(APP_ID, SCAN_ID);
    PolicyEvaluationResult policyEvaluationResult = buildPolicyEvaluationResult(5, 5, 5);
    addActionToPolicyResult(policyEvaluationResult, Action.newWarnAction());
    addActionToPolicyResult(policyEvaluationResult, Action.newFailAction());

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromPolicyEvaluation(policyEvaluation,
        policyEvaluationResult, mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(FAILURE_STATE, FAILURE_DESCRIPTION, DEFAULT_GITHUB_TARGET_URL);
  }

  @Test
  public void testCreateStatusRequestFromPolicyEvaluation_withPrioritiesUrl() {
    doReturn(true).when(developmentPrioritiesUtilsService).arePrioritiesFeaturesEnabled();

    // given: a policy evaluation and a policy evaluation result
    PolicyEvaluation policyEvaluation = buildPolicyEvaluation(APP_ID, SCAN_ID);
    PolicyEvaluationResult policyEvaluationResult = buildPolicyEvaluationResult(0, 0, 0);
    addActionToPolicyResult(policyEvaluationResult, Action.newWarnAction());

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromPolicyEvaluation(policyEvaluation,
        policyEvaluationResult, mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(SUCCESS_STATE, SUCCESS_DESCRIPTION, PRIORITIES_TARGET_URL);
  }

  @Test
  public void testCreateStatusRequestFromSourceControlEvent_withSuccessState() {
    // given: a source control event
    SourceControlEvent sourceControlEvent = buildSourceControlEvent(0, 0, 0,
        ScmStatusHelper.DEFAULT_OUTCOME, APP_ID, SCAN_ID);

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromSourceControlEvent(sourceControlEvent,
        mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(SUCCESS_STATE, SUCCESS_DESCRIPTION, DEFAULT_GITHUB_TARGET_URL);
  }

  @Test
  public void testCreateStatusRequestFromSourceControlEvent_withFailureState() {
    // given: a source control event
    SourceControlEvent sourceControlEvent = buildSourceControlEvent(5, 5, 5,
        Action.ID_FAIL, APP_ID, SCAN_ID);

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromSourceControlEvent(sourceControlEvent,
        mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(FAILURE_STATE, FAILURE_DESCRIPTION, DEFAULT_GITHUB_TARGET_URL);
  }

  @Test
  public void testCreateStatusRequestFromSourceControlEvent_withPrioritiesUrl() {
    doReturn(true).when(developmentPrioritiesUtilsService).arePrioritiesFeaturesEnabled();

    // given: a source control event
    SourceControlEvent sourceControlEvent = buildSourceControlEvent(0, 0, 0,
        ScmStatusHelper.DEFAULT_OUTCOME, APP_ID, SCAN_ID);

    // when: creating a new status request
    scmStatusHelper.createStatusRequestFromSourceControlEvent(sourceControlEvent,
        mockGitApiClient, SourceControlProvider.GITHUB);

    // then: the proper status request is created
    verifyStatusRequest(SUCCESS_STATE, SUCCESS_DESCRIPTION, PRIORITIES_TARGET_URL);
  }

  private void verifyStatusRequest(
      String expectedState,
      String expectedDescription,
      String expectedTargetUrl)
  {
    // verify gets the proper state from scm
    StateType expectedSateType = SUCCESS_STATE.equals(expectedState) ? StateType.SUCCESS : StateType.FAILURE;
    ArgumentCaptor<StateType> stateType = ArgumentCaptor.forClass(StateType.class);
    verify(mockGitApiClient, times(1)).getState(stateType.capture());
    assertThat(stateType.getValue()).isEqualTo(expectedSateType);

    // verify gets base url
    verify(mockBaseUrl, times(1)).get();

    // verify gets application from the DB
    ArgumentCaptor<String> applicationId = ArgumentCaptor.forClass(String.class);
    verify(mockApplicationDAO, times(1)).getByIdNotNull(applicationId.capture());
    assertThat(applicationId.getValue()).isEqualTo(APP_ID);

    // verify creates status with proper values
    ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> targetUrl = ArgumentCaptor.forClass(String.class);
    verify(mockGitApiClient, times(1)).createStatusRequest(state.capture(),
        context.capture(), description.capture(), targetUrl.capture());
    assertThat(state.getValue()).isEqualTo(expectedState);
    assertThat(context.getValue()).isEqualTo(ScmStatusHelper.IQ_POLICY_EVALUATION);
    assertThat(description.getValue()).isEqualTo(expectedDescription);
    assertThat(targetUrl.getValue()).isEqualTo(expectedTargetUrl);
  }

  private PolicyEvaluation buildPolicyEvaluation(String applicationId, String scanId) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setOwnerId(applicationId);
    policyEvaluation.setScanId(scanId);
    return policyEvaluation;
  }

  private PolicyEvaluationResult buildPolicyEvaluationResult(
      int criticalCount,
      int severeCount,
      int moderateCount)
  {
    PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setCriticalComponentCount(criticalCount);
    policyEvaluationResult.setSevereComponentCount(severeCount);
    policyEvaluationResult.setModerateComponentCount(moderateCount);
    policyEvaluationResult.setAlerts(new ArrayList<>());

    return policyEvaluationResult;
  }

  private void addActionToPolicyResult(PolicyEvaluationResult result, Action action) {
    PolicyAlert alert = new PolicyAlert(new PolicyFact(), Collections.singletonList(action));
    result.getAlerts().add(alert);
  }

  private SourceControlEvent buildSourceControlEvent(
      int criticalCount,
      int severeCount,
      int moderateCount,
      String outcome,
      String applicationId,
      String scanId)
  {
    return new SourceControlEvent()
        .setApplicationId(applicationId)
        .setScanId(scanId)
        .setCriticalComponentCount(criticalCount)
        .setSevereComponentCount(severeCount)
        .setModerateComponentCount(moderateCount)
        .setPolicyEvaluationOutcome(outcome);
  }
}
