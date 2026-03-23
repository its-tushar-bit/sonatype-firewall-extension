/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.oauth2;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Oauth2Configuration.OAUTH2_CONFIGURATION;

/**
 * @since 1.72
 */
@Named
@Singleton
public class OAuth2ConfigurationDAO
    extends AbstractOperationalSqlDAO<OAuth2Configuration>
{
  public static final String INVALID_CONFIGURATION = "Invalid configuration";

  public static final String IDP_ISSUER_REQUIRED = "The IDP Issuer is required";

  public static final String IDP_JWS_ALGORITHM_REQUIRED = "The IDP JWS Algorithm is required";

  public static final String IDP_JWKS_REQUIRED = "Either the IDP JWKS Url or a JWKS string should be provided";

  public static final String EXACT_MATCH_CLAIMS_JSON_IS_INVALID = "Exact match claims json is invalid";

  public static final List<String> DENIED_ALGORITHMS = Arrays.asList("none");

  public static final String JWS_ALGORITHM_NOT_ALLOWED = "JWS Algorithm not allowed";

  @Inject
  public OAuth2ConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public void insert(final TransactionContext tx, final OAuth2Configuration configuration) {
    validate(configuration);
    super.insert(tx, configuration);
  }

  @Override
  public void update(final TransactionContext tx, final OAuth2Configuration configuration) {
    validate(configuration);
    super.update(tx, configuration);
  }

  private static void validate(final OAuth2Configuration config) {
    if (config == null) {
      throw new IllegalArgumentException(INVALID_CONFIGURATION);
    }
    if (StringUtils.isBlank(config.getId())) {
      throw new IllegalArgumentException(IDP_ISSUER_REQUIRED);
    }
    if (StringUtils.isBlank(config.getIdpJwsAlgorithm())) {
      throw new IllegalArgumentException(IDP_JWS_ALGORITHM_REQUIRED);
    }
    if (isJwsAlgorithmOnDenyList(config.getIdpJwsAlgorithm())) {
      throw new IllegalArgumentException(JWS_ALGORITHM_NOT_ALLOWED);
    }
    if (StringUtils.isBlank(config.getIdpJwksUrl()) && StringUtils.isBlank(config.getIdpJwks())) {
      throw new IllegalArgumentException(IDP_JWKS_REQUIRED);
    }
    if (StringUtils.isNotBlank(config.getExactMatchClaimsJson()) && !isValidJson(config.getExactMatchClaimsJson())) {
      throw new IllegalArgumentException(EXACT_MATCH_CLAIMS_JSON_IS_INVALID);
    }
  }

  private static boolean isJwsAlgorithmOnDenyList(final String jwsAlgorithm) {
    return DENIED_ALGORITHMS.contains(jwsAlgorithm);
  }

  private static boolean isValidJson(final String exactMatchClaimsJson) {
    try {
      JsonUtils.parse(exactMatchClaimsJson, new TypeReference<Map<String, Object>>()
      {
      });
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  @Override
  public List<OAuth2Configuration> getAll(final TransactionContext tx) {
    return tx.dsl()
        .selectFrom(OAUTH2_CONFIGURATION)
        .fetchInto(OAuth2Configuration.class);
  }

  @Override
  public Table<?> getJooqTable() {
    return OAUTH2_CONFIGURATION;
  }

  @Override
  public Class<OAuth2Configuration> getEntityClass() {
    return OAuth2Configuration.class;
  }
}
