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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationCountHistoryDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationCountHistoryDAO applicationCountHistoryDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    applicationCountHistoryDAO = daoFactory.createApplicationCountHistoryDAO();
  }

  @Test
  public void testGetApplicationCountAtOrDefault_shouldBeAbleToGetUpdatedCount_GivenOnlyInitialEntry() {
    // One created above, another created by AbstractDbDAOTest.setup()
    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(new Date())).isEqualTo(0);
  }

  @Test
  public void testGetApplicationCountAtOrDefault_shouldGetClosestPastCount() {
    final Date now = new Date();
    final Date aWeekAgo = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));

    tempEntity.newApplicationCountHistoryEntry(aWeekAgo, 133);
    tempEntity.newApplicationCountHistoryEntry(now, 145);

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(now))
        .isEqualTo(145);

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(
        Date.from(Instant.now().plus(1, ChronoUnit.DAYS))))
            .isEqualTo(145);

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(
        Date.from(Instant.now().minus(3, ChronoUnit.DAYS))))
            .isEqualTo(133);

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(
        Date.from(Instant.now().minus(14, ChronoUnit.DAYS))))
            .isEqualTo(0);
  }

  @Test
  public void testGetApplicationCountAtOrDefault_supportHistoryNoOlderThanEpoch() {
    final Date givenDate = new Date();
    final int givenGivenTotalCount = 1344;

    tempEntity.newApplicationCountHistoryEntry(givenDate, givenGivenTotalCount);

    assertThat(applicationCountHistoryDAO.getApplicationCountAtOrDefault(
        Date.from(LocalDate.of(1970, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())))
            .isEqualTo(0);
  }
}
