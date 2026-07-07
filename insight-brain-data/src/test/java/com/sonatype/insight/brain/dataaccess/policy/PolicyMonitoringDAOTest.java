/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyMonitoringDAOTest
    extends AbstractDbDAOTest
{
  private PolicyMonitoringDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyMonitoringDAO();
  }

  @Test
  public void testCRUD() {
    String ownerId = application.getId();
    String stageTypeId = Stage.ID_RELEASE;

    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, stageTypeId);
    assertThat(policyMonitoring.getId()).isNull();
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    // Read
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNotNull();
    assertPolicyMonitoring(ownerId, stageTypeId, policyMonitoring);

    // Update
    policyMonitoring.setStageTypeId(Stage.ID_STAGE_RELEASE);
    dao.update(policyMonitoring);

    // Read
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNotNull();
    assertPolicyMonitoring(ownerId, Stage.ID_STAGE_RELEASE, policyMonitoring);

    // Delete
    dao.delete(policyMonitoring);

    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNull();
  }

  @Test
  public void testCRUD_ComplianceStage() {
    String ownerId = application.getId();
    String stageTypeId = Stage.ID_COMPLIANCE;

    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, stageTypeId);
    assertThat(policyMonitoring.getId()).isNull();
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    // Read
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNotNull();
    assertPolicyMonitoring(ownerId, stageTypeId, policyMonitoring);

    // Update
    policyMonitoring.setStageTypeId(stageTypeId);
    dao.update(policyMonitoring);

    // Read
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNotNull();

    // Delete
    dao.delete(policyMonitoring);

    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNull();
  }

  @Test
  public void testCRUD_MultiLicense() {
    String ownerId = application.getId();

    // Create for Lifecycle
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    assertThat(policyMonitoring.getId()).isNull();
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    // Create for SBOM Manager
    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_COMPLIANCE);
    assertThat(policyMonitoring.getId()).isNull();
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    // Update and Read for Lifecycle
    policyMonitoring = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_RELEASE);
    policyMonitoring.setStageTypeId(Stage.ID_DEVELOP);
    dao.update(policyMonitoring);
    List<PolicyMonitoring> policyMonitorings = dao.getByOwnerId(ownerId);
    assertThat(policyMonitorings).isNotEmpty().hasSize(2);
    policyMonitoring = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_DEVELOP);
    assertThat(policyMonitoring).isNotNull();
    policyMonitoring = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_COMPLIANCE);
    assertThat(policyMonitoring).isNotNull();

    // Update and Read for SBOM Manager
    policyMonitoring.setStageTypeId(Stage.ID_COMPLIANCE);
    dao.update(policyMonitoring);
    policyMonitorings = dao.getByOwnerId(ownerId);
    assertThat(policyMonitorings).isNotEmpty().hasSize(2);
    policyMonitoring = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_COMPLIANCE);
    assertThat(policyMonitoring).isNotNull();

    // Delete and Read for SBOM Manager
    dao.delete(policyMonitoring);
    policyMonitorings = dao.getByOwnerId(ownerId);
    assertThat(policyMonitorings).isNotEmpty().hasSize(1);
    assertPolicyMonitoring(ownerId, Stage.ID_DEVELOP, policyMonitorings.get(0));

    // Delete for Lifecycle
    dao.delete(policyMonitorings.get(0));
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertThat(policyMonitoring).isNull();
  }

  private void assertPolicyMonitoring(String ownerId, String stageTypeId, PolicyMonitoring actual) {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
  }

  @Test
  public void testAddDuplicate_DifferentStage() {
    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring1 = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring1);

    PolicyMonitoring policyMonitoring2 = new PolicyMonitoring(ownerId, Stage.ID_STAGE_RELEASE);
    assertThatThrownBy(() -> dao.insert(policyMonitoring2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This application/organization already has policy monitoring.");
  }

  @Test
  public void testAddDuplicate_SameStage() {
    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring1 = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring1);

    PolicyMonitoring policyMonitoring2 = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    assertThatThrownBy(() -> dao.insert(policyMonitoring2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This application/organization already has policy monitoring.");
  }

  @Test
  public void testAddDuplicate_ComplianceStage() {
    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring1 = new PolicyMonitoring(ownerId, Stage.ID_COMPLIANCE);
    dao.insert(policyMonitoring1);

    PolicyMonitoring policyMonitoring2 = new PolicyMonitoring(ownerId, Stage.ID_COMPLIANCE);
    assertThatThrownBy(() -> dao.insert(policyMonitoring2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This application/organization already has policy monitoring.");
  }

  @Test
  public void testSet_Insert() {
    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    List<PolicyMonitoring> policyMonitoringsRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringsRetrieved).hasSize(1);
    assertThat(policyMonitoringsRetrieved.get(0).getId()).isEqualTo(policyMonitoring.getId());
    assertThat(policyMonitoringsRetrieved.get(0).getStageTypeId()).isEqualTo(Stage.ID_RELEASE);

    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_COMPLIANCE);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    policyMonitoringsRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringsRetrieved).hasSize(2);
    PolicyMonitoring policyMonitoringRetrieved = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_COMPLIANCE);
    assertThat(policyMonitoringRetrieved).isNotNull();
    assertThat(policyMonitoringRetrieved.getId()).isEqualTo(policyMonitoring.getId());
    assertThat(policyMonitoringRetrieved.getStageTypeId()).isEqualTo(Stage.ID_COMPLIANCE);
  }

  @Test
  public void testSet_Update() {
    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();
    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_COMPLIANCE);
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_BUILD);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();
    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_COMPLIANCE);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    List<PolicyMonitoring> policyMonitoringsRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringsRetrieved).hasSize(2);
    PolicyMonitoring policyMonitoringRetrieved = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_COMPLIANCE);
    assertThat(policyMonitoringRetrieved).isNotNull();
    assertThat(policyMonitoringRetrieved.getId()).isEqualTo(policyMonitoring.getId());
    assertThat(policyMonitoringRetrieved.getStageTypeId()).isEqualTo(Stage.ID_COMPLIANCE);
    policyMonitoringRetrieved = dao.getByOwnerIdAndStageTypeId(ownerId, Stage.ID_BUILD);
    assertThat(policyMonitoringRetrieved).isNotNull();
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsMonitoringAcrossAncestors() {
    PolicyMonitoring orgMonitoring = new PolicyMonitoring(organization.getId(), Stage.ID_RELEASE);
    dao.insert(orgMonitoring);
    PolicyMonitoring appMonitoring = new PolicyMonitoring(application.getId(), Stage.ID_BUILD);
    dao.insert(appMonitoring);

    List<PolicyMonitoring> result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).extracting(PolicyMonitoring::getId)
        .containsExactly(appMonitoring.getId(), orgMonitoring.getId());
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_emptyWhenNoMonitoring() {
    List<PolicyMonitoring> result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_NullList() {
    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(null, Stage.ID_RELEASE);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_EmptyList() {
    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(Set.of(), Stage.ID_RELEASE);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_DirectPolicyMonitoring() {
    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring);

    Map<String, PolicyMonitoring> result =
        dao.getByOwnerIdsAndStageTypeIdsWithInheritance(Set.of(ownerId), Stage.ID_RELEASE);

    assertThat(result).hasSize(1);
    assertThat(result).containsKey(ownerId);
    assertThat(result.get(ownerId).getId()).isEqualTo(policyMonitoring.getId());
    assertThat(result.get(ownerId).getStageTypeId()).isEqualTo(Stage.ID_RELEASE);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_InheritedFromParentOrg() {
    String parentOrgId = organization.getId();
    PolicyMonitoring parentPolicy = new PolicyMonitoring(parentOrgId, Stage.ID_RELEASE);
    dao.insert(parentPolicy);
    String childAppId = application.getId();

    Map<String, PolicyMonitoring> result =
        dao.getByOwnerIdsAndStageTypeIdsWithInheritance(Set.of(childAppId), Stage.ID_RELEASE);

    assertThat(result).hasSize(1);
    assertThat(result).containsKey(childAppId);
    assertThat(result.get(childAppId).getId()).isEqualTo(parentPolicy.getId());
    assertThat(result.get(childAppId).getOwnerId()).isEqualTo(parentOrgId);
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdWithInheritance_MultipleOwners_MixedDirectAndInherited() {
    String parentOrgId = organization.getId();
    PolicyMonitoring parentPolicy = new PolicyMonitoring(parentOrgId, Stage.ID_RELEASE);
    dao.insert(parentPolicy);

    Organization childOrg = tempEntity.newOrganization(organization);
    PolicyMonitoring childOrgPolicy = new PolicyMonitoring(childOrg.getId(), Stage.ID_RELEASE);
    dao.insert(childOrgPolicy);

    String appWithoutPolicy = application.getId();

    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(
        Set.of(parentOrgId, childOrg.getId(), appWithoutPolicy), Stage.ID_RELEASE);

    assertThat(result).hasSize(3);
    assertThat(result.get(parentOrgId).getId()).isEqualTo(parentPolicy.getId());
    assertThat(result.get(childOrg.getId()).getId()).isEqualTo(childOrgPolicy.getId());
    assertThat(result.get(appWithoutPolicy).getId()).isEqualTo(parentPolicy.getId());
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_ClosestAncestorWins() {
    Organization grandparentOrg = tempEntity.newOrganization("grandparent");
    PolicyMonitoring grandparentPolicy = new PolicyMonitoring(grandparentOrg.getId(), Stage.ID_RELEASE);
    dao.insert(grandparentPolicy);

    Organization parentOrg = tempEntity.newOrganization(grandparentOrg);
    PolicyMonitoring parentPolicy = new PolicyMonitoring(parentOrg.getId(), Stage.ID_RELEASE);
    dao.insert(parentPolicy);

    Organization childOrg = tempEntity.newOrganization(parentOrg);

    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(
        Set.of(childOrg.getId()), Stage.ID_RELEASE);

    assertThat(result).hasSize(1);
    assertThat(result.get(childOrg.getId()).getId()).isEqualTo(parentPolicy.getId());
    assertThat(result.get(childOrg.getId()).getOwnerId()).isEqualTo(parentOrg.getId());
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_NoMatchingPolicyInHierarchy() {
    Organization parentOrg = tempEntity.newOrganization("parent-no-policy");
    Organization childOrg = tempEntity.newOrganization(parentOrg);

    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(
        Set.of(childOrg.getId()), Stage.ID_RELEASE);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdWithInheritance_DifferentStageTypes() {
    String orgId = organization.getId();
    PolicyMonitoring releasePolicy = new PolicyMonitoring(orgId, Stage.ID_RELEASE);
    dao.insert(releasePolicy);

    PolicyMonitoring compliancePolicy = new PolicyMonitoring(orgId, Stage.ID_COMPLIANCE);
    dao.insert(compliancePolicy);

    String appId = application.getId();

    Map<String, PolicyMonitoring> releaseResult =
        dao.getByOwnerIdsAndStageTypeIdsWithInheritance(Set.of(appId), Stage.ID_RELEASE);
    assertThat(releaseResult).hasSize(1);
    assertThat(releaseResult.get(appId).getStageTypeId()).isEqualTo(Stage.ID_RELEASE);

    Map<String, PolicyMonitoring> complianceResult =
        dao.getByOwnerIdsAndStageTypeIdsWithInheritance(Set.of(appId), Stage.ID_COMPLIANCE);
    assertThat(complianceResult).hasSize(1);
    assertThat(complianceResult.get(appId).getStageTypeId()).isEqualTo(Stage.ID_COMPLIANCE);

    Map<String, PolicyMonitoring> buildResult =
        dao.getByOwnerIdsAndStageTypeIdsWithInheritance(Set.of(appId), Stage.ID_BUILD);
    assertThat(buildResult).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_DirectPolicyOverridesInherited() {
    String parentOrgId = organization.getId();
    PolicyMonitoring parentPolicy = new PolicyMonitoring(parentOrgId, Stage.ID_RELEASE);
    dao.insert(parentPolicy);

    Organization childOrg = tempEntity.newOrganization(organization);
    PolicyMonitoring childPolicy = new PolicyMonitoring(childOrg.getId(), Stage.ID_RELEASE);
    dao.insert(childPolicy);

    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(
        Set.of(childOrg.getId()), Stage.ID_RELEASE);

    assertThat(result).hasSize(1);
    assertThat(result.get(childOrg.getId()).getId()).isEqualTo(childPolicy.getId());
    assertThat(result.get(childOrg.getId()).getOwnerId()).isEqualTo(childOrg.getId());
  }

  @Test
  public void testGetByOwnerIdsAndStageTypeIdsWithInheritance_MultipleOwnersWithDifferentHierarchies() {
    Organization org1 = tempEntity.newOrganization("org1");
    PolicyMonitoring org1Policy = new PolicyMonitoring(org1.getId(), Stage.ID_RELEASE);
    dao.insert(org1Policy);
    Organization childOrg1 = tempEntity.newOrganization(org1);

    Organization org2 = tempEntity.newOrganization("org2");
    PolicyMonitoring org2Policy = new PolicyMonitoring(org2.getId(), Stage.ID_RELEASE);
    dao.insert(org2Policy);
    Organization childOrg2 = tempEntity.newOrganization(org2);

    Map<String, PolicyMonitoring> result = dao.getByOwnerIdsAndStageTypeIdsWithInheritance(
        Set.of(childOrg1.getId(), childOrg2.getId()), Stage.ID_RELEASE);

    assertThat(result).hasSize(2);
    assertThat(result.get(childOrg1.getId()).getId()).isEqualTo(org1Policy.getId());
    assertThat(result.get(childOrg2.getId()).getId()).isEqualTo(org2Policy.getId());
  }
}
