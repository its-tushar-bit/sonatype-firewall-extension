/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiApplicationCategoryResource;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertTag;
import static com.sonatype.insight.brain.tag.TagService.fromDTO;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiApplicationCategoryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD() throws Exception {
    Organization org = tempEntity.newOrganization("TagResourceTest");
    tempEntity.newTag(org.getParentOrganizationId(), "Root Tag");

    HttpRequest request = restRequest()
        .path(ApiApplicationCategoryResource.RESOURCE_PATH,
            ApiApplicationCategoryResource.ORGANIZATION_PATH)
        .parameter(org.getId());

    HttpRequest getApplicableTagsPath = restRequest().path(ApiApplicationCategoryResource.RESOURCE_PATH,
        ApiApplicationCategoryResource.ORGANIZATION_APPLICABLE_TAGS_PATH).parameter(org.getId());

    // Get
    HttpResponse response = getApplicableTagsPath.get();
    assertResponseStatus(200, response);
    ApplicableTagsDTO tags = response.getBody(ApplicableTagsDTO.class);
    assertThat(tags).isNotNull();
    assertThat(tags.applicationCategoriesByOwner).hasSize(2);
    assertThat(tags.applicationCategoriesByOwner.get(0).applicationCategories).hasSize(0);
    assertThat(tags.applicationCategoriesByOwner.get(1).applicationCategories).hasSize(1);

    // Add
    ApiApplicationCategoryDTO dto = TagService.toDTO(new Tag(org.getId(), "Tag Name", "Tag description", Color.yellow));
    response = request.body(dto).post();
    assertResponseStatus(200, response);
    assertTag(fromDTO(dto, org.getId()), response.getBody(Tag.class));

    // Get
    response = getApplicableTagsPath.get();
    assertResponseStatus(200, response);
    tags = response.getBody(ApplicableTagsDTO.class);
    assertThat(tags.applicationCategoriesByOwner).hasSize(2);
    assertThat(tags.applicationCategoriesByOwner.get(0).applicationCategories).hasSize(1);
    assertTag(fromDTO(dto, org.getId()),
        fromDTO(tags.applicationCategoriesByOwner.get(0).applicationCategories.get(0), org.getId()));

    // Update
    dto = tags.applicationCategoriesByOwner.get(0).applicationCategories.get(0);
    dto.name = "Tag Updated Name";
    response = request.body(dto).put();
    assertResponseStatus(200, response);
    assertTag(fromDTO(dto, org.getId()), response.getBody(Tag.class));

    // Get
    response = getApplicableTagsPath.get();
    assertResponseStatus(200, response);
    tags = response.getBody(ApplicableTagsDTO.class);
    assertThat(tags).isNotNull();
    assertThat(tags.applicationCategoriesByOwner).hasSize(2);
    assertThat(tags.applicationCategoriesByOwner.get(0).applicationCategories).hasSize(1);
    assertTag(fromDTO(dto, org.getId()),
        fromDTO(tags.applicationCategoriesByOwner.get(0).applicationCategories.get(0), org.getId()));

    // Delete
    response = request.subpath("{tagId}").parameter(dto.id).delete();
    assertResponseStatus(204, response);

    // Get
    response = getApplicableTagsPath.get();
    assertResponseStatus(200, response);
    tags = response.getBody(ApplicableTagsDTO.class);
    assertThat(tags).isNotNull();
    assertThat(tags.applicationCategoriesByOwner).hasSize(2);
    assertThat(tags.applicationCategoriesByOwner.get(0).applicationCategories).hasSize(0);
  }

  @Test
  public void testGetTags() throws Exception {
    tempEntity.newTag(Organization.ROOT_ORGANIZATION_ID, "Root Tag");

    HttpRequest request = restRequest()
        .path(ApiApplicationCategoryResource.RESOURCE_PATH,
            ApiApplicationCategoryResource.ORGANIZATION_PATH)
        .parameter(Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = request.get();

    assertResponseStatus(200, response);
    ApiApplicationCategoryDTO[] apiApplicationCategoryDTO = response.getBody(ApiApplicationCategoryDTO[].class);
    assertThat(apiApplicationCategoryDTO).hasSize(1);
  }
}
