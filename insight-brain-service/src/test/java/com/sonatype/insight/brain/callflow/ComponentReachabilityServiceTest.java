/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.callflow;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.NON_REACHABLE;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.REACHABLE;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class ComponentReachabilityServiceTest
    extends AbstractComponentTest
{
  private static final String COMPONENT_HASH = "componentHash";

  @Inject
  private ComponentReachabilityService componentReachabilityService;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private ReportService reportService;

  private Application application;

  private Policy policy;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
    policy = tempEntity.newPolicy(application);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(ReportService.class).toInstance(reportService);
    super.configure(binder);
  }

  @Test
  public void testIsComponentReachable() {
    // Single
    assertThat(isComponentReachable(REACHABLE)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(NON_REACHABLE)).isEqualTo(NON_REACHABLE);
    assertThat(isComponentReachable(UNKNOWN)).isEqualTo(UNKNOWN);
    assertThat(isComponentReachable((ReachabilityStatus) null)).isEqualTo(UNKNOWN);

    // Double
    assertThat(isComponentReachable(REACHABLE, REACHABLE)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(REACHABLE, NON_REACHABLE)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(REACHABLE, UNKNOWN)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(REACHABLE, null)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(NON_REACHABLE, NON_REACHABLE)).isEqualTo(NON_REACHABLE);
    assertThat(isComponentReachable(NON_REACHABLE, UNKNOWN)).isEqualTo(UNKNOWN);
    assertThat(isComponentReachable(NON_REACHABLE, null)).isEqualTo(UNKNOWN);
    assertThat(isComponentReachable(UNKNOWN, UNKNOWN)).isEqualTo(UNKNOWN);
    assertThat(isComponentReachable(UNKNOWN, null)).isEqualTo(UNKNOWN);
    assertThat(isComponentReachable(null, null)).isEqualTo(UNKNOWN);

    // Triple
    assertThat(isComponentReachable(REACHABLE, NON_REACHABLE, UNKNOWN)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(REACHABLE, NON_REACHABLE, null)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(REACHABLE, UNKNOWN, null)).isEqualTo(REACHABLE);
    assertThat(isComponentReachable(NON_REACHABLE, UNKNOWN, null)).isEqualTo(UNKNOWN);

    // Quad
    assertThat(isComponentReachable(REACHABLE, NON_REACHABLE, UNKNOWN, null)).isEqualTo(REACHABLE);
  }

  @Test
  public void testIsComponentReachable_IgnoresNonSecurityViolations() {
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(
        policyEvaluation,
        policy,
        8,
        PolicyThreatCategory.SECURITY,
        null,
        null,
        null,
        COMPONENT_HASH
    );
    policyViolation1.setReachabilityStatus(REACHABLE);
    policyViolationDAO.update(policyViolation1);
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(
        policyEvaluation,
        policy,
        8,
        PolicyThreatCategory.LICENSE,
        null,
        null,
        null,
        COMPONENT_HASH
    );
    PolicyThreats policyThreats =
        createPolicyThreats(createPolicyThreatViolations(List.of(policyViolation1, policyViolation2)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(policyThreats);

    ReachabilityStatus isComponentReachable =
        componentReachabilityService.isComponentReachable(application.getId(), scanId, COMPONENT_HASH);

    assertThat(isComponentReachable).isEqualTo(REACHABLE);
  }

  @Test
  public void testIsComponentReachable_SameComponent_DifferentScans() {
    // Scan 1
    final String scanId1 = "scanId1";
    final PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId1);

    // Include 1 reachable violation to demonstrate results from other scans with the same component are not used
    final List<PolicyViolation> policyViolations1 = List.of(
        createSecurityPolicyViolation(policy, policyEvaluation1, REACHABLE),
        createSecurityPolicyViolation(policy, policyEvaluation1, NON_REACHABLE),
        createSecurityPolicyViolation(policy, policyEvaluation1, UNKNOWN)
    );
    final List<PolicyThreats.PolicyViolation> policyThreatViolations1 = createPolicyThreatViolations(policyViolations1);
    final PolicyThreats policyThreats1 = createPolicyThreats(policyThreatViolations1);

    // Scan 2 (same component, different scan and evaluation)
    final String scanId2 = "scanId2";
    final PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId2);
    final List<PolicyViolation> policyViolations2 = List.of(
        createSecurityPolicyViolation(policy, policyEvaluation2, NON_REACHABLE),
        createSecurityPolicyViolation(policy, policyEvaluation2, NON_REACHABLE),
        createSecurityPolicyViolation(policy, policyEvaluation2, NON_REACHABLE)
    );
    final List<PolicyThreats.PolicyViolation> policyThreatViolations2 = createPolicyThreatViolations(policyViolations2);
    final PolicyThreats policyThreats2 = createPolicyThreats(policyThreatViolations2);

    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(policyThreats1).thenReturn(policyThreats2);

    // Scan 1 had 1 reachable
    final ReachabilityStatus isComponentReachable1 =
        componentReachabilityService.isComponentReachable(application.getId(), scanId1, COMPONENT_HASH);
    assertThat(isComponentReachable1).isEqualTo(REACHABLE);

    // Scan 2 had no reachable
    final ReachabilityStatus isComponentReachable2 =
        componentReachabilityService.isComponentReachable(application.getId(), scanId2, COMPONENT_HASH);
    assertThat(isComponentReachable2).isEqualTo(NON_REACHABLE);
  }

  private ReachabilityStatus isComponentReachable(final ReachabilityStatus... statuses) {
    String scanId = "scanId";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    for (ReachabilityStatus reachabilityStatus : statuses) {
      policyViolations.add(createSecurityPolicyViolation(policy, policyEvaluation, reachabilityStatus));
    }
    List<PolicyThreats.PolicyViolation> policyThreatViolations = createPolicyThreatViolations(policyViolations);
    PolicyThreats policyThreats = createPolicyThreats(policyThreatViolations);
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(policyThreats);
    return componentReachabilityService.isComponentReachable(application.getId(), scanId, COMPONENT_HASH);
  }

  private PolicyThreats createPolicyThreats(
      final List<PolicyThreats.PolicyViolation> policyViolations)
  {
    final PolicyThreats policyThreats = new PolicyThreats();
    final PolicyThreats.Component component = createComponent();

    component.activeViolations.addAll(policyViolations);
    policyThreats.aaData.add(component);

    return policyThreats;
  }

  private PolicyThreats.Component createComponent() {
    final Map<String, String> coordinates = new HashMap<>();
    coordinates.put("extension", "jar");
    coordinates.put("groupId", "com.sonatype");
    coordinates.put("artifactId", "test");
    coordinates.put("version", "1.1.1");

    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = COMPONENT_HASH;
    component.componentIdentifier = new ComponentIdentifier("maven", coordinates);

    return component;
  }

  private List<PolicyThreats.PolicyViolation> createPolicyThreatViolations(
      final List<PolicyViolation> policyViolations)
  {
    final List<PolicyThreats.PolicyViolation> policyThreatViolations = new ArrayList<>(policyViolations.size());

    policyViolations.forEach(policyViolation -> {
      final PolicyThreats.PolicyViolation policyThreatViolation = new PolicyThreats.PolicyViolation();
      policyThreatViolation.reachabilityStatus = policyViolation.getReachabilityStatus();
      policyThreatViolation.policyThreatLevel = policyViolation.getThreatLevel();
      policyThreatViolation.policyThreatCategory = policyViolation.getThreatCategory().getName();
      policyThreatViolation.policyId = policyViolation.getPolicyId();
      policyThreatViolation.policyViolationId = policyViolation.getId();
      policyThreatViolation.constraintFactsJson = policyViolation.getConstraintFactsJson();
      policyThreatViolations.add(policyThreatViolation);
    });

    return policyThreatViolations;
  }

  private PolicyViolation createSecurityPolicyViolation(
      final Policy policy,
      final PolicyEvaluation policyEvaluation,
      final ReachabilityStatus reachabilityStatus)
  {
    final PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 8, PolicyThreatCategory.SECURITY, null, null, null,
            COMPONENT_HASH);
    policyViolation.setReachabilityStatus(reachabilityStatus);
    policyViolationDAO.update(policyViolation);
    return policyViolation;
  }
}
