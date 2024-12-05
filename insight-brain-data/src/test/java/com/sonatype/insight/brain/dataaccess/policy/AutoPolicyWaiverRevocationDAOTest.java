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
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoPolicyWaiverRevocationDAOTest
    extends AbstractDbDAOTest
{
  private AutoPolicyWaiverRevocationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createAutoPolicyWaiverRevocationDAO();
  }

  @Test
  public void testCRUD() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(app.getId());

    // Create
    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverOne.getId(),
        "scanId",
        "fakehash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    dao.insert(revocation);

    // Read
    AutoPolicyWaiverRevocation instance = dao.getById(revocation.getId());
    assertThat(instance.getId()).isEqualTo(revocation.getId());
    assertThat(instance.getOwnerId()).isEqualTo(revocation.getOwnerId());
    assertThat(instance.getCreatorId()).isEqualTo(revocation.getCreatorId());
    assertThat(instance.getCreatorName()).isEqualTo(revocation.getCreatorName());
    assertThat(instance.getCreateTime()).isEqualTo(revocation.getCreateTime());
    assertThat(instance.getHash()).isEqualTo(revocation.getHash());
    assertThat(instance.getComponentIdentifier()).isEqualTo(revocation.getComponentIdentifier());
    assertThat(instance.getScanId()).isEqualTo(revocation.getScanId());
    assertThat(instance.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);

    // Update
    instance.setHash("anotherFakeHash");
    instance.setScanId("anotherFakeScanId");
    instance.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    dao.update(instance);
    instance = dao.getById(revocation.getId());
    assertThat(instance.getHash()).isEqualTo("anotherFakeHash");
    assertThat(instance.getScanId()).isEqualTo("anotherFakeScanId");
    assertThat(instance.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);

    // Delete
    dao.delete(instance);
    instance = dao.getById(revocation.getId());
    assertThat(instance).isNull();
  }

  @Test
  public void testGetByOwnerId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverRevocation revocationOne = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverOne.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationTwo = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationThree = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationFour = new AutoPolicyWaiverRevocation(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    dao.insert(revocationOne);
    dao.insert(revocationTwo);
    dao.insert(revocationThree);
    dao.insert(revocationFour);

    List<AutoPolicyWaiverRevocation> appOneRevocations = dao.getByOwnerId(app.getId());
    assertThat(appOneRevocations).hasSize(1).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(app.getId());
    });

    List<AutoPolicyWaiverRevocation> parentOrgRevocations = dao.getByOwnerId(app.getOrganizationId());
    assertThat(parentOrgRevocations).hasSize(2).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(app.getOrganizationId());
    });

    List<AutoPolicyWaiverRevocation> otherOrgRevocations = dao.getByOwnerId(otherOrg.getId());
    assertThat(otherOrgRevocations).hasSize(1).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(otherOrg.getId());
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverThree.getId());
    });
  }

  @Test
  public void testGetByOwnerIdAndHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverRevocation revocationOne = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverOne.getId(),
        "fakeScan",
        "fakeHashOne",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationTwo = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHashTwo",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationThree = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHashThree",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationFour = new AutoPolicyWaiverRevocation(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHashTwo",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    dao.insert(revocationOne);
    dao.insert(revocationTwo);
    dao.insert(revocationThree);
    dao.insert(revocationFour);

    List<AutoPolicyWaiverRevocation> appOneRevocations = dao.getByOwnerIdAndHash(app.getId(), "fakeHashOne");
    assertThat(appOneRevocations).hasSize(1).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(app.getId());
      assertThat(waiverRevocation.getHash()).isEqualTo("fakeHashOne");
    });

    List<AutoPolicyWaiverRevocation> nonExtantHashes = dao.getByOwnerIdAndHash(app.getId(), "fakeHashTen");
    assertThat(nonExtantHashes).hasSize(0);

    List<AutoPolicyWaiverRevocation> parentOrgRevocations =
        dao.getByOwnerIdAndHash(app.getOrganizationId(), "fakeHashTwo");
    assertThat(parentOrgRevocations).hasSize(1).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(app.getOrganizationId());
      assertThat(waiverRevocation.getHash()).isEqualTo("fakeHashTwo");
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverTwo.getId());
    });

    List<AutoPolicyWaiverRevocation> otherOrgRevocations =
        dao.getByOwnerIdAndHash(otherOrg.getId(), "fakeHashTwo");
    assertThat(otherOrgRevocations).hasSize(1).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(otherOrg.getId());
      assertThat(waiverRevocation.getHash()).isEqualTo("fakeHashTwo");
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverThree.getId());
    });
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverId() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverForApp = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    List<AutoPolicyWaiverRevocation> revocationsOne =
        dao.getByOwnerIdAndAutoPolicyWaiverId(app.getId(), autoPolicyWaiverForApp.getId());
    assertThat(revocationsOne).hasSize(0);

    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fake",
        "fake",
        new Date(),
        autoPolicyWaiverForApp.getId(),
        "scanId",
        "hash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    dao.insert(revocation);

    List<AutoPolicyWaiverRevocation> revocationsTwo =
        dao.getByOwnerIdAndAutoPolicyWaiverId(app.getId(), autoPolicyWaiverForApp.getId());
    assertThat(revocationsTwo).hasSize(1).allSatisfy(waiverRevocation -> {
      waiverRevocation.getAutoPolicyWaiverId().equals(autoPolicyWaiverForApp.getId());
      waiverRevocation.getOwnerId().equals(app.getId());
    });
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverIdAndHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiverForApp = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiver(app.getOrganizationId());

    AutoPolicyWaiverRevocation revocationOne =
        dao.getByOwnerIdAndAutoPolicyWaiverIdAndHash(app.getId(), autoPolicyWaiverForApp.getId(), "hash");
    assertThat(revocationOne).isNull();

    AutoPolicyWaiverRevocation revocation = new AutoPolicyWaiverRevocation(
        app.getId(),
        "fake",
        "fake",
        new Date(),
        autoPolicyWaiverForApp.getId(),
        "scanId",
        "hash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    dao.insert(revocation);

    AutoPolicyWaiverRevocation revocationTwo =
        dao.getByOwnerIdAndAutoPolicyWaiverIdAndHash(app.getId(), autoPolicyWaiverForApp.getId(), "hash");
    assertThat(revocationTwo).isNotNull();
    assertThat(revocationTwo.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverForApp.getId());
    assertThat(revocationTwo.getOwnerId()).isEqualTo(app.getId());
    assertThat(revocationTwo.getHash()).isEqualTo("hash");

    AutoPolicyWaiverRevocation revocationThree = dao.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
        app.getOrganizationId(),
        autoPolicyWaiverForApp.getId(),
        "hash"
    );
    assertThat(revocationThree).isNull();
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverIdPaginated() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());

    List<AutoPolicyWaiverRevocation> resultOne = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiver.getOwnerId(),
        waiver.getId(),
        1,
        3
    );

    assertThat(resultOne).hasSize(3);

    List<AutoPolicyWaiverRevocation> resultTwo = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiver.getOwnerId(),
        waiver.getId(),
        2,
        3
    );
    assertThat(resultTwo).hasSize(2);
  }

  @Test
  public void testGetByOwnerIdAndAutoPolicyWaiverIdPaginated_MultipleAppsAndWaivers() {
    Application appOne = tempEntity.newApplicationWithParent();
    Application appTwo = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiverOne = tempEntity.newAutoPolicyWaiver(appOne.getId());
    AutoPolicyWaiver waiverTwo = tempEntity.newAutoPolicyWaiver(appTwo.getId());

    tempEntity.newAutoPolicyWaiverRevocation(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appOne.getId(), waiverOne.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appTwo.getId(), waiverTwo.getId());
    tempEntity.newAutoPolicyWaiverRevocation(appTwo.getId(), waiverTwo.getId());

    List<AutoPolicyWaiverRevocation> resultOne = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverOne.getOwnerId(),
        waiverOne.getId(),
        1,
        3
    );

    assertThat(resultOne).hasSize(3).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(waiverOne.getOwnerId());
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(waiverOne.getId());
    });

    List<AutoPolicyWaiverRevocation> resultTwo = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverOne.getOwnerId(),
        waiverOne.getId(),
        2,
        3
    );

    assertThat(resultTwo).hasSize(2).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(waiverOne.getOwnerId());
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(waiverOne.getId());
    });

    List<AutoPolicyWaiverRevocation> resultThree = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverTwo.getOwnerId(),
        waiverTwo.getId(),
        1,
        3
    );

    assertThat(resultThree).hasSize(3).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(waiverTwo.getOwnerId());
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(waiverTwo.getId());
    });

    List<AutoPolicyWaiverRevocation> resultFour = dao.getByOwnerIdAndAutoPolicyWaiverIdPaginated(
        waiverTwo.getOwnerId(),
        waiverTwo.getId(),
        2,
        3
    );

    assertThat(resultFour).hasSize(2).allSatisfy(waiverRevocation -> {
      assertThat(waiverRevocation.getOwnerId()).isEqualTo(waiverTwo.getOwnerId());
      assertThat(waiverRevocation.getAutoPolicyWaiverId()).isEqualTo(waiverTwo.getId());
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
    tempEntity.newAutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        waiver.getId(),
        eval.getScanId(),
        violation.getHash(),
        ComponentMatcherStrategyForRevocation.POLICY_VIOLATION,
        violation.getId(),
        violation.getThreatLevel(),
        "CVE-111-223",
        policy.getName(),
        "a1 v1",
        policy.getId(),
        identifier,
        violation.getConstraintFacts()
    );

    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverRevocation revocationTwo = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationThree = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationFour = new AutoPolicyWaiverRevocation(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    dao.insert(revocationTwo);
    dao.insert(revocationThree);
    dao.insert(revocationFour);

    // when
    AutoPolicyWaiverRevocation result = dao.getByOwnerIdPolicyViolation(
        app.getId(),
        waiver.getId(),
        "someotherPolicyViolationId"
    );

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
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(
        app.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        waiver.getId(),
        eval.getScanId(),
        violation.getHash(),
        ComponentMatcherStrategyForRevocation.POLICY_VIOLATION,
        violation.getId(),
        violation.getThreatLevel(),
        "CVE-111-223",
        policy.getName(),
        "a1 v1",
        policy.getId(),
        identifier,
        violation.getConstraintFacts()
    );

    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Organization otherOrg = tempEntity.newOrganization("fakeOrgOne");
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(otherOrg.getId());

    AutoPolicyWaiverRevocation revocationTwo = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationThree = new AutoPolicyWaiverRevocation(
        app.getOrganizationId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverTwo.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );

    AutoPolicyWaiverRevocation revocationFour = new AutoPolicyWaiverRevocation(
        otherOrg.getId(),
        "fakeCreatorId",
        "fakeCreatorName",
        new Date(),
        autoPolicyWaiverThree.getId(),
        "fakeScan",
        "fakeHash",
        ComponentMatcherStrategyForRevocation.EXACT_COMPONENT
    );
    dao.insert(revocationTwo);
    dao.insert(revocationThree);
    dao.insert(revocationFour);

    // when
    AutoPolicyWaiverRevocation result = dao.getByOwnerIdPolicyViolation(
        app.getId(),
        waiver.getId(),
        violation.getId()
    );

    // then
    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(app.getId());
    assertThat(result.getAutoPolicyWaiverId()).isEqualTo(waiver.getId());
    assertThat(result.getComponentMatchStrategy()).isEqualTo(ComponentMatcherStrategyForRevocation.POLICY_VIOLATION);
    assertThat(result.getPolicyId()).isEqualTo(revocation.getPolicyId());
    assertThat(result.getThreatLevel()).isEqualTo(revocation.getThreatLevel());
    assertThat(result.getPolicyName()).isEqualTo(revocation.getPolicyName());
    assertThat(result.getConstraintFactsJson()).isEqualTo(revocation.getConstraintFactsJson());
    assertThat(result.getPolicyViolationId()).isEqualTo(revocation.getPolicyViolationId());
    assertThat(result.getHash()).isEqualTo(revocation.getHash());
    assertThat(result.getComponentIdentifier()).isEqualTo(revocation.getComponentIdentifier());
  }
}
