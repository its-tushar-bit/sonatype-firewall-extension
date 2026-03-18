/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;

/**
 * Utility class that generates a hash from a policy violation diff and a remediation map, which are used in
 * PR comments content creation. Only relevant information for PR commenting contributes to the hash.
 * <p>
 * The resulting hash is used to determine if a an existing PR comment needs to be updated or not
 * (i.e. on update, if the current computed hash is equal to the previous one, the update is skipped).
 */
public class PullRequestCommentingHashBuilder
{
  private static final BaseEncoding encoder = BaseEncoding.base16().lowerCase();

  private PolicyViolationDiff<PolicyViolation> policyViolationDiff;

  private SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap;

  public PullRequestCommentingHashBuilder withPolicyViolationDiff(
      PolicyViolationDiff<PolicyViolation> policyViolationDiff)
  {
    this.policyViolationDiff = policyViolationDiff;
    return this;
  }

  public PullRequestCommentingHashBuilder withRemediationVersionMap(
      SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap)
  {
    this.remediationVersionMap = remediationVersionMap;
    return this;
  }

  public String generateHash() throws NoSuchAlgorithmException {
    Preconditions.checkNotNull(policyViolationDiff, "Policy Violation Diff is required and cannot be null");
    Preconditions.checkNotNull(remediationVersionMap, "Remediation Version Map is required and cannot be null");

    StringBuilder stringBuilder = new StringBuilder(4000);
    collectRelevantDataFromDiff(stringBuilder, policyViolationDiff);
    collectRelevantDataFromRemediations(stringBuilder, remediationVersionMap);

    byte[] digest = MessageDigest.getInstance("SHA-1")
        .digest(
            stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
    return encoder.encode(digest);
  }

  private void collectRelevantDataFromDiff(
      final StringBuilder stringBuilder,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff)
  {
    stringBuilder.append("added:\n");
    collectRelevantDataFromViolationList(stringBuilder, policyViolationDiff.getAppeared());
    stringBuilder.append("removed:\n");
    collectRelevantDataFromViolationList(stringBuilder, policyViolationDiff.getCleared());
  }

  private void collectRelevantDataFromRemediations(
      final StringBuilder stringBuilder,
      final SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap)
  {
    stringBuilder.append("remediationMap:\n");
    for (Entry<ComponentIdentifier, RemediationVersionDTO> entry : remediationVersionMap.entrySet()) {
      stringBuilder.append(entry.getKey().toString()); // component identifier
      stringBuilder.append(" : ");
      stringBuilder.append(entry.getValue().getVersion()); // remediation version
      stringBuilder.append('\n');
    }
  }

  private void collectRelevantDataFromViolationList(
      final StringBuilder stringBuilder,
      final List<PolicyViolation> list)
  {
    // Policy violations need to be grouped by component
    final SortedMap<String, List<PolicyViolation>> componentPolicyViolationsMap = list
        .stream()
        .collect(Collectors.groupingBy(x -> {
          if (null != x.getComponentIdentifier()) {
            return ComponentDisplayNameUtil.fromIdentifier(x.getComponentIdentifier()).toString();
          }
          else {
            return x.getHash();
          }
        }, TreeMap::new, Collectors.toList()));

    // Ensure the policy violation order is always the same
    ensureConsistentOrdering(componentPolicyViolationsMap.values());

    // collect relevant information
    collectRelevantDataFromComponentViolations(stringBuilder, componentPolicyViolationsMap);
  }

  /**
   * Only relevant information contributes to the content to be hashed:
   * <ul>
   * <li>For each {@link ComponentIdentifier} - its string representation (toString())
   * <li>For each {@link PolicyViolation} - policy name and threat level
   * <li>For each {@link ConstraintFact} - constraint name
   * <li>For each {@link ConditionFact} - reason and reference's value field
   * </ul>
   */
  private void collectRelevantDataFromComponentViolations(
      final StringBuilder stringBuilder,
      final SortedMap<String, List<PolicyViolation>> componentPolicyViolationsMap)
  {
    for (Entry<String, List<PolicyViolation>> entry : componentPolicyViolationsMap.entrySet()) {
      stringBuilder.append(entry.getKey()); // component identifier
      for (PolicyViolation violation : entry.getValue()) {
        stringBuilder.append(violation.getPolicyName());
        stringBuilder.append(violation.getThreatLevel());
        for (ConstraintFact fact : violation.getConstraintFacts()) {
          stringBuilder.append(fact.getConstraintName());
          for (ConditionFact conditionFact : fact.getConditionFacts()) {
            stringBuilder.append(conditionFact.getReason());
            if (conditionFact.getReference() != null) {
              stringBuilder.append(conditionFact.getReference().getValue());
            }
          }
        }
      }
      stringBuilder.append('\n');
    }
  }

  private void ensureConsistentOrdering(final Collection<List<PolicyViolation>> listOfLists) {
    for (List<PolicyViolation> violations : listOfLists) {
      // sort policy violations
      violations.sort(
          Comparator.comparing(PolicyViolation::getPolicyName).thenComparing(PolicyViolation::getThreatLevel));
    }
  }
}
