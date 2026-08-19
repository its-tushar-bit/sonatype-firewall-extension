/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.oauth2;

import java.sql.SQLException;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OidcConfigurationDAOTest
    extends AbstractDbDAOTest
{
  public static final String ISSUER = "https://www.an-idp.com/";

  public static final String CLIENT_ID = "client-id";

  public static final String CLIENT_SECRET = "client-secret";

  public static final String AUTHORIZATION_URL = "https://www.an-idp.com/authorize";

  public static final String TOKEN_URL = "https://www.an-idp.com/token";

  private DAOSecretRotator daoSecretRotator;

  private OidcConfigurationDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createOidcConfigurationDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testCRUD() {
    // Insert
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    dao.insert(config);

    // Get
    config = dao.getById(config.getId());
    assertOidcConfigurationIsTheExpected(config);

    // Update
    String newClientId = "new-client-id";
    config.setClientId(newClientId);
    dao.update(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(ISSUER);
    assertThat(config.getClientId()).isEqualTo(newClientId);

    // Delete
    dao.delete(config);

    assertThat(dao.get()).isNull();
  }

  @Test
  public void testInsert_Null() {
    assertThatThrownBy(() -> dao.insert(null)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.INVALID_CONFIGURATION);
  }

  @Test
  public void testInsert_IssuerNull() {
    OidcConfiguration config = new OidcConfiguration(null, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_ISSUER_REQUIRED);
  }

  @Test
  public void testInsert_IssuerBlank() {
    OidcConfiguration config = new OidcConfiguration(null, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_ISSUER_REQUIRED);
  }

  @Test
  public void testInsert_ClientIdNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, null, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_ID_REQUIRED);
  }

  @Test
  public void testInsert_ClientIdBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, "", CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_ID_REQUIRED);
  }

  @Test
  public void testInsert_ClientSecretNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, null, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_SECRET_REQUIRED);
  }

  @Test
  public void testInsert_ClientSecretBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, "", AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_SECRET_REQUIRED);
  }

  @Test
  public void testInsert_AuthorizationUrlNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, null, TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_AUTHORIZATION_URL_REQUIRED);
  }

  @Test
  public void testInsert_AuthorizationUrlBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, "", TOKEN_URL);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_AUTHORIZATION_URL_REQUIRED);
  }

  @Test
  public void testInsert_TokenUrlNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, null);

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_TOKEN_URL_REQUIRED);
  }

  @Test
  public void testInsert_TokenUrlBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, "");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_TOKEN_URL_REQUIRED);
  }

  @Test
  public void testInsert_InvalidAuthorizationCustomParamsJson() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    config.setAuthorizationCustomParamsJson("{asjkdhrfk");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.AUTHORIZATION_PARAMS_JSON_IS_INVALID);
  }

  @Test
  public void testInsert_InvalidTokenRequestCustomParamsJson() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    config.setTokenRequestCustomParamsJson("{asjkdhrfk");

    assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.TOKEN_REQUEST_PARAMS_JSON_IS_INVALID);
  }

  @Test
  public void testUpdate_ClientIdNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, null, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_ID_REQUIRED);
  }

  @Test
  public void testUpdate_ClientIdBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, "", CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_ID_REQUIRED);
  }

  @Test
  public void testUpdate_ClientSecretNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, null, AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_SECRET_REQUIRED);
  }

  @Test
  public void testUpdate_ClientSecretBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, "", AUTHORIZATION_URL, TOKEN_URL);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.CLIENT_SECRET_REQUIRED);
  }

  @Test
  public void testUpdate_AuthorizationUrlNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, null, TOKEN_URL);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_AUTHORIZATION_URL_REQUIRED);
  }

  @Test
  public void testUpdate_AuthorizationUrlBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, "", TOKEN_URL);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_AUTHORIZATION_URL_REQUIRED);
  }

  @Test
  public void testUpdate_TokenUrlNull() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, null);

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_TOKEN_URL_REQUIRED);
  }

  @Test
  public void testUpdate_TokenUrlBlank() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, "");

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.IDP_TOKEN_URL_REQUIRED);
  }

  @Test
  public void testUpdate_InvalidAuthorizationCustomParamsJson() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    config.setAuthorizationCustomParamsJson("{asjkdhrfk");

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.AUTHORIZATION_PARAMS_JSON_IS_INVALID);
  }

  @Test
  public void testUpdate_InvalidTokenRequestCustomParamsJson() {
    OidcConfiguration config = new OidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    config.setTokenRequestCustomParamsJson("{asjkdhrfk");

    assertThatThrownBy(() -> dao.update(config)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage(OidcConfigurationDAO.TOKEN_REQUEST_PARAMS_JSON_IS_INVALID);
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET + "_Old", AUTHORIZATION_URL, TOKEN_URL);
    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    OidcConfiguration result = dao.get();
    assertThat(String.valueOf(result.getClientSecret())).isEqualTo(CLIENT_SECRET + "_New");
  }

  public void assertOidcConfigurationIsTheExpected(OidcConfiguration config) {
    assertThat(config).isNotNull();
    assertThat(config.getId()).isEqualTo(ISSUER);
    assertThat(config.getClientId()).isEqualTo(CLIENT_ID);
    assertThat(config.getClientSecret()).isEqualTo(CLIENT_SECRET);
    assertThat(config.getIdpAuthorizationUrl()).isEqualTo(AUTHORIZATION_URL);
    assertThat(config.getIdpTokenUrl()).isEqualTo(TOKEN_URL);
  }
}
