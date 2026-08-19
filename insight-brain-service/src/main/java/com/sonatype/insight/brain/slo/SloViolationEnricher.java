/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.policy.utils.ConstraintFactsUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SloViolationEnricher
{
  private static final Logger log = LoggerFactory.getLogger(SloViolationEnricher.class);

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final SloRemediationEnricher sloRemediationEnricher;

  @Inject
  SloViolationEnricher(
      final PolicyViolationDAO policyViolationDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final PolicyWaiverReasonDAO policyWaiverReasonDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final SloRemediationEnricher sloRemediationEnricher)
  {
    this.policyViolationDAO = policyViolationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.sloRemediationEnricher = sloRemediationEnricher;
  }

  public List<SloViolation> enrich(
      final Application application,
      final String stageId,
      final String scanId,
      final List<PolicyViolation> violations)
  {
    policyViolationDAO.loadConstraintFacts(violations);

    final Set<String> manualWaiverIds = violations.stream()
        .map(PolicyViolation::getPolicyWaiverId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    final Map<String, PolicyWaiver> manualWaivers = manualWaiverIds.isEmpty()
        ? Map.of()
        : policyWaiverDAO.getByIds(manualWaiverIds)
            .stream()
            .collect(Collectors.toMap(PolicyWaiver::getId, w -> w, (a, b) -> a));
    final Map<String, PolicyWaiverReason> reasonsById = manualWaivers.isEmpty()
        ? Map.of()
        : policyWaiverReasonDAO.getPolicyWaiverReasonIdToPolicyWaiverReasonMap();

    final Set<String> autoWaiverIds = violations.stream()
        .map(PolicyViolation::getAutoPolicyWaiverId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    final Map<String, AutoPolicyWaiver> autoWaivers = autoWaiverIds.isEmpty()
        ? Map.of()
        : autoPolicyWaiverDAO.getByIds(autoWaiverIds)
            .stream()
            .collect(Collectors.toMap(AutoPolicyWaiver::getId, w -> w, (a, b) -> a));

    final Map<String, SloRemediationEnricher.SloRemediationInfo> remediation =
        sloRemediationEnricher.loadByComponentHash(application, stageId, scanId, violations);

    final List<SloViolation> results = new ArrayList<>(violations.size());
    for (PolicyViolation pv : violations) {
      SloViolation v = toBaseViolation(application, pv);
      v.waiver = buildWaiver(pv, manualWaivers, reasonsById, autoWaivers);
      if (pv.getComponentIdentifier() != null) {
        String hash = pv.getComponentIdentifier().toSyntheticHash();
        SloRemediationEnricher.SloRemediationInfo info = hash == null ? null : remediation.get(hash);
        if (info != null) {
          v.dependencyType = info.dependencyType();
          v.recommendedRemediationVersion = info.recommendedVersion();
        }
      }
      results.add(v);
    }
    return results;
  }

  private SloWaiver buildWaiver(
      final PolicyViolation pv,
      final Map<String, PolicyWaiver> manualWaivers,
      final Map<String, PolicyWaiverReason> reasonsById,
      final Map<String, AutoPolicyWaiver> autoWaivers)
  {
    if (!pv.isWaived()) {
      return null;
    }
    if (pv.getAutoPolicyWaiverId() != null) {
      AutoPolicyWaiver auto = autoWaivers.get(pv.getAutoPolicyWaiverId());
      if (auto != null) {
        return new SloWaiver(auto.getId(), null, true,
            auto.getCreatorId(), auto.getCreatorName(), auto.getCreateTime(), null);
      }
    }
    if (pv.getPolicyWaiverId() != null) {
      PolicyWaiver w = manualWaivers.get(pv.getPolicyWaiverId());
      if (w != null) {
        PolicyWaiverReason reason = w.getWaiverReasonId() != null ? reasonsById.get(w.getWaiverReasonId()) : null;
        String reasonText = reason != null ? reason.getReasonText() : w.getComment();
        return new SloWaiver(w.getId(), reasonText, false,
            w.getCreatorId(), w.getCreatorName(), w.getCreateTime(), w.getExpiryTime());
      }
    }
    // Waived, but the referenced waiver row could not be resolved (e.g. the waiver was deleted). This is expected
    // graceful degradation: we still expose waiveTime on the DTO, we just cannot describe the waiver itself.
    log.debug("Waived violation {} could not be resolved to a waiver row; leaving waiver detail null",
        pv.getId());
    return null;
  }

  private SloViolation toBaseViolation(final Application application, final PolicyViolation pv) {
    SloViolation v = new SloViolation();
    v.violationId = pv.getId();
    v.applicationPublicId = application.getPublicId();
    v.applicationInternalId = application.getId();
    v.stage = pv.getStageTypeId();
    v.policyId = pv.getPolicyId();
    v.policyName = pv.getPolicyName();
    v.threatLevel = pv.getThreatLevel();
    v.threatCategory = pv.getThreatCategory() == null ? null : pv.getThreatCategory().name();
    // Default to "Unknown" so the field is always present in the API contract. Violations tied to a component that
    // is resolvable in the latest report get overwritten with the real type; component-less violations (e.g. pure
    // policy violations) and components absent from the report stay "Unknown" rather than being omitted.
    v.dependencyType = PrioritizedComponent.DEPENDENCY_TYPE_UNKNOWN;
    v.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(pv.getComponentIdentifier());
    v.componentHash = pv.getHash();
    v.reachabilityStatus = pv.getReachabilityStatus() == null ? null : pv.getReachabilityStatus().getName();
    v.openTime = pv.getOpenTime();
    v.fixTime = pv.getFixTime();
    v.waiveTime = pv.getWaiveTime();
    // Legacy (grandfathered) violations follow the identical SLO rules as regular ones; the flag is purely
    // informational so consumers can distinguish them. It is NOT an SLO-suppression signal (only waive_time is).
    v.legacyViolationTime = pv.getLegacyViolationTime();
    v.legacy = pv.getLegacyViolationTime() != null;
    if (pv.getConstraintFactsId() != null) {
      final ConstraintFactsUtil.CveData cve = ConstraintFactsUtil.extractCveData(pv.getConstraintFacts());
      v.vulnerabilityRefId = cve.cveNumber();
      v.cvssScore = cve.cvssScore();
      v.cvssVector = cve.cvssAttackVector();
    }
    return v;
  }
}
