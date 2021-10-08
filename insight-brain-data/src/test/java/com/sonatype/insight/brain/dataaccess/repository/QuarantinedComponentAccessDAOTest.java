/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarantinedComponentAccessDAOTest
    extends AbstractDbDAOTest
{
  private QuarantinedComponentAccessDAO dao = new QuarantinedComponentAccessDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    Date date = new Date();
    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess("repoComponentId", date);
    String id = quarantinedComponentAccess.getId();
    quarantinedComponentAccess = dao.getById(id);
    assertThat(quarantinedComponentAccess.getRepositoryComponentId()).isEqualTo("repoComponentId");
    assertThat(quarantinedComponentAccess.getGenerateTime()).isEqualTo(date);

    // Update
    quarantinedComponentAccess.setRepositoryComponentId("repoComponentId2");
    dao.update(quarantinedComponentAccess);
    quarantinedComponentAccess = dao.getById(id);
    assertThat(quarantinedComponentAccess.getRepositoryComponentId()).isEqualTo("repoComponentId2");

    // Delete
    dao.delete(quarantinedComponentAccess);
    quarantinedComponentAccess = dao.getById(id);
    assertThat(quarantinedComponentAccess).isNull();
  }

  @Test
  public void testDeleteAllBeforeDate() {
    final Date cutoffDate = DateUtils.addDays(new Date(), -2);

    for (int i = 0; i < 201; i++) {
      tempEntity.newQuarantinedComponentAccess("beforeComp" + i, DateUtils.addDays(cutoffDate, -1));
    }
    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess("afterComp" + i);
    }

    dao.deleteAllBeforeDate(cutoffDate);
    assertThat(dao.getAll()).hasSize(10);
  }
}
