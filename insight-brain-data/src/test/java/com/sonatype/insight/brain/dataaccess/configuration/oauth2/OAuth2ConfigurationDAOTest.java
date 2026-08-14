/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.oauth2;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OAuth2ConfigurationDAOTest
    extends AbstractDbDAOTest
{
  public static final String ISSUER = "https://www.an-idp.com/";

  public static final String JWKS_URL = String.format("%s/jwks.json", ISSUER);

  public static final String JWS_ALGORITHM = "RS256";

  public static final String IDP_JWKS = "{\"keys\":[{\"ki\":\"value\"}]}";

  private OAuth2ConfigurationDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createOAuth2ConfigurationDAO();
  }

  @Test
  public void testCRUD() {
    // Insert
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, JWKS_URL, IDP_JWKS);
    dao.insert(config);

    // Get
    config = dao.getById(config.getId());
    assertOAuth2ConfigurationIsTheExpected(config);

    // Update
    String newAlgorithm = "HS256";
    config.setIdpJwsAlgorithm(newAlgorithm);
    dao.update(config);

    config = dao.getById(config.getId());
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(ISSUER);
    assertThat(config.getIdpJwsAlgorithm()).isEqualTo(newAlgorithm);
    assertThat(config.getIdpJwks()).isEqualTo(IDP_JWKS);

    // Delete
    String id = config.getId();
    dao.delete(config);

    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testInsert_Null() {
    assertThatThrownBy(() -> dao.insert(null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.INVALID_CONFIGURATION);
  }

  @Test
  public void testInsert_IssuerNull() {
    OAuth2Configuration config = new OAuth2Configuration(null, JWS_ALGORITHM, JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_ISSUER_REQUIRED);
  }

  @Test
  public void testInsert_IssuerBlank() {
    OAuth2Configuration config = new OAuth2Configuration("", JWS_ALGORITHM, JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_ISSUER_REQUIRED);
  }

  @Test
  public void testInsert_AlgorithmNull() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, null, JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWS_ALGORITHM_REQUIRED);
  }

  @Test
  public void testInsert_AlgorithmBlank() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, "", JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWS_ALGORITHM_REQUIRED);
  }

  @Test
  public void testInsert_AlgorithmOnDenyList() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, "none", JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.JWS_ALGORITHM_NOT_ALLOWED);
  }

  @Test
  public void testInsert_JwksNotSet_Null() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, null, null);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWKS_REQUIRED);
  }

  @Test
  public void testInsert_JwksNotSet_Blank() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, null, "");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWKS_REQUIRED);
  }

  @Test
  public void testInsert_InvalidExactMatchClaimsJson() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, JWKS_URL, null);
    config.setExactMatchClaimsJson("{asjkdhrfk");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.EXACT_MATCH_CLAIMS_JSON_IS_INVALID);
  }

  @Test
  public void testUpdate_AlgorithmNull() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, null, JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWS_ALGORITHM_REQUIRED);
  }

  @Test
  public void testUpdate_AlgorithmBlank() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, "", JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWS_ALGORITHM_REQUIRED);
  }

  @Test
  public void testUpdate_AlgorithmOnDenyList() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, "none", JWKS_URL, IDP_JWKS);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.JWS_ALGORITHM_NOT_ALLOWED);
  }

  @Test
  public void testUpdate_JwksNotSet_Null() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, null, null);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWKS_REQUIRED);
  }

  @Test
  public void testUpdate_JwksNotSet_Blank() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, null, "");

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.IDP_JWKS_REQUIRED);
  }

  @Test
  public void testUpdate_InvalidExactMatchClaimsJson() {
    OAuth2Configuration config = new OAuth2Configuration(ISSUER, JWS_ALGORITHM, JWKS_URL, null);
    config.setExactMatchClaimsJson("{asjkdhrfk");

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OAuth2ConfigurationDAO.EXACT_MATCH_CLAIMS_JSON_IS_INVALID);
  }

  @Test
  public void testInsert_MultpleConfigurations() {
    String anotherIssuer = "https://www.another-idp.com/";
    String anotherJwksUrl = String.format("%s/jwks.json", anotherIssuer);

    tempEntity.newOAuth2Configuration(ISSUER, JWS_ALGORITHM, JWKS_URL, IDP_JWKS);
    tempEntity.newOAuth2Configuration(anotherIssuer, JWS_ALGORITHM, anotherJwksUrl, "");

    List<OAuth2Configuration> configurations = dao.getAll();
    assertThat(configurations).hasSize(2);
    assertThat(configurations.stream().map(c -> c.getId())).contains(ISSUER, anotherIssuer);
  }

  public void assertOAuth2ConfigurationIsTheExpected(OAuth2Configuration config) {
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(ISSUER);
    assertThat(config.getIdpJwsAlgorithm()).isEqualTo(JWS_ALGORITHM);
    assertThat(config.getIdpJwks()).isEqualTo(IDP_JWKS);
  }
}
