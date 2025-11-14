/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.oauth2;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.security.RotatableSecret;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.177
 */
@Entity
@Table(name = "oidc_configuration")
public class OidcConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "idp_issuer")
  private String id;

  @Column(name = "client_id")
  private String clientId;

  @RotatableSecret
  @Column(name = "client_secret")
  private String clientSecret;

  @Column(name = "idp_authorization_url")
  private String idpAuthorizationUrl;

  @Column(name = "idp_token_url")
  private String idpTokenUrl;

  @Column(name = "authorization_custom_params_json")
  private String authorizationCustomParamsJson;

  @Column(name = "token_request_custom_params_json")
  private String tokenRequestCustomParamsJson;

  public OidcConfiguration() {
  }

  public OidcConfiguration(
      final String id,
      final String clientId,
      final String clientSecret,
      final String idpAuthorizationUrl,
      final String idpTokenUrl)
  {
    this.id = id;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.idpAuthorizationUrl = idpAuthorizationUrl;
    this.idpTokenUrl = idpTokenUrl;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getIdpAuthorizationUrl() {
    return idpAuthorizationUrl;
  }

  public void setIdpAuthorizationUrl(final String idpAuthorizationUrl) {
    this.idpAuthorizationUrl = idpAuthorizationUrl;
  }

  public String getIdpTokenUrl() {
    return idpTokenUrl;
  }

  public void setIdpTokenUrl(final String idpTokenUrl) {
    this.idpTokenUrl = idpTokenUrl;
  }

  public String getAuthorizationCustomParamsJson() {
    return authorizationCustomParamsJson;
  }

  public void setAuthorizationCustomParamsJson(final String authorizationCustomParamsJson) {
    this.authorizationCustomParamsJson = authorizationCustomParamsJson;
  }

  public String getTokenRequestCustomParamsJson() {
    return tokenRequestCustomParamsJson;
  }

  public void setTokenRequestCustomParamsJson(final String tokenRequestCustomParamsJson) {
    this.tokenRequestCustomParamsJson = tokenRequestCustomParamsJson;
  }

  public void setAuthorizationCustomParams(final Map<String, String> authorizationCustomParams) {
    this.authorizationCustomParamsJson = JsonUtils.format(authorizationCustomParams);
  }

  public Map<String, String> getAuthorizationCustomParams() {
    if (StringUtils.isBlank(authorizationCustomParamsJson)) {
      return new HashMap<>();
    }
    return JsonUtils.asType(authorizationCustomParamsJson, new TypeReference<Map<String, String>>() { });
  }

  public void setTokenRequestCustomParams(final Map<String, String> tokenRequestCustomParams) {
    this.tokenRequestCustomParamsJson = JsonUtils.format(tokenRequestCustomParams);
  }

  public Map<String, String> getTokenRequestCustomParams() {
    if (StringUtils.isBlank(tokenRequestCustomParamsJson)) {
      return new HashMap<>();
    }
    return JsonUtils.asType(tokenRequestCustomParamsJson, new TypeReference<Map<String, String>>() { });
  }
}
