/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationListDTO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSecureSharingResourceV2Test
    extends AbstractResourceTest
{
  private static final Permission EXPORT_PERMISSION = Permission.EXPORT_SBOM;

  private static final Permission IMPORT_PERMISSION = Permission.IMPORT_SBOM;

  private User user;

  @Before
  public void before() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(true);

    tempEntity.newApplicationWithParent("app0");
    Application app1 = tempEntity.newApplicationWithParent("app1");
    Application app2 = tempEntity.newApplicationWithParent("app2");
    Application app3 = tempEntity.newApplicationWithParent("app3");
    Application app4 = tempEntity.newApplicationWithParent("app4");
    Application app5 = tempEntity.newApplicationWithParent("app5");
    Application app6 = tempEntity.newApplicationWithParent("app6");
    Application app7 = tempEntity.newApplicationWithParent("app7");

    user = tempEntity.newUser();

    Role exportRole = tempEntity.newRole(false, EXPORT_PERMISSION);
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);

    tempEntity.newMembershipMapping(app1.getId(), exportRole.getId(), user.getUsername());
    tempEntity.newMembershipMapping(app2.getId(), importRole.getId(), user.getUsername());
    tempEntity.newMembershipMapping(app3.getId(), exportRole.getId(), user.getUsername());
    tempEntity.newMembershipMapping(app4.getId(), importRole.getId(), user.getUsername());
    tempEntity.newMembershipMapping(app5.getId(), exportRole.getId(), user.getUsername());
    tempEntity.newMembershipMapping(app6.getId(), importRole.getId(), user.getUsername());

    tempEntity.newMembershipMapping(app7.getId(), exportRole.getId(), user.getUsername());
    tempEntity.newMembershipMapping(app7.getId(), importRole.getId(), user.getUsername());
  }

  @Test
  public void testGetApplicationsWithPermissions_MissingSbomManager() throws Exception {
    setFeatures();

    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .get();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).isEqualTo("Your IQ Server license does not enable this feature.");
  }

  @Test
  public void testGetApplicationsWithPermissions_MissingFeature() throws Exception {
    SystemConfigurationPropertyFeature.SECURE_SHARING.setEnabled(false);

    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Feature not supported.");
  }

  @Test
  public void testGetApplicationsWithPermissions_UnrecognizedOrUnsupportedPermission() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .query("permission", "unrecognizedOrUnsupported")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Unrecognized or unsupported permission 'unrecognizedOrUnsupported' expected one of 'export', 'import'.");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .get();

    assertResponseStatus(200, response);
    ApiSecureSharingApplicationListDTO dto = response.getBody(ApiSecureSharingApplicationListDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.applications).extracting(app -> app.publicId)
        .containsExactly("app1", "app2", "app3", "app4", "app5", "app6", "app7");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_ExportPermission() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .query("permission", "export")
        .get();

    assertResponseStatus(200, response);
    ApiSecureSharingApplicationListDTO dto = response.getBody(ApiSecureSharingApplicationListDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.applications).extracting(app -> app.publicId).containsExactly("app1", "app3", "app5", "app7");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_ImportPermission() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .query("permission", "import")
        .get();

    assertResponseStatus(200, response);
    ApiSecureSharingApplicationListDTO dto = response.getBody(ApiSecureSharingApplicationListDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.applications).extracting(app -> app.publicId).containsExactly("app2", "app4", "app6", "app7");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_BothPermissions() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .query("permission", "export", "import")
        .get();

    assertResponseStatus(200, response);
    ApiSecureSharingApplicationListDTO dto = response.getBody(ApiSecureSharingApplicationListDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.applications).extracting(app -> app.publicId)
        .containsExactly("app1", "app2", "app3", "app4", "app5", "app6", "app7");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_Paged() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .query("page", 2)
        .query("pageSize", 2)
        .get();

    assertResponseStatus(200, response);
    ApiSecureSharingApplicationListDTO dto = response.getBody(ApiSecureSharingApplicationListDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.applications).extracting(app -> app.publicId).containsExactly("app3", "app4");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_PagedAndFiltered() throws Exception {
    HttpResponse response = restRequest()
        .auth(user)
        .path(PublicApiPaths.DISTRIBUTE_PATH)
        .path(ApiSecureSharingResourceV2.APPLICATIONS_PATH)
        .query("page", 2)
        .query("pageSize", 2)
        .query("permission", "import")
        .get();

    assertResponseStatus(200, response);
    ApiSecureSharingApplicationListDTO dto = response.getBody(ApiSecureSharingApplicationListDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.applications).extracting(app -> app.publicId).containsExactly("app6", "app7");
  }
}
