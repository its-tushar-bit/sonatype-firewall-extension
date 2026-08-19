/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.oauth2;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.OidcConfiguration.OIDC_CONFIGURATION;

/**
 * @since 1.72
 */
@Named
@Singleton
public class OidcConfigurationDAO
    extends AbstractOperationalSqlDAO<OidcConfiguration>
    implements RotatableSecrets
{
  public static final String INVALID_CONFIGURATION = "Invalid configuration";

  public static final String IDP_ISSUER_REQUIRED = "The IDP Issuer is required";

  public static final String AUTHORIZATION_PARAMS_JSON_IS_INVALID = "Authorization custom parameters json is invalid";

  public static final String TOKEN_REQUEST_PARAMS_JSON_IS_INVALID = "Token request custom parameters json is invalid";

  public static final String CLIENT_ID_REQUIRED = "The client id is required";

  public static final String CLIENT_SECRET_REQUIRED = "The client secret is required";

  public static final String IDP_AUTHORIZATION_URL_REQUIRED = "The idp authorization url is required";

  public static final String IDP_TOKEN_URL_REQUIRED = "The idp token url is required";

  @Inject
  public OidcConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int insert(final TransactionContext tx, final OidcConfiguration configuration) {
    validate(configuration);
    return super.insert(tx, configuration);
  }

  @Override
  public int update(final TransactionContext tx, final OidcConfiguration configuration) {
    validate(configuration);
    return super.update(tx, configuration);
  }

  public OidcConfiguration get() {
    try (TransactionContext tx = createTransactionContext()) {
      return get(tx);
    }
  }

  public OidcConfiguration get(final TransactionContext tx) {
    return tx.dsl()
        .selectFrom(OIDC_CONFIGURATION)
        .limit(1)
        .fetchOneInto(OidcConfiguration.class);
  }

  private void validate(final OidcConfiguration config) {
    if (config == null) {
      throw new IllegalArgumentException(INVALID_CONFIGURATION);
    }
    if (StringUtils.isBlank(config.getId())) {
      throw new IllegalArgumentException(IDP_ISSUER_REQUIRED);
    }
    if (StringUtils.isBlank(config.getClientId())) {
      throw new IllegalArgumentException(CLIENT_ID_REQUIRED);
    }
    if (StringUtils.isBlank(config.getClientSecret())) {
      throw new IllegalArgumentException(CLIENT_SECRET_REQUIRED);
    }
    if (StringUtils.isBlank(config.getIdpAuthorizationUrl())) {
      throw new IllegalArgumentException(IDP_AUTHORIZATION_URL_REQUIRED);
    }
    if (StringUtils.isBlank(config.getIdpTokenUrl())) {
      throw new IllegalArgumentException(IDP_TOKEN_URL_REQUIRED);
    }
    if (StringUtils.isNotBlank(config.getAuthorizationCustomParamsJson()) &&
        !isValidJson(config.getAuthorizationCustomParamsJson()))
    {
      throw new IllegalArgumentException(AUTHORIZATION_PARAMS_JSON_IS_INVALID);
    }
    if (StringUtils.isNotBlank(config.getTokenRequestCustomParamsJson()) &&
        !isValidJson(config.getTokenRequestCustomParamsJson()))
    {
      throw new IllegalArgumentException(TOKEN_REQUEST_PARAMS_JSON_IS_INVALID);
    }
  }

  private boolean isValidJson(final String json) {
    try {
      JsonUtils.parse(json, new TypeReference<Map<String, Object>>()
      {
      });
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  @Override
  public Table<?> getJooqTable() {
    return OIDC_CONFIGURATION;
  }

  @Override
  public Class<OidcConfiguration> getEntityClass() {
    return OidcConfiguration.class;
  }
}
