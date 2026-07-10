/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/applicationCategories} — covers org-scoped CRUD plus
 * the read-only views that answer "which categories does application X see" and "which
 * categories are applied across the org today". Unlike applications and organizations, this
 * resource does not expose a bare {@code /api/v2/applicationCategories/{id}} — all mutating
 * routes are scoped by organization at {@code /organization/{organizationId}[/{tagId}]}.
 */
@Category(ApiRegressionTest.class)
public class ApplicationCategoriesApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String CATEGORIES_BASE = PublicApiPaths.APPLICATION_CATEGORY_RESOURCE_PATH;

  /**
   * Arbitrary color used across the create/update DTOs — the specific value is not part of what
   * these tests exercise, but the API requires a non-null value.
   */
  private static final String DEFAULT_COLOR = Color.dark_red.name();

  @Test
  public void testCreateApplicationCategory_success() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Create"));

    ApiApplicationCategoryDTO body = new ApiApplicationCategoryDTO();
    body.name = uniqueName("Api Cat");
    body.description = "regression-test tag";
    body.color = DEFAULT_COLOR;

    HttpResponse response = apiPostJson(orgCategoriesPath(org), body);
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("name").isEqualTo(body.name);
    assertThatJson(responseBody).node("description").isEqualTo(body.description);
    assertThatJson(responseBody).node("organizationId").isEqualTo(org.getId());
    assertThatJson(responseBody).node("id").isString().isNotEmpty();
  }

  @Test
  public void testGetTagsByOrganization_returnsSeededTag() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat List"));
    Tag tag = tempEntity.newTag(org.getId(), uniqueName("Api Cat"), "desc", Color.dark_red);

    HttpResponse response = apiGet(orgCategoriesPath(org));
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).isArray().hasSize(1);
    assertThatJson(responseBody).node("[0].id").isEqualTo(tag.getId());
    assertThatJson(responseBody).node("[0].name").isEqualTo(tag.getName());
    assertThatJson(responseBody).node("[0].organizationId").isEqualTo(org.getId());
  }

  @Test
  public void testGetApplicableTagsByOrganization_returnsSeededTag() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Applicable"));
    Tag tag = tempEntity.newTag(org.getId(), uniqueName("Api Cat"), "desc", Color.dark_red);

    HttpResponse response = apiGet(orgCategoriesPath(org) + "/applicable");
    assertResponseStatus(200, response);

    // The /applicable endpoint returns ApplicableTagsDTO (nested shape), so we use "$..id" to
    // find any id field at any depth rather than "$[*].id" which assumes a bare array.
    assertThatJson(response.getBodyText())
        .inPath("$..id")
        .isArray()
        .contains(tag.getId());
  }

  @Test
  public void testGetTagsUsedByApplications_returnsAppliedTag() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Used"));
    Application app = tempEntity.newApplication(uniqueId("api-cat-used-app"), org.getId());
    Tag tag = tempEntity.newTag(org.getId(), uniqueName("Api Cat Used"), "desc", Color.dark_red);
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    HttpResponse response = apiGet(appCategoriesPath());
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .inPath("$[*].id")
        .isArray()
        .contains(tag.getId());
  }

  @Test
  public void testGetApplicableTagsByApplication_returnsAvailableTag() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat App-Applicable"));
    Application app = tempEntity.newApplication(uniqueId("api-cat-app-applicable"), org.getId());
    Tag tag = tempEntity.newTag(org.getId(), uniqueName("Api Cat App-Applicable"), "desc", Color.dark_red);

    HttpResponse response = apiGet(appApplicableCategoriesPath(app.getPublicId()));
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .inPath("$[*].id")
        .isArray()
        .contains(tag.getId());
  }

  @Test
  public void testUpdateApplicationCategory_success() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Update"));
    Tag tag = tempEntity.newTag(org.getId(), uniqueName("Api Cat"), "original", Color.dark_red);

    String renamed = uniqueName("Api Cat Renamed");
    ApiApplicationCategoryDTO body = new ApiApplicationCategoryDTO();
    body.id = tag.getId();
    body.name = renamed;
    body.description = "updated";
    body.organizationId = org.getId();
    body.color = DEFAULT_COLOR;

    HttpResponse update = apiPutJson(orgCategoriesPath(org), body);
    assertResponseStatus(200, update);

    String updateBody = update.getBodyText();
    assertThatJson(updateBody).node("name").isEqualTo(renamed);
    assertThatJson(updateBody).node("description").isEqualTo("updated");

    HttpResponse afterUpdate = apiGet(orgCategoriesPath(org));
    assertResponseStatus(200, afterUpdate);
    String afterUpdateBody = afterUpdate.getBodyText();
    assertThatJson(afterUpdateBody).isArray().hasSize(1);
    assertThatJson(afterUpdateBody).node("[0].name").isEqualTo(renamed);
    assertThatJson(afterUpdateBody).node("[0].description").isEqualTo("updated");
  }

  @Test
  public void testDeleteApplicationCategory_success() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Delete"));
    Tag tag = tempEntity.newTag(org.getId(), uniqueName("Api Cat"), "desc", Color.dark_red);

    assertResponseStatus(204, apiDelete(categoryPath(org, tag)));
    // Tag is deleted via API above; TemporaryEntity cleanup at teardown is a no-op today.

    HttpResponse afterDelete = apiGet(orgCategoriesPath(org));
    assertResponseStatus(200, afterDelete);
    assertThatJson(afterDelete.getBodyText()).isArray().isEmpty();
  }

  @Test
  public void testDeleteApplicationCategory_notFound() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Delete 404"));

    HttpResponse response = apiDelete(orgCategoriesPath(org) + "/" + uniqueId("nonexistent-tag"));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetTagsByOrganization_unauthenticated_returns401() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Anon"));

    HttpResponse response = anonApiGet(orgCategoriesPath(org));
    assertResponseStatus(401, response);
  }

  /** Auth contract on POST: unauthenticated create fails with 401 before the body is parsed. */
  @Test
  public void testCreateApplicationCategory_unauthenticated_returns401() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Anon POST"));

    HttpResponse response = anonApiPostJson(orgCategoriesPath(org), new ApiApplicationCategoryDTO());
    assertResponseStatus(401, response);
  }

  /** Auth contract on PUT: unauthenticated update fails with 401 before the body is parsed. */
  @Test
  public void testUpdateApplicationCategory_unauthenticated_returns401() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Anon PUT"));

    HttpResponse response = anonApiPutJson(orgCategoriesPath(org), new ApiApplicationCategoryDTO());
    assertResponseStatus(401, response);
  }

  /** Auth contract on DELETE: unauthenticated delete fails with 401. */
  @Test
  public void testDeleteApplicationCategory_unauthenticated_returns401() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Cat Anon DELETE"));

    HttpResponse response = anonApiDelete(orgCategoriesPath(org) + "/" + uniqueId("any-tag"));
    assertResponseStatus(401, response);
  }

  private static String orgCategoriesPath(final Organization org) {
    return CATEGORIES_BASE + "/organization/" + org.getId();
  }

  private static String categoryPath(final Organization org, final Tag tag) {
    return orgCategoriesPath(org) + "/" + tag.getId();
  }

  private static String appCategoriesPath() {
    return CATEGORIES_BASE + "/application";
  }

  private static String appApplicableCategoriesPath(final String applicationPublicId) {
    return appCategoriesPath() + "/" + applicationPublicId + "/applicable";
  }
}
