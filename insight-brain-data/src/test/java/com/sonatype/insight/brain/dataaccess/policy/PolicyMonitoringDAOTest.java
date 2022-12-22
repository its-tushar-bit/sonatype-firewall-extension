/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PolicyMonitoringDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

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

  private void assertPolicyMonitoring(String ownerId, String stageTypeId, PolicyMonitoring actual) {
    assertThat(actual.getOwnerId()).isEqualTo(ownerId);
    assertThat(actual.getStageTypeId()).isEqualTo(stageTypeId);
  }

  @Test
  public void testAddDuplicate() {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring1 = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring1);

    PolicyMonitoring policyMonitoring2 = new PolicyMonitoring(ownerId, Stage.ID_STAGE_RELEASE);
    assertThatThrownBy(() -> dao.insert(policyMonitoring2)).isInstanceOf(BadRequestException.class)
        .hasMessage("This application/organization already has policy monitoring.");
  }

  @Test
  public void testSet_Insert() {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    PolicyMonitoring policyMonitoringRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringRetrieved.getId()).isEqualTo(policyMonitoring.getId());
    assertThat(policyMonitoringRetrieved.getStageTypeId()).isEqualTo(Stage.ID_RELEASE);
  }

  @Test
  public void testSet_Update() {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = application.getId();
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_BUILD);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId()).isNotNull();

    PolicyMonitoring policyMonitoringRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringRetrieved.getId()).isEqualTo(policyMonitoring.getId());
    assertThat(policyMonitoringRetrieved.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
  }
}
