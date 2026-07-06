/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.dm.tables.AnnouncementBanner.ANNOUNCEMENT_BANNER;

/**
 * Singleton DAO for the deployment-global {@link AnnouncementBanner}.
 */
@Named
@Singleton
public class AnnouncementBannerDAO
    extends AbstractDatamartSqlDAO<AnnouncementBanner>
{
  public static final String SINGLETON_ENTITY_ID = "announcement-banner";

  private static final String DEFAULT_SEVERITY = "info";

  @Inject
  public AnnouncementBannerDAO(final DataMartDataStore dataMartDataStore) {
    super(dataMartDataStore);
  }

  public AnnouncementBanner get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  public int insert(
      final TransactionContext tx,
      final AnnouncementBanner banner,
      final boolean ignoreDuplicateKey)
  {
    throw new UnsupportedOperationException("insert() is not supported for singleton entity AnnouncementBanner");
  }

  @Override
  public int update(final TransactionContext tx, final AnnouncementBanner banner) {
    Objects.requireNonNull(banner, "Banner must not be null");
    banner.setId(SINGLETON_ENTITY_ID);
    banner.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    if (banner.getSeverity() == null) {
      banner.setSeverity(DEFAULT_SEVERITY);
    }
    return super.update(tx, banner);
  }

  @Override
  public void delete(final TransactionContext tx, final AnnouncementBanner banner) {
    throw new UnsupportedOperationException("delete() is not supported for singleton entity AnnouncementBanner");
  }

  @Override
  public int insertBatch(
      final TransactionContext tx,
      final List<AnnouncementBanner> banners,
      final boolean ignoreDuplicateKey)
  {
    throw new UnsupportedOperationException("insertBatch() is not supported for singleton entity AnnouncementBanner");
  }

  @Override
  public int updateBatch(final TransactionContext tx, final List<AnnouncementBanner> banners) {
    throw new UnsupportedOperationException("updateBatch() is not supported for singleton entity AnnouncementBanner");
  }

  @Override
  public Table<?> getJooqTable() {
    return ANNOUNCEMENT_BANNER;
  }

  @Override
  public Class<AnnouncementBanner> getEntityClass() {
    return AnnouncementBanner.class;
  }
}
