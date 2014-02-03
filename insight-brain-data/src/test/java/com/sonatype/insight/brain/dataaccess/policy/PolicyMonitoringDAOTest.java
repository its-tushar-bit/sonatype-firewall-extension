/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PolicyMonitoringDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = applicationId;
    String stageTypeId = Stage.ID_RELEASE;

    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, stageTypeId);
    assertNull(policyMonitoring.getId());
    dao.insert(policyMonitoring);
    assertNotNull(policyMonitoring.getId());

    // Read
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertNotNull(policyMonitoring);
    assertPolicyMonitoring(ownerId, stageTypeId, policyMonitoring);

    // Update
    policyMonitoring.setStageTypeId(Stage.ID_STAGE_RELEASE);
    dao.update(policyMonitoring);

    // Read
    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertNotNull(policyMonitoring);
    assertPolicyMonitoring(ownerId, Stage.ID_STAGE_RELEASE, policyMonitoring);

    // Delete
    dao.delete(policyMonitoring);

    policyMonitoring = dao.getById(policyMonitoring.getId());
    assertNull(policyMonitoring);
  }

  private void assertPolicyMonitoring(String ownerId, String stageTypeId, PolicyMonitoring actual)
  {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(stageTypeId, actual.getStageTypeId());
  }

  @Test
  public void testAddDuplicate() throws Exception {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = applicationId;
    PolicyMonitoring policyMonitoring1 = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring1);

    PolicyMonitoring policyMonitoring2 = new PolicyMonitoring(ownerId, Stage.ID_STAGE_RELEASE);
    try {
      dao.insert(policyMonitoring2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("This application/organization already has policy monitoring.", expected.getMessage());
    }
  }

  @Test
  public void testSet_Insert() {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = applicationId;
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId(), is(notNullValue()));

    PolicyMonitoring policyMonitoringRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringRetrieved.getId(), is(policyMonitoring.getId()));
    assertThat(policyMonitoringRetrieved.getStageTypeId(), is(Stage.ID_RELEASE));
  }

  @Test
  public void testSet_Update() {
    PolicyMonitoringDAO dao = new PolicyMonitoringDAO();

    String ownerId = applicationId;
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_RELEASE);
    dao.insert(policyMonitoring);
    assertThat(policyMonitoring.getId(), is(notNullValue()));

    policyMonitoring = new PolicyMonitoring(ownerId, Stage.ID_BUILD);
    dao.set(policyMonitoring);
    assertThat(policyMonitoring.getId(), is(notNullValue()));

    PolicyMonitoring policyMonitoringRetrieved = dao.getByOwnerId(ownerId);
    assertThat(policyMonitoringRetrieved.getId(), is(policyMonitoring.getId()));
    assertThat(policyMonitoringRetrieved.getStageTypeId(), is(Stage.ID_BUILD));
  }
}
