/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Collections;

import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CallFlowAnalysisConfigDAOTest
    extends AbstractDbDAOTest
{
  private CallFlowAnalysisConfigDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createCallFlowAnalysisConfigDAO();
  }

  @Test
  public void testCRUD() {
    CallFlowAnalysisConfig callFlowAnalysisConfig = new CallFlowAnalysisConfig();
    String ownerId = tempEntity.newOrganization().getId();

    // create
    assertThat(callFlowAnalysisConfig.getId()).isNull();
    callFlowAnalysisConfig.setEnabled(true);
    callFlowAnalysisConfig.setOwnerId(ownerId);
    callFlowAnalysisConfig.setAlgorithm(CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS);
    dao.insert(callFlowAnalysisConfig);
    assertThat(callFlowAnalysisConfig.getId()).isNotNull();

    // read
    callFlowAnalysisConfig = dao.getById(callFlowAnalysisConfig.getId());

    assertThat(callFlowAnalysisConfig.isEnabled()).isTrue();
    assertThat(callFlowAnalysisConfig.getOwnerId()).isEqualTo(ownerId);
    assertThat(callFlowAnalysisConfig.getAlgorithm()).isEqualTo(CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS);

    // update
    callFlowAnalysisConfig.setEnabled(false);
    dao.update(callFlowAnalysisConfig);

    callFlowAnalysisConfig = dao.getById(callFlowAnalysisConfig.getId());

    assertThat(callFlowAnalysisConfig).isNotNull();
    assertThat(callFlowAnalysisConfig.isEnabled()).isFalse();

    // delete
    String id = callFlowAnalysisConfig.getId();
    dao.delete(callFlowAnalysisConfig);

    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsClosestAncestor() {
    CallFlowAnalysisConfig configAtOrg = tempEntity.newCallFlowAnalysisConfig(organization.getId(), 2);
    tempEntity.newCallFlowAnalysisConfig(organization.getParentOrganizationId(), 4);

    CallFlowAnalysisConfig result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(configAtOrg.getId());
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsNullWhenNone() {
    CallFlowAnalysisConfig result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_prefersSelfOverAncestor() {
    CallFlowAnalysisConfig configAtApp = tempEntity.newCallFlowAnalysisConfig(application.getId(), 2);
    tempEntity.newCallFlowAnalysisConfig(organization.getId(), 4);

    CallFlowAnalysisConfig result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(configAtApp.getId());
  }

  @Test
  public void testInsert_ownerIdExist() {
    Organization owner = tempEntity.newOrganization();
    CallFlowAnalysisConfig callFlowAnalysisConfig = tempEntity.newCallFlowAnalysisConfig(owner.getId(), 3);
    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThatThrownBy(() -> dao.insert(tx, callFlowAnalysisConfig))
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void testUpdate_ownerIdExist() {
    Organization owner = tempEntity.newOrganization();
    tempEntity.newCallFlowAnalysisConfig(owner.getId(), 3);
    CallFlowAnalysisConfig callFlowAnalysisConfig2 =
        new CallFlowAnalysisConfig(
            true, Collections.singletonList("nameSpace1"), CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS, 3,
            owner.getId());

    try (TransactionContext tx = dao.createTransactionContext()) {
      assertThatThrownBy(() -> dao.update(tx, callFlowAnalysisConfig2))
          .isInstanceOf(BadRequestException.class);
    }
  }
}
