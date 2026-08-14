/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SuccessMetricsReportDataDAOTest
    extends AbstractDbDAOTest
{
  private SuccessMetricsReportDataDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSuccessMetricsReportDataDAO();
  }

  @Test
  public void testCRUD() {
    SuccessMetricsReport report = tempEntity.newSuccessMetricsReport("username", "metrics", "{}");
    SuccessMetricsReportData reportData = tempEntity.newSuccessMetricsReportData(report.getId());

    assertThat(reportData.getId()).isEqualTo(report.getId());

    reportData = dao.getById(reportData.getId());

    int originalMonthCount = reportData.getMonthCount();

    reportData.setMonthCount(originalMonthCount + 1);
    dao.update(reportData);

    reportData = dao.getById(reportData.getId());
    assertThat(reportData.getMonthCount()).isEqualTo(originalMonthCount + 1);

    dao.delete(reportData);
    assertThat(dao.getById(reportData.getId())).isNull();
  }
}
