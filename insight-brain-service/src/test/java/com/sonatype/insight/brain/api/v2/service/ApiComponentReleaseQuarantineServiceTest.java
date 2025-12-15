/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentReleasedFromQuarantineDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentPolicyViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaivedPolicyViolationDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ApiComponentReleaseQuarantineServiceTest
    extends AbstractComponentTest
{
  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Rule
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Mock
  private PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  @Mock
  private RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  @Mock
  private CurrentUser currentUser;

  @Inject
  private ApiComponentReleaseQuarantineService service;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");

  private PolicyViolationLogDTOAssert policyViolationLogDTOAssert;

  @Override
  public void configure(Binder binder) {
    binder.bind(PolicyWaiverTelemetryCreator.class).toInstance(policyWaiverTelemetryCreator);
    binder.bind(RepositoryComponentTelemetryCreator.class).toInstance(repositoryComponentTelemetryCreator);
    super.configure(binder);
  }

  @Before
  public void before() {
    policyViolationLogDTOAssert = new PolicyViolationLogDTOAssert(repositoryManagerDAO);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval() throws Exception {
    when(currentUser.getUsername()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(),
            quarantineTime, quarantineTime);

    RepositoryPolicyViolation repositoryPolicyViolation =
        createRepositoryPolicyViolation(repositoryComponent, false, 10, policy, Action.ID_FAIL);

    Date before = new Date();
    ApiComponentReleasedFromQuarantineDTO result =
        service.releaseQuarantineWithoutReEval(repositoryComponent.getId(), "comment");
    Date after = new Date();

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;

    repositoryPolicyViolation = repositoryPolicyViolationDAO.getById(repositoryPolicyViolation.getId());
    repositoryPolicyViolationDAO.loadConstraintFacts(Collections.singleton(repositoryPolicyViolation));
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(repositoryPolicyViolation.getPolicyWaiverId());
    assertThat(policyWaiver.getComment()).isEqualTo("comment");
    assertThat(policyWaiver.getCreateTime()).isAfter(quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, repositoryComponent, quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();

    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO apiWaivedPolicyViolationDTO =
        repositoryComponentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(apiWaivedPolicyViolationDTO, repositoryPolicyViolation, policyWaiver);

    assertViolationWaiverDetails(repositoryPolicyViolation, policyWaiver);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before, after,
        Collections.singletonList(repositoryPolicyViolation));

    verify(policyWaiverTelemetryCreator).sendRepositoryWaiverTelemetry(any(), any());
    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE), eq(ReleaseQuarantineType.MANUAL),
            eq("Waived"), any());
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_NullComment() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.releaseQuarantineWithoutReEval(repositoryComponent.getId(), null))
        .withMessage("Comment has not been specified.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_EmptyComment() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.releaseQuarantineWithoutReEval(repositoryComponent.getId(), ""))
        .withMessage("Comment has not been specified.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_WasNotQuarantined() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.releaseQuarantineWithoutReEval(repositoryComponent.getId(), "comment")).withMessage(
          "Component with quarantineId " + repositoryComponent.getId() + " is not quarantined.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_WithNoFailedViolations() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(),
            quarantineTime, quarantineTime);

    // add a violation with actionType of fail, but waive it
    createRepositoryPolicyViolation(repositoryComponent, true, 10, policy, Action.ID_FAIL);

    createRepositoryPolicyViolation(repositoryComponent, false, 10, policy, Action.ID_WARN);

    Date before = new Date();
    ApiComponentReleasedFromQuarantineDTO result =
        service.releaseQuarantineWithoutReEval(repositoryComponent.getId(), "comment");
    Date after = new Date();

    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;

    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, repositoryComponent, quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).isEmpty();

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();

    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before, after, new ArrayList<>());

    verifyNoInteractions(policyWaiverTelemetryCreator);
    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE),
            eq(ReleaseQuarantineType.MANUAL), eq("Waived"), any());
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_UnknownQuarantineId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.releaseQuarantineWithoutReEval("unknownId", "comment")).withMessage(
            "Cannot find a component with quarantineId unknownId.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_NullComponentIdentifier() throws Exception {
    when(currentUser.getUsername()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash", null, quarantineTime,
            quarantineTime);

    RepositoryPolicyViolation repositoryPolicyViolation =
        createRepositoryPolicyViolation(repositoryComponent, false, 10, policy, Action.ID_FAIL);

    Date before = new Date();
    ApiComponentReleasedFromQuarantineDTO result =
        service.releaseQuarantineWithoutReEval(repositoryComponent.getId(), "comment");
    Date after = new Date();

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;

    repositoryPolicyViolation = repositoryPolicyViolationDAO.getById(repositoryPolicyViolation.getId());
    repositoryPolicyViolationDAO.loadConstraintFacts(Collections.singleton(repositoryPolicyViolation));
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(repositoryPolicyViolation.getPolicyWaiverId());
    assertThat(policyWaiver.getComment()).isEqualTo("comment");
    assertThat(policyWaiver.getCreateTime()).isAfter(quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, repositoryComponent, quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();

    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO apiWaivedPolicyViolationDTO =
        repositoryComponentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(apiWaivedPolicyViolationDTO, repositoryPolicyViolation, policyWaiver);

    assertViolationWaiverDetails(repositoryPolicyViolation, policyWaiver);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before, after,
        Collections.singletonList(repositoryPolicyViolation));

    verify(policyWaiverTelemetryCreator).sendRepositoryWaiverTelemetry(any(), any());
    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE), eq(ReleaseQuarantineType.MANUAL),
            eq("Waived"), any());
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(
      final RepositoryComponent repositoryComponent,
      final boolean waived,
      final int threatLevel,
      final Policy policy,
      final String action)
  {
    return tempEntity.newRepositoryPolicyViolation(repositoryComponent.getRepositoryId(), threatLevel,
        repositoryComponent.getPathname(), waived, action, policy.getId(), policy.getName(),
        repositoryComponent.getComponentIdentifier());
  }

  private void assertRepositoryComponentDTO(
      ApiRepositoryComponentDTO repositoryComponentDTO,
      RepositoryComponent repositoryComponent,
      Date quarantineTime)
  {
    assertThat(repositoryComponentDTO.hash).isEqualTo(repositoryComponent.getHash());
    if (repositoryComponent.getComponentIdentifier() != null) {
      assertThat(repositoryComponentDTO.componentIdentifier.toComponentIdentifier())
          .isEqualTo(repositoryComponent.getComponentIdentifier());
      assertThat(repositoryComponentDTO.packageUrl)
          .isEqualTo(PackageUrlIdentifier.toPackageUrl(repositoryComponent.getComponentIdentifier()));
    }
    else {
      assertThat(repositoryComponentDTO.componentIdentifier).isNull();
      assertThat(repositoryComponentDTO.packageUrl).isNull();
    }
    assertThat(repositoryComponentDTO.displayName).isEqualTo(repositoryComponent.getDisplayName());
    assertThat(repositoryComponentDTO.proprietary).isNull();
    assertThat(repositoryComponentDTO.quarantineTime).isEqualTo(quarantineTime);
    assertThat(repositoryComponentDTO.quarantineReleaseTime).isAfter(quarantineTime);
  }

  private void assertWaivedPolicyViolationDTO(
      ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO,
      RepositoryPolicyViolation repositoryPolicyViolation,
      PolicyWaiver policyWaiver)
  {
    assertThat(waivedPolicyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(waivedPolicyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(waivedPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(waivedPolicyViolationDTO.threatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(waivedPolicyViolationDTO.openTime).isNotNull().isEqualTo(repositoryPolicyViolation.getOpenTime());
    assertThat(waivedPolicyViolationDTO.waiveTime).isNotNull().isEqualTo(repositoryPolicyViolation.getWaiveTime());

    assertThat(waivedPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = waivedPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId)
        .isEqualTo(repositoryPolicyViolation.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(repositoryPolicyViolation.getConstraintFacts().get(0).getConstraintName());
    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(repositoryPolicyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver);
  }

  private void assertViolationWaiverDetails(
      RepositoryPolicyViolation repositoryPolicyViolation,
      PolicyWaiver policyWaiver)
  {
    assertThat(repositoryPolicyViolation.getPolicyWaiverId()).isEqualTo(policyWaiver.getId());
    assertThat(repositoryPolicyViolation.getPolicyWaiverComment()).isEqualTo(policyWaiver.getComment());
    assertThat(repositoryPolicyViolation.getWaiveTime()).isEqualTo(repositoryPolicyViolation.getWaiveTime());
    assertThat(repositoryPolicyViolation.isWaived()).isTrue();
  }

  private void assertPolicyWaiverDTO(
      ApiPolicyWaiverDTO policyWaiverDTO,
      PolicyWaiver waiver)
  {
    assertThat(policyWaiverDTO.hash).isEqualTo(waiver.getHash());
    assertThat(policyWaiverDTO.policyId).isEqualTo(waiver.getPolicyId());
    assertThat(policyWaiverDTO.comment).isEqualTo(waiver.getComment());
    assertThat(policyWaiverDTO.createTime).isEqualTo(waiver.getCreateTime());
    assertThat(policyWaiverDTO.policyWaiverId).isEqualTo(waiver.getId());
    assertThat(policyWaiverDTO.associatedPackageUrl).isEqualTo(waiver.getAssociatedPackageUrl());

    if (waiver.getComponentIdentifier() != null) {
      assertThat(policyWaiverDTO.componentIdentifier.toComponentIdentifier())
          .isEqualTo(waiver.getComponentIdentifier());
      assertThat(policyWaiverDTO.getDisplayName().toString())
          .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(waiver.getComponentIdentifier()).toString());
    }

    if (waiver.getComponentIdentifier() != null &&
        !ComponentMatcherStrategyForWaiver.ALL_COMPONENTS.equals(waiver.getComponentMatchStrategy())) {
      assertThat(policyWaiverDTO.associatedPackageUrl).isNotNull();
    }
  }

  private void assertPolicyViolationsLogged(
      PolicyViolationLogEvent policyViolationLogEvent,
      Repository repository,
      Date before,
      Date after,
      List<RepositoryPolicyViolation> policyViolations)
      throws Exception
  {
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    policyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        repository, before, after, policyViolations, currentUser.getUsername());
  }
}
