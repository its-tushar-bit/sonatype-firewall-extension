/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationListDTO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class ApiSecureSharingServiceTest
    extends AbstractComponentTest
{
  private static final Permission EXPORT_PERMISSION = Permission.EXPORT_SBOM;

  private static final Permission IMPORT_PERMISSION = Permission.IMPORT_SBOM;

  @Inject
  private ApiSecureSharingService service;

  @Test
  public void testGetApplicationsWithPermissions_NegativePage() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getApplicationsWithPermissions(Collections.singleton(EXPORT_PERMISSION), -1, 1))
        .withMessageContaining("page must be at least 1.");
  }

  @Test
  public void testGetApplicationsWithPermissions_ZeroPage() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getApplicationsWithPermissions(Collections.singleton(EXPORT_PERMISSION), 0, 1))
        .withMessageContaining("page must be at least 1.");
  }

  @Test
  public void testGetApplicationsWithPermissions_NegativePageSize() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getApplicationsWithPermissions(Collections.singleton(EXPORT_PERMISSION), 1, -1))
        .withMessageContaining("pageSize must be at least 1.");
  }

  @Test
  public void testGetApplicationsWithPermissions_ZeroPageSize() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getApplicationsWithPermissions(Collections.singleton(EXPORT_PERMISSION), 1, 0))
        .withMessageContaining("pageSize must be at least 1.");
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_Export_ViaUsername() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID));
    Role exportRole = tempEntity.newRole(false, EXPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), exportRole.getId(), user.getUsername(), MemberType.USER);
    tempEntity.newMembershipMapping(app21.getId(), exportRole.getId(), user.getUsername(), MemberType.USER);

    ApiSecureSharingApplicationListDTO result =
        service.getApplicationsWithPermissions(Collections.singleton(EXPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(3);
    assertThat(result.total).isEqualTo(3);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "export"),
        application -> assertApplicationDTO(application, app12, "export"),
        application -> assertApplicationDTO(application, app21, "export")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_Export_ViaGroup() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    String groupName = "someGroup";
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID,
            Collections.singleton(groupName)));
    Role exportRole = tempEntity.newRole(false, EXPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), exportRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app21.getId(), exportRole.getId(), groupName, MemberType.GROUP);

    ApiSecureSharingApplicationListDTO result =
        service.getApplicationsWithPermissions(Collections.singleton(EXPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(3);
    assertThat(result.total).isEqualTo(3);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "export"),
        application -> assertApplicationDTO(application, app12, "export"),
        application -> assertApplicationDTO(application, app21, "export")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_Import_ViaUsername() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID));
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), importRole.getId(), user.getUsername(), MemberType.USER);
    tempEntity.newMembershipMapping(app21.getId(), importRole.getId(), user.getUsername(), MemberType.USER);

    ApiSecureSharingApplicationListDTO result =
        service.getApplicationsWithPermissions(Collections.singleton(IMPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(3);
    assertThat(result.total).isEqualTo(3);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "import"),
        application -> assertApplicationDTO(application, app12, "import"),
        application -> assertApplicationDTO(application, app21, "import")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_Import_ViaGroup() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    String groupName = "someGroup";
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID,
            Collections.singleton(groupName)));
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), importRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app21.getId(), importRole.getId(), groupName, MemberType.GROUP);

    ApiSecureSharingApplicationListDTO result =
        service.getApplicationsWithPermissions(Collections.singleton(IMPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(3);
    assertThat(result.total).isEqualTo(3);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "import"),
        application -> assertApplicationDTO(application, app12, "import"),
        application -> assertApplicationDTO(application, app21, "import")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_ExportOrImport_ViaUsername() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    Application app22 = tempEntity.newApplication("app22", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID));
    Role exportRole = tempEntity.newRole(false, EXPORT_PERMISSION);
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), exportRole.getId(), user.getUsername(), MemberType.USER);
    tempEntity.newMembershipMapping(app21.getId(), importRole.getId(), user.getUsername(), MemberType.USER);
    tempEntity.newMembershipMapping(app22.getId(), exportRole.getId(), user.getUsername(), MemberType.USER);
    tempEntity.newMembershipMapping(app22.getId(), importRole.getId(), user.getUsername(), MemberType.USER);

    ApiSecureSharingApplicationListDTO result =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(4);
    assertThat(result.total).isEqualTo(4);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "export"),
        application -> assertApplicationDTO(application, app12, "export"),
        application -> assertApplicationDTO(application, app21, "import"),
        application -> assertApplicationDTO(application, app22, "export", "import")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_ExportOrImport_ViaGroup() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    Application app22 = tempEntity.newApplication("app22", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    String groupName = "someGroup";
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID,
            Collections.singleton(groupName)));
    Role exportRole = tempEntity.newRole(false, EXPORT_PERMISSION);
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), exportRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app21.getId(), importRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app22.getId(), exportRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app22.getId(), importRole.getId(), groupName, MemberType.GROUP);

    ApiSecureSharingApplicationListDTO result =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(4);
    assertThat(result.total).isEqualTo(4);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "export"),
        application -> assertApplicationDTO(application, app12, "export"),
        application -> assertApplicationDTO(application, app21, "import"),
        application -> assertApplicationDTO(application, app22, "export", "import")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_ExportOrImport_ViaUsernameOrGroup() {
    Organization org1 = tempEntity.newOrganization();
    Application app11 = tempEntity.newApplication("app11", org1.getId());
    Application app12 = tempEntity.newApplication("app12", org1.getId());
    Organization org2 = tempEntity.newOrganization();
    Application app21 = tempEntity.newApplication("app21", org2.getId());
    Application app22 = tempEntity.newApplication("app22", org2.getId());
    tempEntity.newApplication(org2.getId());
    tempEntity.newApplicationWithParent();
    User user = tempEntity.newUser();
    String groupName = "someGroup";
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID,
            Collections.singleton(groupName)));
    Role exportRole = tempEntity.newRole(false, EXPORT_PERMISSION);
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);
    tempEntity.newMembershipMapping(org1.getId(), exportRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app21.getId(), importRole.getId(), user.getUsername(), MemberType.USER);
    tempEntity.newMembershipMapping(app22.getId(), exportRole.getId(), groupName, MemberType.GROUP);
    tempEntity.newMembershipMapping(app22.getId(), importRole.getId(), user.getUsername(), MemberType.USER);

    ApiSecureSharingApplicationListDTO result;

    result = service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(4);
    assertThat(result.total).isEqualTo(4);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "export"),
        application -> assertApplicationDTO(application, app12, "export"),
        application -> assertApplicationDTO(application, app21, "import"),
        application -> assertApplicationDTO(application, app22, "export", "import")
    );

    result = service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(3);
    assertThat(result.total).isEqualTo(3);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app11, "export"),
        application -> assertApplicationDTO(application, app12, "export"),
        application -> assertApplicationDTO(application, app22, "export", "import")
    );

    result = service.getApplicationsWithPermissions(Set.of(IMPORT_PERMISSION), 1, 10);

    assertThat(result).isNotNull();
    assertThat(result.applications).hasSize(2);
    assertThat(result.total).isEqualTo(2);
    assertThat(result.applications).satisfiesExactly(
        application -> assertApplicationDTO(application, app21, "import"),
        application -> assertApplicationDTO(application, app22, "export", "import")
    );
  }

  @Test
  @PostgresTest
  public void testGetApplicationsWithPermissions_Paged() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication("app1", "app1", org.getId());
    Application app2 = tempEntity.newApplication("app2", "app2", org.getId());
    Application app3 = tempEntity.newApplication("app3", "app3", org.getId());
    User user = tempEntity.newUser();
    when(subject.getPrincipal()).thenReturn(
        new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID));
    Role importRole = tempEntity.newRole(false, IMPORT_PERMISSION);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, importRole.getId(), user.getUsername(),
        MemberType.USER);

    ApiSecureSharingApplicationListDTO page1PageSize1 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 1, 1);

    assertThat(page1PageSize1).isNotNull();
    assertThat(page1PageSize1.applications).hasSize(1);
    assertThat(page1PageSize1.total).isEqualTo(3);
    assertApplicationDTO(page1PageSize1.applications.get(0), app1, "import");

    ApiSecureSharingApplicationListDTO page2PageSize1 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 2, 1);

    assertThat(page2PageSize1).isNotNull();
    assertThat(page2PageSize1.applications).hasSize(1);
    assertThat(page2PageSize1.total).isEqualTo(3);
    assertApplicationDTO(page2PageSize1.applications.get(0), app2, "import");

    ApiSecureSharingApplicationListDTO page3PageSize1 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 3, 1);

    assertThat(page3PageSize1).isNotNull();
    assertThat(page3PageSize1.applications).hasSize(1);
    assertThat(page3PageSize1.total).isEqualTo(3);
    assertApplicationDTO(page3PageSize1.applications.get(0), app3, "import");

    ApiSecureSharingApplicationListDTO page4PageSize1 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 4, 1);

    assertThat(page4PageSize1).isNotNull();
    assertThat(page4PageSize1.applications).isEmpty();

    ApiSecureSharingApplicationListDTO page1PageSize2 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 1, 2);

    assertThat(page1PageSize2).isNotNull();
    assertThat(page1PageSize2.applications).hasSize(2);
    assertThat(page1PageSize2.total).isEqualTo(3);
    assertApplicationDTO(page1PageSize2.applications.get(0), app1, "import");
    assertApplicationDTO(page1PageSize2.applications.get(1), app2, "import");

    ApiSecureSharingApplicationListDTO page2PageSize2 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 2, 2);

    assertThat(page2PageSize2).isNotNull();
    assertThat(page2PageSize2.applications).hasSize(1);
    assertThat(page2PageSize2.total).isEqualTo(3);
    assertApplicationDTO(page2PageSize2.applications.get(0), app3, "import");

    ApiSecureSharingApplicationListDTO page3PageSize2 =
        service.getApplicationsWithPermissions(Set.of(EXPORT_PERMISSION, IMPORT_PERMISSION), 3, 2);

    assertThat(page3PageSize2).isNotNull();
    assertThat(page3PageSize2.applications).isEmpty();
    assertThat(page3PageSize2.total).isEqualTo(3);
  }

  private void assertApplicationDTO(
      final ApiSecureSharingApplicationDTO dto,
      final Application expectedApplication,
      final String... expectedPermissions)
  {
    assertThat(dto.id).isEqualTo(expectedApplication.getId());
    assertThat(dto.publicId).isEqualTo(expectedApplication.getPublicId());
    assertThat(dto.name).isEqualTo(expectedApplication.getName());
    assertThat(dto.permissions).containsExactlyElementsOf(Arrays.asList(expectedPermissions));
  }
}
