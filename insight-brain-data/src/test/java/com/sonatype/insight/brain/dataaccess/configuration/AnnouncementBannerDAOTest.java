/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AnnouncementBannerDAOTest
    extends AbstractDbDAOTest
{
  private AnnouncementBannerDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createAnnouncementBannerDAO();
    // Reset the singleton row between tests.
    AnnouncementBanner reset = new AnnouncementBanner();
    reset.setEnabled(false);
    reset.setSeverity("info");
    dao.update(reset);
  }

  @Test
  public void testSingletonRowExistsAfterReset() {
    AnnouncementBanner banner = dao.get();

    assertThat(banner).isNotNull();
    assertThat(banner.getId()).isEqualTo(AnnouncementBannerDAO.SINGLETON_ENTITY_ID);
    assertThat(banner.isEnabled()).isFalse();
    assertThat(banner.getSeverity()).isEqualTo("info");
  }

  @Test
  public void testReadUpdate() {
    AnnouncementBanner expected = createBanner();

    dao.update(expected);

    AnnouncementBanner actual = dao.get();
    assertThat(actual.getId()).isEqualTo(AnnouncementBannerDAO.SINGLETON_ENTITY_ID);
    assertThat(actual.isEnabled()).isTrue();
    assertThat(actual.getWindowId()).isEqualTo(expected.getWindowId());
    assertThat(actual.getMessage()).isEqualTo(expected.getMessage());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getDisplayFrom()).isEqualTo(expected.getDisplayFrom());
    assertThat(actual.getDisplayUntil()).isEqualTo(expected.getDisplayUntil());
    assertThat(actual.getUpdatedAt()).isNotNull();
  }

  @Test
  public void testUpdateEnforcesSingletonId() {
    AnnouncementBanner banner = createBanner();
    banner.setId("attacker-supplied-id");

    dao.update(banner);

    assertThat(dao.getById("attacker-supplied-id")).isNull();
    assertThat(dao.get()).isNotNull();
    assertThat(dao.get().getWindowId()).isEqualTo("2026-05-26-us");
  }

  @Test
  public void testUpdateAlwaysStampsUpdatedAtEvenWhenCallerLeavesItStale() {
    AnnouncementBanner banner = createBanner();
    // Caller's stale updatedAt should be overwritten by the DAO.
    OffsetDateTime stale = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    banner.setUpdatedAt(stale);
    OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(5);

    dao.update(banner);

    OffsetDateTime persisted = dao.get().getUpdatedAt();
    assertThat(persisted).isAfter(stale);
    assertThat(persisted).isAfterOrEqualTo(before);
  }

  @Test
  public void testInsertUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> dao.insert(createBanner()));
  }

  @Test
  public void testDeleteUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> dao.delete(dao.get()));
  }

  @Test
  public void testInsertBatchUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> dao.insertBatch(null, java.util.Collections.singletonList(createBanner()), false));
  }

  @Test
  public void testUpdateBatchUnsupported() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> dao.updateBatch(null, java.util.Collections.singletonList(createBanner())));
  }

  @Test
  public void testUpdateNullBannerThrowsNPE() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> dao.update(null));
  }

  @Test
  public void testUpdateNullSeverityDefaultsToInfo() {
    AnnouncementBanner banner = new AnnouncementBanner();
    banner.setEnabled(true);
    banner.setSeverity(null); // null severity should be defaulted to "info"

    dao.update(banner);

    AnnouncementBanner persisted = dao.get();
    assertThat(persisted.getSeverity()).isEqualTo("info");
  }

  private AnnouncementBanner createBanner() {
    AnnouncementBanner banner = new AnnouncementBanner();
    banner.setEnabled(true);
    banner.setWindowId("2026-05-26-us");
    banner.setSeverity("info");
    banner.setMessage("Scheduled maintenance: May 26, 6-10 PM EDT.");
    banner.setDisplayFrom(OffsetDateTime.of(2026, 5, 20, 0, 0, 0, 0, ZoneOffset.UTC));
    banner.setDisplayUntil(OffsetDateTime.of(2026, 5, 26, 23, 0, 0, 0, ZoneOffset.UTC));
    banner.setUpdatedAt(OffsetDateTime.of(2026, 5, 20, 0, 0, 0, 0, ZoneOffset.UTC));
    return banner;
  }
}
