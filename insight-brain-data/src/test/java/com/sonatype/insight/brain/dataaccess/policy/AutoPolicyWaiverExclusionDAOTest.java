/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoPolicyWaiverExclusionDAOTest
    extends AbstractDbDAOTest
{
  private AutoPolicyWaiverExclusionDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createAutoPolicyWaiverExclusionDAO();
  }

  @Test
  public void testCRUD() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(app.getId());

    // Create
    AutoPolicyWaiverExclusion exclusion = new AutoPolicyWaiverExclusion(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverOne.getId(),
        "scanId",
        "fakehash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    dao.insert(exclusion);

    // Read
    AutoPolicyWaiverExclusion instance = dao.getById(exclusion.getId());
    assertThat(instance.getId()).isEqualTo(exclusion.getId());
    assertThat(instance.getOwnerId()).isEqualTo(exclusion.getOwnerId());
    assertThat(instance.getCreatorId()).isEqualTo(exclusion.getCreatorId());
    assertThat(instance.getCreatorName()).isEqualTo(exclusion.getCreatorName());
    assertThat(instance.getCreateTime()).isEqualTo(exclusion.getCreateTime());
    assertThat(instance.getHash()).isEqualTo(exclusion.getHash());
    assertThat(instance.getComponentIdentifier()).isEqualTo(exclusion.getComponentIdentifier());
    assertThat(instance.getScanId()).isEqualTo(exclusion.getScanId());
    assertThat(instance.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    // Update
    instance.setHash("anotherFakeHash");
    instance.setScanId("anotherFakeScanId");
    instance.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);
    dao.update(instance);
    instance = dao.getById(exclusion.getId());
    assertThat(instance.getHash()).isEqualTo("anotherFakeHash");
    assertThat(instance.getScanId()).isEqualTo("anotherFakeScanId");
    assertThat(instance.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForExclusion.ALL_VERSIONS);

    // Delete
    dao.delete(instance);
    instance = dao.getById(exclusion.getId());
    assertThat(instance).isNull();
  }

  @Test
  public void testGetByOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverExclusion exclusionOne = new AutoPolicyWaiverExclusion(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverOne.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionTwo = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionThree = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionFour = new AutoPolicyWaiverExclusion(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    dao.insert(exclusionOne);
    dao.insert(exclusionTwo);
    dao.insert(exclusionThree);
    dao.insert(exclusionFour);

    List<AutoPolicyWaiverExclusion> appOneExclusions = dao.getByOwnerId(app.getId());
    assertThat(appOneExclusions).hasSize(1).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(app.getId());
    });

    List<AutoPolicyWaiverExclusion> parentOrgExclusions = dao.getByOwnerId(app.getOrganizationId());
    assertThat(parentOrgExclusions).hasSize(2).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(app.getOrganizationId());
    });

    List<AutoPolicyWaiverExclusion> otherOrgExclusions = dao.getByOwnerId(otherOrg.getId());
    assertThat(otherOrgExclusions).hasSize(1).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(otherOrg.getId());
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverThree.getId());
    });
  }

  @Test
  public void testGetByOwnerIdAndHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverExclusion exclusionOne = new AutoPolicyWaiverExclusion(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverOne.getId(),
        "fakeScan",
        "fakeHashOne",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionTwo = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHashTwo",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionThree = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHashThree",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionFour = new AutoPolicyWaiverExclusion(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHashTwo",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    dao.insert(exclusionOne);
    dao.insert(exclusionTwo);
    dao.insert(exclusionThree);
    dao.insert(exclusionFour);

    List<AutoPolicyWaiverExclusion> appOneExclusions = dao.getByOwnerIdAndHash(app.getId(), "fakeHashOne");
    assertThat(appOneExclusions).hasSize(1).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(app.getId());
      assertThat(waiverExclusion.getHash()).isEqualTo("fakeHashOne");
    });

    List<AutoPolicyWaiverExclusion> nonExtantHashes = dao.getByOwnerIdAndHash(app.getId(), "fakeHashTen");
    assertThat(nonExtantHashes).hasSize(0);

    List<AutoPolicyWaiverExclusion> parentOrgExclusions =
        dao.getByOwnerIdAndHash(app.getOrganizationId(), "fakeHashTwo");
    assertThat(parentOrgExclusions).hasSize(1).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(app.getOrganizationId());
      assertThat(waiverExclusion.getHash()).isEqualTo("fakeHashTwo");
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverTwo.getId());
    });

    List<AutoPolicyWaiverExclusion> otherOrgExclusions =
        dao.getByOwnerIdAndHash(otherOrg.getId(), "fakeHashTwo");
    assertThat(otherOrgExclusions).hasSize(1).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(otherOrg.getId());
      assertThat(waiverExclusion.getHash()).isEqualTo("fakeHashTwo");
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverThree.getId());
    });
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverForApp = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    List<AutoPolicyWaiverExclusion> exclusionsOne =
        dao.getByOwnerIdAndAutoPolicyWaiverId(app.getId(), autoPolicyWaiverForApp.getId());
    assertThat(exclusionsOne).hasSize(0);

    AutoPolicyWaiverExclusion exclusion = new AutoPolicyWaiverExclusion(
        app.getId(),
        "fake",
        "fake",
        new Date(),
        autoPolicyWaiverForApp.getId(),
        "scanId",
        "hash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    dao.insert(exclusion);

    List<AutoPolicyWaiverExclusion> exclusionsTwo =
        dao.getByOwnerIdAndAutoPolicyWaiverId(app.getId(), autoPolicyWaiverForApp.getId());
    assertThat(exclusionsTwo).hasSize(1).allSatisfy(waiverExclusion -> {
      waiverExclusion.getAutoPolicyWaiverId().equals(autoPolicyWaiverForApp.getId());
      waiverExclusion.getOwnerId().equals(app.getId());
    });
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverIdAndHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverForApp = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    AutoPolicyWaiverExclusion exclusionOne =
        dao.getByOwnerIdAndAutoPolicyWaiverIdAndHash(app.getId(), autoPolicyWaiverForApp.getId(), "hash");
    assertThat(exclusionOne).isNull();

    AutoPolicyWaiverExclusion exclusion = new AutoPolicyWaiverExclusion(
        app.getId(),
        "fake",
        "fake",
        new Date(),
        autoPolicyWaiverForApp.getId(),
        "scanId",
        "hash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    dao.insert(exclusion);

    AutoPolicyWaiverExclusion exclusionTwo =
        dao.getByOwnerIdAndAutoPolicyWaiverIdAndHash(app.getId(), autoPolicyWaiverForApp.getId(), "hash");
    assertThat(exclusionTwo).isNotNull();
    assertThat(exclusionTwo.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverForApp.getId());
    assertThat(exclusionTwo.getOwnerId()).isEqualTo(app.getId());
    assertThat(exclusionTwo.getHash()).isEqualTo("hash");

    AutoPolicyWaiverExclusion exclusionThree = dao.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
        app.getOrganizationId(),
        autoPolicyWaiverForApp.getId(),
        "hash");
    assertThat(exclusionThree).isNull();
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverIdPaginated() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());

    List<AutoPolicyWaiverExclusion> resultOne = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiver.getOwnerId(),
        waiver.getId(),
        1,
        3);

    assertThat(resultOne).hasSize(3);

    List<AutoPolicyWaiverExclusion> resultTwo = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiver.getOwnerId(),
        waiver.getId(),
        2,
        3);
    assertThat(resultTwo).hasSize(2);
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverIdPaginated_MultipleAppsAndWaivers() {
    Application appOne = tempEntity.newApplicationWithParent();
    Application appTwo = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiverOne = tempEntity.newAutoPolicyWaiver(appOne.getId());
    AutoPolicyWaiver waiverTwo = tempEntity.newAutoPolicyWaiver(appTwo.getId());

    tempEntity.newAutoPolicyWaiverExclusion(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverExclusion(appTwo.getId(), waiverTwo.getId());

    List<AutoPolicyWaiverExclusion> resultOne = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverOne.getOwnerId(),
        waiverOne.getId(),
        1,
        3);

    assertThat(resultOne).hasSize(3).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(waiverOne.getOwnerId());
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(waiverOne.getId());
    });

    List<AutoPolicyWaiverExclusion> resultTwo = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverOne.getOwnerId(),
        waiverOne.getId(),
        2,
        3);

    assertThat(resultTwo).hasSize(2).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(waiverOne.getOwnerId());
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(waiverOne.getId());
    });

    List<AutoPolicyWaiverExclusion> resultThree = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverTwo.getOwnerId(),
        waiverTwo.getId(),
        1,
        3);

    assertThat(resultThree).hasSize(3).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(waiverTwo.getOwnerId());
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(waiverTwo.getId());
    });

    List<AutoPolicyWaiverExclusion> resultFour = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverTwo.getOwnerId(),
        waiverTwo.getId(),
        2,
        3);

    assertThat(resultFour).hasSize(2).allSatisfy(waiverExclusion -> {
      assertThat(waiverExclusion.getOwnerId()).isEqualTo(waiverTwo.getOwnerId());
      assertThat(waiverExclusion.getAutoPolicyWaiverId()).isEqualTo(waiverTwo.getId());
    });
  }

  @Test
  public void testGetByOwnerIdPolicyViolation_NoMatch() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiverExclusion(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        waiver.getId(),
        eval.getScanId(),
        violation.getHash(),
        ComponentMatcherStrategyForExclusion.POLICY_VIOLATION,
        violation.getId(),
        violation.getThreatLevel(),
        "CVE-111-223",
        policy.getName(),
        "a1 v1",
        policy.getId(),
        identifier,
        violation.getConstraintFacts());

    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverExclusion exclusionTwo = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionThree = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionFour = new AutoPolicyWaiverExclusion(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    dao.insert(exclusionTwo);
    dao.insert(exclusionThree);
    dao.insert(exclusionFour);

    // when
    AutoPolicyWaiverExclusion result = dao.getByOwnerIdPolicyViolation(
        app.getId(),
        waiver.getId(),
        "someotherPolicyViolationId");

    // then
    assertThat(result).isNull();
  }

  @Test
  public void testGetByOwnerIdPolicyViolation_Match() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        waiver.getId(),
        eval.getScanId(),
        violation.getHash(),
        ComponentMatcherStrategyForExclusion.POLICY_VIOLATION,
        violation.getId(),
        violation.getThreatLevel(),
        "CVE-111-223",
        policy.getName(),
        "a1 v1",
        policy.getId(),
        identifier,
        violation.getConstraintFacts());

    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverExclusion exclusionTwo = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionThree = new AutoPolicyWaiverExclusion(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    AutoPolicyWaiverExclusion exclusionFour = new AutoPolicyWaiverExclusion(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    dao.insert(exclusionTwo);
    dao.insert(exclusionThree);
    dao.insert(exclusionFour);

    // when
    AutoPolicyWaiverExclusion result = dao.getByOwnerIdPolicyViolation(
        app.getId(),
        waiver.getId(),
        violation.getId());

    // then
    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(app.getId());
    assertThat(result.getAutoPolicyWaiverId()).isEqualTo(waiver.getId());
    assertThat(result.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForExclusion.POLICY_VIOLATION);
    assertThat(result.getPolicyId()).isEqualTo(exclusion.getPolicyId());
    assertThat(result.getThreatLevel()).isEqualTo(exclusion.getThreatLevel());
    assertThat(result.getPolicyName()).isEqualTo(exclusion.getPolicyName());
    assertThat(result.getConstraintFactsJson()).isEqualTo(exclusion.getConstraintFactsJson());
    assertThat(result.getPolicyViolationId()).isEqualTo(exclusion.getPolicyViolationId());
    assertThat(result.getHash()).isEqualTo(exclusion.getHash());
    assertThat(result.getComponentIdentifier()).isEqualTo(exclusion.getComponentIdentifier());
  }
}
