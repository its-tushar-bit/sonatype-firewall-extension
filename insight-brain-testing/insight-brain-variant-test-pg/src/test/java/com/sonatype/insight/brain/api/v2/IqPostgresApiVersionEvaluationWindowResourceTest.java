/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiVersionEvaluationWindowsDTO;
import com.sonatype.insight.brain.dataaccess.configuration.VersionEvaluationWindowDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresApiVersionEvaluationWindowResourceTest
{
  private IqTestContext ctx;

  private VersionEvaluationWindowDAO dao;

  @BeforeEach
  void setUp() {
    dao = ctx.lookup(VersionEvaluationWindowDAO.class);
  }

  private HttpRequest restRequest(final OwnerType ownerType, final String ownerId) {
    return ctx.restRequest()
        .path(PublicApiPaths.VERSION_EVALUATION_WINDOW_RESOURCE_PATH, ApiVersionEvaluationWindowResource.OWNER_PATH)
        .parameter(ownerType, ownerId);
  }

  @Test
  void testGetVersionEvaluationWindows_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context2", 20, 60);

    HttpResponse response = restRequest(org.getType(), org.getId()).get();

    ctx.assertResponseStatus(200, response);
    ApiVersionEvaluationWindowsDTO dto = response.getBody(ApiVersionEvaluationWindowsDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.versionEvaluationWindows()).hasSize(2);
    assertThat(dto.versionEvaluationWindows())
        .extracting(ApiVersionEvaluationWindowDTO::contextId)
        .containsExactlyInAnyOrder("context1", "context2");
  }

  @Test
  void testGetVersionEvaluationWindows_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newVersionEvaluationWindow(app.getId(), "context1", 15, 45);

    HttpResponse response = restRequest(app.getType(), app.getId()).get();

    ctx.assertResponseStatus(200, response);
    ApiVersionEvaluationWindowsDTO dto = response.getBody(ApiVersionEvaluationWindowsDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.versionEvaluationWindows()).hasSize(1);
    assertThat(dto.versionEvaluationWindows().get(0).contextId()).isEqualTo("context1");
    assertThat(dto.versionEvaluationWindows().get(0).maxVersions()).isEqualTo(15);
    assertThat(dto.versionEvaluationWindows().get(0).maxAgeInDays()).isEqualTo(45);
  }

  @Test
  void testGetVersionEvaluationWindows_Empty() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();

    HttpResponse response = restRequest(org.getType(), org.getId()).get();

    ctx.assertResponseStatus(200, response);
    ApiVersionEvaluationWindowsDTO dto = response.getBody(ApiVersionEvaluationWindowsDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.versionEvaluationWindows()).isEmpty();
  }

  @Test
  void testGetVersionEvaluationWindow_PublicId() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newVersionEvaluationWindow(app.getId(), "context1", 10, 30);

    HttpResponse response = restRequest(app.getType(), app.getPublicId()).get();

    ctx.assertResponseStatus(200, response);
    ApiVersionEvaluationWindowsDTO dto = response.getBody(ApiVersionEvaluationWindowsDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.versionEvaluationWindows()).hasSize(1);
  }

  @Test
  void testSetVersionEvaluationWindow_Insert_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    HttpResponse response = restRequest(org.getType(), org.getId()).body(dto).put();

    ctx.assertResponseStatus(204, response);
    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getContextId()).isEqualTo("context1");
    assertThat(stored.getMaxVersions()).isEqualTo(10);
    assertThat(stored.getMaxAgeInDays()).isEqualTo(30);
  }

  @Test
  void testSetVersionEvaluationWindow_Insert_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    HttpResponse response = restRequest(app.getType(), app.getId()).body(dto).put();

    ctx.assertResponseStatus(204, response);
    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(app.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getContextId()).isEqualTo("context1");
    assertThat(stored.getMaxVersions()).isEqualTo(10);
    assertThat(stored.getMaxAgeInDays()).isEqualTo(30);
  }

  @Test
  void testSetVersionEvaluationWindow_Update_Organization() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 20, 60);

    HttpResponse response = restRequest(org.getType(), org.getId()).body(dto).put();

    ctx.assertResponseStatus(204, response);
    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(org.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isEqualTo(20);
    assertThat(stored.getMaxAgeInDays()).isEqualTo(60);
  }

  @Test
  void testSetVersionEvaluationWindow_Update_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newVersionEvaluationWindow(app.getId(), "context1", 10, 30);
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 20, 60);

    HttpResponse response = restRequest(app.getType(), app.getId()).body(dto).put();

    ctx.assertResponseStatus(204, response);
    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(app.getId(), "context1");
    assertThat(stored).isNotNull();
    assertThat(stored.getMaxVersions()).isEqualTo(20);
    assertThat(stored.getMaxAgeInDays()).isEqualTo(60);
  }

  @Test
  void testSetVersionEvaluationWindow_PublicId() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ApiVersionEvaluationWindowDTO dto = new ApiVersionEvaluationWindowDTO("context1", 10, 30);

    HttpResponse response = restRequest(app.getType(), app.getPublicId()).body(dto).put();

    ctx.assertResponseStatus(204, response);
    VersionEvaluationWindow stored = dao.getByOwnerIdAndContextId(app.getId(), "context1");
    assertThat(stored).isNotNull();
  }

  @Test
  void testDeleteVersionEvaluationWindow_SpecificContext() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    VersionEvaluationWindow window1 = ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    VersionEvaluationWindow window2 = ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context2", 20, 60);

    HttpResponse response = restRequest(org.getType(), org.getId())
        .query("contextId", "context1")
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNotNull();
  }

  @Test
  void testDeleteVersionEvaluationWindow_AllContexts() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    VersionEvaluationWindow window1 = ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context1", 10, 30);
    VersionEvaluationWindow window2 = ctx.tempEntity().newVersionEvaluationWindow(org.getId(), "context2", 20, 60);

    HttpResponse response = restRequest(org.getType(), org.getId()).delete();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNull();
  }

  @Test
  void testDeleteVersionEvaluationWindow_PublicId() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    VersionEvaluationWindow window = ctx.tempEntity().newVersionEvaluationWindow(app.getId(), "context1", 10, 30);

    HttpResponse response = restRequest(app.getType(), app.getPublicId())
        .query("contextId", "context1")
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.getById(window.getId())).isNull();
  }
}
