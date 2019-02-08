/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class RepositoryPolicyEvaluatorTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Override
  public void configure(Binder binder) {
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    super.configure(binder);
  }

  private void mockHdsRequest(RepositoryComponentEvaluationDataRequestList serviceRequest,
                              ComponentEvaluationDataList hdsResult,
                              boolean quarantine)
      throws IOException
  {
    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.cause = serviceRequest.cause;
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      String pathname = componentEvaluationDataRequest.pathname
          .substring(componentEvaluationDataRequest.pathname.startsWith("/") ? 1 : 0);
      hdsRequest.components
          .add(new RepositoryComponentEvaluationDataRequest(componentEvaluationDataRequest.format, pathname, hash));
    }
    when((quarantine ? quarantineHdsClient : auditHdsClient).post(any(), eq(ComponentEvaluationDataList.class),
        eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH), isNull(), eq(hdsRequest))).thenReturn(hdsResult);
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier,
                                                                String hash,
                                                                MatchState matchState,
                                                                int index,
                                                                Set<License> declaredLicenses,
                                                                Set<License> observedLicenses,
                                                                List<SecurityVulnerability> securityVulnerabilities,
                                                                Integer relativePopularity)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.matchState = matchState.getId();
    componentEvaluationData.declaredLicenses = declaredLicenses == null ? Collections.emptySet() : declaredLicenses;
    componentEvaluationData.observedLicenses = observedLicenses == null ? Collections.emptySet() : observedLicenses;
    componentEvaluationData.catalogDate = System.currentTimeMillis();
    componentEvaluationData.securityVulnerabilities = securityVulnerabilities;
    componentEvaluationData.relativePopularity = relativePopularity;
    return componentEvaluationData;
  }

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
    return Arrays.asList(new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f, ""));
  }

  private void assertPolicyViolationsLogged(PolicyViolationLogEvent policyViolationLogEvent,
                                            Repository repository,
                                            Date before,
                                            Date after,
                                            List<RepositoryPolicyViolation> policyViolations)
      throws Exception
  {
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    for (RepositoryPolicyViolation policyViolation : policyViolations) {
      PolicyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
          repository, before, after, policyViolation);
    }
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_CreatePolicyViolations() throws Exception {
    Repository repository = tempEntity.newRepository();

    tempEntity.newPolicy(repository.getParentOwnerId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies. All policy violations should be logged.
    Date before1 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false, null);
    final Date after1 = new Date();
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before1, after1, policyViolations);

    policyViolationLoggerOutput.clear();

    // Add a new policy and evaluate again. Only the new policy violations should be logged.
    Awaitility.await().until(() -> System.currentTimeMillis() > after1.getTime());
    Policy newPolicy = tempEntity.newPolicy(repository.getParentOwnerId());
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false, null);
    final Date after2 = new Date();
    policyViolations = repositoryPolicyViolationDAO.getActiveByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(4);
    List<RepositoryPolicyViolation> newPolicyViolations =
        policyViolations.stream().filter(policyViolation -> policyViolation.getPolicyId().equals(newPolicy.getId()))
            .collect(Collectors.toList());
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before2, after2, newPolicyViolations);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_FixPolicyViolations() throws Exception {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies.
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false, null);
    final Date after1 = new Date();
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(2);

    policyViolationLoggerOutput.clear();

    // Delete the policy and evaluate again. All policy violations should be logged as fixed.
    Awaitility.await().until(() -> System.currentTimeMillis() > after1.getTime());
    new PolicyDAO().delete(policy);
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false, null);
    Date after2 = new Date();
    assertThat(repositoryPolicyViolationDAO.getActiveByRepositoryId(repository.getId())).hasSize(0);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.FIX, repository, before2, after2, policyViolations);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_WaiveAndUnwaivePolicyViolations() throws Exception {
    Repository repository = tempEntity.newRepository();

    Policy policy1 = tempEntity.newPolicy(repository.getParentOwnerId());
    Policy policy2 = tempEntity.newPolicy(repository.getParentOwnerId());
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy2.getId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 1; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // perform initial evaluation
    Date before1 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false, null);
    final Date after1 = new Date();
    // ... yielding two active violations, both of which logged as new
    List<RepositoryPolicyViolation> activeViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before1, after1, activeViolations);
    // ... and one logged as waived
    List<RepositoryPolicyViolation> waivedViolations = activeViolations.stream()
        .filter(violation -> violation.isWaived()).collect(toList());
    assertThat(waivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before1, after1, waivedViolations);

    policyViolationLoggerOutput.clear();

    // remove the original waiver, add a waiver for the other policy and re-evaluate
    new PolicyWaiverDAO().delete(policyWaiver);
    tempEntity.newWaiver(policy1.getId(), repository.getId());
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false, null);
    final Date after2 = new Date();
    // ... yielding again two violations, none of which logged as new
    activeViolations = repositoryPolicyViolationDAO.getActiveByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before2, after2, Collections.emptyList());
    // ... but one logged as unwaived
    List<RepositoryPolicyViolation> unwaivedViolations = activeViolations.stream()
        .filter(violation -> policy2.getId().equals(violation.getPolicyId())).collect(toList());
    assertThat(unwaivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNWAIVE, repository, before2, after2, unwaivedViolations);
    // ... and one logged as freshly waived
    waivedViolations = activeViolations.stream()
        .filter(violation -> policy1.getId().equals(violation.getPolicyId())).collect(toList());
    assertThat(waivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before2, after2, waivedViolations);
  }
}
