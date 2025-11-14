/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.oauth2;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.177
 */
@Entity
@Table(name = "oauth2_configuration")
public class OAuth2Configuration
    implements HasStringId
{
  @Id
  @Column(name = "idp_issuer")
  private String id;

  @Column(name = "idp_jwks_url")
  private String idpJwksUrl;

  @Column(name = "idp_jws_algorithm")
  private String idpJwsAlgorithm;

  @Column(name = "idp_jwks")
  private String idpJwks;

  @Column(name = "username_claim")
  private String usernameClaim;

  @Column(name = "first_name_claim")
  private String firstNameClaim;

  @Column(name = "last_name_claim")
  private String lastNameClaim;

  @Column(name = "email_claim")
  private String emailClaim;

  @Column(name = "groups_claim")
  private String groupsClaim;

  @Column(name = "exact_match_claims_json")
  private String exactMatchClaimsJson;

  public OAuth2Configuration() {
  }

  public OAuth2Configuration(
      final String idpIssuer,
      final String idpJwsAlgorithm,
      final String idpJwksUrl,
      final String idpJwks)
  {
    id = idpIssuer;
    this.idpJwsAlgorithm = idpJwsAlgorithm;
    this.idpJwksUrl = idpJwksUrl;
    this.idpJwks = idpJwks;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getIdpJwksUrl() {
    return idpJwksUrl;
  }

  public void setIdpJwksUrl(final String idpJwksUrl) {
    this.idpJwksUrl = idpJwksUrl;
  }

  public String getIdpJwsAlgorithm() {
    return idpJwsAlgorithm;
  }

  public void setIdpJwsAlgorithm(final String idpJwsAlgorithm) {
    this.idpJwsAlgorithm = idpJwsAlgorithm;
  }

  public String getIdpJwks() {
    return idpJwks;
  }

  public void setIdpJwks(final String idpJwks) {
    this.idpJwks = idpJwks;
  }

  public String getUsernameClaim() {
    return usernameClaim;
  }

  public void setUsernameClaim(final String usernameClaim) {
    this.usernameClaim = usernameClaim;
  }

  public String getFirstNameClaim() {
    return firstNameClaim;
  }

  public void setFirstNameClaim(final String firstNameClaim) {
    this.firstNameClaim = firstNameClaim;
  }

  public String getLastNameClaim() {
    return lastNameClaim;
  }

  public void setLastNameClaim(final String lastNameClaim) {
    this.lastNameClaim = lastNameClaim;
  }

  public String getEmailClaim() {
    return emailClaim;
  }

  public void setEmailClaim(final String emailClaim) {
    this.emailClaim = emailClaim;
  }

  public String getGroupsClaim() {
    return groupsClaim;
  }

  public void setGroupsClaim(final String groupsClaim) {
    this.groupsClaim = groupsClaim;
  }

  public String getExactMatchClaimsJson() {
    return exactMatchClaimsJson;
  }

  public void setExactMatchClaimsJson(final String exactMatchClaimsJson) {
    this.exactMatchClaimsJson = exactMatchClaimsJson;
  }

  public void setExactMatchClaims(final Map<String, String> exactMatchClaims) {
    this.exactMatchClaimsJson = JsonUtils.format(exactMatchClaims);
  }

  public Map<String, String> getExactMatchClaims() {
    if (StringUtils.isBlank(exactMatchClaimsJson)) {
      return new HashMap<>();
    }
    return JsonUtils.asType(exactMatchClaimsJson, new TypeReference<Map<String, String>>() { });
  }
}
