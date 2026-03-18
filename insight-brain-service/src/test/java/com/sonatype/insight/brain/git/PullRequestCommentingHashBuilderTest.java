/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.policy.TriggerReference.Type;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.security.CurrentUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestCommentingHashBuilderTest
{
  private final ComponentIdentifier[] identifiers = new ComponentIdentifier[]{
    ComponentIdentifier.createNpmCoordinates("comp-1", "1.1.0"),
    ComponentIdentifier.createNpmCoordinates("comp-2", "1.2.0"),
    ComponentIdentifier.createNpmCoordinates("comp-3", "1.3.0"),
    ComponentIdentifier.createNpmCoordinates("comp-4", "1.4.0")
  };

  private final String[] componentHashes = new String[]{
    "HASH_0",
    "HASH_1",
    "HASH_2",
    "HASH_3"
  };

  private PolicyEvaluation evaluation;

  @Before
  public void setUp() {
    evaluation = new PolicyEvaluation("app-id", "stage-type-id", "scan-id", CurrentUser.SYSTEM, ScanTriggerType.CLI);
    evaluation.setTime(new Date(System.currentTimeMillis() - 12345));
  }

  @Test
  public void testGenerateHash_ViolationsAndRemediations_Success() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff = new PolicyViolationDiffBuilder()
        .withAddedViolations(2)
        .withUnknownComponentsAdded(2)
        .withRemovedViolations(1)
        .withUnknownComponentsRemoved(1)
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then: the hash is successfully calculated
    assertThat(hash).isNotNull();
    assertThat(hash.length()).isEqualTo(40);
  }

  @Test
  public void testGenerateHash_ViolationsAndNoRemediations_Success() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff = new PolicyViolationDiffBuilder().build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(0);

    // when:
    String hash = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then: the hash is successfully calculated
    assertThat(hash).isNotNull();
    assertThat(hash.length()).isEqualTo(40);
  }

  @Test
  public void testGenerateHash_ClearedViolationsOnlyAndNoRemediations_Success() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff = new PolicyViolationDiffBuilder()
        .withAddedViolations(0)
        .withUnknownComponentsAdded(0)
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(0);

    // when:
    String hash = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then: the hash is successfully calculated
    assertThat(hash).isNotNull();
    assertThat(hash.length()).isEqualTo(40);
  }

  @Test
  public void testGenerateHash_NoViolationsOnlyAndNoRemediations_Success() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff = new PolicyViolationDiffBuilder()
        .withAddedViolations(0)
        .withUnknownComponentsAdded(0)
        .withRemovedViolations(0)
        .withUnknownComponentsRemoved(0)
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(0);

    // when:
    String hash = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then: the hash is successfully calculated
    assertThat(hash).isNotNull();
    assertThat(hash.length()).isEqualTo(40);
  }

  @Test
  public void testGenerateHash_NoConditionFactReferences_Success() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff = new PolicyViolationDiffBuilder()
        .withAddedViolations(2)
        .withUnknownComponentsAdded(2)
        .withRemovedViolations(1)
        .withUnknownComponentsRemoved(1)
        .withNoReferencesForConditionFacts()
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then: the hash is successfully calculated
    assertThat(hash).isNotNull();
    assertThat(hash.length()).isEqualTo(40);
  }

  @Test
  public void testGenerateHash_NoChanges_SameHash() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff1 = new PolicyViolationDiffBuilder().build();
    PolicyViolationDiff<PolicyViolation> diff2 = new PolicyViolationDiffBuilder().build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash1 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff1)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();
    String hash2 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff2)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then:
    assertThat(hash1).isNotNull();
    assertThat(hash2).isNotNull();
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void testGenerateHash_DifferentOrderOnly_SameHash() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff1 = new PolicyViolationDiffBuilder()
        .withAddedViolations(2)
        .withUnknownComponentsAdded(2)
        .withRemovedViolations(2)
        .withUnknownComponentsRemoved(2)
        .build();
    PolicyViolationDiff<PolicyViolation> diff2 = new PolicyViolationDiffBuilder()
        .withAddedViolations(2)
        .withUnknownComponentsAdded(2)
        .withRemovedViolations(2)
        .withUnknownComponentsRemoved(2)
        .withReversedViolationOrder()
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash1 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff1)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();
    String hash2 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff2)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then:
    assertThat(hash1).isNotNull();
    assertThat(hash2).isNotNull();
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void testGenerateHash_DifferentViolations_DifferentHash() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff1 = new PolicyViolationDiffBuilder()
        .withAddedViolations(3)
        .withUnknownComponentsAdded(3)
        .build();
    PolicyViolationDiff<PolicyViolation> diff2 = new PolicyViolationDiffBuilder()
        .withAddedViolations(2)
        .withUnknownComponentsAdded(2)
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash1 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff1)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();
    String hash2 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff2)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then:
    assertThat(hash1).isNotNull();
    assertThat(hash2).isNotNull();
    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  public void testGenerateHash_DifferentViolations_KnownOnly_DifferentHash() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff1 = new PolicyViolationDiffBuilder()
        .withAddedViolations(3)
        .withUnknownComponentsAdded(0)
        .build();
    PolicyViolationDiff<PolicyViolation> diff2 = new PolicyViolationDiffBuilder()
        .withAddedViolations(2)
        .withUnknownComponentsAdded(0)
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash1 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff1)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();
    String hash2 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff2)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then:
    assertThat(hash1).isNotNull();
    assertThat(hash2).isNotNull();
    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  public void testGenerateHash_DifferentViolations_UnknownOnly_DifferentHash() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff1 = new PolicyViolationDiffBuilder()
        .withAddedViolations(0)
        .withUnknownComponentsAdded(3)
        .build();
    PolicyViolationDiff<PolicyViolation> diff2 = new PolicyViolationDiffBuilder()
        .withAddedViolations(0)
        .withUnknownComponentsAdded(2)
        .build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = createRemediationVersionMap(2);

    // when:
    String hash1 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff1)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();
    String hash2 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff2)
        .withRemediationVersionMap(remediationVersionMap)
        .generateHash();

    // then:
    assertThat(hash1).isNotNull();
    assertThat(hash2).isNotNull();
    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  public void testGenerateHash_DifferentRemediations_DifferentHash() throws Exception {
    // given:
    PolicyViolationDiff<PolicyViolation> diff1 = new PolicyViolationDiffBuilder().build();
    PolicyViolationDiff<PolicyViolation> diff2 = new PolicyViolationDiffBuilder().build();
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap1 = createRemediationVersionMap(1);
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap2 = createRemediationVersionMap(2);

    // when:
    String hash1 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff1)
        .withRemediationVersionMap(remediationVersionMap1)
        .generateHash();
    String hash2 = new PullRequestCommentingHashBuilder()
        .withPolicyViolationDiff(diff2)
        .withRemediationVersionMap(remediationVersionMap2)
        .generateHash();

    // then:
    assertThat(hash1).isNotNull();
    assertThat(hash2).isNotNull();
    assertThat(hash1).isNotEqualTo(hash2);
  }

  private SortedMap<ComponentIdentifier, RemediationVersionDTO> createRemediationVersionMap(final int entryCount) {
    SortedMap<ComponentIdentifier, RemediationVersionDTO> map = new TreeMap<>();
    for (int i = 0; i < entryCount; i++) {
      map.put(identifiers[i], new RemediationVersionDTO("2.0.0", ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));
    }
    return map;
  }

  private class PolicyViolationDiffBuilder
  {
    int addedViolations = 1;

    int removedViolations = 1;

    int unknownComponentAddedViolations = 1;

    int unknownComponentRemovedViolations = 1;

    boolean reverseOrder = false;

    private int constraintFactsPerViolation = 1;

    private int conditionFactsPerConstraintFact = 1;

    private boolean conditionFactsHaveReferences = true;

    PolicyViolationDiffBuilder withAddedViolations(int addedViolations) {
      this.addedViolations = addedViolations;
      return this;
    }

    PolicyViolationDiffBuilder withRemovedViolations(int removedViolations) {
      this.removedViolations = removedViolations;
      return this;
    }

    public PolicyViolationDiffBuilder withReversedViolationOrder() {
      reverseOrder = true;
      return this;
    }

    public PolicyViolationDiffBuilder withNoReferencesForConditionFacts() {
      conditionFactsHaveReferences = false;
      return this;
    }

    public PolicyViolationDiffBuilder withUnknownComponentsAdded(int unknownComponentsAdded) {
      this.unknownComponentAddedViolations = unknownComponentsAdded;
      return this;
    }

    public PolicyViolationDiffBuilder withUnknownComponentsRemoved(int unknownComponentsRemoved) {
      this.unknownComponentRemovedViolations = unknownComponentsRemoved;
      return this;
    }

    PolicyViolationDiff<PolicyViolation> build() {
      PolicyViolationDiff<PolicyViolation> diff = new PolicyViolationDiff<>();
      buildViolations(false, diff);
      buildViolations(true, diff);
      return diff;
    }

    private void buildViolations(
        boolean isUnknownComponents,
        final PolicyViolationDiff<PolicyViolation> diff)
    {
      int addedCount;
      int removedCount;
      if (!isUnknownComponents) {
        addedCount = this.addedViolations;
        removedCount = this.removedViolations;
      }
      else {
        addedCount = this.unknownComponentAddedViolations;
        removedCount = this.unknownComponentRemovedViolations;
      }

      int k = 0;
      if (reverseOrder) {
        k = addedCount - 1;
      }
      for (int i = 0; i < addedCount; i++) {
        PolicyViolationBuilder violationBuilder = new PolicyViolationBuilder();
        ComponentIdentifier ci = isUnknownComponents ? null : identifiers[k];
        String hash = isUnknownComponents ? componentHashes[k] : "hash";
        if (reverseOrder) {
          k--;
        }
        else {
          k++;
        }
        diff.addAppeared(violationBuilder.withComponentIdentifier(ci)
            .withHash(hash)
            .withConstraintFacts(constraintFactsPerViolation)
            .withConditionFacts(conditionFactsPerConstraintFact)
            .withConditionFactReference(conditionFactsHaveReferences)
            .build());
      }
      if (reverseOrder) {
        k = addedCount + removedCount - 1;
      }
      for (int i = 0; i < removedCount; i++) {
        PolicyViolationBuilder violationBuilder = new PolicyViolationBuilder();
        ComponentIdentifier ci = isUnknownComponents ? null : identifiers[k];
        String hash = isUnknownComponents ? componentHashes[k] : "hash";
        if (reverseOrder) {
          k--;
        }
        else {
          k++;
        }
        diff.addAppeared(violationBuilder.withComponentIdentifier(ci)
            .withHash(hash)
            .withConstraintFacts(constraintFactsPerViolation)
            .withConditionFacts(conditionFactsPerConstraintFact)
            .withConditionFactReference(conditionFactsHaveReferences)
            .build());
      }
    }
  }

  private class PolicyViolationBuilder
  {
    private int constraintFacts = 1;

    private int conditionFacts = 1;

    private boolean hasReference = true;

    private ComponentIdentifier componentIdentifier;

    private String hash = "hash";

    PolicyViolationBuilder withComponentIdentifier(ComponentIdentifier componentIdentifier) {
      this.componentIdentifier = componentIdentifier;
      return this;
    }

    PolicyViolationBuilder withConstraintFacts(int constraintFacts) {
      this.constraintFacts = constraintFacts;
      return this;
    }

    PolicyViolationBuilder withConditionFacts(int conditionFacts) {
      this.conditionFacts = conditionFacts;
      return this;
    }

    PolicyViolationBuilder withConditionFactReference(boolean hasReference) {
      this.hasReference = hasReference;
      return this;
    }

    PolicyViolationBuilder withHash(String hash) {
      this.hash = hash;
      return this;
    }

    PolicyViolation build() {
      List<ConstraintFact> constraintFactList = new LinkedList<>();
      for (int i = 0; i < constraintFacts; i++) {
        ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
        for (int j = 0; j < conditionFacts; j++) {
          ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
              j, "some summary" + j, "some reason" + j);
          if (hasReference) {
            conditionFact.setReference(new TriggerReference(Type.SECURITY_VULNERABILITY_REFID, "refid-" + j));
          }
          constraintFact.addConditionFact(conditionFact);
          constraintFactList.add(constraintFact);
        }
      }
      return new PolicyViolation(evaluation, "policyId", "policyName", 5, PolicyThreatCategory.LICENSE, hash,
          hash.equals("hash") ? componentIdentifier : null, constraintFactList, null);
    }
  }
}
