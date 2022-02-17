/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserListDTO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.assertEqualExceptNullDTOPassword;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.assertEqualIgnoringPassword;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.assertMatchingUser;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToAdd;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToUpdate;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiUserResourceTest
    extends AbstractResourceTest
{
  private final UserDAO userDAO = new UserDAO();

  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2);
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    ApiUserDTO inputUserDTO = createUserDTOToAdd();
    tempEntity.registerUsernames(inputUserDTO.username);

    HttpResponse response = restRequest().body(inputUserDTO).post();

    assertResponseStatus(204, response);
    User user = userDAO.getByUsernameNotNull(inputUserDTO.username);
    assertMatchingUser(inputUserDTO);

    // Read
    response = restRequest().path(DefaultApiUserResource.USERNAME_PATH).parameter(inputUserDTO.username).get();

    assertResponseStatus(200, response);
    ApiUserDTO outputUserDTO = response.getBody(ApiUserDTO.class);
    assertEqualExceptNullDTOPassword(user, outputUserDTO);

    // Update
    inputUserDTO = createUserDTOToUpdate(user);

    response =
        restRequest()
            .path(DefaultApiUserResource.USERNAME_PATH)
            .parameter(inputUserDTO.username)
            .body(inputUserDTO)
            .put();

    assertResponseStatus(200, response);
    outputUserDTO = response.getBody(ApiUserDTO.class);
    assertEqualIgnoringPassword(inputUserDTO, outputUserDTO);
    user = userDAO.getByUsernameNotNull(inputUserDTO.username);
    assertEqualExceptNullDTOPassword(user, outputUserDTO);

    // Delete
    response = restRequest().path(DefaultApiUserResource.USERNAME_PATH).parameter(user.getUsername()).delete();

    assertResponseStatus(204, response);
    assertThat(userDAO.getById(user.getId())).isNull();
  }

  @Test
  public void testGetAll_SamlUserTokensDisabled() throws Exception {
    setMissingFeature(LicensedFeature.SAML_USER_TOKENS);
    testGetAll(null, null);
  }

  @Test
  public void testGetAll_SamlUserTokensDisabled_Saml() throws Exception {
    setMissingFeature(LicensedFeature.SAML_USER_TOKENS);
    testGetAll("saml", null);
  }

  @Test
  public void testGetAll_SamlUserTokensEnabled() throws Exception {
    setFeatures(LicensedFeature.SAML_USER_TOKENS);
    testGetAll(null, User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGetAll_SamlUserTokensEnabled_Saml() throws Exception {
    setFeatures(LicensedFeature.SAML_USER_TOKENS);
    testGetAll("saml", SamlUser.SAML_REALM_ID);
  }

  private void testGetAll(String queryRealm, String expectedRealm) throws Exception {
    User user = tempEntity.newUser();
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();

    HttpResponse response = restRequest().query("realm", queryRealm).get();

    assertResponseStatus(200, response);
    ApiUserListDTO apiUserListDTO = response.getBody(ApiUserListDTO.class);
    assertThat(apiUserListDTO).isNotNull();
    if (SamlUser.SAML_REALM_ID.equals(expectedRealm)) {
      assertThat(apiUserListDTO.users).extracting(apiUserDTO -> apiUserDTO.username)
          .containsExactlyInAnyOrder(samlUser1.getUsername(), samlUser2.getUsername());
    }
    else {
      assertThat(apiUserListDTO.users).extracting(apiUserDTO -> apiUserDTO.username)
          .containsExactlyInAnyOrder(User.ADMIN_USERNAME, user.getUsername());
    }
    assertThat(apiUserListDTO.users).allSatisfy(apiUserDTO -> assertThat(apiUserDTO.realm).isEqualTo(expectedRealm));
    assertPresenceOfRealmField(expectedRealm, response);
  }

  @Test
  public void testGet_SamlUserTokensDisabled() throws Exception {
    testGet(null, null, false);
  }

  @Test
  public void testGet_SamlUserTokensDisabled_Saml() throws Exception {
    testGet("saml", null, false);
  }

  @Test
  public void testGet_SamlUserTokensEnabled() throws Exception {
    testGet(null, null, true);
  }

  @Test
  public void testGet_SamlUserTokensEnabled_Saml() throws Exception {
    testGet("saml", SamlUser.SAML_REALM_ID, true);
  }

  @Test
  public void testGet_SamlUserTokensEnabled_Internal() throws Exception {
    testGet(User.INTERNAL_REALM_ID, null, true);
  }

  @Test
  public void testGet_SamlUserTokensDisabled_Internal() throws Exception {
    testGet(User.INTERNAL_REALM_ID, null, false);
  }

  @Test
  public void testGet_SamlUserTokensEnabled_Other() throws Exception {
    testGet("AnyRealm", null, true);
  }

  @Test
  public void testGet_SamlUserTokensDisabled_Other() throws Exception {
    testGet("AnyRealm", null, false);
  }

  private void testGet(String queryRealm, String expectedRealm, boolean samlUserTokenEnabled) throws Exception {
    User user = tempEntity.newUser();
    SamlUser samlUser = tempEntity.newSamlUser();
    if (samlUserTokenEnabled) {
      setFeatures(LicensedFeature.SAML_USER_TOKENS);
    }
    else {
      setMissingFeature(LicensedFeature.SAML_USER_TOKENS);
    }
    HttpResponse response = restRequest()
        .path(samlUserTokenEnabled
            && SamlUser.SAML_REALM_ID.equalsIgnoreCase(queryRealm)
            ? samlUser.getUsername()
            : user.getUsername())
        .query("realm", queryRealm).get();

    assertResponseStatus(200, response);
    ApiUserDTO apiUserDTO = response.getBody(ApiUserDTO.class);
    assertThat(apiUserDTO).isNotNull();
    if (SamlUser.SAML_REALM_ID.equals(expectedRealm)) {
      assertThat(apiUserDTO.username).isEqualTo(samlUser.getUsername());
    }
    else {
      assertThat(apiUserDTO.username).isEqualTo(user.getUsername());
    }
    assertThat(apiUserDTO.realm).isEqualTo(expectedRealm);
    assertPresenceOfRealmField(expectedRealm, response);
  }

  private void assertPresenceOfRealmField(final String expectedRealm, final HttpResponse response)
      throws JsonProcessingException
  {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(response.getBodyText());
    if (jsonNode.has("users")) {
      for (JsonNode child : jsonNode.get("users")) {
        assertThat(child.has("realm")).isEqualTo(expectedRealm != null);
      }
    }
    else {
      assertThat(jsonNode.has("realm")).isEqualTo(expectedRealm != null);
    }
  }

  @Test
  public void testDelete_SamlUserTokensEnabled_NoRealmId() throws Exception {
    testDelete(true, null);
  }

  @Test
  public void testDelete_SamlUserTokensEnabled_UnknownRealmId() throws Exception {
    testDelete(true, "unknown");
  }

  @Test
  public void testDelete_SamlUserTokensEnabled_InternalRealmId() throws Exception {
    testDelete(true, "InTeRnAl");
  }

  @Test
  public void testDelete_SamlUserTokensEnabled_SamlRealmId() throws Exception {
    testDelete(true, "SaMl");
  }

  @Test
  public void testDelete_SamlUserTokensDisabled_NoRealmId() throws Exception {
    testDelete(false, null);
  }

  @Test
  public void testDelete_SamlUserTokensDisabled_UnknownRealmId() throws Exception {
    testDelete(false, "unknown");
  }

  @Test
  public void testDelete_SamlUserTokensDisabled_InternalRealmId() throws Exception {
    testDelete(false, "InTeRnAl");
  }

  @Test
  public void testDelete_SamlUserTokensDisabled_SamlRealmId() throws Exception {
    testDelete(false, "SaMl");
  }

  private void testDelete(boolean isSamlUserTokensEnabled, String realmId) throws Exception {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    User user = tempEntity.newUser(samlUser1.getUsername());

    if (isSamlUserTokensEnabled) {
      setFeatures(LicensedFeature.SAML_USER_TOKENS);
    }
    else {
      setMissingFeature(LicensedFeature.SAML_USER_TOKENS);
    }
    HttpRequest httpRequest =
        restRequest().path(DefaultApiUserResource.USERNAME_PATH).parameter(samlUser1.getUsername());
    if (realmId != null) {
      httpRequest.query("realm", realmId);
    }

    HttpResponse response = httpRequest.delete();

    assertResponseStatus(204, response);
    if (isSamlUserTokensEnabled && SamlUser.SAML_REALM_ID.equalsIgnoreCase(realmId)) {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNotNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
    else {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNotNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
  }
}
