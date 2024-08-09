/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.SpdxMediaType;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationListDTO;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingSbomListDTO;
import com.sonatype.insight.brain.dataaccess.NotAcceptableException;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.file.SbomFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.cyclonedx.CycloneDxMediaType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.setupScenarioWithMetadataComponentSecurityLicenseAndVex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiSecureSharingServiceTest
    extends AbstractComponentTest
{
  private static final Permission EXPORT_PERMISSION = Permission.EXPORT_SBOM;

  private static final Permission IMPORT_PERMISSION = Permission.IMPORT_SBOM;

  @Inject
  private ApiSecureSharingService service;

  @Inject
  private InsightWork insightWork;

  @Mock
  private ApiSbomService mockApiSbomService;

  @Captor
  private ArgumentCaptor<SbomExportParams> sbomExportParamsArgumentCaptor;

  private String applicationId;

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.bindInterceptor(Matchers.subclassesOf(ApiSbomService.class), Matchers.any(), invocation -> {
      if (invocation.getMethod().getModifiers() == Modifier.PUBLIC) {
        invocation.getMethod().invoke(mockApiSbomService, invocation.getArguments());
      }
      return invocation.proceed();
    });
  }

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

  @Test
  public void testExportSbom_ApplicationDoesNotExist() {
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.newThirdPartySbomMetadata("doesNotExist", SbomStatus.ACTIVE.name(), "bom.xml");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.exportSbom(thirdPartySbomMetadata.getApplicationId(),
            thirdPartySbomMetadata.getSbomVersion(), null))
        .withMessageContaining("Cannot find an application with id/public id 'doesNotExist'.");
  }

  @Test
  public void testExportSbom_WrongOwnerType() {
    Organization organization = tempEntity.newOrganization();
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.newThirdPartySbomMetadata(organization.getId(), SbomStatus.ACTIVE.name(), "bom.xml");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.exportSbom(thirdPartySbomMetadata.getApplicationId(),
            thirdPartySbomMetadata.getSbomVersion(), null))
        .withMessageContaining("Cannot find an application with id/public id '" + organization.getId() + "'.");
  }

  @Test
  public void testExportSbom_SbomNotFound() {
    Application application = tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.exportSbom(application.getId(), "doesNotExist", null))
        .withMessageContaining("Cannot find version doesNotExist for application with ID " + application.getId() + ".");
  }

  @Test
  public void testExportSbom_SbomNotActive() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.newThirdPartySbomMetadata(application.getId(), SbomStatus.PENDING.name(), "bom.xml");

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.exportSbom(application.getId(), thirdPartySbomMetadata.getSbomVersion(), null))
        .withMessageContaining(
            "Cannot find version " + thirdPartySbomMetadata.getSbomVersion() + " for application with ID " +
                application.getId() + ".");
  }

  @Test
  public void testExportSbom_UnacceptableType() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        tempEntity.newThirdPartySbomMetadata(application.getId(), SbomStatus.ACTIVE.name(), "bom.xml");

    assertThatExceptionOfType(NotAcceptableException.class)
        .isThrownBy(() -> service.exportSbom(application.getId(), thirdPartySbomMetadata.getSbomVersion(),
            MediaType.APPLICATION_SVG_XML))
        .withMessageContaining("Media type 'application/svg+xml' is unacceptable, expected one of " +
            "['application/vnd.cyclonedx+json', 'application/vnd.cyclonedx+xml', 'application/spdx+json', " +
            "'application/spdx+xml']");
  }

  @Test
  public void testExportSbom_ApplicationPublicId() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "valid-cyclonedx-result-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, application, zippedBom, sbomVersion,
        "CycloneDx", "1.5", SbomFormat.XML);

    assertThatNoException().isThrownBy(() -> service.exportSbom(application.getPublicId(), sbomVersion, null));
  }

  @Test
  public void testExportSbom_NullAccept() throws Exception {
    testExportSbom(
        null,
        CycloneDxMediaType.APPLICATION_CYCLONEDX_XML,
        SbomSpecification.CYCLONEDX,
        "1.6",
        SbomFormat.XML
    );
  }

  @Test
  public void testExportSbom_WildcardAccept() throws Exception {
    testExportSbom(
        MediaType.WILDCARD,
        CycloneDxMediaType.APPLICATION_CYCLONEDX_XML,
        SbomSpecification.CYCLONEDX,
        "1.6",
        SbomFormat.XML
    );
  }

  @Test
  public void testExportSbom_CycloneDX_Json_Accept() throws Exception {
    testExportSbom(
        CycloneDxMediaType.APPLICATION_CYCLONEDX_JSON,
        CycloneDxMediaType.APPLICATION_CYCLONEDX_JSON,
        SbomSpecification.CYCLONEDX,
        "1.6",
        SbomFormat.JSON
    );
  }

  @Test
  public void testExportSbom_CycloneDX_Xml_Accept() throws Exception {
    testExportSbom(
        CycloneDxMediaType.APPLICATION_CYCLONEDX_XML,
        CycloneDxMediaType.APPLICATION_CYCLONEDX_XML,
        SbomSpecification.CYCLONEDX,
        "1.6",
        SbomFormat.XML
    );
  }

  @Test
  public void testExportSbom_Spdx_Json_Accept() throws Exception {
    testExportSbom(
        SpdxMediaType.APPLICATION_SPDX_JSON,
        SpdxMediaType.APPLICATION_SPDX_JSON,
        SbomSpecification.SPDX,
        "2.3",
        SbomFormat.JSON
    );
  }

  @Test
  public void testExportSbom_Spdx_Xml_Accept() throws Exception {
    testExportSbom(
        SpdxMediaType.APPLICATION_SPDX_XML,
        SpdxMediaType.APPLICATION_SPDX_XML,
        SbomSpecification.SPDX,
        "2.3",
        SbomFormat.XML
    );
  }

  private void testExportSbom(
      final String accept,
      final String expectedAccept,
      final SbomSpecification expectedSbomSpecification,
      final String expectedVersion,
      final SbomFormat expectedFormat) throws Exception
  {
    Application application = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "valid-cyclonedx-result-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, application, zippedBom, sbomVersion,
        "CycloneDx", "1.5", SbomFormat.XML);

    Response response = service.exportSbom(application.getId(), sbomVersion, accept);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);
    String content = new String((byte[]) response.getEntity(), StandardCharsets.UTF_8);
    assertThat(content).isNotEmpty();
    ObjectMapper objectMapper = expectedFormat == SbomFormat.JSON ? new ObjectMapper() : new XmlMapper();
    JsonNode jsonNode = objectMapper.readTree(content);
    assertThat(jsonNode).isNotNull();
    verify(mockApiSbomService).buildSbomResponse(
        sbomExportParamsArgumentCaptor.capture(),
        eq(application.getId()),
        eq(sbomVersion),
        eq(expectedAccept)
    );
    SbomExportParams sbomExportParams = sbomExportParamsArgumentCaptor.getValue();
    assertThat(sbomExportParams.getExportSpecification().getSpecification()).isEqualTo(expectedSbomSpecification);
    assertThat(sbomExportParams.getExportSpecification().getVersion()).isEqualTo(expectedVersion);
    assertThat(sbomExportParams.getTargetFormat()).isEqualTo(expectedFormat);
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

  @Test
  public void testGetSbomMetadataByApplication_NegativePage() {
    initSbomMetadata();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomMetadataByApplication(applicationId, -1, 1))
        .withMessageContaining("page must be at least 1.");
  }

  @Test
  public void testGetSbomMetadataByApplication_ZeroPage() {
    initSbomMetadata();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomMetadataByApplication(applicationId, 0, 1))
        .withMessageContaining("page must be at least 1.");
  }

  @Test
  public void testGetSbomMetadataByApplication_NegativePageSize() {
    initSbomMetadata();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomMetadataByApplication(applicationId, 1, -1))
        .withMessageContaining("pageSize must be at least 1.");
  }

  @Test
  public void testGetSbomMetadataByApplication_ZeroPageSize() {
    initSbomMetadata();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomMetadataByApplication(applicationId, 1, 0))
        .withMessageContaining("pageSize must be at least 1.");
  }

  @Test
  public void testGetSbomMetadataByApplication_ValidParameters() {
    List<ThirdPartySbomMetadata> sbomMetadataList = initSbomMetadata();
    ApiSecureSharingSbomListDTO result = service.getSbomMetadataByApplication(applicationId, 1, 10);

    assertThat(result.total).isEqualTo(2);
    assertThat(result.sboms).hasSize(2);
    assertThat(result.sboms.get(0))
        .extracting("id", "sbomVersion", "created")
        .containsExactly(
            sbomMetadataList.get(1).getId(),
            sbomMetadataList.get(1).getSbomVersion(),
            sbomMetadataList.get(1).getCreatedAt()
        );

    assertThat(result.sboms.get(1))
        .extracting("id", "sbomVersion", "created")
        .containsExactly(
            sbomMetadataList.get(0).getId(),
            sbomMetadataList.get(0).getSbomVersion(),
            sbomMetadataList.get(0).getCreatedAt()
        );
  }

  @Test
  public void testGetSbomMetadataByApplication_PageSize_SmallerThanResults() {
    List<ThirdPartySbomMetadata> sbomMetadataList = initSbomMetadata();
    ApiSecureSharingSbomListDTO result = service.getSbomMetadataByApplication(applicationId, 1, 1);

    assertThat(result).isNotNull();
    assertThat(result.total).isEqualTo(2);
    assertThat(result.sboms).hasSize(1);
    assertThat(result.sboms.get(0))
        .extracting("id", "sbomVersion", "created")
        .containsExactly(
            sbomMetadataList.get(1).getId(),
            sbomMetadataList.get(1).getSbomVersion(),
            sbomMetadataList.get(1).getCreatedAt()
        );
  }

  @Test
  public void testGetSbomMetadataByApplication_ApplicationPublicId() throws Exception {
    initSbomMetadata();

    assertThatNoException().isThrownBy(() -> service.getSbomMetadataByApplication(applicationId, 1, 1));
  }

  private List<ThirdPartySbomMetadata> initSbomMetadata() {
    Organization organization = tempEntity.newOrganization("test-organization");
    applicationId = tempEntity.newApplication("Test Application", "test-application", organization.getId())
        .getId();

    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);

    return List.of(tempEntity.newThirdPartySbomMetadata(
            scannedFile.getId(),
            applicationId,
            "test-version",
            "ACTIVE",
            scannedFile.getFilename(),
            SbomSpecification.CYCLONEDX.name(),
            SbomFormat.XML.name(),
            "0.0",
            new Date(0)
        ), tempEntity.newThirdPartySbomMetadata(
            scannedFile.getId(),
            applicationId,
            "test-version2",
            "ACTIVE",
            scannedFile.getFilename(),
            SbomSpecification.CYCLONEDX.name(),
            SbomFormat.XML.name(),
            "0.0",
            new Date(1)
        ), tempEntity.newThirdPartySbomMetadata(
            scannedFile.getId(),
            applicationId,
            "test-version3",
            "PENDING",
            scannedFile.getFilename(),
            SbomSpecification.CYCLONEDX.name(),
            SbomFormat.XML.name(),
            "0.0",
            new Date(1)
        )
    );
  }
}
