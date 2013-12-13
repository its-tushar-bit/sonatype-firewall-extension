/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

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

    String ownerId = "MyOwnerId";
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

    String ownerId = "MyOwnerId";
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

    dao.delete(policyMonitoring1);
  }
}
