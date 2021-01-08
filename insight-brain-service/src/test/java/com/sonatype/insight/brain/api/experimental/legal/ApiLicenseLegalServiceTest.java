/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApplicationLicenseUsageTelemetry;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.api.v2.service.DefaultApiLicenseDataAdapter;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentDTO;
import com.sonatype.insight.license.dto.model.ComponentLegalFileDTO;
import com.sonatype.insight.license.dto.model.LegalCommentDTO;
import com.sonatype.insight.license.dto.model.LegalCopyrightDTO;
import com.sonatype.insight.license.dto.model.LegalFileDTO;
import com.sonatype.insight.license.dto.model.LicenseMetadataDTO;
import com.sonatype.insight.license.dto.model.LicenseObligationDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiLicenseLegalServiceTest
    extends AbstractComponentTest
{
  private static final String[] EXPECTED_LICENSE_IDS = new String[]{
      "Apache-2.0",
      "No-Source-License",
      "BSD-3-Clause",
      "BSD-2-Clause",
      "CC0-1.0",
      "PUBLIC-DOMAIN",
      "CC-BY-2.5",
      "MIT"
  };

  private static final String[] EXPECTED_LICENSE_IDS_FOR_MULTILICENSE = new String[]{
      "CDDL-1.0",
      "GPL-2.0",
      "No-Source-License",
      "Apache-2.0",
      "BSD-UNSPECIFIED",
      "EPL-1.0",
      "EPL-2.0",
      "GPL-3.0",
      "LGPL-2.1",
      "LGPL-3.0"
  };

  @Inject
  private ApiLicenseLegalService apiLicenseLegalService;

  private ApiLicenseLegalService apiLicenseLegalServiceSpy;

  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Mock
  private ApiLicenseLegalHdsService mockApiLicenseLegalHdsService;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Captor
  private ArgumentCaptor<Collection<String>> licenseIdArgumentCaptor;

  @Captor
  private ArgumentCaptor<Collection<ComponentIdentifier>> componentIdentifiersArgumentCaptor;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  private ApiLicenseDataAdapter apiLicenseDataAdapterSpy;

  private ComponentInfoService componentInfoServiceSpy;

  @Captor
  private ArgumentCaptor<Component> componentArgumentCaptor;

  @Mock
  private ThirdPartyComponentDAO mockThirdPartyComponentDAO;

  @Inject
  private LegalReportBuilder legalReportBuilder;

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  public void configure(Binder binder) {
    binder.bind(ApiLicenseLegalHdsService.class).toInstance(mockApiLicenseLegalHdsService);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    apiLicenseDataAdapterSpy = spy(new DefaultApiLicenseDataAdapter(new MultiLicenseDAO()));
    binder.bind(ApiLicenseDataAdapter.class).toInstance(apiLicenseDataAdapterSpy);
    componentInfoServiceSpy = spy(new ComponentInfoService(null, null, null, mockThirdPartyComponentDAO));
    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceSpy);
    binder.bind(ThirdPartyComponentDAO.class).toInstance(mockThirdPartyComponentDAO);
    super.configure(binder);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_WithoutApplications() {
    assertThat(apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_WithoutEvaluations() {
    tempEntity.newApplicationWithParent();
    assertThat(apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    List<ApiLicenseLegalApplicationDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null);

    assertThat(result).isNotEmpty();
    ApiLicenseLegalApplicationDashboardDTO dto = result.get(0);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, dto);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_MultipleApplications() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app1 = triple.getLeft();
    Tag tag1 = triple.getMiddle();
    PolicyEvaluation policyEvaluation1 = triple.getRight();

    triple = setupApplicationDashboardEntities("Test-Tag-2", BuildStageType.ID);
    Application app2 = triple.getLeft();
    Tag tag2 = triple.getMiddle();
    PolicyEvaluation policyEvaluation2 = triple.getRight();

    List<ApiLicenseLegalApplicationDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null);

    assertThat(result).isNotEmpty();
    ApiLicenseLegalApplicationDashboardDTO dto = result.get(0);
    if (dto.applicationPublicId.equals(app1.getPublicId())) {
      assertLegalLicenseApplicationDashboardDTO(app1, tag1, policyEvaluation1, dto);
      assertLegalLicenseApplicationDashboardDTO(app2, tag2, policyEvaluation2, result.get(1));
    }
    else {
      assertLegalLicenseApplicationDashboardDTO(app2, tag2, policyEvaluation2, dto);
      assertLegalLicenseApplicationDashboardDTO(app1, tag1, policyEvaluation1, result.get(1));
    }
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByOrganization() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    setupApplicationDashboardEntities("Test-Tag-2", BuildStageType.ID);

    List<ApiLicenseLegalApplicationDashboardDTO> result = apiLicenseLegalService
        .getLicenseLegalApplicationsDashboard(Sets.newHashSet(app.getOrganizationId()), null, null, null);

    assertThat(result).hasSize(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, result.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByApplication() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    setupApplicationDashboardEntities("Test-Tag-2", BuildStageType.ID);

    List<ApiLicenseLegalApplicationDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, Sets.newHashSet(app.getId()), null, null);

    assertThat(result).hasSize(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, result.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByTag() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    setupApplicationDashboardEntities("Test-Tag-2", BuildStageType.ID);

    List<ApiLicenseLegalApplicationDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, Sets.newHashSet(tag.getId()), null);

    assertThat(result).hasSize(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, result.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByStageTypeId() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", ReleaseStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid(), new Date());

    List<ApiLicenseLegalApplicationDashboardDTO> result = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(
        null, null, Sets.newHashSet(tag.getId()), Sets.newHashSet(ReleaseStageType.ID));

    assertThat(result).hasSize(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, result.get(0));
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null));
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithoutApplications() {
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithoutEvaluations() {
    tempEntity.newApplicationWithParent();
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithoutLicenses() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "someHash", componentIdentifier);
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithUnknownComponent() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "someHash", null);
    assertThat(apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null)).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");

    List<ApiLicenseLegalComponentDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null);

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_MultipleApplications() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");

    List<ApiLicenseLegalComponentDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, null, null, null);

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 2);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByOrganization() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT").getLeft();
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "otherHash", componentIdentifier, "MIT");

    List<ApiLicenseLegalComponentDashboardDTO> result = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(Sets.newHashSet(app.getOrganizationId()), null, null, null, null);

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByApplication() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT").getLeft();
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "otherHash", componentIdentifier, "MIT");

    List<ApiLicenseLegalComponentDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, Sets.newHashSet(app.getId()), null, null, null);

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByTag() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Tag tag =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT").getRight();
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "otherHash", componentIdentifier, "MIT");

    List<ApiLicenseLegalComponentDashboardDTO> result =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(null, null, Sets.newHashSet(tag.getId()), null, null);

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByStageTypeId() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");
    setupComponentDashboardEntities("Tag1", ReleaseStageType.ID, "otherHash", componentIdentifier, "Apache-1.0");

    List<ApiLicenseLegalComponentDashboardDTO> result = apiLicenseLegalService.getLicenseLegalComponentsDashboard(null,
        null, null, Sets.newHashSet(BuildStageType.ID), null);

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByLicense() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT", "AGPL-2.0");
    setupComponentDashboardEntities("Tag1", ReleaseStageType.ID, "somHash2", componentIdentifier, "AGPL-3.0");

    List<ApiLicenseLegalComponentDashboardDTO> result = apiLicenseLegalService.getLicenseLegalComponentsDashboard(null,
        null, null, null, Sets.newHashSet("MIT"));

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT", "AGPL-2.0"), 1);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByLicenseWithOverride() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app = setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier,
        "license-test-1", "license-test-2").getLeft();

    tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    List<ApiLicenseLegalComponentDashboardDTO> result = apiLicenseLegalService.getLicenseLegalComponentsDashboard(null,
        null, null, null, Sets.newHashSet("Apache-2.0"));

    assertThat(result).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = result.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("Apache-2.0"), 1);
  }

  @Test
  public void testGetLastRawApplicationReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid(), new Date(1));
    tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID, tempEntity.uuid(), new Date(2));
    PolicyEvaluation policyEvaluation3 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(3));
    mockReport(policyEvaluation3);
    tempEntity.newPolicyEvaluation(otherApp.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(4));

    Optional<ApiReportRawDataDTOV2> lastRawReportForApplication =
        apiLicenseLegalService.getLastRawApplicationReport(app.getPublicId());

    assertThat(lastRawReportForApplication).isPresent().get().usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation3.getScanId()));
  }

  @Test
  public void testGetLastRawApplicationReport_NoApplication() {
    assertThat(apiLicenseLegalService.getLastRawApplicationReport("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetLastRawApplicationReport_NoEvaluations() {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(apiLicenseLegalService.getLastRawApplicationReport(app.getPublicId())).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation.getScanId());
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata.json", EXPECTED_LICENSE_IDS);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_WithSingleLicensesInMultiLicenseIds() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("lls-raw-report-multilicenses.json", ApiReportRawDataDTOV2.class);
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    doReturn(Optional.of(rawReport)).when(apiLicenseLegalServiceSpy).getLastRawApplicationReport(anyString());
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata-multilicense.json",
        EXPECTED_LICENSE_IDS_FOR_MULTILICENSE);
  }

  private void testGetLicenseLegalApplicationReport(
      Application app,
      ApiReportRawDataDTOV2 rawReport,
      String licenseMetadataResource,
      String[] expectedLicenseFiles)
      throws Exception
  {
    ComponentIdentifier[] expectedComponentIdentifiers = rawReport.components.stream()
        .map(component -> component.componentIdentifier.toComponentIdentifier()).toArray(ComponentIdentifier[]::new);
    LicenseMetadataDTO[] licenseMetadata = getContent(licenseMetadataResource, LicenseMetadataDTO[].class);
    when(mockApiLicenseLegalHdsService.getLicenseMetadata(licenseIdArgumentCaptor.capture()))
        .thenReturn(Arrays.asList(licenseMetadata));
    ComponentLegalCommentDTO[] componentLegalComments =
        getContent("lls-legal-comments.json", ComponentLegalCommentDTO[].class);
    when(mockApiLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new LinkedHashSet<>(Arrays.asList(componentLegalComments)));
    ComponentLegalFileDTO[] componentLegalFiles =
        getContent("lls-legal-files.json", ComponentLegalFileDTO[].class);
    when(mockApiLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new LinkedHashSet<>(Arrays.asList(componentLegalFiles)));

    ApiLicenseLegalApplicationReportDTO licenseMetadataReport =
        apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app.getPublicId());

    assertThat(licenseMetadataReport).isNotNull();
    assertlicenseLegalMetadata(licenseMetadataReport.components, licenseMetadataReport.licenseLegalMetadata, rawReport,
        expectedLicenseFiles);
    assertObligationsArePresent(licenseMetadataReport.licenseLegalMetadata, Arrays.asList(licenseMetadata));
    assertComponentLegalComments(licenseMetadataReport.components,
        new LinkedHashSet<>(Arrays.asList(componentLegalComments)));
    assertComponentLegalFiles(licenseMetadataReport.components,
        new LinkedHashSet<>(Arrays.asList(componentLegalFiles)));
    assertComponentData(licenseMetadataReport.components, rawReport);
    assertLicenseThreatGroup(licenseMetadataReport.components);
    List<Collection<ComponentIdentifier>> queriedComponents = componentIdentifiersArgumentCaptor.getAllValues();
    assertThat(queriedComponents).hasSize(2);
    queriedComponents.forEach(
        componentIdentifiers -> assertThat(componentIdentifiers).containsExactly(expectedComponentIdentifiers));

    assertApplicationTelemetry(app, rawReport);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoComponentsWithLicenses() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("raw-report-no-licenses.json", ApiReportRawDataDTOV2.class);
    ApiLicenseLegalService apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    when(apiLicenseLegalServiceSpy.getLastRawApplicationReport(app.getPublicId())).thenReturn(Optional.of(rawReport));
    when(mockApiLicenseLegalHdsService.getComponentLegalComments(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());
    when(mockApiLicenseLegalHdsService.getComponentLegalFiles(componentIdentifiersArgumentCaptor.capture()))
        .thenReturn(new HashSet<>());

    ApiLicenseLegalApplicationReportDTO licenseMetadataReport =
        apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app.getPublicId());

    verify(mockApiLicenseLegalHdsService, never()).getLicenseMetadata(any());
    assertThat(licenseMetadataReport.components).hasSize(3);
    assertThat(licenseMetadataReport.licenseLegalMetadata).isEmpty();

    assertApplicationTelemetry(app, rawReport);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoReport() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(tempEntity.newApplicationWithParent().getId()));
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetLicenseLegalApplicationReport_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport("someAppId"));
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    testGetLicenseLegalComponentReport(tempEntity.newApplicationWithParent(), createNamedComponentDetails(),
        componentIdentifier, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_PackageUrl() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    testGetLicenseLegalComponentReport(tempEntity.newApplicationWithParent(), createNamedComponentDetails(), null,
        packageUrl, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, hash, componentIdentifier);
    testGetLicenseLegalComponentReport(application, createNamedComponentDetails(), null, null, hash);
  }

  @Test
  public void testGetLicenseLegalComponentReport_HashComponentIdentifier() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    tempEntity.newClaimedComponent(hash, componentIdentifier);
    testGetLicenseLegalComponentReport(application, createNamedComponentDetails(), null, null, hash);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationLicenseOverride() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(owner.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_OrganizationLicenseOverride() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity
        .newLicenseOverride(owner.getParentOwnerId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationLicenseOverride() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_EmptyLicenses() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails =
        createNamedComponentDetails(Collections.emptyList(), Collections.emptyList());
    testGetLicenseLegalComponentReport(owner, namedComponentDetails, componentIdentifier, null, null);
    assertThat(namedComponentDetails.getDeclaredLicenseIds())
        .containsExactly(com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID);
    assertThat(namedComponentDetails.getObservedLicenseIds())
        .containsExactly(com.sonatype.insight.brain.model.license.License.UNSPECIFIED_ID);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ThirdParty() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null,
        IdentificationSource.CLAIR.getId(), "scanId");
  }

  private void testGetLicenseLegalComponentReport(
      Owner owner,
      NamedComponentDetails namedComponentDetails,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash) throws Exception
  {
    testGetLicenseLegalComponentReport(owner, namedComponentDetails, componentIdentifier, packageUrl, hash, null, null);
  }

  private void testGetLicenseLegalComponentReport(
      Owner owner,
      NamedComponentDetails namedComponentDetails,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      String identificationSource,
      String scanId) throws Exception
  {
    lenient().doAnswer(invocationOnMock -> {
      namedComponentDetails.setComponentIdentifier(invocationOnMock.getArgument(2, ComponentIdentifier.class));
      return namedComponentDetails;
    }).when(componentInfoServiceSpy).getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    lenient().doAnswer(invocationOnMock -> {
      namedComponentDetails.setComponentIdentifier(invocationOnMock.getArgument(0, ComponentIdentifier.class));
      return namedComponentDetails;
    }).when(mockThirdPartyComponentDAO).getComponentDetailsByIdentifier(any(), any(), any());
    List<LicenseMetadataDTO> expectedLicenseMetadataDTOs = new ArrayList<>();
    doAnswer(invocationOnMock -> {
      List<LicenseMetadataDTO> licenseMetadataDTOS = createLicenseMetadataDTOs(invocationOnMock.getArgument(0));
      expectedLicenseMetadataDTOs.addAll(licenseMetadataDTOS);
      return licenseMetadataDTOS;
    }).when(mockApiLicenseLegalHdsService).getLicenseMetadata(any());
    doAnswer(invocationOnMock -> {
      Collection<?> argument = invocationOnMock.getArgument(0, Collection.class);
      assertThat(argument).hasSize(1);
      ComponentIdentifier c = (ComponentIdentifier) argument.iterator().next();
      return new LinkedHashSet<>(Arrays.asList(createComponentLegalCommentDTO(c), createComponentLegalCommentDTO(c)));
    }).when(mockApiLicenseLegalHdsService).getComponentLegalComments(any());
    doAnswer(invocationOnMock -> {
      Collection<?> argument = invocationOnMock.getArgument(0, Collection.class);
      assertThat(argument).hasSize(1);
      ComponentIdentifier c = (ComponentIdentifier) argument.iterator().next();
      return new LinkedHashSet<>(Arrays.asList(createComponentLegalFileDTO(c), createComponentLegalFileDTO(c)));
    }).when(mockApiLicenseLegalHdsService).getComponentLegalFiles(any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getId(), componentIdentifier,
            packageUrl, hash, null, identificationSource, scanId);

    verify(apiLicenseDataAdapterSpy).convertToDTOV2(componentArgumentCaptor.capture());
    Component component = componentArgumentCaptor.getValue();
    componentIdentifier = component.getComponentIdentifier();
    assertThat(licenseLegalComponentReport).isNotNull();
    ApiLicenseLegalComponentDTO licenseLegalComponent = licenseLegalComponentReport.component;
    assertThat(licenseLegalComponent).isNotNull();
    assertThat(licenseLegalComponent.componentIdentifier).isNotNull();
    assertThat(licenseLegalComponent.componentIdentifier.toComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(licenseLegalComponent.hash).isEqualTo(namedComponentDetails.getHash());
    assertThat(licenseLegalComponent.packageUrl).isNotNull()
        .isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl());
    assertThat(licenseLegalComponent.displayName).isNotNull().isEqualTo(
        ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier()).toString());
    assertThat(licenseLegalComponent.licenseLegalData).isNotNull();
    assertThat(licenseLegalComponent.licenseLegalData.declaredLicenses)
        .containsExactly(namedComponentDetails.getDeclaredLicenseIds().toArray(new String[0]));
    assertThat(licenseLegalComponent.licenseLegalData.observedLicenses)
        .containsExactly(namedComponentDetails.getObservedLicenseIds().toArray(new String[0]));
    Set<String> expectedLicenseIds = getExpectedLicenseIds(namedComponentDetails);
    assertThat(licenseLegalComponent.licenseLegalData.effectiveLicenses)
        .containsExactly(expectedLicenseIds.toArray(new String[0]));
    assertThat(licenseLegalComponent.licenseLegalData.effectiveLicenseThreats)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(apiLicenseDataAdapterSpy.convertToDTOV2(component).effectiveLicenseThreats
            .toArray(new ApiLicenseThreatDTOV2[0]));
    assertThat(licenseLegalComponent.licenseLegalData.copyrights).hasSize(8)
        .allMatch(copyright -> copyright.endsWith("content"));
    assertThat(licenseLegalComponent.licenseLegalData.licenseFiles).hasSize(4)
        .allMatch(licenseFile -> licenseFile.content.endsWith("contentLicense"));
    assertThat(licenseLegalComponent.licenseLegalData.noticeFiles).hasSize(4)
        .allMatch(noticeFile -> noticeFile.content.endsWith("contentNotice"));
    Set<com.sonatype.insight.brain.model.license.License> licenses =
        licenseLegalComponent.licenseLegalData.effectiveLicenses.stream()
            .flatMap(multiLicenseId -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicenseId).stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    ApiLicenseLegalMetadataDTO[] expectedLicenseLegalMetadata = legalReportBuilder.getLicenseLegalMetadata(
        licenseLegalComponent.licenseLegalData.effectiveLicenses.stream()
            .map(multiLicenseDAO::getByIdNotNull)
            .map(license -> new ApiLicenseDTO(license.getId(), license.getShortDisplayName()))
        .collect(Collectors.toList()), 
        licenses,
        expectedLicenseMetadataDTOs.stream()
            .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity())))
        .toArray(new ApiLicenseLegalMetadataDTO[0]);
    assertThat(licenseLegalComponentReport.licenseLegalMetadata).isNotNull()
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(expectedLicenseLegalMetadata);
    if (identificationSource != null && scanId != null) {
      verify(mockThirdPartyComponentDAO).getComponentDetailsByIdentifier(componentIdentifier, owner.getId(), scanId);
      verify(componentInfoServiceSpy, never()).getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    }
    else {
      verify(mockThirdPartyComponentDAO, never()).getComponentDetailsByIdentifier(any(), any(), any());
      verify(componentInfoServiceSpy).getComponentDetailsFromHDS(any(), any(), eq(componentIdentifier), any(), any());
    }
  }

  private Set<String> getExpectedLicenseIds(NamedComponentDetails namedComponentDetails) {
    return ComponentDetailsLoader.calculateEffectiveLicenses(
        namedComponentDetails.getDeclaredLicenseIds(),
        namedComponentDetails.getObservedLicenseIds(),
        namedComponentDetails.getOverriddenLicenses().stream()
            .map(License::getLicenseId)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
  }

  @Test
  public void testGetLicenseLegalComponentReport_OwnerDoesNotExist() {
    String ownerId = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION, ownerId, null,
            null, null, null, null, null))
        .withMessageContaining("Could not find an application with ID " + ownerId + ".");
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION, ownerId, null,
            null, null, null, null, null))
        .withMessageContaining("Cannot find organization with ID " + ownerId + ".");
  }

  @Test
  public void testGetLicenseLegalComponentReport_NoComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getId(), null, null, "hash", null, null, null))
        .withMessageContaining("Unable to determine componentIdentifier.");
  }

  @Test
  public void testInitialize_ComponentInfoServiceToolNameSet() {
    verify(componentInfoServiceSpy).setToolName("ci");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifierAndPackageUrl() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getId(), componentIdentifier, packageUrl, null, null, null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifierAndHash() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getId(), componentIdentifier, "hash", null, null, null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_PackageUrlAndHash() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getId(), null, packageUrl, "hash", null, null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifierAndPackageUrlAndHash() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getId(), componentIdentifier, packageUrl, "hash", null, null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> apiLicenseLegalService
        .getLicenseLegalComponentReport(OwnerType.APPLICATION, "someAppId", null, null, "hash", null, null, null));
  }

  private NamedComponentDetails createNamedComponentDetails() {
    return createNamedComponentDetails(Arrays.asList("Apache-2.0+", "Apache-2.0-MIT"),
        Arrays.asList("GPL-3.0-LGPL-2.0", "Beerware"));
  }

  private NamedComponentDetails createNamedComponentDetails(
      List<String> declaredLicenses,
      List<String> observedLicenses)
  {
    NamedComponentDetails namedComponentDetails = new NamedComponentDetails();
    namedComponentDetails.setHash("hash");
    namedComponentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    namedComponentDetails.setDeclaredLicenses(declaredLicenses.stream()
        .map(licenseId -> new License(licenseId, null))
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    namedComponentDetails.setObservedLicenses(observedLicenses.stream()
        .map(licenseId -> new License(licenseId, null))
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    return namedComponentDetails;
  }

  private ComponentLegalCommentDTO createComponentLegalCommentDTO(ComponentIdentifier componentIdentifier) {
    ComponentLegalCommentDTO componentLegalCommentDTO = new ComponentLegalCommentDTO();
    componentLegalCommentDTO.setComponentIdentifier(componentIdentifier);
    componentLegalCommentDTO.setHash("hash");
    componentLegalCommentDTO
        .setComments(new LinkedHashSet<>(Arrays.asList(createLegalCommentDTO(), createLegalCommentDTO())));
    return componentLegalCommentDTO;
  }

  private LegalCommentDTO createLegalCommentDTO() {
    LegalCommentDTO legalCommentDTO = new LegalCommentDTO();
    legalCommentDTO.setContent("content");
    legalCommentDTO
        .setCopyrights(new LinkedHashSet<>(Arrays.asList(createLegalCopyrightDTO(), createLegalCopyrightDTO())));
    return legalCommentDTO;
  }

  private LegalCopyrightDTO createLegalCopyrightDTO() {
    LegalCopyrightDTO legalCopyrightDTO = new LegalCopyrightDTO();
    legalCopyrightDTO.setAuthor("author");
    legalCopyrightDTO.setYear("year");
    legalCopyrightDTO.setContent(tempEntity.uuid() + " content");
    return legalCopyrightDTO;
  }

  private ComponentLegalFileDTO createComponentLegalFileDTO(ComponentIdentifier componentIdentifier) {
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(componentIdentifier);
    componentLegalFileDTO.setHash("hash");
    componentLegalFileDTO.setLegalFiles(new LinkedHashSet<>(Arrays
        .asList(createLicenseLegalFileDTO(), createLicenseLegalFileDTO(), createNoticeLegalFileDTO(),
            createNoticeLegalFileDTO())));
    return componentLegalFileDTO;
  }

  private LegalFileDTO createLicenseLegalFileDTO() {
    LegalFileDTO licenseLegalFileDTO = createLegalFileDTO("LICENSE");
    licenseLegalFileDTO.setContent(tempEntity.uuid() + " contentLicense");
    return licenseLegalFileDTO;
  }

  private LegalFileDTO createNoticeLegalFileDTO() {
    LegalFileDTO noticeLegalFileDTO = createLegalFileDTO("NOTICE");
    noticeLegalFileDTO.setContent(tempEntity.uuid() + " contentNotice");
    return noticeLegalFileDTO;
  }

  private LegalFileDTO createLegalFileDTO(String type) {
    LegalFileDTO legalFileDTO = new LegalFileDTO();
    legalFileDTO.setRelPath("relPath");
    legalFileDTO.setType(type);
    return legalFileDTO;
  }

  private void assertlicenseLegalMetadata(
      List<ApiLicenseLegalComponentDTO> components,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      ApiReportRawDataDTOV2 rawReport,
      String[] expectedLicenseIds)
  {
    assertThat(components).hasSize(rawReport.components.size());
    List<Collection<String>> licenseIds = licenseIdArgumentCaptor.getAllValues();
    assertThat(licenseIds).hasSize(1);
    assertThat(licenseIds.get(0)).containsExactly(expectedLicenseIds);
    Set<String> expectedLicenseLegalMetadataLicenseIds = new LinkedHashSet<>(Arrays.asList(expectedLicenseIds));
    expectedLicenseLegalMetadataLicenseIds.addAll(rawReport.components.stream()
        .flatMap(component -> Stream.concat(Stream.concat(component.licenseData.declaredLicenses.stream(),
                    component.licenseData.observedLicenses.stream()),
            component.licenseData.effectiveLicenses.stream()))
        .map(license -> license.licenseId)
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    assertThat(licenseLegalMetadata).hasSize(expectedLicenseLegalMetadataLicenseIds.size());
    assertThat(licenseLegalMetadata).extracting(license -> license.licenseId)
        .containsExactly(expectedLicenseLegalMetadataLicenseIds.toArray(new String[0]));
    assertThat(components.stream()
        .flatMap(c -> c.licenseLegalData.copyrights.stream())
        .collect(Collectors.toSet())).hasSize(3);
    assertThat(components.stream()
        .flatMap(c -> c.licenseLegalData.licenseFiles.stream())
        .collect(Collectors.toSet())).hasSize(2);
    assertThat(components.stream()
        .flatMap(c -> c.licenseLegalData.noticeFiles.stream())
        .collect(Collectors.toSet())).hasSize(1);
  }

  private List<LicenseMetadataDTO> createLicenseMetadataDTOs(Collection<String> licenseIds) {
    return licenseIds.stream().map(this::createLicenseMetadataDTO).collect(Collectors.toList());
  }

  private LicenseMetadataDTO createLicenseMetadataDTO(String licenseId) {
    LicenseMetadataDTO licenseMetadataDTO = new LicenseMetadataDTO();
    licenseMetadataDTO.setLicenseId(licenseId);
    licenseMetadataDTO.setLicenseText("licenseText");
    licenseMetadataDTO.setLicenseObligations(
        new LinkedHashSet<>(Arrays.asList(createLicenseObligationDTO(), createLicenseObligationDTO())));
    return licenseMetadataDTO;
  }

  private LicenseObligationDTO createLicenseObligationDTO() {
    LicenseObligationDTO licenseObligationDTO = new LicenseObligationDTO();
    licenseObligationDTO.setName("name");
    licenseObligationDTO.setObligationTexts(new LinkedHashSet<>(Arrays.asList("obligationText1", "obligationText2")));
    return licenseObligationDTO;
  }

  private void assertObligationsArePresent(
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      List<LicenseMetadataDTO> licenseMetadata)
  {
    licenseMetadata.forEach(lm -> {
      Set<ApiLicenseLegalObligationDTO> legalLicenseObligations =
          getLicenseObligationByLicenseId(licenseLegalMetadata, lm.getLicenseId());
      lm.getLicenseObligations().forEach(lo -> {
        Optional<ApiLicenseLegalObligationDTO> legalLicenseObligation = legalLicenseObligations.stream()
            .filter(llo -> llo.licenseObligation.getName().equals(lo.getName()))
            .findFirst();
        assertThat(legalLicenseObligation.isPresent())
            .withFailMessage("Legal Report Data did not contain License Obligation: " + lo.getName()).isTrue();
        assertThat(lo.getObligationTexts())
            .isEqualTo(legalLicenseObligation.get().licenseObligation.getObligationTexts());
      });
    });
  }

  private Set<ApiLicenseLegalObligationDTO> getLicenseObligationByLicenseId(
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      String licenseId)
  {
    List<ApiLicenseLegalMetadataDTO> filterdLicenseLegalMetadataList = licenseLegalMetadata.stream()
        .filter(lm -> lm.licenseId.equals(licenseId))
        .collect(Collectors.toList());
    assertThat(filterdLicenseLegalMetadataList).withFailMessage(
        "Should only contain one element for each license. Found multiple for " + licenseId).hasSize(1);
    return filterdLicenseLegalMetadataList.get(0).obligations;
  }

  private void assertComponentLegalComments(
      List<ApiLicenseLegalComponentDTO> licenseLegalComponents,
      Set<ComponentLegalCommentDTO> componentLegalComments)
  {
    licenseLegalComponents.forEach(lrc -> assertThat(lrc.licenseLegalData.copyrights).containsExactly(
        componentLegalComments.stream()
            .filter(clc -> LegalReportBuilder.removeClassifierAndExtension(clc.getComponentIdentifier())
                .equals(
                    LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier.toComponentIdentifier())))
            .flatMap(clc -> clc.getUniqueCopyrights().stream())
            .map(LegalCopyrightDTO::getContent)
            .sorted()
            .toArray(String[]::new)));
  }

  private void assertComponentLegalFiles(
      List<ApiLicenseLegalComponentDTO> licenseLegalComponents,
      Set<ComponentLegalFileDTO> componentLegalFiles)
  {
    licenseLegalComponents.forEach(lrc -> {
      assertThat(lrc.licenseLegalData.noticeFiles).usingRecursiveFieldByFieldElementComparator().containsExactly(
          componentLegalFiles.stream()
              .filter(clf -> LegalReportBuilder.removeClassifierAndExtension(clf.getComponentIdentifier())
                  .equals(
                      LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier.toComponentIdentifier())))
              .flatMap(clf -> clf.getLegalFiles().stream())
              .filter(c -> c.getType().equals("NOTICE"))
              .map(legalFileDTO -> new ApiLicenseLegalFileDTO(legalFileDTO.getRelPath(), legalFileDTO.getContent()))
              .toArray(ApiLicenseLegalFileDTO[]::new));
      assertThat(lrc.licenseLegalData.licenseFiles).usingRecursiveFieldByFieldElementComparator().containsExactly(
          componentLegalFiles.stream()
              .filter(clf -> LegalReportBuilder.removeClassifierAndExtension(clf.getComponentIdentifier())
                  .equals(
                      LegalReportBuilder.removeClassifierAndExtension(lrc.componentIdentifier.toComponentIdentifier())))
              .flatMap(clf -> clf.getLegalFiles().stream())
              .filter(c -> c.getType().equals("LICENSE"))
              .map(legalFileDTO -> new ApiLicenseLegalFileDTO(legalFileDTO.getRelPath(), legalFileDTO.getContent()))
              .toArray(ApiLicenseLegalFileDTO[]::new));
    });
  }

  private void assertComponentData(List<ApiLicenseLegalComponentDTO> components, ApiReportRawDataDTOV2 rawReport) {
    components.forEach(this::assertValidComponent);

    Map<String, ApiLicenseDataDTOV2> expectedComponentData = rawReport.components.stream()
        .filter(comp -> comp.displayName != null)
        .collect(Collectors.toMap(c -> c.displayName,
            c -> c.licenseData == null ? new ApiLicenseDataDTOV2() : c.licenseData));

    components.forEach(comp -> {
      validateLicenseData(comp, comp.licenseLegalData, expectedComponentData.get(comp.displayName));
    });
  }

  private void assertValidComponent(ApiLicenseLegalComponentDTO component) {
    assertThat(component).satisfiesAnyOf(
        lrc -> assertThat(lrc.componentIdentifier).isNotNull(),
        lrc -> assertThat(lrc.packageUrl).isNotNull(),
        lrc -> assertThat(lrc.hash).isNotNull()
    );
  }

  private void validateLicenseData(
      ApiLicenseLegalComponentDTO component,
      ApiLicenseLegalDataDTO actual,
      ApiLicenseDataDTOV2 expected)
  {
    if (actual == null) {
      assertThat(expected).usingRecursiveComparison().isEqualTo(new ApiLicenseDataDTOV2());
    }
    else {
      assertThat(expected).usingRecursiveComparison().isNotEqualTo(new ApiLicenseDataDTOV2());
    }
    if (actual == null) {
      return;
    }
    Set<String> expectedLicenses = Stream
        .concat(Stream.concat(expected.declaredLicenses.stream(), expected.observedLicenses.stream()),
            expected.effectiveLicenses.stream())
        .map(license -> license.licenseId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Stream<String> licenses =
        Stream.concat(Stream.concat(actual.declaredLicenses.stream(), actual.observedLicenses.stream()),
            actual.effectiveLicenses.stream());
    licenses.forEach(actualLicense -> {
      assertThat(actualLicense).withFailMessage("Component " + component.displayName).isInstanceOf(String.class);
      assertThat(expectedLicenses)
          .withFailMessage("Component " + component.displayName + " does not contain actual license: " + actualLicense)
          .contains(actualLicense);
    });
  }

  private void assertLicenseThreatGroup(List<ApiLicenseLegalComponentDTO> components) {
    ApiLicenseThreatDTOV2 expectedLicenseThreatGroup = new ApiLicenseThreatDTOV2();
    expectedLicenseThreatGroup.licenseThreatGroupName = "Sonatype Special Licenses";
    expectedLicenseThreatGroup.licenseThreatGroupLevel = 5;
    expectedLicenseThreatGroup.licenseThreatGroupCategory = "severe";

    Set<ApiLicenseThreatDTOV2> licenseThreatGroups = components.stream()
        .filter(component -> component.displayName
            .equals("com.fasterxml.jackson.datatype : jackson-datatype-jdk8 : 2.10.3"))
        .flatMap(component -> component.licenseLegalData.effectiveLicenseThreats.stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));

    assertThat(licenseThreatGroups).usingRecursiveFieldByFieldElementComparator().contains(expectedLicenseThreatGroup);
  }

  private <T> T getContent(String resource, Class<? extends T> type) throws Exception {
    return JsonUtils.parse(IOUtils.toString(getClass().getResource("/" + getClass().getSimpleName() + "/" + resource),
        StandardCharsets.UTF_8), type);
  }

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = insightWork.getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
        zos.putNextEntry(new ZipEntry("index.html"));
      }
      String[] filenames = {
          Report.BOM_JSON_FILENAME, Report.SECURITY_JSON_FILENAME, Report.LICENSES_JSON_FILENAME,
          Report.DATA_JSON_FILENAME, Report.DEPENDENCIES_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(getClass().getResource("/" + getClass().getSimpleName() + "/report/" + filename), file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void assertApplicationTelemetry(Application application, ApiReportRawDataDTOV2 rawReport) {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put(ApplicationLicenseUsageTelemetry.ATTRIBUTE_NAME, new ApplicationLicenseUsageTelemetry(
        application.getPublicId(),
        rawReport.components.stream()
            .map(component -> component.hash)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        rawReport.components.stream().filter(component -> component.licenseData != null)
            .map(component -> component.licenseData)
            .flatMap(licenseData -> Stream.concat(
                Stream.concat(licenseData.declaredLicenses.stream(), licenseData.observedLicenses.stream()),
                licenseData.effectiveLicenses.stream()))
            .map(license -> license.licenseId)
            .collect(Collectors.toCollection(LinkedHashSet::new))));

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.APPLICATION_LICENSE_USAGE);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).hasSize(1);
    assertThat(telemetryData.getAttributes().keySet().iterator().next())
        .isEqualTo(expectedAttributes.keySet().iterator().next());
    assertThat((ApplicationLicenseUsageTelemetry) telemetryData.getAttributes().values().iterator().next())
        .usingRecursiveComparison().isEqualTo(expectedAttributes.values().iterator().next());
  }

  private void setUnlicensedForAdvancedLegalPack() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  private Triple<Application, Tag, PolicyEvaluation> setupApplicationDashboardEntities(
      String tagName,
      String stageTypeId)
  {
    Application app = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(app.getOrganizationId(), tagName);
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, tempEntity.uuid(), new Date());
    mockReport(policyEvaluation);

    return new ImmutableTriple<>(app, tag, policyEvaluation);
  }

  private void assertLegalLicenseApplicationDashboardDTO(
      Application app,
      Tag tag,
      PolicyEvaluation latestPolicyEvaluation,
      ApiLicenseLegalApplicationDashboardDTO dto)
  {
    assertThat(dto.applicationId).isEqualTo(app.getId());
    assertThat(dto.applicationName).isEqualTo(app.getName());
    assertThat(dto.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(dto.applicationTagNames).containsExactly(tag.getName());
    assertThat(dto.lastScanTime).isEqualTo(latestPolicyEvaluation.getTime().getTime());
    assertThat(dto.reviewCompletedCount).isZero();
    assertThat(dto.reviewTotalCount).isZero();
  }

  private Pair<Application, Tag> setupComponentDashboardEntities(
      String tagName,
      String stageTypeId,
      String hash,
      ComponentIdentifier componentIdentifier,
      String... effectiveLicenseIds)
  {
    Application app = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(app.getOrganizationId(), tagName);
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(app.getId(), stageTypeId, hash, componentIdentifier);

    for (String effectiveLicenseId : effectiveLicenseIds) {
      tempEntity.newApplicationComponentLicense(applicationComponent.getId(), effectiveLicenseId);
    }

    return Pair.of(app, tag);
  }

  private void assertLegalLicenseComponentDashboardDTO(
      ApiLicenseLegalComponentDashboardDTO dto,
      String hash,
      ComponentIdentifier componentIdentifier,
      Set<String> licenseNames,
      int applicationOccurrences)
  {
    assertThat(dto.hash).isEqualTo(hash);
    assertThat(dto.displayName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(dto.licenseNames).containsAll(licenseNames);
    assertThat(dto.applicationOccurrences).isEqualTo(applicationOccurrences);
    assertThat(dto.reviewCompletedCount).isZero();
    assertThat(dto.reviewTotalCount).isZero();
  }
}
