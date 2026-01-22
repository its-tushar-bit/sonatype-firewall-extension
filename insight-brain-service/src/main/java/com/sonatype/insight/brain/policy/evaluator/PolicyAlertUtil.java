/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.utils.ComponentFactUtil;
import com.sonatype.insight.brain.utils.FirewallForContainerImagesHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Named
@Singleton
public class PolicyAlertUtil
{
  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final ReportComponentService reportComponentService;

  private final FirewallForContainerImagesHelper firewallForContainerImagesHelper;

  private static final Logger log = LoggerFactory.getLogger(PolicyAlertUtil.class);

  @Inject
  public PolicyAlertUtil(
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyViolationDAO policyViolationDAO,
      final ReportComponentService reportComponentService,
      final FirewallForContainerImagesHelper firewallForContainerImagesHelper)
  {
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.reportComponentService = reportComponentService;
    this.firewallForContainerImagesHelper = firewallForContainerImagesHelper;
  }

  public List<PolicyAlert> createPolicyAlerts(
      List<PolicyViolation> policyViolations,
      String stageTypeId,
      String applicationId,
      boolean forMonitoring,
      boolean enableActions)
  {
    return createPolicyAlerts(Collections.emptyList(), policyViolations, stageTypeId, applicationId, forMonitoring,
        enableActions, null);
  }

  public List<PolicyAlert> createPolicyAlerts(
      List<Component> components,
      List<PolicyViolation> policyViolations,
      String stageTypeId,
      String applicationId,
      boolean forMonitoring,
      boolean enableActions)
  {
    return createPolicyAlerts(components, policyViolations, stageTypeId, applicationId, forMonitoring,
        enableActions, null);
  }

  public List<PolicyAlert> createPolicyAlerts(
      List<Component> components,
      List<PolicyViolation> policyViolations,
      String stageTypeId,
      String applicationId,
      boolean forMonitoring,
      boolean enableActions,
      String scanId)
  {
    Owner owner = ownerDAO.getById(applicationId);
    List<String> ownerIdsForPolicies =
        firewallForContainerImagesHelper.getApplicableOwnersForPolicies(stageTypeId, owner);
    Map<String, Policy> policiesById =
        policyDAO.getByIds(policyViolations.stream().map(PolicyViolation::getPolicyId).collect(toSet())).stream()
            .collect(toMap(Policy::getId, Function.identity()));
    List<PolicyAlert> result = new ArrayList<>();

    policyViolationDAO.loadConstraintFacts(policyViolations);

    components = getEffectiveComponents(components, scanId, owner);

    for (PolicyViolation policyViolation : policyViolations) {
      String policyId = policyViolation.getPolicyId();
      PolicyFact policyFact = new PolicyFact(policyId, policyViolation.getPolicyName(),
          policyViolation.getThreatLevel(), policyViolation.getId());
      Policy policy = policiesById.get(policyId);
      List<Action> actions;
      if (policy == null || !enableActions) {
        actions = Collections.emptyList();
      }
      else {
        actions = policy.toActions(stageTypeId, forMonitoring, ownerIdsForPolicies);
      }
      PolicyAlert policyAlert = new PolicyAlert(policyFact, actions);
      result.add(policyAlert);

      ComponentFact componentFact = new ComponentFact(policyViolation.getComponentIdentifier(),
          policyViolation.getHash());
      componentFact.addPathnames(new ArrayList<>(getPathnames(components, policyViolation)));
      ComponentFactUtil.injectDisplayName(componentFact);
      for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
        removeDataUnnecessaryForPolicyAlert(constraintFact);
        componentFact.addConstraintFact(constraintFact);
      }
      policyFact.addComponentFact(componentFact);
    }

