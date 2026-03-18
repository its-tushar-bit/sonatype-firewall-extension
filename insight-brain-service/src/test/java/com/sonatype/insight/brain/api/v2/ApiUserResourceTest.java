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
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.SamlRealm;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
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
  private UserDAO userDAO;

  private SamlUserDAO samlUserDAO;

  private OAuth2UserDAO oAuth2UserDAO;

  @Before
  public void setUp() {
    userDAO = lookup(UserDAO.class);
    samlUserDAO = lookup(SamlUserDAO.class);
    oAuth2UserDAO = lookup(OAuth2UserDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2);
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    ApiUserDTO inputUserDTO = createUserDTOToAdd();

    HttpResponse response = restRequest().body(inputUserDTO).post();

    assertResponseStatus(204, response);
    User user = userDAO.getByUsernameNotNull(inputUserDTO.username);
    assertMatchingUser(inputUserDTO, user);

    // Read
    response = restRequest().path(ApiUserResource.USERNAME_PATH).parameter(inputUserDTO.username).get();

    assertResponseStatus(200, response);
    ApiUserDTO outputUserDTO = response.getBody(ApiUserDTO.class);
    assertEqualExceptNullDTOPassword(user, outputUserDTO);

    // Update
    inputUserDTO = createUserDTOToUpdate(user);

    response =
        restRequest()
            .path(ApiUserResource.USERNAME_PATH)
            .parameter(inputUserDTO.username)
            .body(inputUserDTO)
            .put();

    assertResponseStatus(200, response);
    outputUserDTO = response.getBody(ApiUserDTO.class);
    assertEqualIgnoringPassword(inputUserDTO, outputUserDTO);
    user = userDAO.getByUsernameNotNull(inputUserDTO.username);
    assertEqualExceptNullDTOPassword(user, outputUserDTO);

    // Delete
    response = restRequest().path(ApiUserResource.USERNAME_PATH).parameter(user.getUsername()).delete();

    assertResponseStatus(204, response);
    assertThat(userDAO.getById(user.getId())).isNull();
  }

  @Test
  public void testGetAll_Null() throws Exception {
    testGetAll(null, User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGetAll_Saml() throws Exception {
    testGetAll("saml", SamlRealm.ID);
  }

  @Test
  public void testGetAll_OAuth2() throws Exception {
    enableSsoWithOAuth2();
    testGetAll("oauth2", OAuth2Realm.ID);
  }

  private void testGetAll(String queryRealm, String expectedRealm) throws Exception {
    User user = tempEntity.newUser();
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();

    HttpResponse response = restRequest().query("realm", queryRealm).get();

    assertResponseStatus(200, response);
    ApiUserListDTO apiUserListDTO = response.getBody(ApiUserListDTO.class);
    assertThat(apiUserListDTO).isNotNull();
    if (SamlRealm.ID.equals(expectedRealm)) {
      assertThat(apiUserListDTO.users).extracting(apiUserDTO -> apiUserDTO.username)
          .containsExactlyInAnyOrder(samlUser1.getUsername(), samlUser2.getUsername());
    }
    else if (OAuth2Realm.ID.equals(expectedRealm)) {
      assertThat(apiUserListDTO.users).extracting(apiUserDTO -> apiUserDTO.username)
          .containsExactlyInAnyOrder(oAuth2User1.getUsername(), oAuth2User2.getUsername());
    }
    else {
      assertThat(apiUserListDTO.users).extracting(apiUserDTO -> apiUserDTO.username)
          .containsExactlyInAnyOrder(User.ADMIN_USERNAME, user.getUsername());
    }
    assertThat(apiUserListDTO.users).allSatisfy(apiUserDTO -> assertThat(apiUserDTO.realm).isEqualTo(expectedRealm));
    assertPresenceOfRealmField(expectedRealm, response);
  }

  @Test
  public void testGet_Null() throws Exception {
    testGet(null, User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGet_Saml() throws Exception {
    testGet("saml", SamlRealm.ID);
  }

  @Test
  public void testGet_Oauth2() throws Exception {
    enableSsoWithOAuth2();
    testGet("oauth2", OAuth2Realm.ID);
  }

  @Test
  public void testGet_Internal() throws Exception {
    testGet(User.INTERNAL_REALM_ID, User.INTERNAL_REALM_ID);
  }

  @Test
  public void testGet_Other() throws Exception {
    testGet("AnyRealm", User.INTERNAL_REALM_ID);
  }

  private void testGet(String queryRealm, String expectedRealm) throws Exception {
    User user = tempEntity.newUser();
    SamlUser samlUser = tempEntity.newSamlUser();
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    String username = user.getUsername();

    if (SamlRealm.ID.equals(expectedRealm)) {
      username = samlUser.getUsername();
    }
    else if (OAuth2Realm.ID.equals(expectedRealm)) {
      username = oAuth2User.getUsername();
    }

    HttpResponse response = restRequest()
        .path(username)
        .query("realm", queryRealm)
        .get();

    assertResponseStatus(200, response);
    ApiUserDTO apiUserDTO = response.getBody(ApiUserDTO.class);
    assertThat(apiUserDTO).isNotNull();
    if (SamlRealm.ID.equals(expectedRealm)) {
      assertThat(apiUserDTO.username).isEqualTo(samlUser.getUsername());
    }
    else if (OAuth2Realm.ID.equals(expectedRealm)) {
      assertThat(apiUserDTO.username).isEqualTo(oAuth2User.getUsername());
    }
    else {
      assertThat(apiUserDTO.username).isEqualTo(user.getUsername());
    }
    assertThat(apiUserDTO.realm).isEqualTo(expectedRealm);
    assertPresenceOfRealmField(expectedRealm, response);
  }

  private void assertPresenceOfRealmField(
      final String expectedRealm,
      final HttpResponse response) throws JsonProcessingException
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
  public void testDelete_SamlRealmId() throws Exception {
    testDelete("SaMl");
  }

  @Test
  public void testDelete_OAuth2RealmId() throws Exception {
    enableSsoWithOAuth2();
    testDelete("OaUth2");
  }

  @Test
  public void testDelete_NoRealmId() throws Exception {
    testDelete(null);
  }

  @Test
  public void testDelete_UnknownRealmId() throws Exception {
    testDelete("unknown");
  }

  @Test
  public void testDelete_InternalRealmId() throws Exception {
    testDelete("InTeRnAl");
  }

  private void testDelete(String realmId) throws Exception {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User(samlUser1.getUsername());
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    User user = tempEntity.newUser(samlUser1.getUsername());

    HttpRequest httpRequest =
        restRequest().path(ApiUserResource.USERNAME_PATH).parameter(samlUser1.getUsername());
    if (realmId != null) {
      httpRequest.query("realm", realmId);
    }

    HttpResponse response = httpRequest.delete();

    assertResponseStatus(204, response);
    if (SamlRealm.ID.equalsIgnoreCase(realmId)) {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oAuth2User1.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oAuth2User2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNotNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
    else if (OAuth2Realm.ID.equalsIgnoreCase(realmId)) {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNotNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oAuth2User1.getId())).isNull();
      assertThat(oAuth2UserDAO.getById(oAuth2User2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNotNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
    else {
      assertThat(samlUserDAO.getById(samlUser1.getId())).isNotNull();
      assertThat(samlUserDAO.getById(samlUser2.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oAuth2User1.getId())).isNotNull();
      assertThat(oAuth2UserDAO.getById(oAuth2User2.getId())).isNotNull();
      assertThat(userDAO.getById(user.getId())).isNull();
      assertThat(userDAO.getByUsername(User.ADMIN_USERNAME)).isNotNull();
    }
  }
}
