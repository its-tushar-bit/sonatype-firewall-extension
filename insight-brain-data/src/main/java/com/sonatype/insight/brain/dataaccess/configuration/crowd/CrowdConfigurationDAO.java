/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.crowd;

import java.nio.CharBuffer;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

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
  public CrowdConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public CrowdConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(CrowdConfiguration crowdConfiguration) {
    update(crowdConfiguration);
  }

  public void delete() {
    CrowdConfiguration crowdConfiguration = get();
    if (crowdConfiguration != null) {
      delete(crowdConfiguration);
    }
  }

  @Override
  public void insert(TransactionContext tx, CrowdConfiguration crowdConfiguration) {
    crowdConfiguration.setId(SINGLETON_ENTITY_ID);
    validate(crowdConfiguration);
    super.insert(tx, crowdConfiguration);
  }

  @Override
  public void update(TransactionContext tx, CrowdConfiguration crowdConfiguration) {
    crowdConfiguration.setId(SINGLETON_ENTITY_ID);
    validate(crowdConfiguration);
    super.update(tx, crowdConfiguration);
  }

  @Override
  public void delete(TransactionContext tx, CrowdConfiguration crowdConfiguration) {
    crowdConfiguration.setId(SINGLETON_ENTITY_ID);
    super.delete(tx, crowdConfiguration);
  }

  public void validate(CrowdConfiguration crowdConfiguration) {
    validateField(crowdConfiguration.getServerUrl(), "server url", MAX_SERVER_URL_SIZE);
    validateField(crowdConfiguration.getApplicationName(), "application name", MAX_APPLICATION_NAME_SIZE);
    validateField(crowdConfiguration.getApplicationPassword() == null
        ? null
        : CharBuffer.wrap(
            crowdConfiguration.getApplicationPassword()),
        "application password", MAX_APPLICATION_PASSWORD_SIZE);
  }

  private void validateField(CharSequence value, String name, int maxLength) {
    if (StringUtils.isBlank(value)) {
      throw new BadRequestException(String.format("A Crowd %s is required.", name));
    }
    if (value.length() > maxLength) {
      throw new BadRequestException(
          String.format("A Crowd %s cannot exceed %s characters.", name, maxLength));
    }
  }
}
