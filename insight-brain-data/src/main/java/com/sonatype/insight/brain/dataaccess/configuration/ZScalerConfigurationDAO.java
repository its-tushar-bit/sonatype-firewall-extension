/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class ZScalerConfigurationDAO
    extends AbstractOperationalSqlDAO<ZScalerConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "zscaler-configuration";

  public static final String QUERY = "SELECT entity FROM ZScalerConfiguration entity" + //
      " WHERE entity.id=?1";

  @Inject
  public ZScalerConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * @return The zScaler server configuration or {@code null} if none.
   */
  public ZScalerConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  public ZScalerConfiguration getById(TransactionContext tx, String id) {
    return super.getById(tx, SINGLETON_ENTITY_ID);
  }

  public void set(ZScalerConfiguration zscalerConfiguration) {
    update(zscalerConfiguration);
  }

  @Override
  public void insert(TransactionContext tx, ZScalerConfiguration zscalerConfiguration) {
    validate(zscalerConfiguration);
    zscalerConfiguration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, zscalerConfiguration);
  }

  @Override
  public void update(TransactionContext tx, ZScalerConfiguration zscalerConfiguration) {
    validate(zscalerConfiguration);
    zscalerConfiguration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, zscalerConfiguration);
  }

  public void validate(ZScalerConfiguration zscalerConfiguration) {
    if (StringUtils.isBlank(zscalerConfiguration.getHostname())) {
      throw new BadRequestException("The zScaler host is required.");
    }
  }

  public void delete() {
    ZScalerConfiguration zscalerConfiguration = get();
    if (zscalerConfiguration != null) {
      delete(zscalerConfiguration);
    }
  }
}
