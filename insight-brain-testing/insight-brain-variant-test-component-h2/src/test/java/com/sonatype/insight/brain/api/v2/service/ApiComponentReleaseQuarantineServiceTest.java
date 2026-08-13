/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryCreator;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiComponentReleaseQuarantineServiceTest
    extends AbstractComponentH2Test
{
  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Rule
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Mock
  private PolicyWaiverTelemetryCreator policyWaiverTelemetryCreator;

  @Mock
  private ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator;

  @Mock
  private CurrentUser currentUser;

  @Inject
  private ApiComponentReleaseQuarantineService service;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier("pkg:maven/g1/a1@v1?type=e1");

  private PolicyViolationLogDTOAssert policyViolationLogDTOAssert;

  @BeforeEach
  public void before() {
    SecurityManager securityManager = lookup(SecurityManager.class);
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(USERNAME, USERNAME, User.INTERNAL_REALM_ID), User.INTERNAL_REALM_ID);

    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());

    Subject authenticatedSubject = new Subject.Builder(securityManager)
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(securityManager);
    ThreadContext.bind(authenticatedSubject);

    policyViolationLogDTOAssert = new PolicyViolationLogDTOAssert(repositoryManagerDAO);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(),
            quarantineTime, quarantineTime);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        createRepositoryPolicyViolation(proxyRepositoryComponent, false, 10, policy, Action.ID_FAIL);

    Date before = new Date();
    ApiComponentReleasedFromQuarantineDTO result =
        service.releaseQuarantineWithoutReEval(repository.getId(), proxyRepositoryComponent.getId(), "comment");
    Date after = new Date();

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;

    proxyRepositoryPolicyViolation = proxyRepositoryPolicyViolationDAO.getById(proxyRepositoryPolicyViolation.getId());
    proxyRepositoryPolicyViolationDAO.loadConstraintFacts(Collections.singleton(proxyRepositoryPolicyViolation));
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(proxyRepositoryPolicyViolation.getPolicyWaiverId());
    assertThat(policyWaiver.getComment()).isEqualTo("comment");
    assertThat(policyWaiver.getCreateTime()).isAfter(quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, proxyRepositoryComponent,
        quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();

    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO apiWaivedPolicyViolationDTO =
        repositoryComponentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(apiWaivedPolicyViolationDTO, proxyRepositoryPolicyViolation, policyWaiver);

    assertViolationWaiverDetails(proxyRepositoryPolicyViolation, policyWaiver);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before, after,
        Collections.singletonList(proxyRepositoryPolicyViolation));

    verify(policyWaiverTelemetryCreator).sendRepositoryWaiverTelemetry(any(), any());
    verify(proxyRepositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE),
            eq(ReleaseQuarantineType.MANUAL), eq("Waived"), eq(Collections.emptyList()));
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_NullComment() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.releaseQuarantineWithoutReEval(repository.getId(), proxyRepositoryComponent.getId(), null))
        .withMessage("Comment has not been specified.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_EmptyComment() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.releaseQuarantineWithoutReEval(repository.getId(), proxyRepositoryComponent.getId(), ""))
        .withMessage("Comment has not been specified.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_WasNotQuarantined() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(), false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.releaseQuarantineWithoutReEval(repository.getId(), proxyRepositoryComponent.getId(),
                "comment"))
        .withMessage(
            "Component with quarantineId " + proxyRepositoryComponent.getId() + " is not quarantined.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_WithNoFailedViolations() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash",
            packageURLIdentifier.ensureCompleteIdentifier(),
            quarantineTime, quarantineTime);

    // add a violation with actionType of fail, but waive it
    createRepositoryPolicyViolation(proxyRepositoryComponent, true, 10, policy, Action.ID_FAIL);

    createRepositoryPolicyViolation(proxyRepositoryComponent, false, 10, policy, Action.ID_WARN);

    Date before = new Date();
    ApiComponentReleasedFromQuarantineDTO result =
        service.releaseQuarantineWithoutReEval(repository.getId(), proxyRepositoryComponent.getId(), "comment");
    Date after = new Date();

    proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(proxyRepositoryComponent.getId());

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;

    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, proxyRepositoryComponent,
        quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).isEmpty();

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();

    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before, after, new ArrayList<>());

    verifyNoInteractions(policyWaiverTelemetryCreator);
    verify(proxyRepositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE),
            eq(ReleaseQuarantineType.MANUAL), eq("Waived"), eq(Collections.emptyList()));
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_UnknownQuarantineId() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.releaseQuarantineWithoutReEval(repository.getId(), "unknownId", "comment"))
        .withMessage(
            "Cannot find a component with quarantineId unknownId.");
    verifyNoInteractions(policyWaiverTelemetryCreator);
    verifyNoInteractions(proxyRepositoryComponentTelemetryCreator);
  }

  @Test
  public void testReleaseQuarantineWithoutReEval_NullComponentIdentifier() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date quarantineTime = new Date(System.currentTimeMillis() - 1000);

    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), MatchState.EXACT, "pathname", "hash", null, quarantineTime,
            quarantineTime);

    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        createRepositoryPolicyViolation(proxyRepositoryComponent, false, 10, policy, Action.ID_FAIL);

    Date before = new Date();
    ApiComponentReleasedFromQuarantineDTO result =
        service.releaseQuarantineWithoutReEval(repository.getId(), proxyRepositoryComponent.getId(), "comment");
    Date after = new Date();

    ApiRepositoryComponentPolicyViolationDTO repositoryComponentPolicyViolationDTO =
        result.componentReleasedFromQuarantine;

    proxyRepositoryPolicyViolation = proxyRepositoryPolicyViolationDAO.getById(proxyRepositoryPolicyViolation.getId());
    proxyRepositoryPolicyViolationDAO.loadConstraintFacts(Collections.singleton(proxyRepositoryPolicyViolation));
    PolicyWaiver policyWaiver = policyWaiverDAO.getByIdNotNull(proxyRepositoryPolicyViolation.getPolicyWaiverId());
    assertThat(policyWaiver.getComment()).isEqualTo("comment");
    assertThat(policyWaiver.getCreateTime()).isAfter(quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO).isNotNull();
    assertRepositoryComponentDTO(repositoryComponentPolicyViolationDTO.component, proxyRepositoryComponent,
        quarantineTime);

    assertThat(repositoryComponentPolicyViolationDTO.policyViolations).isEmpty();

    assertThat(repositoryComponentPolicyViolationDTO.waivedPolicyViolations).hasSize(1);
    ApiWaivedPolicyViolationDTO apiWaivedPolicyViolationDTO =
        repositoryComponentPolicyViolationDTO.waivedPolicyViolations.get(0);
    assertWaivedPolicyViolationDTO(apiWaivedPolicyViolationDTO, proxyRepositoryPolicyViolation, policyWaiver);

    assertViolationWaiverDetails(proxyRepositoryPolicyViolation, policyWaiver);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before, after,
        Collections.singletonList(proxyRepositoryPolicyViolation));

    verify(policyWaiverTelemetryCreator).sendRepositoryWaiverTelemetry(any(), any());
    verify(proxyRepositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE),
            eq(ReleaseQuarantineType.MANUAL), eq("Waived"), eq(Collections.emptyList()));
  }

  private ProxyRepositoryPolicyViolation createRepositoryPolicyViolation(
      final ProxyRepositoryComponent proxyRepositoryComponent,
      final boolean waived,
      final int threatLevel,
      final Policy policy,
      final String action)
  {
    return tempEntity.newRepositoryPolicyViolation(proxyRepositoryComponent.getRepositoryId(), threatLevel,
        proxyRepositoryComponent.getPathname(), waived, action, policy.getId(), policy.getName(),
        proxyRepositoryComponent.getComponentIdentifier());
  }

  private void assertRepositoryComponentDTO(
      ApiRepositoryComponentDTO repositoryComponentDTO,
      ProxyRepositoryComponent proxyRepositoryComponent,
      Date quarantineTime)
  {
    assertThat(repositoryComponentDTO.hash).isEqualTo(proxyRepositoryComponent.getHash());
    if (proxyRepositoryComponent.getComponentIdentifier() != null) {
      assertThat(repositoryComponentDTO.componentIdentifier.toComponentIdentifier())
          .isEqualTo(proxyRepositoryComponent.getComponentIdentifier());
      assertThat(repositoryComponentDTO.packageUrl)
          .isEqualTo(PackageUrlIdentifier.toPackageUrl(proxyRepositoryComponent.getComponentIdentifier()));
    }
    else {
      assertThat(repositoryComponentDTO.componentIdentifier).isNull();
      assertThat(repositoryComponentDTO.packageUrl).isNull();
    }
    assertThat(repositoryComponentDTO.displayName).isEqualTo(proxyRepositoryComponent.getDisplayName());
    assertThat(repositoryComponentDTO.proprietary).isNull();
    assertThat(repositoryComponentDTO.quarantineTime).isEqualTo(quarantineTime);
    assertThat(repositoryComponentDTO.quarantineReleaseTime).isAfter(quarantineTime);
  }

  private void assertWaivedPolicyViolationDTO(
      ApiWaivedPolicyViolationDTO waivedPolicyViolationDTO,
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation,
      PolicyWaiver policyWaiver)
  {
    assertThat(waivedPolicyViolationDTO.policyId).isEqualTo(proxyRepositoryPolicyViolation.getPolicyId());
    assertThat(waivedPolicyViolationDTO.policyName).isEqualTo(proxyRepositoryPolicyViolation.getPolicyName());
    assertThat(waivedPolicyViolationDTO.policyViolationId).isEqualTo(proxyRepositoryPolicyViolation.getId());
    assertThat(waivedPolicyViolationDTO.threatLevel).isEqualTo(proxyRepositoryPolicyViolation.getThreatLevel());
    assertThat(waivedPolicyViolationDTO.openTime).isNotNull().isEqualTo(proxyRepositoryPolicyViolation.getOpenTime());
    assertThat(waivedPolicyViolationDTO.waiveTime).isNotNull().isEqualTo(proxyRepositoryPolicyViolation.getWaiveTime());

    assertThat(waivedPolicyViolationDTO.constraintViolations).hasSize(1);
    ApiConstraintViolationDTO apiConstraintViolationDTO = waivedPolicyViolationDTO.constraintViolations.get(0);
    assertThat(apiConstraintViolationDTO.constraintId)
        .isEqualTo(proxyRepositoryPolicyViolation.getConstraintFacts().get(0).getConstraintId());
    assertThat(apiConstraintViolationDTO.constraintName)
        .isEqualTo(proxyRepositoryPolicyViolation.getConstraintFacts().get(0).getConstraintName());
    assertThat(apiConstraintViolationDTO.reasons).hasSize(1);
    ApiConstraintViolationReasonDTO apiConstraintViolationReasonDTO = apiConstraintViolationDTO.reasons.get(0);
    assertThat(apiConstraintViolationReasonDTO.reason)
        .isEqualTo(proxyRepositoryPolicyViolation.getConstraintFacts().get(0).getConditionFacts().get(0).getReason());

    assertPolicyWaiverDTO(waivedPolicyViolationDTO.policyWaiver, policyWaiver);
  }

  private void assertViolationWaiverDetails(
      ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation,
      PolicyWaiver policyWaiver)
  {
    assertThat(proxyRepositoryPolicyViolation.getPolicyWaiverId()).isEqualTo(policyWaiver.getId());
    assertThat(proxyRepositoryPolicyViolation.getPolicyWaiverComment()).isEqualTo(policyWaiver.getComment());
    assertThat(proxyRepositoryPolicyViolation.getWaiveTime()).isEqualTo(proxyRepositoryPolicyViolation.getWaiveTime());
    assertThat(proxyRepositoryPolicyViolation.isWaived()).isTrue();
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
        !ComponentMatcherStrategyForWaiver.ALL_COMPONENTS.equals(waiver.getComponentMatchStrategy()))
    {
      assertThat(policyWaiverDTO.associatedPackageUrl).isNotNull();
    }
  }

  private void assertPolicyViolationsLogged(
      PolicyViolationLogEvent policyViolationLogEvent,
      Repository repository,
      Date before,
      Date after,
      List<ProxyRepositoryPolicyViolation> policyViolations) throws Exception
  {
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    policyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        repository, before, after, policyViolations, currentUser.getUsernameOrSystem());
  }
}
