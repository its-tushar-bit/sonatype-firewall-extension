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
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

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
  private UserDAO userDAO = new UserDAO();

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
  public void testGetAll() throws Exception {
    User user = tempEntity.newUser();

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiUserListDTO apiUserListDTO = response.getBody(ApiUserListDTO.class);
    assertThat(apiUserListDTO).isNotNull();
    assertThat(apiUserListDTO.users).extracting(apiUserDTO -> apiUserDTO.username)
        .containsExactlyInAnyOrder(User.ADMIN_USERNAME, user.getUsername());
  }
}
