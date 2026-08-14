/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DevelopmentPrioritizationDAOTest
    extends AbstractDbDAOTest
{
  private DevelopmentPrioritizationDAO dao;

  private DevelopmentPrioritizationComponentInfoDAO childDao;

  private DevelopmentPrioritization scan1prioritization1;

  private DevelopmentPrioritization scan1prioritization2;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createDevelopmentPrioritizationDAO();
    childDao = daoFactory.createDevelopmentPrioritizationComponentInfoDAO();

    scan1prioritization1 = tempEntity.newDevelopmentPrioritization("scan1");
    scan1prioritization2 = tempEntity.newDevelopmentPrioritization("scan2");
  }

  @Test
  public void testGetAllByScanId() {
    assertThat(dao.getByScanId("scan1"))
        .isNotNull()
        .isEqualTo(scan1prioritization1);
    assertThat(dao.getByScanId("scan2"))
        .isNotNull()
        .isEqualTo(scan1prioritization2);
    assertThat(dao.getByScanId("scanX"))
        .isNull();
  }

  @Test
  public void testDeleteAllByScanIdCascade() {
    tempEntity.newDevelopmentPrioritizationComponentInfo(scan1prioritization1.getId(), "scan1", "hash1",
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, "1.0.1");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByScanIdCascade(tx, "scan1");
      tx.commit();
    }

    assertThat(childDao.getAllByScanId("scan1"))
        .isEmpty();
    assertThat(dao.getByScanId("scan1"))
        .isNull();
  }
}
