/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(ConditionValueTypeResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testGetConditionValueTypes_Application() throws Exception {
    String appPublicId = "ConditionValueTypeResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    final HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
  }

  @Test
  public void testGetConditionValueTypes_Organization() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    final HttpResponse response = restRequest(OwnerType.ORGANIZATION, orgId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
  }

  @Test
  public void testGetConditionValueTypes_RepositoryContainer() throws Exception {
    HttpResponse response =
        restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
  }

  @Test
  public void testGetConditionValueTypes_RepositoryManager() throws Exception {
    String ownerId = tempEntity.newRepositoryManager().getId();

    HttpResponse response = restRequest(OwnerType.REPOSITORY_MANAGER, ownerId).get();
    assertResponseStatus(200, response);
    Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
  }

  @Test
  public void testGetConditionValueTypes_Repository() throws Exception {
    String ownerId = tempEntity.newRepository().getId();

    HttpResponse response = restRequest(OwnerType.REPOSITORY, ownerId).get();
    assertResponseStatus(200, response);
    Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
  }
}