    return result;
  }

  private static Set<String> getPathnames(List<Component> components, PolicyViolation policyViolation) {
    return components.stream()
        .filter(component -> componentMatchesPolicyViolation(component, policyViolation))
        .flatMap(component -> component.getPathnames().stream())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static boolean componentMatchesPolicyViolation(Component component, PolicyViolation policyViolation) {
    if (component == null || policyViolation == null) {
      return false;
    }
    if (component.getHash() != null && component.getHash().equals(policyViolation.getHash())) {
      return true;
    }
    if (component.getComponentIdentifier() != null &&
        component.getComponentIdentifier().equals(policyViolation.getComponentIdentifier())) {
      return true;
    }
    return false;
  }

  private static void removeDataUnnecessaryForPolicyAlert(ConstraintFact constraintFact) {
    for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
      conditionFact.setConditionIndex(0);
      conditionFact.setTriggerJson(null);
    }
  }

  public static List<PolicyViolation> getPolicyViolationsFromAlertsAndEvaluation(
      final PolicyEvaluation policyEvaluation,
      final List<PolicyAlert> allPolicyAlerts)
  {
    return getPolicyViolationsFromAlertsAndEvaluation(policyEvaluation, allPolicyAlerts, 0);
  }

  public static List<PolicyViolation> getPolicyViolationsFromAlertsAndEvaluation(
      final PolicyEvaluation policyEvaluation,
      final List<PolicyAlert> allPolicyAlerts,
      final int minimumThreatLevel)
  {
    List<PolicyViolation> allViolations = new ArrayList<>();
    for (PolicyAlert policyAlert : allPolicyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      if (policyFact.getThreatLevel() >= minimumThreatLevel) {
        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          PolicyViolation policyViolation =
              new PolicyViolation(policyEvaluation, policyFact.getPolicyId(), policyFact.getPolicyName(),
                  policyFact.getThreatLevel(), null, componentFact.getHash(), componentFact.getComponentIdentifier(),
                  componentFact.getConstraintFacts(), getFilename(componentFact));
          policyViolation.setId(policyFact.getPolicyViolationId());
          allViolations.add(policyViolation);
        }
      }
    }
    return allViolations;
  }

  private static String getFilename(ComponentFact componentFact) {
    return new ComponentDisplayFilename().addPathnames(componentFact.getPathnames()).getFilename().orElse(null);
  }

  public static List<PolicyViolation> getDummyPolicyViolationsFromPolicyThreatsForCounts(PolicyThreats policyThreats) {
    List<PolicyViolation> allViolations = new ArrayList<>();
    for (PolicyThreats.Component component : policyThreats.aaData) {
      for (PolicyThreats.PolicyViolation violation : component.allViolations) {
        // We only need the threat level and the fix/waive/grandfather times to be set or not
        // (doesn't matter what their times are) to get accurate counts
        PolicyViolation policyViolation = new PolicyViolation();
        policyViolation.setThreatLevel(violation.policyThreatLevel);
        policyViolation.setHash(component.hash);
        boolean waived = violation.waived;
        boolean legacyViolation = violation.legacyViolation;
        boolean fixed =
            !waived && !legacyViolation && !isActive(component.activeViolations, violation.policyViolationId);
        if (fixed) {
          policyViolation.setFixTime(new Date());
        }
        if (waived) {
          policyViolation.setWaiveTime(new Date());
        }
        if (legacyViolation) {
          policyViolation.setLegacyViolationTime(new Date());
        }
        allViolations.add(policyViolation);
      }
    }
    return allViolations;
  }

  private static boolean isActive(List<PolicyThreats.PolicyViolation> activeViolations, String policyViolationId) {
    return activeViolations.stream().anyMatch(v -> v.policyViolationId.equals(policyViolationId));
  }

  // Refreshes the components from the scan report
  private List<Component> getEffectiveComponents(List<Component> components, String scanId, Owner owner) {
    if (scanId == null) {
      return components;
    }

    if (components != null && !components.isEmpty()) {
      return components;
    }

    try {
      List<Component> reportComponents = reportComponentService.getReportComponents(scanId, (Application) owner);
      if (reportComponents != null && !reportComponents.isEmpty()) {
        return reportComponents;
      }
    }
    catch (IOException e) {
      log.debug("Could not read components from the report file.", e);
    }

    return components;
  }
}

