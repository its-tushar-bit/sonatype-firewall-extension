/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SidebarResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SidebarResource.RESOURCE_PATH);
  }

  @Test
  public void testGetOwnerList() throws Exception {
    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    OwnerHierarchyDTO ownerHierarchyDTO = response.getBody(OwnerHierarchyDTO.class);
    assertThat(ownerHierarchyDTO).isNotNull();
  }

  @Test
  public void testGetOwnerDetails_Organization() throws Exception {
    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  public void testGetOwnerDetails_Application() throws Exception {
    final String applicationPublicId = "SidebarResourceTest_Application";
    tempEntity.newApplicationWithParent(applicationPublicId);

    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.APPLICATION, applicationPublicId).get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  public void testGetOwnerDetails_RepositoryContaier() throws Exception {
    HttpResponse response = restRequest().path(SidebarResource.GET_GLOBAL_OWNER_DETAILS_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER).get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  public void testGetOwnerDetails_RepositoryManager() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repoManager.getId()).get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  public void testGetOwnerDetails_Repository() throws Exception {
    Repository repo = tempEntity.newRepository();
    HttpResponse response =
        restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH).parameter(OwnerType.REPOSITORY, repo.getId()).get();

    assertValidOwnerDetailsDTO(response);
  }

  private void assertValidOwnerDetailsDTO(HttpResponse response) {
    assertResponseStatus(200, response);
    OwnerDetailsDTO ownerDetailsDTO = response.getBody(OwnerDetailsDTO.class);
    assertThat(ownerDetailsDTO).isNotNull();
  }
}
