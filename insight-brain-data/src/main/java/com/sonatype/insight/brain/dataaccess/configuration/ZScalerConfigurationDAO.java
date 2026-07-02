/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ZscalerConfiguration.ZSCALER_CONFIGURATION;

@Named
@Singleton
public class ZScalerConfigurationDAO
    extends AbstractOperationalSqlDAO<ZScalerConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "zscaler-configuration";

  private final ZscalerFormatDAO zscalerFormatDAO;

  @Inject
  public ZScalerConfigurationDAO(
      final OperationalDataStore operationalDataStore,
      final ZscalerFormatDAO zscalerFormatDAO)
  {
    super(operationalDataStore);
    this.zscalerFormatDAO = zscalerFormatDAO;
  }

  public ZScalerConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(final ZScalerConfiguration zscalerConfiguration, final List<ZscalerFormat> zscalerFormats) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      // First ensure the parent zscaler_configuration record exists before inserting child zscaler_format records
      // to avoid foreign key constraint violations
      if (getById(tx, SINGLETON_ENTITY_ID) == null) {
        insert(tx, zscalerConfiguration);
      }
      else {
        update(tx, zscalerConfiguration);
      }
      for (ZscalerFormat zscalerFormat : zscalerFormats) {
        zscalerFormat.setZscalerConfigurationId(SINGLETON_ENTITY_ID);
        if (zscalerFormat.getId() == null) {
          zscalerFormatDAO.insert(tx, zscalerFormat);
        }
        else {
          zscalerFormatDAO.update(tx, zscalerFormat);
        }
      }
      tx.commit();
    }
  }

  @Override
  public int insert(final TransactionContext tx, final ZScalerConfiguration entity) {
    entity.setId(SINGLETON_ENTITY_ID);
    return super.insert(tx, entity);
  }

  @Override
  public void update(final TransactionContext tx, final ZScalerConfiguration entity) {
    entity.setId(SINGLETON_ENTITY_ID);
    super.update(tx, entity);
  }

  public void delete() {
    ZScalerConfiguration zscalerConfiguration = get();
    if (zscalerConfiguration != null) {
      delete(zscalerConfiguration);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return ZSCALER_CONFIGURATION;
  }

  @Override
  public Class<ZScalerConfiguration> getEntityClass() {
    return ZScalerConfiguration.class;
  }
}
