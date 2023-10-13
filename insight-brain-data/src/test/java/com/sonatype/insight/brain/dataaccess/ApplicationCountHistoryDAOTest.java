/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationCountHistoryDAOTest
    extends AbstractDbDAOTest
{
  private final ApplicationCountHistoryDAO applicationCountHistoryDAO = new ApplicationCountHistoryDAO();

  @Test
  public void testRecordApplicationCount_shouldInsertOneRowEachDayWithoutModifyingPrevious() {
    // The "initialization" row
    assertThat(applicationCountHistoryDAO.getCount()).isEqualTo(1);
    assertThat(applicationCountHistoryDAO.getAll().get(0).getId()).isEqualTo("initialization");

    applicationCountHistoryDAO.recordApplicationCount();
    applicationCountHistoryDAO.recordApplicationCount();
    assertThat(applicationCountHistoryDAO.getCount()).isEqualTo(3);
  }

  @Test
  public void testgetApplicationCountAtOrDefault_shouldBeAbleToGetUpdatedCount() {
    tempEntity.newApplicationWithParent();
    applicationCountHistoryDAO.recordApplicationCount();

    // One created above, another created by AbstractDbDAOTest.setup()
    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(new Date())).isEqualTo(2);
  }

  @Test
  public void testgetApplicationCountAtOrDefault_shouldBeAbleToGetHistoryCount() {
    tempEntity.newApplicationWithParent();
    applicationCountHistoryDAO.recordApplicationCount();

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(
        Date.from(Instant.now().minus(1, ChronoUnit.DAYS))))
        .isEqualTo(0);
  }

  @Test
  public void testgetApplicationCountAtOrDefault_supportHistoryNoOlderThanEpoch() {
    tempEntity.newApplicationWithParent();
    applicationCountHistoryDAO.recordApplicationCount();

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(
        Date.from(LocalDate.of(1970,1,1).atStartOfDay(ZoneId.systemDefault()).toInstant())))
        .isEqualTo(0);
  }
}
