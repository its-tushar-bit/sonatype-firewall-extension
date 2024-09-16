/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
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
}
