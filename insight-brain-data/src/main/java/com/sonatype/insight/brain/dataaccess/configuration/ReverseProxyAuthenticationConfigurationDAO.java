/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.net.URI;
import java.net.URISyntaxException;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ReverseProxyAuthenticationConfiguration.REVERSE_PROXY_AUTHENTICATION_CONFIGURATION;

@Named
@Singleton
public class ReverseProxyAuthenticationConfigurationDAO
    extends AbstractOperationalSqlDAO<ReverseProxyAuthenticationConfiguration>
{
  public static final String SINGLETON_ENTITY_ID = "reverse-proxy-authentication-configuration";

  // Visible for testing
  static final int MAX_USERNAME_HEADER_LENGTH = 255;

  // Visible for testing
  static final int MAX_LOGOUT_URL_LENGTH = 2048;

  // Visible for testing
  static final String NO_CONFIG_ERROR_MSG = "A configuration must be given.";

  // Visible for testing
  static final String NO_USERNAME_HEADER_ERROR_MSG = "The username header is required.";

  // Visible for testing
  static final String LONG_USERNAME_HEADER_ERROR_MSG = "The username header cannot exceed 255 characters.";

  // Visible for testing
  static final String EMPTY_LOGOUT_URL_ERROR_MSG = "The logout URL cannot be empty.";

  // Visible for testing
  static final String LONG_LOGOUT_URL_ERROR_MSG = "The logout URL cannot exceed 2048 characters.";

  // Visible for testing
  static final String INVALID_LOGOUT_URL_ERROR_MSG = "The logout URL is invalid.";

  public static final String NOT_FOUND_ERROR_MSG = "Reverse proxy authentication not configured.";

  @Inject
  public ReverseProxyAuthenticationConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ReverseProxyAuthenticationConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public ReverseProxyAuthenticationConfiguration getNotNull() {
    ReverseProxyAuthenticationConfiguration config = get();
    if (config == null) {
      throw new NotFoundException(NOT_FOUND_ERROR_MSG);
    }
    return config;
  }

  public void set(final ReverseProxyAuthenticationConfiguration configuration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      if (getById(tx, SINGLETON_ENTITY_ID) == null) {
        insert(tx, configuration);
      }
      else {
        update(tx, configuration);
      }
      tx.commit();
    }
  }

  @Override
  public int insert(final TransactionContext tx, final ReverseProxyAuthenticationConfiguration configuration) {
    validate(configuration);
    configuration.setId(SINGLETON_ENTITY_ID);
    return super.insert(tx, configuration);
  }

  @Override
  public void update(final TransactionContext tx, final ReverseProxyAuthenticationConfiguration configuration) {
    validate(configuration);
    configuration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, configuration);
  }

  public void delete() {
    ReverseProxyAuthenticationConfiguration configuration = get();
    if (configuration != null) {
      delete(configuration);
    }
  }

  private void validate(final ReverseProxyAuthenticationConfiguration config) {
    if (config == null) {
      throw new BadRequestException(NO_CONFIG_ERROR_MSG);
    }
    if (StringUtils.isBlank(config.getUsernameHeader())) {
      throw new BadRequestException(NO_USERNAME_HEADER_ERROR_MSG);
    }
    if (config.getUsernameHeader().length() > MAX_USERNAME_HEADER_LENGTH) {
      throw new BadRequestException(LONG_USERNAME_HEADER_ERROR_MSG);
    }
    if (config.getLogoutUrl() != null) {
      if (StringUtils.isBlank(config.getLogoutUrl())) {
        throw new BadRequestException(EMPTY_LOGOUT_URL_ERROR_MSG);
      }
      if (config.getLogoutUrl().length() > MAX_LOGOUT_URL_LENGTH) {
        throw new BadRequestException(LONG_LOGOUT_URL_ERROR_MSG);
      }
      try {
        new URI(config.getLogoutUrl());
      }
      catch (URISyntaxException e) {
        throw new BadRequestException(INVALID_LOGOUT_URL_ERROR_MSG, e);
      }
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return REVERSE_PROXY_AUTHENTICATION_CONFIGURATION;
  }

  @Override
  public Class<ReverseProxyAuthenticationConfiguration> getEntityClass() {
    return ReverseProxyAuthenticationConfiguration.class;
  }
}
