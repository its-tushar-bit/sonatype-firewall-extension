/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.dataaccess;

import java.sql.Statement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractDAOTest
{
  @Test
  public void sumBatchResult_sumsPerStatementCounts() {
    // 1 = inserted/updated, 0 = skipped ON CONFLICT DO NOTHING.
    assertThat(AbstractDAO.sumBatchResult(new int[]{1, 1, 1})).isEqualTo(3);
    assertThat(AbstractDAO.sumBatchResult(new int[]{1, 0, 1})).isEqualTo(2);
    assertThat(AbstractDAO.sumBatchResult(new int[]{0, 0})).isEqualTo(0);
    assertThat(AbstractDAO.sumBatchResult(new int[]{})).isEqualTo(0);
  }

  @Test
  public void sumBatchResult_ignoresSuccessNoInfoAndExecuteFailed() {
    // SUCCESS_NO_INFO (-2) and EXECUTE_FAILED (-3) carry no real row count, so they are not summed (and are logged).
    assertThat(AbstractDAO.sumBatchResult(new int[]{Statement.SUCCESS_NO_INFO, Statement.SUCCESS_NO_INFO}))
        .isEqualTo(0);
    assertThat(AbstractDAO.sumBatchResult(new int[]{Statement.EXECUTE_FAILED})).isEqualTo(0);
    // A mix still counts the statements that reported a real affected-row count.
    assertThat(AbstractDAO.sumBatchResult(new int[]{1, Statement.SUCCESS_NO_INFO, Statement.EXECUTE_FAILED, 1}))
        .isEqualTo(2);
  }
}
