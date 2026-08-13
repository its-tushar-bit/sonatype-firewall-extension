/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.DEFAULT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class DashboardPolicyWaiverDTOAdapterTest
    extends AbstractComponentH2Test
{
  private DashboardPolicyWaiverDTOAdapter dtoAdapter;

  private Organization org;

  private Application app;

  private Policy testPolicy;

  private PolicyWaiver testPolicyWaiver;

  private final Map<String, Owner> ownersById = new HashMap<>();

  private final Map<String, Policy> policiesById = new HashMap<>();

  @BeforeEach
  public void beforeEach() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("testApplication", "testApplication", org.getId());
    ownersById.put(app.getId(), app);
    testPolicyWaiver = createPolicyWaiverWithFullDetails();
    policiesById.put(testPolicy.getId(), testPolicy);
  }

  @Test
  public void testPolicyWaiverToDTO_ExcludeDetails() {
    dtoAdapter = new DashboardPolicyWaiverDTOAdapter(policiesById, ownersById, false);
    DashboardPolicyWaiverDTO dto = dtoAdapter.toDto(
        testPolicyWaiver,
        new PolicyWaiverReason("system", "Other"));

    assertPolicyWaiverWithoutDetails(dto, testPolicyWaiver);
    assertThat(dto.comment).isNull();
    assertThat(dto.constraintFacts).isNull();
    assertThat(dto.creatorId).isNull();
    assertThat(dto.creatorName).isNull();
    assertThat(dto.policyWaiverReason).isNull();
  }

  @Test
  public void testPolicyWaiverToDTO_IncludeDetails() {
    dtoAdapter = new DashboardPolicyWaiverDTOAdapter(policiesById, ownersById, true);

    final var policyWaiverReason = new PolicyWaiverReason("system", "Other");
    DashboardPolicyWaiverDTO dto = dtoAdapter.toDto(
        testPolicyWaiver,
        policyWaiverReason);

    assertPolicyWaiverWithoutDetails(dto, testPolicyWaiver);
    assertThat(dto.comment).isEqualTo(testPolicyWaiver.getComment());
    assertThat(dto.constraintFacts).isEqualTo(testPolicyWaiver.getConstraintFacts());
    assertThat(dto.creatorId).isEqualTo(testPolicyWaiver.getCreatorId());
    assertThat(dto.creatorName).isEqualTo(testPolicyWaiver.getCreatorName());
    assertThat(dto.policyWaiverReason).isEqualTo(policyWaiverReason);
  }

  @Test
  public void testAutoPolicyWaiverToDTO_ExcludeDetails() {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        app.getId(),
        7,
        true,
        true,
        "creator",
        "Creator Name",
        new Date());
    dtoAdapter = new DashboardPolicyWaiverDTOAdapter(policiesById, ownersById, false);
    DashboardPolicyWaiverDTO dto = dtoAdapter.toDto(autoPolicyWaiver);

    assertAutoPolicyWaiverWithoutDetails(dto, autoPolicyWaiver);
    assertThat(dto.comment).isNull();
    assertThat(dto.constraintFacts).isNull();
    assertThat(dto.creatorId).isNull();
    assertThat(dto.creatorName).isNull();
  }

  @Test
  public void testAutoPolicyWaiverToDTO_IncludeDetails() {
    AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver(
        app.getId(),
        7,
        true,
        true,
        "creator",
        "Creator Name",
        new Date());
    dtoAdapter = new DashboardPolicyWaiverDTOAdapter(policiesById, ownersById, true);
    DashboardPolicyWaiverDTO dto = dtoAdapter.toDto(autoPolicyWaiver);

    assertAutoPolicyWaiverWithoutDetails(dto, autoPolicyWaiver);
    assertThat(dto.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(dto.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
  }

  private void assertPolicyWaiverWithoutDetails(DashboardPolicyWaiverDTO dto, PolicyWaiver testPolicyWaiver) {
    assertThat(dto.id).isEqualTo(testPolicyWaiver.getId());
    assertThat(dto.threatLevel).isEqualTo(policiesById.get(testPolicyWaiver.getPolicyId()).getThreatLevel());
    assertThat(dto.createTime).isEqualTo(testPolicyWaiver.getCreateTime());
    assertThat(dto.expiryTime).isEqualTo(testPolicyWaiver.getExpiryTime());
    assertThat(dto.policyId).isEqualTo(testPolicyWaiver.getPolicyId());
    assertThat(dto.policyName).isEqualTo(policiesById.get(testPolicyWaiver.getPolicyId()).getName());
    assertThat(dto.ownerId).isEqualTo(testPolicyWaiver.getOwnerId());
    assertThat(dto.ownerName).isEqualTo(ownersById.get(testPolicyWaiver.getOwnerId()).getName());
    assertThat(dto.ownerType).isEqualTo(ownersById.get(testPolicyWaiver.getOwnerId()).getType().toString());
    assertThat(dto.componentMatchStrategy).isEqualTo(testPolicyWaiver.getComponentMatchStrategy());
    assertThat(dto.hash).isEqualTo(testPolicyWaiver.getHash());
    assertThat(dto.componentIdentifier.toComponentIdentifier()).isEqualTo(testPolicyWaiver.getComponentIdentifier());
    assertThat(dto.getDisplayName().toString())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(testPolicyWaiver.getComponentIdentifier()).toString());
    assertThat(dto.componentUpgradeAvailable).isEqualTo(testPolicyWaiver.isComponentUpgradeAvailable());
    assertThat(dto.forContainerImage).isEqualTo(testPolicyWaiver.isForContainerImage());
  }

  private void assertAutoPolicyWaiverWithoutDetails(DashboardPolicyWaiverDTO dto, AutoPolicyWaiver autoPolicyWaiver) {
    assertThat(dto.id).isEqualTo(autoPolicyWaiver.getId());
    assertThat(dto.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(dto.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
    assertThat(dto.expiryTime).isNull();
    assertThat(dto.policyId).isNull();
    assertThat(dto.policyName).isNull();
    assertThat(dto.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(dto.ownerName).isEqualTo(ownersById.get(autoPolicyWaiver.getOwnerId()).getName());
    assertThat(dto.ownerType).isEqualTo(ownersById.get(autoPolicyWaiver.getOwnerId()).getType().toString());
    assertThat(dto.componentMatchStrategy).isEqualTo(DEFAULT);
    assertThat(dto.hash).isNull();
    assertThat(dto.componentIdentifier).isNull();
    assertThat(dto.componentUpgradeAvailable).isFalse();
    assertThat(dto.isAutoWaiver).isTrue();
  }

  private PolicyWaiver createPolicyWaiverWithFullDetails() {
    Instant now = Instant.now();
    Date today = Date.from(now);
    Date aWeekFromNow = Date.from(now.plus(7, ChronoUnit.DAYS));
    testPolicy = tempEntity.newPolicy(app.getId(), "Very bad security threat", 9);

    TriggerReference triggerReference = new TriggerReference(TriggerReference.Type.SECURITY_VULNERABILITY_REFID,
        "vulnerability-1");
    ConditionFact conditionFact =
        new ConditionFact(ConditionTypes.SecurityVulnerabilityStatusConditionType.getId(), 0, "summary", "reason",
            triggerReference);
    TreeMap<String, String> coordinates = new TreeMap<>()
    {
      {
        this.put("artifactId", "a1");
        this.put("groupId", "g1");
        this.put("version", "v1");
        this.put("classifier", "c1");
        this.put("extension", "jar");
      }
    };
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    ConstraintFact constraintFact = new ConstraintFact("constraint id", "constraint name", "operator", conditionFact);

    PolicyWaiver policyWaiver = new PolicyWaiver()
        .setHash("hash")
        .setPolicyId(testPolicy.getId())
        .setOwnerId(app.getId())
        .setConstraintFacts(singletonList(constraintFact))
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("a comment")
        .setCreateTime(today)
        .setExpiryTime(aWeekFromNow)
        .setComponentUpgradeAvailable(true)
        .setForContainerImage(false);

    return tempEntity.newWaiver(policyWaiver);
  }
}
