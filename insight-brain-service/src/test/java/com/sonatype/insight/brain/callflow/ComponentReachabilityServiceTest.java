/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.callflow;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Binder;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ComponentReachabilityServiceTest extends AbstractComponentTest
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
  public void testIsComponentReachable_ReturnsTrueWhenAtLeastOneViolationIsReachable() {
    final String scanId = "scanId1";
    final PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    final List<PolicyViolation> policyViolations = List.of(
        createSecurityPolicyViolation(policy, policyEvaluation, true),
        createSecurityPolicyViolation(policy, policyEvaluation, false),
        createSecurityPolicyViolation(policy, policyEvaluation, false)
    );
    final List<PolicyThreats.PolicyViolation> policyThreatViolations = createPolicyThreatViolations(policyViolations);
    final PolicyThreats policyThreats = createPolicyThreats(policyThreatViolations);

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(policyThreats);

    final boolean isComponentReachable =
        componentReachabilityService.isComponentReachable(application.getId(), scanId, COMPONENT_HASH);
    assertThat(isComponentReachable).isTrue();
  }

  @Test
  public void testIsComponentReachable_ReturnsFalseWhenNoViolationIsReachable() {
    final String scanId = "scanId1";
    final PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    final List<PolicyViolation> policyViolations = List.of(
        createSecurityPolicyViolation(policy, policyEvaluation, false),
        createSecurityPolicyViolation(policy, policyEvaluation, false),
        createSecurityPolicyViolation(policy, policyEvaluation, false)
    );
    final List<PolicyThreats.PolicyViolation> policyThreatViolations = createPolicyThreatViolations(policyViolations);
    final PolicyThreats policyThreats = createPolicyThreats(policyThreatViolations);

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(policyThreats);

    final boolean isComponentReachable =
        componentReachabilityService.isComponentReachable(application.getId(), scanId, COMPONENT_HASH);
    assertThat(isComponentReachable).isFalse();
  }

  @Test
  public void testIsComponentReachable_ReturnsCorrectlyForSameComponentAcrossSeparateScans() {
    // Scan 1
    final String scanId1 = "scanId1";
    final PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId1);

    // Include 1 reachable violation to demonstrate results from other scans with the same component are not used
    final List<PolicyViolation> policyViolations1 = List.of(
        createSecurityPolicyViolation(policy, policyEvaluation1, true),
        createSecurityPolicyViolation(policy, policyEvaluation1, false),
        createSecurityPolicyViolation(policy, policyEvaluation1, false)
    );
    final List<PolicyThreats.PolicyViolation> policyThreatViolations1 = createPolicyThreatViolations(policyViolations1);
    final PolicyThreats policyThreats1 = createPolicyThreats(policyThreatViolations1);

    // Scan 2 (same component, different scan and evaluation)
    final String scanId2 = "scanId2";
    final PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId2);
    final List<PolicyViolation> policyViolations2 = List.of(
        createSecurityPolicyViolation(policy, policyEvaluation2, false),
        createSecurityPolicyViolation(policy, policyEvaluation2, false),
        createSecurityPolicyViolation(policy, policyEvaluation2, false)
    );
    final List<PolicyThreats.PolicyViolation> policyThreatViolations2 = createPolicyThreatViolations(policyViolations2);
    final PolicyThreats policyThreats2 = createPolicyThreats(policyThreatViolations2);

    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(policyThreats1).thenReturn(policyThreats2);

    // Scan 1 had 1 reachable
    final boolean isComponentReachable1 =
        componentReachabilityService.isComponentReachable(application.getId(), scanId1, COMPONENT_HASH);
    assertThat(isComponentReachable1).isTrue();

    // Scan 2 had no reachable
    final boolean isComponentReachable2 =
        componentReachabilityService.isComponentReachable(application.getId(), scanId2, COMPONENT_HASH);
    assertThat(isComponentReachable2).isFalse();
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
      final boolean isReachable)
  {
    final PolicyViolation policyViolation =
        tempEntity.newPolicyViolation(policyEvaluation, policy, 8, PolicyThreatCategory.SECURITY, null, null, null,
            COMPONENT_HASH);
    policyViolation
        .setReachabilityStatus(isReachable ? ReachabilityStatus.REACHABLE : ReachabilityStatus.NON_REACHABLE);
    policyViolationDAO.update(policyViolation);
    return policyViolation;
  }
}
