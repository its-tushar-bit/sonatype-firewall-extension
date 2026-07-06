/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.crowd;

import java.nio.CharBuffer;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.CrowdConfiguration.CROWD_CONFIGURATION;

@Named
@Singleton
public class CrowdConfigurationDAO
    extends AbstractOperationalSqlDAO<CrowdConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "crowd-configuration";

  public static final int MAX_SERVER_URL_SIZE = 2048;

  public static final int MAX_APPLICATION_NAME_SIZE = 255;

  public static final int MAX_APPLICATION_PASSWORD_SIZE = 255;

  @Inject
  public CrowdConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public CrowdConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(final CrowdConfiguration crowdConfiguration) {
    CrowdConfiguration existing = get();
    if (existing != null) {
      update(crowdConfiguration);
    }
    else {
      insert(crowdConfiguration);
    }
  }

  public void delete() {
    CrowdConfiguration crowdConfiguration = get();
    if (crowdConfiguration != null) {
      delete(crowdConfiguration);
    }
  }

  @Override
  public int insert(final TransactionContext tx, final CrowdConfiguration entity) {
    entity.setId(SINGLETON_ENTITY_ID);
    validate(entity);
    return super.insert(tx, entity);
  }

  @Override
  public int update(final TransactionContext tx, final CrowdConfiguration entity) {
    entity.setId(SINGLETON_ENTITY_ID);
    validate(entity);
    return super.update(tx, entity);
  }

  public void validate(final CrowdConfiguration crowdConfiguration) {
    validateField(crowdConfiguration.getServerUrl(), "server url", MAX_SERVER_URL_SIZE);
    validateField(crowdConfiguration.getApplicationName(), "application name", MAX_APPLICATION_NAME_SIZE);
    validateField(crowdConfiguration.getApplicationPassword() == null
        ? null
        : CharBuffer.wrap(
            crowdConfiguration.getApplicationPassword()),
        "application password", MAX_APPLICATION_PASSWORD_SIZE);
  }

  private void validateField(final CharSequence value, final String name, final int maxLength) {
    if (StringUtils.isBlank(value)) {
      throw new BadRequestException(String.format("A Crowd %s is required.", name));
    }
    if (value.length() > maxLength) {
      throw new BadRequestException(
          String.format("A Crowd %s cannot exceed %s characters.", name, maxLength));
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return CROWD_CONFIGURATION;
  }

  @Override
  public Class<CrowdConfiguration> getEntityClass() {
    return CrowdConfiguration.class;
  }
}
