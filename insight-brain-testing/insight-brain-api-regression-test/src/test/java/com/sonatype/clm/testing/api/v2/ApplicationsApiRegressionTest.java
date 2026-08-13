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
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/applications} — covers create, get (single + list),
 * organization-scoped list, filter by publicId, update, delete, and the auth contract.
 */
@Category(ApiRegressionTest.class)
public class ApplicationsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String APPS_BASE = PublicApiPaths.APP_RESOURCE_PATH;

  @Test
  public void testCreateApplication_success() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api App Create"));

    ApiApplicationDTO body = new ApiApplicationDTO();
    body.publicId = uniqueId("api-app");
    body.name = uniqueName("Api App");
    body.organizationId = org.getId();

    HttpResponse response = apiPostJson(APPS_BASE, body);
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("publicId").isEqualTo(body.publicId);
    assertThatJson(responseBody).node("name").isEqualTo(body.name);
    assertThatJson(responseBody).node("organizationId").isEqualTo(org.getId());
    assertThatJson(responseBody).node("id").isString().isNotEmpty();
  }

  @Test
  public void testCreateApplication_duplicatePublicId_returns400() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api App Duplicate"));

    ApiApplicationDTO first = new ApiApplicationDTO();
    first.publicId = uniqueId("api-app-dup");
    first.name = uniqueName("Api App Dup First");
    first.organizationId = org.getId();
    assertResponseStatus(200, apiPostJson(APPS_BASE, first));

    ApiApplicationDTO duplicate = new ApiApplicationDTO();
    duplicate.publicId = first.publicId;
    duplicate.name = uniqueName("Api App Dup Second");
    duplicate.organizationId = org.getId();

    HttpResponse response = apiPostJson(APPS_BASE, duplicate);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("already");
  }

  @Test
  public void testCreateApplication_appearsInList() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api App List After Create"));

    ApiApplicationDTO body = new ApiApplicationDTO();
    body.publicId = uniqueId("api-app-list");
    body.name = uniqueName("Api App List");
    body.organizationId = org.getId();

    assertResponseStatus(200, apiPostJson(APPS_BASE, body));

    HttpResponse list = apiGet(APPS_BASE, "publicId", body.publicId);
    assertResponseStatus(200, list);

    String listBody = list.getBodyText();
    assertThatJson(listBody).node("applications").isArray().hasSize(1);
    assertThatJson(listBody).node("applications[0].publicId").isEqualTo(body.publicId);
  }

  @Test
  public void testGetApplications_returnsSeededApp() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api App Seeded"));
    Application app = tempEntity.newApplication(uniqueId("api-app-seeded"), org.getId());

    HttpResponse response = apiGet(APPS_BASE, "publicId", app.getPublicId());
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("applications").isArray().hasSize(1);
    assertThatJson(responseBody).node("applications[0].id").isEqualTo(app.getId());
  }

  @Test
  public void testGetApplications_filterByUnknownPublicId_returnsEmpty() throws Exception {
    HttpResponse response = apiGet(APPS_BASE, "publicId", uniqueId("no-such-app"));
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .node("applications")
        .isArray()
        .isEmpty();
  }

  @Test
  public void testGetApplicationsByOrganizationId_returnsAppsUnderOrg() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api App By Org"));
    Application app1 = tempEntity.newApplication(uniqueId("api-app-by-org-1"), org.getId());
    Application app2 = tempEntity.newApplication(uniqueId("api-app-by-org-2"), org.getId());

    HttpResponse response = apiGet(APPS_BASE + "/organization/" + org.getId());
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("applications").isArray().hasSize(2);
    assertThatJson(responseBody)
        .inPath("$.applications[*].id")
        .isArray()
        .contains(app1.getId(), app2.getId());
  }

  @Test
  public void testGetApplicationById_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-app-by-id"), Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(appPath(app));
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("id").isEqualTo(app.getId());
    assertThatJson(responseBody).node("publicId").isEqualTo(app.getPublicId());
    assertThatJson(responseBody).node("name").isEqualTo(app.getName());
  }

  @Test
  public void testGetApplicationById_notFound() throws Exception {
    HttpResponse response = apiGet(appPath(uniqueId("nonexistent-app")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testUpdateApplication_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-app-update"), Organization.ROOT_ORGANIZATION_ID);

    String renamed = uniqueName("Api App Renamed");
    ApiApplicationDTO body = new ApiApplicationDTO();
    body.id = app.getId();
    body.publicId = app.getPublicId();
    body.name = renamed;
    body.organizationId = app.getOrganizationId();

    HttpResponse update = apiPutJson(appPath(app), body);
    assertResponseStatus(200, update);
    assertThatJson(update.getBodyText()).node("name").isEqualTo(renamed);

    HttpResponse get = apiGet(appPath(app));
    assertResponseStatus(200, get);
    assertThatJson(get.getBodyText()).node("name").isEqualTo(renamed);
  }

  @Test
  public void testUpdateApplication_mismatchedIds_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-app-mismatch"), Organization.ROOT_ORGANIZATION_ID);

    ApiApplicationDTO body = new ApiApplicationDTO();
    body.id = uniqueId("different-id");
    body.publicId = app.getPublicId();
    body.name = app.getName();
    body.organizationId = app.getOrganizationId();

    HttpResponse response = apiPutJson(appPath(app), body);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("did not match");
  }

  /**
   * PUT to a nonexistent application ID returns <b>400, not 404</b>: the server throws
   * {@code InvalidApplicationException("Attempting to edit an application that doesn't exist")}
   * from {@code ApplicationDAO.update}, which is mapped to HTTP 400 via
   * {@code @HttpStatusCode(400)}. Test pins that mapping.
   */
  @Test
  public void testUpdateApplication_notFound() throws Exception {
    String missingId = uniqueId("nonexistent-app");
    ApiApplicationDTO body = new ApiApplicationDTO();
    body.id = missingId;
    body.publicId = uniqueId("api-app-update-404");
    body.name = uniqueName("Api App Update 404");
    body.organizationId = Organization.ROOT_ORGANIZATION_ID;

    HttpResponse response = apiPutJson(appPath(missingId), body);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("doesn't exist");
  }

  @Test
  public void testDeleteApplication_success() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-app-delete"), Organization.ROOT_ORGANIZATION_ID);

    assertResponseStatus(204, apiDelete(appPath(app)));
    assertResponseStatus(404, apiGet(appPath(app)));
    // App is deleted via API above; TemporaryEntity cleanup at teardown is a no-op today.
  }

  @Test
  public void testDeleteApplication_notFound() throws Exception {
    HttpResponse response = apiDelete(appPath(uniqueId("nonexistent-app")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetApplications_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(APPS_BASE);
    assertResponseStatus(401, response);
  }

  /** Auth contract on POST: unauthenticated create fails with 401 before the body is parsed. */
  @Test
  public void testCreateApplication_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(APPS_BASE, new ApiApplicationDTO());
    assertResponseStatus(401, response);
  }

  /** Auth contract on PUT: unauthenticated update fails with 401 before the body is parsed. */
  @Test
  public void testUpdateApplication_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPutJson(appPath(uniqueId("any-app")), new ApiApplicationDTO());
    assertResponseStatus(401, response);
  }

  /** Auth contract on DELETE: unauthenticated delete fails with 401. */
  @Test
  public void testDeleteApplication_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(appPath(uniqueId("any-app")));
    assertResponseStatus(401, response);
  }

  private static String appPath(final Application app) {
    return appPath(app.getId());
  }

  private static String appPath(final String applicationId) {
    return APPS_BASE + "/" + applicationId;
  }
}
