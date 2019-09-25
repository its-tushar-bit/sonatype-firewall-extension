/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierTest
    extends AbstractComponentTest
{
  private static final String PUBLIC_ID = "abc123";

  private static final String NAME = "reponame";

  private static final String ORGANIZATION_ID = "sonatype";

  @Mock
  private PullRequestFeatureCheck pullRequestFeatureCheck;

  @Mock
  private ApiComponentRemediationService remediationService;

  private PolicyAlertScmNotifier scmNotifier;

  private PolicyAlertSourceCodeOrganizer policyAlertSourceCodeOrganizer;

  private Application application;

  @Rule
  public LogOutput logOutput = new LogOutput(PolicyAlertScmNotifier.class);

  @Before
  public void setup() throws Exception {
    policyAlertSourceCodeOrganizer = new PolicyAlertSourceCodeOrganizer();
    scmNotifier =
        new PolicyAlertScmNotifier(pullRequestFeatureCheck, remediationService, policyAlertSourceCodeOrganizer);
    application = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);

    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(any(Application.class))).thenReturn(true);
  }

  @Test
  public void test_noRemediationOptions() throws Exception {
    ApiComponentRemediationDTO emptyRemediationDTO = new ApiComponentRemediationDTO();
    when(remediationService
        .getSuggestedRemediationForComponent(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(application.getId()), isNull()))
        .thenReturn(emptyRemediationDTO);

    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"), randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release")
    );
    PolicyNotification policyNotification1 = new PolicyNotification(policyFact1, notifications);
    List<PolicyNotification> policyNotifications = Arrays.asList(policyNotification1);

    scmNotifier.sendNotifications(application, policyNotifications);

    assertThat(logOutput).atDebugLevel()
        .contains("No remediation options found for component [nuget: {packageId=Package1, version=1.2.3}]");
    assertThat(logOutput).atDebugLevel()
        .doesNotContain("Invoke PR engine to construct a PR");
  }

  @Test
  public void test_invokePREngine() throws Exception {
    // construct a remediation DTO with a suggested remediation
    ApiVersionChangeOptionDTO versionChangeOptionDTO = new ApiVersionChangeOptionDTO();
    versionChangeOptionDTO.setType(ApiVersionChangeOptionType.NEXT_NON_FAILING);
    ApiComponentChangeActionDTO changeActionDTO = new ApiComponentChangeActionDTO();
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    changeActionDTO.setComponent(componentDTOV2);
    // upgrade version
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createNugetCoordinates("Package1", "2.0.1"));
    ApiComponentRemediationDTO remediationDTO = new ApiComponentRemediationDTO();
    remediationDTO.remediation.versionChanges = Arrays.asList(versionChangeOptionDTO);

    when(remediationService
        .getSuggestedRemediationForComponent(any(ApiComponentDTOV2.class), eq(OwnerType.APPLICATION),
            eq(application.getId()), isNull()))
        .thenReturn(remediationDTO);

    PolicyFact policyFact1 = new PolicyFact("policyid-1", "policyname-1", 3);
    policyFact1.addComponentFact(
        new ComponentFact(ComponentIdentifier.createNugetCoordinates("Package1", "1.2.3"), randomString()));

    Notifications notifications = new Notifications(
        new UserNotification("foo@mail.com", "release")
    );
    PolicyNotification policyNotification1 = new PolicyNotification(policyFact1, notifications);
    List<PolicyNotification> policyNotifications = Arrays.asList(policyNotification1);

    scmNotifier.sendNotifications(application, policyNotifications);

    assertThat(logOutput).atDebugLevel()
        .contains("Invoke PR engine to construct a PR for [nuget: {packageId=Package1, version=1.2.3}]");
    assertThat(logOutput).atDebugLevel()
        .doesNotContain("No remediation options found");
  }

  private String randomString() {
    return UUID.randomUUID().toString();
  }
}
