/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.dataaccess;

import java.sql.Statement;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractDAOTest
{
  @Test
  public void countInsertedRows_sumsPerStatementCounts() {
    // 1 = inserted, 0 = skipped ON CONFLICT DO NOTHING.
    assertThat(AbstractDAO.countInsertedRows(new int[]{1, 1, 1})).isEqualTo(3);
    assertThat(AbstractDAO.countInsertedRows(new int[]{1, 0, 1})).isEqualTo(2);
    assertThat(AbstractDAO.countInsertedRows(new int[]{0, 0})).isEqualTo(0);
    assertThat(AbstractDAO.countInsertedRows(new int[]{})).isEqualTo(0);
  }

  @Test
  public void countInsertedRows_ignoresSuccessNoInfo() {
    // If reWriteBatchedInserts were ever enabled the driver returns SUCCESS_NO_INFO (-2) per statement. We can't
    // recover a real count then, so those statements are not counted (and the method logs a warning).
    assertThat(AbstractDAO.countInsertedRows(new int[]{Statement.SUCCESS_NO_INFO, Statement.SUCCESS_NO_INFO}))
        .isEqualTo(0);
    // A mix still counts the statements that reported a real affected-row count.
    assertThat(AbstractDAO.countInsertedRows(new int[]{1, Statement.SUCCESS_NO_INFO, 1})).isEqualTo(2);
  }
}
