/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.scan;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistedScanTicketDAOTest
    extends AbstractDbDAOTest
{
  private PersistedScanTicketDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPersistedScanTicketDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    PersistedScanTicket persistedScanTicket = createPersistedScanTicket();
    assertThat(persistedScanTicket.getCreateTime()).isAfterOrEqualTo(now).isCloseTo(now, 5000);
    dao.insert(persistedScanTicket);
    assertThat(persistedScanTicket.getId()).isNotNull();

    // Read
    assertPersistedScanTicket(dao.getById(persistedScanTicket.getId()), persistedScanTicket);

    // Update
    persistedScanTicket.setStateId("otherStateId");
    dao.update(persistedScanTicket);
    assertPersistedScanTicket(dao.getById(persistedScanTicket.getId()), persistedScanTicket);

    // Delete
    dao.delete(persistedScanTicket);
    assertThat(dao.getById(persistedScanTicket.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    PersistedScanTicket persistedScanTicket1 = createPersistedScanTicket();
    dao.insert(persistedScanTicket1);
    PersistedScanTicket persistedScanTicket2 = createPersistedScanTicket();
    dao.insert(persistedScanTicket2);

    assertThat(dao.getAll()).extracting(PersistedScanTicket::getId)
        .doesNotContainNull()
        .containsExactlyInAnyOrder(persistedScanTicket1.getId(), persistedScanTicket2.getId());
  }

  @Test
  public void testDeleteBeforeOrOn() {
    long now = System.currentTimeMillis();
    PersistedScanTicket persistedScanTicketBefore = createPersistedScanTicket();
    persistedScanTicketBefore.setCreateTime(new Date(now - 1));
    dao.insert(persistedScanTicketBefore);
    PersistedScanTicket persistedScanTicketOn = createPersistedScanTicket();
    persistedScanTicketOn.setCreateTime(new Date(now));
    dao.insert(persistedScanTicketOn);
    PersistedScanTicket persistedScanTicketAfter = createPersistedScanTicket();
    persistedScanTicketAfter.setCreateTime(new Date(now + 1));
    dao.insert(persistedScanTicketAfter);

    dao.deleteBeforeOrOn(persistedScanTicketOn.getCreateTime());

    assertThat(dao.getById(persistedScanTicketBefore.getId())).isNull();
    assertThat(dao.getById(persistedScanTicketOn.getId())).isNull();
    assertThat(dao.getById(persistedScanTicketAfter.getId())).isNotNull();
  }

  private PersistedScanTicket createPersistedScanTicket() {
    PersistedScanTicket persistedScanTicket = new PersistedScanTicket();
    persistedScanTicket.setApplicationId(application.getId());
    persistedScanTicket.setScanId("scanId");
    persistedScanTicket.setStateId("stateId");
    persistedScanTicket.setErrorId("errorId");
    return persistedScanTicket;
  }

  private void assertPersistedScanTicket(PersistedScanTicket actual, PersistedScanTicket expected) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getApplicationId()).isEqualTo(expected.getApplicationId());
    assertThat(actual.getScanId()).isEqualTo(expected.getScanId());
    assertThat(actual.getStateId()).isEqualTo(expected.getStateId());
    assertThat(actual.getErrorId()).isEqualTo(expected.getErrorId());
    assertThat(actual.getCreateTime()).isEqualTo(expected.getCreateTime());
  }
}
