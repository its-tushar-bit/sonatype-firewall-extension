/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PersistedPromoteScanResult;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistedPromoteScanResultDAOTest
    extends AbstractDbDAOTest
{
  private final PersistedPromoteScanResultDAO dao = new PersistedPromoteScanResultDAO();

  @Test
  public void testDeleteBeforeOrOn() {
    long now = System.currentTimeMillis();
    PersistedPromoteScanResult result1 = tempEntity.newPersistedPromoteScanResult(new Date(now - 1));
    PersistedPromoteScanResult result2 = tempEntity.newPersistedPromoteScanResult(new Date(now));
    PersistedPromoteScanResult result3 = tempEntity.newPersistedPromoteScanResult(new Date(now + 1));

    dao.deleteBeforeOrOn(result2.getCreateTime());

    assertThat(dao.getById(result1.getId())).isNull();
    assertThat(dao.getById(result2.getId())).isNull();
    assertThat(dao.getById(result3.getId())).isNotNull();
  }
}
