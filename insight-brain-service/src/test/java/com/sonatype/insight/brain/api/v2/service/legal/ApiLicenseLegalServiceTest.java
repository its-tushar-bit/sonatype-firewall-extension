/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.experimental.legal.ComponentLegalService;
import com.sonatype.insight.brain.api.experimental.legal.LegalComponentIdentifierUtil;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalDataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalMetadataDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalStageScanDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApplicationLicenseUsageTelemetry;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LegalSourceLinkDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalResultsOrder;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalReviewStatus;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiLicenseDataAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.brain.dataaccess.legal.SourceLinkOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.repository.RepositoryQueryService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.utils.ReportHelper;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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

  private static final ComponentIdentifier INNER_SOURCE_COMPONENT_IDENTIFIER =
      ComponentIdentifier.createMavenCoordinates("org.company.lib", "lib-web", "1.0-SNAPSHOT", "", "jar");

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
  private ComponentLegalService mockComponentLegalService;

  @Mock
  private SourceLinkOverrideDAO mockSourceLinkOverrideDAO;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Captor
  private ArgumentCaptor<Collection<String>> licenseIdArgumentCaptor;

  @Captor
  private ArgumentCaptor<Collection<ComponentIdentifier>> componentIdentifiersArgumentCaptor;

  @Captor
  private ArgumentCaptor<Set<ComponentIdentifier>> componentIdentifiersArgumentCaptorForSourceLinks;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  private ApiLicenseDataAdapter apiLicenseDataAdapterSpy;

  private ComponentInfoService componentInfoServiceSpy;

  @Captor
  private ArgumentCaptor<Component> componentArgumentCaptor;

  @Mock
  private ThirdPartyComponentDAO mockThirdPartyComponentDAO;

  @Mock
  private ApiComponentDetailsServiceV2 apiComponentDetailsServiceV2;

  @Inject
  private LegalReportBuilder legalReportBuilder;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private IdUtils idUtils;

  @Mock
  private RepositoryQueryService repositoryQueryService;

  @Mock
  private Configuration configurationMock;

  private LicenseDAO licenseDAO;

  private ApplicationDAO applicationDAO;

  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  private OwnerDAO ownerDAO;

  private PolicyDAO policyDAO;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Override
  public void configure(Binder binder) {
    binder.bind(ApiLicenseLegalHdsService.class).toInstance(mockApiLicenseLegalHdsService);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);

    // Init DAOs
    licenseDAO = daoFactory.createLicenseDAO();
    applicationDAO = daoFactory.createApplicationDAO();
    innerSourceApplicationDAO = daoFactory.createInnerSourceApplicationDAO();
    ownerDAO = daoFactory.createOwnerDAO();
    policyDAO = daoFactory.createPolicyDAO();

    // Init Spys
    apiLicenseDataAdapterSpy =
        spy(new ApiLicenseDataAdapter(daoFactory.createMultiLicenseDAO()));
    binder.bind(ApiLicenseDataAdapter.class).toInstance(apiLicenseDataAdapterSpy);
    lenient().when(configurationMock.isALPObservedLicenseDetectionEnabled()).thenReturn(true);
    componentInfoServiceSpy = spy(buildComponentInfoService());

    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceSpy);
    binder.bind(ThirdPartyComponentDAO.class).toInstance(mockThirdPartyComponentDAO);
    binder.bind(ComponentLegalService.class).toInstance(mockComponentLegalService);
    binder.bind(SourceLinkOverrideDAO.class).toInstance(mockSourceLinkOverrideDAO);
    super.configure(binder);
  }

  private ComponentInfoService buildComponentInfoService() {
    ComponentCategoryDAO componentCategoryDAO = daoFactory.createComponentCategoryDAO();
    LicenseThreatGroupDAO licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    LicenseOverrideDAO licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO =
        daoFactory.createSecurityVulnerabilityOverrideDAO();
    ComponentLabelDAO componentLabelDAO = daoFactory.createComponentLabelDAO();
    VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO =
        daoFactory.createVulnerabilityCustomRemediationDAO();
    VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO = daoFactory.createVulnerabilityCustomCweDAO();
    VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO =
        daoFactory.createVulnerabilityCustomCvssVectorDAO();
    VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO =
        daoFactory.createVulnerabilityCustomCvssSeverityDAO();
    MultiLicenseDAO multiLicenseDAO = daoFactory.createMultiLicenseDAO();
    VulnerabilityGroupDAO vulnerabilityGroupDAO = daoFactory.createVulnerabilityGroupDAO();
    VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO =
        daoFactory.createVulnerabilityGroupVulnerabilityDAO();

    ComponentLoaderFactory componentLoaderFactory =
        new ComponentLoaderFactory(multiLicenseDAO, licenseThreatGroupDAO, licenseThreatGroupLicenseDAO,
            licenseOverrideDAO, securityVulnerabilityOverrideDAO, ownerDAO, componentLabelDAO,
            vulnerabilityCustomRemediationDAO, vulnerabilityCustomCweDAO, vulnerabilityCustomCvssVectorDAO,
            vulnerabilityCustomCvssSeverityDAO, vulnerabilityGroupDAO, vulnerabilityGroupVulnerabilityDAO);

    ComponentInfoService componentInfoService =
        new ComponentInfoService(null, null,
            new ComponentDetailsLoaderFactory(null, configurationMock, licenseDAO, componentLoaderFactory),
            null,
            mockThirdPartyComponentDAO, repositoryQueryService, apiComponentDetailsServiceV2, multiLicenseDAO,
            applicationDAO, licenseDAO, componentCategoryDAO, licenseThreatGroupDAO, ownerDAO, policyDAO, null, idUtils,
            null, null, null, null, null, null);
    return componentInfoService;
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
            null, 0, 0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_WithoutApplications() {
    ApiLicenseLegalApplicationDashboardResultDTO dto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);
    assertThat(dto).isNotNull();
    assertThat(dto.results).isEmpty();
    assertThat(dto.totalResultsCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_WithoutEvaluations() {
    tempEntity.newApplicationWithParent();
    ApiLicenseLegalApplicationDashboardResultDTO dto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);
    assertThat(dto).isNotNull();
    assertThat(dto.results).isEmpty();
    assertThat(dto.totalResultsCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);

    assertThat(resultDto).isNotNull();
    assertThat(resultDto.results).isNotEmpty();
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    ApiLicenseLegalApplicationDashboardDTO dto = resultDto.results.get(0);
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

    setupApplicationDashboardEntities("Test-Tag-3", StageTypes.COMPLIANCE.getId());

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);

    assertThat(resultDto).isNotNull();
    assertThat(resultDto.results).isNotEmpty();
    assertThat(resultDto.totalResultsCount).isEqualTo(2);
    ApiLicenseLegalApplicationDashboardDTO dto = resultDto.results.get(0);
    if (dto.applicationPublicId.equals(app1.getPublicId())) {
      assertLegalLicenseApplicationDashboardDTO(app1, tag1, policyEvaluation1, dto);
      assertLegalLicenseApplicationDashboardDTO(app2, tag2, policyEvaluation2, resultDto.results.get(1));
    }
    else {
      assertLegalLicenseApplicationDashboardDTO(app2, tag2, policyEvaluation2, dto);
      assertLegalLicenseApplicationDashboardDTO(app1, tag1, policyEvaluation1, resultDto.results.get(1));
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

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(Sets.newHashSet(app.getOrganizationId()), null,
            null, null, null, null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByApplication() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    setupApplicationDashboardEntities("Test-Tag-2", BuildStageType.ID);

    ApiLicenseLegalApplicationDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalApplicationsDashboard(null, Sets.newHashSet(app.getId()), null, null, null, null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByTag() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", BuildStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    setupApplicationDashboardEntities("Test-Tag-2", BuildStageType.ID);

    ApiLicenseLegalApplicationDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalApplicationsDashboard(null, null, Sets.newHashSet(tag.getId()), null, null, null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByStageTypeId() {
    Triple<Application, Tag, PolicyEvaluation> triple =
        setupApplicationDashboardEntities("Test-Tag-1", ReleaseStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date());

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, Sets.newHashSet(tag.getId()),
            Sets.newHashSet(ReleaseStageType.ID), null, null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0));
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_IgnoresInnerSourceComponents() {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date());

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    ApplicationComponent innerSourceComponent1 = tempEntity.newApplicationComponent(app.getId(),
        BuildStageType.ID, "hash1", componentIdentifier1);
    ApplicationComponent innerSourceComponent2 = tempEntity.newApplicationComponent(app.getId(),
        BuildStageType.ID, "hash3", componentIdentifier3);

    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash2", componentIdentifier2);

    tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent1.getComponentIdentifier()).getPackageUrl(),
        app);
    tempEntity.newInnerSourceApplication(
        InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent2.getComponentIdentifier()).getPackageUrl(),
        otherApp);

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalApplicationDashboardDTO dto = resultDto.results.get(0);
    assertThat(dto.applicationId).isEqualTo(app.getId());
    assertThat(dto.applicationName).isEqualTo(app.getName());
    assertThat(dto.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(dto.lastScanTime).isEqualTo(policyEvaluation.getTime().getTime());
    assertThat(dto.stageTypeId).isEqualTo(policyEvaluation.getStageTypeId());
    assertThat(dto.stageTypeName).isEqualTo(StageTypes.getById(policyEvaluation.getStageTypeId()).getName());
    assertThat(dto.componentsReviewedCount).isEqualTo(0);
    assertThat(dto.componentsTotalCount).isEqualTo(1);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_WithComponentsReviewed() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    Triple<Application, Tag, PolicyEvaluation> appTagEval1 = setupApplicationWithLicenses(componentIdentifier1, "MIT");
    Triple<Application, Tag, PolicyEvaluation> appTagEval2 = setupApplicationWithLicenses(componentIdentifier2, "MIT");
    tempEntity.newLicenseOverride(appTagEval2.getLeft().getId(), componentIdentifier2, LicenseOverrideStatus.OVERRIDDEN,
        Sets.newHashSet("Apache-1.0", "Apache-2.0"));

    tempEntity.newComponentObligation(componentIdentifier1, appTagEval1.getLeft().getId(), "obligation1", "comment1",
        ObligationStatus.FULFILLED, "hash1");
    tempEntity.newComponentObligation(componentIdentifier1, appTagEval1.getLeft().getId(), "obligation2", "comment2",
        ObligationStatus.OPEN, "hash2");

    tempEntity.newComponentObligation(componentIdentifier2, appTagEval2.getLeft().getId(), "obligation3", "comment3",
        ObligationStatus.FULFILLED, "hash3");
    tempEntity.newComponentObligation(componentIdentifier2, appTagEval2.getLeft().getId(), "obligation4", "comment4",
        ObligationStatus.IGNORED, "hash4");
    tempEntity.newComponentObligation(componentIdentifier2, appTagEval2.getLeft().getId(), "obligation5", "comment5",
        ObligationStatus.FULFILLED, "hash5");

    List<String> licenses = Lists.newArrayList("MIT", "Apache-1.0", "Apache-2.0");

    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);
    licenseMetadataDTOs.get(0)
        .setLicenseObligations(new LinkedHashSet<>(Arrays
            .asList(new LicenseObligationDTO("obligation1", Collections.emptySet()),
                new LicenseObligationDTO("obligation2", Collections.emptySet()))));
    licenseMetadataDTOs.get(1)
        .setLicenseObligations(new LinkedHashSet<>(
            Collections.singletonList(new LicenseObligationDTO("obligation3", Collections.emptySet()))));
    licenseMetadataDTOs.get(2)
        .setLicenseObligations(new LinkedHashSet<>(Arrays
            .asList(new LicenseObligationDTO("obligation4", Collections.emptySet()),
                new LicenseObligationDTO("obligation5", Collections.emptySet()))));

    doReturn(licenseMetadataDTOs)
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);

    assertThat(resultDto.results).hasSize(2);
    assertThat(resultDto.totalResultsCount).isEqualTo(2);
    assertLegalLicenseApplicationDashboardDTO(appTagEval1.getLeft(), appTagEval1.getMiddle(), appTagEval1.getRight(),
        findResult(resultDto.results, appTagEval1.getLeft().getId()), 0, 1);
    assertLegalLicenseApplicationDashboardDTO(appTagEval2.getLeft(), appTagEval2.getMiddle(), appTagEval2.getRight(),
        findResult(resultDto.results, appTagEval2.getLeft().getId()), 1, 1);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_NoObligationsForComponent() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    Triple<Application, Tag, PolicyEvaluation> appTagEval = setupApplicationWithLicenses(componentIdentifier, "MIT");
    tempEntity.newLicenseOverride(appTagEval.getLeft().getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        Sets.newHashSet("Apache-1.0", "Apache-2.0"));

    // No obligations will match the previous component licenses override
    doReturn(createLicenseMetadataDTOs(Sets.newHashSet("MIT")))
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(any());

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(appTagEval.getLeft(), appTagEval.getMiddle(), appTagEval.getRight(),
        resultDto.results.get(0), 0, 1);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_ByReviewProgress() {
    Triple<Application, Tag, PolicyEvaluation> triple = setupApplicationDashboardEntities("tag1", ReleaseStageType.ID);
    Application app = triple.getLeft();
    Tag tag = triple.getMiddle();
    PolicyEvaluation policyEvaluation = triple.getRight();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createGolangCoordinates("n", "v");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, "hash1", componentIdentifier);
    tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN,
        Sets.newHashSet("MIT"));

    doReturn(createLicenseMetadataDTOs(Sets.newHashSet("MIT")))
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(any());

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null,
            Sets.newHashSet(LicenseLegalReviewStatus.NOT_STARTED), null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0), 0, 1);

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null,
        Sets.newHashSet(LicenseLegalReviewStatus.OPEN), null, 1, 10);

    assertThat(resultDto.results).isEmpty();
    assertThat(resultDto.totalResultsCount).isZero();

    tempEntity.newComponentObligation(componentIdentifier, app.getId(), "name", "comment", ObligationStatus.FULFILLED,
        "hash");

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null,
        Sets.newHashSet(LicenseLegalReviewStatus.NOT_STARTED), null, 1, 10);

    assertThat(resultDto.results).isEmpty();
    assertThat(resultDto.totalResultsCount).isZero();

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null,
        Sets.newHashSet(LicenseLegalReviewStatus.OPEN), null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0), 1, 1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createGolangCoordinates("n2", "v2");
    tempEntity.newApplicationComponent(app.getId(), ReleaseStageType.ID, "hash2", componentIdentifier2);

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null,
        Sets.newHashSet(LicenseLegalReviewStatus.OPEN, LicenseLegalReviewStatus.NOT_STARTED), null, 1, 10);

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseApplicationDashboardDTO(app, tag, policyEvaluation, resultDto.results.get(0), 1, 2);
  }

  @Test
  public void testGetLicenseLegalComponentDashboard_ByReviewProgressOpen() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    setupLicenseObligations(app, componentIdentifier1, licenseIds, ObligationStatus.OPEN, ObligationStatus.OPEN);
    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "hash", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 0, 2, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentDashboard_ByReviewProgressFulfilled() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier1, licenseIds, ObligationStatus.FULFILLED,
        ObligationStatus.FULFILLED);
    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "hash", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 2, 2, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentDashboard_ByReviewProgressIgnored() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier1, licenseIds, ObligationStatus.IGNORED, ObligationStatus.IGNORED);
    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "hash", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 2, 2, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentDashboard_ByReviewProgressIgnoredFullfiled() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier1, licenseIds, ObligationStatus.IGNORED,
        ObligationStatus.FULFILLED);
    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "hash", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 2, 2, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentDashboard_ByReviewProgressFullfilledOpen() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier1, licenseIds, ObligationStatus.FULFILLED, ObligationStatus.OPEN);
    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "hash", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 1, 2, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentDashboard_ByReviewProgressOpenIgnored() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier1, licenseIds, ObligationStatus.OPEN, ObligationStatus.IGNORED);
    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "hash", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 1, 2, "Liberal");
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Ordering() {
    Triple<Application, Tag, PolicyEvaluation> triple1 = setupApplicationDashboardEntities("Tag1", BuildStageType.ID);
    Triple<Application, Tag, PolicyEvaluation> triple2 =
        setupApplicationDashboardEntities("Tag2", BuildStageType.ID, System.currentTimeMillis() + 1000);
    Triple<Application, Tag, PolicyEvaluation> triple3 =
        setupApplicationDashboardEntities("Tag3", BuildStageType.ID, System.currentTimeMillis() + 2000);

    List<String> applicationNames = Stream.of(triple1.getLeft(), triple2.getLeft(), triple3.getLeft())
        .map(Application::getName)
        .sorted()
        .collect(Collectors.toList());

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
            LicenseLegalResultsOrder.APPLICATION_NAME_ASC, 1, 10);

    assertThat(resultDto.results.stream().map(dto -> dto.applicationName).collect(Collectors.toList()))
        .containsExactlyElementsOf(applicationNames);

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.APPLICATION_NAME_DESC, 1, 10);
    Collections.reverse(applicationNames);

    assertThat(resultDto.results.stream().map(dto -> dto.applicationName).collect(Collectors.toList()))
        .containsExactlyElementsOf(applicationNames);

    List<Long> scanTimes = Stream.of(triple1.getRight(), triple2.getRight(), triple3.getRight())
        .map(PolicyEvaluation::getTime)
        .map(Date::getTime)
        .sorted()
        .collect(Collectors.toList());

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.LAST_SCAN_TIME_ASC, 1, 10);

    assertThat(resultDto.results.stream().map(dto -> dto.lastScanTime).collect(Collectors.toList()))
        .containsExactlyElementsOf(scanTimes);

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.LAST_SCAN_TIME_DESC, 1, 10);
    Collections.reverse(scanTimes);

    assertThat(resultDto.results.stream().map(dto -> dto.lastScanTime).collect(Collectors.toList()))
        .containsExactlyElementsOf(scanTimes);

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.TAG_NAMES_ASC, 1, 10);

    assertThat(resultDto.results.stream()
        .flatMap(dto -> dto.applicationTagNames.stream())
        .collect(Collectors.toList()))
            .containsExactly("Tag1", "Tag2", "Tag3");

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.TAG_NAMES_DESC, 1, 10);

    assertThat(resultDto.results.stream()
        .flatMap(dto -> dto.applicationTagNames.stream())
        .collect(Collectors.toList()))
            .containsExactly("Tag3", "Tag2", "Tag1");
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_PaginationInvalidPage() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 0, 1))
        .withMessage("Request must include page and pageSize values greater than zero.");
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_PaginationInvalidPageSize() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 0))
        .withMessage("Request must include page and pageSize values greater than zero.");
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Pagination() {
    setupApplicationDashboardEntities("Tag1", BuildStageType.ID);
    setupApplicationDashboardEntities("Tag2", BuildStageType.ID);
    setupApplicationDashboardEntities("Tag3", BuildStageType.ID);

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
            LicenseLegalResultsOrder.TAG_NAMES_ASC, 1, 2);

    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertThat(resultDto.results).hasSize(2);
    assertThat(resultDto.results.get(0).applicationTagNames.get(0)).isEqualTo("Tag1");
    assertThat(resultDto.results.get(1).applicationTagNames.get(0)).isEqualTo("Tag2");

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.TAG_NAMES_ASC, 2, 2);

    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.results.get(0).applicationTagNames.get(0)).isEqualTo("Tag3");

    resultDto = apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null,
        LicenseLegalResultsOrder.TAG_NAMES_ASC, 3, 2);

    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertThat(resultDto.results).isEmpty();
  }

  private Triple<Application, Tag, PolicyEvaluation> setupApplicationWithLicenses(
      ComponentIdentifier componentIdentifier,
      String... effectiveLicenseIds)
  {
    Application application = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(application.getOrganizationId(), TemporaryEntity.uuid());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date());
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash", componentIdentifier);
    for (String effectiveLicenseId : effectiveLicenseIds) {
      tempEntity.newApplicationComponentLicense(applicationComponent.getId(), effectiveLicenseId);
    }
    return Triple.of(application, tag, policyEvaluation);
  }

  private ApiLicenseLegalApplicationDashboardDTO findResult(
      List<ApiLicenseLegalApplicationDashboardDTO> results,
      String applicationId)
  {
    Optional<ApiLicenseLegalApplicationDashboardDTO> result =
        results.stream().filter(r -> r.applicationId.equals(applicationId)).findFirst();
    assertThat(result).isPresent();
    return result.get();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    LicenseLegalFilterDTO licenseLegalFilterDTO =
        new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> apiLicenseLegalService.getLicenseLegalComponentsDashboard(licenseLegalFilterDTO));
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithoutApplications() {
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithoutEvaluations() {
    tempEntity.newApplicationWithParent();
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithoutLicenses() {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "someHash", componentIdentifier);

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_WithUnknownComponent() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "someHash", null);

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_MultipleApplications() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");
    setupComponentDashboardEntities("Tag1", StageTypes.COMPLIANCE.getId(), "somHash",
        componentIdentifier, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 2, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByOrganization() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT").getLeft();
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "otherHash", componentIdentifier, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(Sets.newHashSet(app.getOrganizationId()), null, null, null, null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByApplication() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT").getLeft();
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "otherHash", componentIdentifier, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, Sets.newHashSet(app.getId()), null, null, null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByTag() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Tag tag =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT").getRight();
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "otherHash", componentIdentifier, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, Sets.newHashSet(tag.getId()), null, null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_ByStageTypeId() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");
    setupComponentDashboardEntities("Tag1", ReleaseStageType.ID, "otherHash", componentIdentifier, "Apache-1.0");

    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(
            null, null, null, Sets.newHashSet(BuildStageType.ID), null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_OverrideAppScopeIgnored() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    Application app = setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier,
        "MIT").getLeft();

    tempEntity.newLicenseOverride(app.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService
        .getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(
            null, null, null, null, null, null, 1, 1, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0,
        0, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_PaginationInvalidPage() {
    LicenseLegalFilterDTO licenseLegalFilterDTO =
        new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 0, null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> apiLicenseLegalService.getLicenseLegalComponentsDashboard(licenseLegalFilterDTO))
        .withMessage("Request must include page and pageSize values greater than zero.");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_PaginationInvalidPageSize() {
    LicenseLegalFilterDTO licenseLegalFilterDTO =
        new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 0, null);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> apiLicenseLegalService.getLicenseLegalComponentsDashboard(licenseLegalFilterDTO))
        .withMessage("Request must include page and pageSize values greater than zero.");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Pagination() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "someHash1", componentIdentifier1, "MIT");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    setupComponentDashboardEntities("Tag2", BuildStageType.ID, "someHash2", componentIdentifier2, "MIT");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");
    setupComponentDashboardEntities("Tag3", BuildStageType.ID, "someHash3", componentIdentifier3, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(
            null, null, null, null, null, null, 1, 2, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertThat(resultDto.results).hasSize(2);

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(
        null, null, null, null, null, null, 2, 2, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertThat(resultDto.results).hasSize(1);

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(
        null, null, null, null, null, null, 3, 2, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Ordering() {
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");
    setupComponentDashboardEntities("Tag3", BuildStageType.ID, "hash3", componentIdentifier3, "GPL-2.0");
    setupComponentDashboardEntities("Tag3.1", BuildStageType.ID, "hash3", componentIdentifier3, "GPL-2.0");

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "hash1", componentIdentifier1, "MIT");

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    setupComponentDashboardEntities("Tag2", BuildStageType.ID, "hash2", componentIdentifier2, "Apache-2.0");
    setupComponentDashboardEntities("Tag2.2", BuildStageType.ID, "hash2", componentIdentifier2, "Apache-2.0");
    setupComponentDashboardEntities("Tag2.3", BuildStageType.ID, "hash2", componentIdentifier2, "Apache-2.0");

    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 3, null));
    assertThat(resultDto.results).extracting(dto -> dto.displayName)
        .containsExactly("g1 : a1 : v1", "g2 : a2 : v2",
            "g3 : a3 : v3");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null,
            LicenseLegalResultsOrder.COMPONENT_NAME_ASC, 1, 3, null));
    assertThat(resultDto.results).extracting(dto -> dto.displayName)
        .containsExactly("g1 : a1 : v1", "g2 : a2 : v2",
            "g3 : a3 : v3");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null,
            LicenseLegalResultsOrder.COMPONENT_NAME_DESC, 1, 3, null));
    assertThat(resultDto.results).extracting(dto -> dto.displayName)
        .containsExactly("g3 : a3 : v3", "g2 : a2 : v2",
            "g1 : a1 : v1");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null, LicenseLegalResultsOrder.LICENSE_NAME_ASC, 1, 3, null));
    assertThat(resultDto.results)
        .extracting(dto -> dto.licenses.stream().map(l -> l.licenseName).collect(Collectors.joining(",")))
        .containsExactly("Apache-2.0", "GPL-2.0", "MIT");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null,
        null, null, LicenseLegalResultsOrder.LICENSE_NAME_DESC, 1, 3, null));
    assertThat(resultDto.results)
        .extracting(dto -> dto.licenses.stream().map(l -> l.licenseName).collect(Collectors.joining(",")))
        .containsExactly("MIT", "GPL-2.0", "Apache-2.0");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null,
            LicenseLegalResultsOrder.APPLICATION_COUNT_ASC, 1, 3, null));
    assertThat(resultDto.results).extracting(dto -> dto.displayName)
        .containsExactly("g1 : a1 : v1", "g3 : a3 : v3",
            "g2 : a2 : v2");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null,
            LicenseLegalResultsOrder.APPLICATION_COUNT_DESC, 1, 3, null));
    assertThat(resultDto.results).extracting(dto -> dto.displayName)
        .containsExactly("g2 : a2 : v2", "g3 : a3 : v3",
            "g1 : a1 : v1");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboardByComponentName_WithoutName() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash", componentIdentifier, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, null));

    ApiLicenseLegalComponentDashboardResultDTO resultDto1 =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, ""));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(resultDto1.totalResultsCount);
    assertThat(resultDto.results.get(0).displayName).isEqualTo(resultDto1.results.get(0).displayName);
    assertThat(resultDto.results.get(0).hash).isEqualTo(resultDto1.results.get(0).hash);
    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertLegalLicenseComponentDashboardDTO(dto, "somHash", componentIdentifier, Sets.newHashSet("MIT"), 1, 0, 0,
        "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboardByComponentName_WithName() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "apache2", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "apache3", "v3");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash1", componentIdentifier1, "MIT");
    setupComponentDashboardEntities("Tag2", BuildStageType.ID, "somHash2", componentIdentifier2, "MIT");
    setupComponentDashboardEntities("Tag3", BuildStageType.ID, "somHash3", componentIdentifier3, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 3, "apache"));

    assertThat(resultDto.totalResultsCount).isEqualTo(2);
    assertThat(resultDto.results).hasSize(2);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "somHash2", componentIdentifier2,
        Sets.newHashSet("MIT"), 1, 0, 0, "Liberal");
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(1), "somHash3", componentIdentifier3,
        Sets.newHashSet("MIT"), 1, 0, 0, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboardByComponentName_NoMatch() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "apache2", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "apache3", "v3");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash1", componentIdentifier1, "MIT");
    setupComponentDashboardEntities("Tag2", BuildStageType.ID, "somHash2", componentIdentifier2, "MIT");
    setupComponentDashboardEntities("Tag3", BuildStageType.ID, "somHash3", componentIdentifier3, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 3, "dontmatch"));

    assertThat(resultDto.totalResultsCount).isZero();
    assertThat(resultDto.results).isEmpty();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboardByComponentName_FilteringComponentNameAndPagination() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "apache2", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "apache3", "v3");
    setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash1", componentIdentifier1, "MIT");
    setupComponentDashboardEntities("Tag2", BuildStageType.ID, "somHash2", componentIdentifier2, "MIT");
    setupComponentDashboardEntities("Tag3", BuildStageType.ID, "somHash3", componentIdentifier3, "MIT");

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 1, "apache"));

    assertThat(resultDto.totalResultsCount).isEqualTo(2);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "somHash2", componentIdentifier2,
        Sets.newHashSet("MIT"), 1, 0, 0, "Liberal");

    resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(
            new LicenseLegalFilterDTO(null, null, null, null, null, null, 2, 1, "apache"));

    assertThat(resultDto.totalResultsCount).isEqualTo(2);
    assertThat(resultDto.results).hasSize(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "somHash3", componentIdentifier3,
        Sets.newHashSet("MIT"), 1, 0, 0, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_FilteringByReviewStatus() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "apache1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "apache2", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "apache3", "v3");
    ComponentIdentifier componentIdentifier4 = ComponentIdentifier.createMavenCoordinates("g4", "apache4", "v4");
    Pair<Application, Tag> applicationTagPair1 =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash1", componentIdentifier1, "MIT");
    Pair<Application, Tag> applicationTagPair2 =
        setupComponentDashboardEntities("Tag2", BuildStageType.ID, "somHash2", componentIdentifier2, "MIT");
    Pair<Application, Tag> applicationTagPair3 =
        setupComponentDashboardEntities("Tag3", BuildStageType.ID, "somHash3", componentIdentifier3, "MIT");
    setupComponentDashboardEntities("Tag4", BuildStageType.ID, "somHash4", componentIdentifier4, "PUBLIC-DOMAIN");

    List<String> licenses = Collections.singletonList("MIT");

    applicationTagPair1.getLeft().setId(Organization.ROOT_ORGANIZATION_ID);
    applicationTagPair2.getLeft().setId(Organization.ROOT_ORGANIZATION_ID);
    applicationTagPair3.getLeft().setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(applicationTagPair1.getLeft(), componentIdentifier1, licenses, ObligationStatus.OPEN);
    setupLicenseObligations(applicationTagPair2.getLeft(), componentIdentifier2, licenses, ObligationStatus.FLAGGED);
    setupLicenseObligations(applicationTagPair3.getLeft(), componentIdentifier3, licenses, ObligationStatus.FULFILLED);

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null,
            Sets.newHashSet(LicenseLegalReviewStatus.OPEN), null, 1, 5, null));

    assertThat(resultDto.results).hasSize(3);
    assertThat(resultDto.totalResultsCount).isEqualTo(3);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "somHash2", componentIdentifier2,
        Sets.newHashSet("MIT"), 1, 0, 1, "Liberal");
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(1), "somHash3", componentIdentifier3,
        Sets.newHashSet("MIT"), 1, 1, 1, "Liberal");
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(2), "somHash4", componentIdentifier4,
        Sets.newHashSet("Public Domain"), 1, 0, 0, "Liberal");

    resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null,
        null, Sets.newHashSet(LicenseLegalReviewStatus.NOT_STARTED), null, 1, 5, null));

    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "somHash1", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 0, 1, "Liberal");
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_FilteringByReviewStatusWithUnspecifiedLicense() {
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "apache1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "apache2", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "apache3", "v3");
    Pair<Application, Tag> applicationTagPair1 =
        setupComponentDashboardEntities("Tag1", BuildStageType.ID, "somHash1", componentIdentifier1, "MIT");
    Pair<Application, Tag> applicationTagPair2 =
        setupComponentDashboardEntities("Tag2", BuildStageType.ID, "somHash2", componentIdentifier2, "MIT");
    setupComponentDashboardEntities("Tag3", BuildStageType.ID, "somHash3", componentIdentifier3, "UNSPECIFIED");

    List<String> licenses = Collections.singletonList("MIT");
    applicationTagPair1.getLeft().setId(Organization.ROOT_ORGANIZATION_ID);
    applicationTagPair2.getLeft().setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(applicationTagPair1.getLeft(), componentIdentifier1, licenses, ObligationStatus.OPEN);
    setupLicenseObligations(applicationTagPair2.getLeft(), componentIdentifier2, licenses, ObligationStatus.FLAGGED);

    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalComponentsDashboard(new LicenseLegalFilterDTO(null, null, null, null,
            Sets.newHashSet(LicenseLegalReviewStatus.NOT_STARTED), null, 1, 5, null));

    assertThat(resultDto.results).hasSize(2);
    assertThat(resultDto.totalResultsCount).isEqualTo(2);
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(0), "somHash1", componentIdentifier1,
        Sets.newHashSet("MIT"), 1, 0, 1, "Liberal");
    assertLegalLicenseComponentDashboardDTO(resultDto.results.get(1), "somHash3", componentIdentifier3,
        Sets.newHashSet("Not Provided"), 1, 0, 0, "Sonatype Special Licenses");
  }

  @Test
  public void testGetLastRawApplicationReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date(1));
    tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID, TemporaryEntity.uuid(), new Date(2));
    PolicyEvaluation policyEvaluation3 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TemporaryEntity.uuid(), new Date(3));
    mockReport(policyEvaluation3);
    tempEntity.newPolicyEvaluation(otherApp.getId(), ReleaseStageType.ID, TemporaryEntity.uuid(), new Date(4));

    Optional<ApiReportRawDataDTOV2> lastRawReportForApplication =
        apiLicenseLegalService.getLastRawApplicationReport(app.getPublicId());

    assertThat(lastRawReportForApplication).isPresent()
        .get()
        .usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation3.getScanId()));
  }

  @Test
  public void testGetLastRawApplicationReportByStageId() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid(), new Date(1));
    tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID, TemporaryEntity.uuid(), new Date(2));

    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TemporaryEntity.uuid(), new Date(2));
    PolicyEvaluation policyEvaluation3 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TemporaryEntity.uuid(), new Date(3));

    mockReport(policyEvaluation1);
    mockReport(policyEvaluation2);
    mockReport(policyEvaluation3);

    tempEntity.newPolicyEvaluation(otherApp.getId(), ReleaseStageType.ID, TemporaryEntity.uuid(), new Date(4));

    Optional<ApiReportRawDataDTOV2> lastRawReportForApplication =
        apiLicenseLegalService.getLastRawApplicationReportByStageId(app.getPublicId(), BuildStageType.ID);

    assertThat(lastRawReportForApplication).isPresent()
        .get()
        .usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation1.getScanId()));

    lastRawReportForApplication =
        apiLicenseLegalService.getLastRawApplicationReportByStageId(app.getPublicId(), ReleaseStageType.ID);

    assertThat(lastRawReportForApplication).isPresent()
        .get()
        .usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation3.getScanId()));
  }

  @Test
  public void testGetLastRawApplicationReportByStage_NoApplication() {
    assertThat(apiLicenseLegalService.getLastRawApplicationReportByStageId("doesNotExist", ReleaseStageType.ID))
        .isEmpty();
  }

  @Test
  public void testGetLastRawApplicationReportByStage_NoEvaluations() {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(apiLicenseLegalService.getLastRawApplicationReportByStageId(app.getPublicId(), ReleaseStageType.ID))
        .isEmpty();
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
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation.getScanId());
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata.json", EXPECTED_LICENSE_IDS, null,
        false);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_WithInnerSource() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation.getScanId());
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata.json", EXPECTED_LICENSE_IDS,
        BuildStageType.ID, true);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_ByStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation1);
    mockReport(policyEvaluation2);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation2.getScanId());
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata.json", EXPECTED_LICENSE_IDS,
        ReleaseStageType.ID, false);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_WithSingleLicensesInMultiLicenseIds() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("lls-raw-report-multilicenses.json", ApiReportRawDataDTOV2.class);
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    doReturn(Optional.of(rawReport)).when(apiLicenseLegalServiceSpy).getLastRawApplicationReport(anyString());
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata-multilicense.json",
        EXPECTED_LICENSE_IDS_FOR_MULTILICENSE, null, false);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoLicenses() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ApiReportRawDataDTOV2 rawReport = getContent("lls-raw-report-no-license.json", ApiReportRawDataDTOV2.class);
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    doReturn(Optional.of(rawReport)).when(apiLicenseLegalServiceSpy).getLastRawApplicationReport(anyString());

    ApiLicenseLegalApplicationReportDTO licenseMetadataReport = apiLicenseLegalServiceSpy
        .getLicenseLegalApplicationReport(app);

    assertThat(licenseMetadataReport).isNotNull();
    assertThat(licenseMetadataReport.licenseLegalMetadata).isEmpty();
    assertThat(licenseMetadataReport.components).hasSize(1);

    ComponentIdentifier expectedComponentIdentifier =
        rawReport.components.get(0).componentIdentifier.toComponentIdentifier();
    ApiLicenseLegalComponentDTO dto = licenseMetadataReport.components.get(0);
    assertThat(dto.componentIdentifier.toComponentIdentifier()).isEqualTo(expectedComponentIdentifier);
    assertThat(dto.packageUrl)
        .isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(expectedComponentIdentifier).getPackageUrl());
    assertThat(dto.displayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(expectedComponentIdentifier).toString());
  }

  private void testGetLicenseLegalApplicationReport(
      Application app,
      ApiReportRawDataDTOV2 rawReport,
      String licenseMetadataResource,
      String[] expectedLicenseFiles,
      String stageId,
      boolean includeInnerSource) throws Exception
  {
    ComponentIdentifier[] expectedComponentIdentifiers = rawReport.components.stream()
        .filter(c -> c.componentIdentifier != null)
        .filter(c -> includeInnerSource
            || !c.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER))
        .map(component -> component.componentIdentifier.toComponentIdentifier())
        .distinct()
        .toArray(ComponentIdentifier[]::new);
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

    LegalSourceLinkDTO[] legalSourceLinks = getContent("lls-legal-source-links.json", LegalSourceLinkDTO[].class);
    when(mockApiLicenseLegalHdsService
        .getSourceLinksFromComponentIdentifierSet(componentIdentifiersArgumentCaptorForSourceLinks.capture()))
            .thenAnswer(invocation -> {
              String sourceLink = "https://mockrepository.com/component.jar";
              Map<ComponentIdentifier, Set<LegalSourceLinkDTO>> sourceLinksForComponents = new HashMap<>();
              for (ComponentIdentifier comp : componentIdentifiersArgumentCaptorForSourceLinks.getValue()) {
                sourceLinksForComponents.put(comp, Sets.newHashSet(
                    new LegalSourceLinkDTO(null, sourceLink, sourceLink, ComponentLegalPartStatus.ENABLED)));
              }
              return sourceLinksForComponents;
            });
    when(mockSourceLinkOverrideDAO.batchGetWithHierarchy(anyString(), any()))
        .thenAnswer(invocation -> {
          Collection<ComponentIdentifier> components = invocation.getArgument(1);
          Map<ComponentIdentifier, List<SourceLinkOverride>> overridesByComponent = new HashMap<>();
          for (ComponentIdentifier comp : components) {
            List<SourceLinkOverride> overrides = Arrays.stream(legalSourceLinks)
                .map(dto -> new SourceLinkOverride(dto.content, dto.originalContent,
                    dto.status, "sourceLink-" + dto.content.hashCode()))
                .toList();
            overridesByComponent.put(comp, overrides);
          }
          return overridesByComponent;
        });

    ApiLicenseLegalApplicationReportDTO licenseMetadataReport =
        stageId == null
            ? apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app)
            : apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app, stageId, includeInnerSource, false);

    assertThat(licenseMetadataReport).isNotNull();
    assertLicenseLegalMetadata(licenseMetadataReport.components, licenseMetadataReport.licenseLegalMetadata, rawReport,
        expectedLicenseFiles, includeInnerSource);
    assertObligationsArePresent(licenseMetadataReport.licenseLegalMetadata, Arrays.asList(licenseMetadata));
    assertComponentLegalComments(licenseMetadataReport.components,
        new LinkedHashSet<>(Arrays.asList(componentLegalComments)));
    assertComponentLegalFiles(licenseMetadataReport.components,
        new LinkedHashSet<>(Arrays.asList(componentLegalFiles)));
    assertComponentData(licenseMetadataReport.components, rawReport);
    assertObligations(licenseMetadataReport.components, licenseMetadata);
    List<Collection<ComponentIdentifier>> queriedComponents = componentIdentifiersArgumentCaptor.getAllValues();
    assertThat(queriedComponents).hasSize(2);
    queriedComponents.forEach(
        componentIdentifiers -> assertThat(componentIdentifiers)
            .containsExactlyInAnyOrder(expectedComponentIdentifiers));

    assertApplicationTelemetry(app, rawReport, includeInnerSource);

    Set<ComponentIdentifier> sourceLinkComponents = componentIdentifiersArgumentCaptorForSourceLinks.getValue();
    assertThat(sourceLinkComponents).containsExactlyInAnyOrder(expectedComponentIdentifiers);
    assertThat(licenseMetadataReport.components.stream()
        .flatMap(c -> c.licenseLegalData.sourceLinks.stream())
        .collect(Collectors.toSet())).hasSize(3)
            .map(sl -> sl.status)
            .areExactly(3,
                new Condition<>(status -> status == ComponentLegalPartStatus.ENABLED, "All source links are enabled"));

    if (includeInnerSource) {
      assertThat(licenseMetadataReport.components)
          .anyMatch(dto -> dto.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER));
    }
    else {
      assertThat(licenseMetadataReport.components)
          .noneMatch(dto -> dto.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER));
    }
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
        apiLicenseLegalServiceSpy.getLicenseLegalApplicationReport(app);

    verify(mockApiLicenseLegalHdsService, never()).getLicenseMetadata(any());
    assertThat(licenseMetadataReport.components).hasSize(3);
    assertThat(licenseMetadataReport.licenseLegalMetadata).isEmpty();

    assertApplicationTelemetry(app, rawReport, false);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NoReport() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(tempEntity.newApplicationWithParent()));
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
  }

  @Test
  public void testGetLicenseLegalApplicationReport_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> apiLicenseLegalService.getLicenseLegalApplicationReport(tempEntity.newApplicationWithParent()));
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    testGetLicenseLegalComponentReport(tempEntity.newApplicationWithParent(), createNamedComponentDetails(),
        componentIdentifier, null, null, LicenseOverrideStatus.OPEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_PackageUrl() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    testGetLicenseLegalComponentReport(tempEntity.newApplicationWithParent(), createNamedComponentDetails(), null,
        packageUrl, null, LicenseOverrideStatus.OPEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, hash, componentIdentifier);
    testGetLicenseLegalComponentReport(application, createNamedComponentDetails(), null, null, hash,
        LicenseOverrideStatus.OPEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_HashComponentIdentifier() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    tempEntity.newClaimedComponent(hash, componentIdentifier);
    testGetLicenseLegalComponentReport(application, createNamedComponentDetails(), null, null, hash,
        LicenseOverrideStatus.OPEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ApplicationLicenseOverride() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(owner.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setEffectiveLicenseStatus(LicenseStatus.Overridden);
    testGetLicenseLegalComponentReport(owner, namedComponentDetails, componentIdentifier, null, null,
        LicenseOverrideStatus.OVERRIDDEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_OrganizationLicenseOverride() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity
        .newLicenseOverride(owner.getParentOwnerId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null,
        LicenseOverrideStatus.OVERRIDDEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationLicenseOverride() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null,
        LicenseOverrideStatus.OVERRIDDEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_RootOrganizationLicenseSelected() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, componentIdentifier,
        LicenseOverrideStatus.SELECTED, "Apache-2.0");
    testGetLicenseLegalComponentReport(owner, createNamedComponentDetails(), componentIdentifier, null, null,
        LicenseOverrideStatus.SELECTED);
  }

  @Test
  public void testGetLicenseLegalComponentReport_EmptyLicenses() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails =
        createNamedComponentDetails(Collections.emptyList(), Collections.emptyList());
    testGetLicenseLegalComponentReport(owner, namedComponentDetails, componentIdentifier, null, null,
        LicenseOverrideStatus.OPEN);
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
        IdentificationSource.CLAIR.getId(), "scanId", LicenseOverrideStatus.OPEN);
  }

  @Test
  public void testGetLicenseLegalComponentReport_WithOverrides() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "componentIdentifier", "e");
    // Set the HDS data
    doReturn(createLicenseMetadataDTOs(Sets.newHashSet("MIT")))
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(any());
    ComponentLegalCommentDTO componentLegalCommentDTO = createComponentLegalCommentDTO(componentIdentifier);
    Set<LegalCopyrightDTO> uniqueCopyrights = componentLegalCommentDTO.getUniqueCopyrights();
    ComponentLegalFileDTO componentLegalFileDTO = createComponentLegalFileDTO(componentIdentifier);
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalCommentDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalComments(any());
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalFileDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport = apiLicenseLegalService
        .getLicenseLegalComponentReport(application.getType(), application.getPublicId(), componentIdentifier, null,
            null, null, null);

    // Without overrides, we get the HDS data
    assertThat(licenseLegalComponentReport.component.licenseLegalData.copyrights)
        .containsExactlyInAnyOrder(uniqueCopyrights.stream()
            .map(legalCopyrightDTO -> new ApiLicenseLegalCopyrightDTO(null, legalCopyrightDTO.getContent(), null,
                ComponentLegalPartStatus.ENABLED))
            .toArray(ApiLicenseLegalCopyrightDTO[]::new));
    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(f -> f.content)
        .containsExactlyInAnyOrder(componentLegalFileDTO.getLegalFiles()
            .stream()
            .filter(l -> l.getType().equals(LegalFileType.NOTICE.name()))
            .map(LegalFileDTO::getContent)
            .toArray(String[]::new));
    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(f -> f.status)
        .containsOnly(ComponentLegalPartStatus.ENABLED);
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(f -> f.content)
        .containsExactlyInAnyOrder(componentLegalFileDTO.getLegalFiles()
            .stream()
            .filter(l -> l.getType().equals(LegalFileType.LICENSE.name()))
            .map(LegalFileDTO::getContent)
            .toArray(String[]::new));
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(f -> f.status)
        .containsOnly(ComponentLegalPartStatus.ENABLED);

    // Set the overrides
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, application.getId(), "legalContentHash");
    CopyrightOverride copyrightOverrideEnabled =
        tempEntity.newCopyrightOverride("originalHash1", "hash1", "overrideContent",
            ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverrideDisabled =
        tempEntity.newCopyrightOverride("originalHash2", "hash2", "overrideContent2",
            ComponentLegalPartStatus.DISABLED, componentCopyright.getId());
    ComponentLegalFile noticeLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, application.getId(),
        LegalFileType.NOTICE, "legalContentHash");
    LegalFileOverride noticeOverride = tempEntity.newLegalFileOverride("originalHash2", "hash2",
        "overrideContent", ComponentLegalPartStatus.DISABLED, noticeLegalFile.getId());
    ComponentLegalFile licenseLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, application.getId(),
        LegalFileType.LICENSE, "legalContentHash");
    LegalFileOverride licenseOverride = tempEntity.newLegalFileOverride("originalHash3", "hash3",
        "overrideContent", ComponentLegalPartStatus.DISABLED, licenseLegalFile.getId());

    licenseLegalComponentReport = apiLicenseLegalService
        .getLicenseLegalComponentReport(application.getType(), application.getPublicId(), componentIdentifier, null,
            null, null, null);

    // With overrides, we get the overridden data
    assertThat(licenseLegalComponentReport.component.licenseLegalData.copyrights)
        .containsExactly(new ApiLicenseLegalCopyrightDTO(
            copyrightOverrideEnabled.getId(),
            copyrightOverrideEnabled.getContent(),
            copyrightOverrideEnabled.getOriginalContentHash(),
            copyrightOverrideEnabled.getStatus()),
            new ApiLicenseLegalCopyrightDTO(
                copyrightOverrideDisabled.getId(),
                copyrightOverrideDisabled.getContent(),
                copyrightOverrideDisabled.getOriginalContentHash(),
                copyrightOverrideDisabled.getStatus()));
    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentCopyrightId)
        .isEqualTo(componentCopyright.getId());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentCopyrightScopeOwnerId).isEqualTo(
        application.getId());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(f -> f.content)
        .containsExactly(noticeOverride.getContent());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(f -> f.status)
        .containsExactly(ComponentLegalPartStatus.DISABLED);
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(f -> f.content)
        .containsExactly(licenseOverride.getContent());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(f -> f.status)
        .containsExactly(ComponentLegalPartStatus.DISABLED);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_WithOverrides() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("org.springframework.boot", "spring-boot-actuator", "2.2.6.RELEASE", "", "jar");

    tempEntity.newLicenseThreatGroup("id", application.getId(), "custom-ltg", 5, "Apache-2.0");

    // Set the HDS data
    doReturn(createLicenseMetadataDTOs(Sets.newHashSet("MIT", "Apache-2.0")))
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(any());
    ComponentLegalCommentDTO componentLegalCommentDTO = createComponentLegalCommentDTO(componentIdentifier);
    ComponentLegalFileDTO componentLegalFileDTO = createComponentLegalFileDTO(componentIdentifier);
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalCommentDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalComments(any());
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalFileDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());

    // Set the overrides
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, application.getId(), "legalContentHash");
    CopyrightOverride copyrightOverride = tempEntity.newCopyrightOverride("originalHash1", "hash1", "overrideContent",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    ComponentLegalFile noticeLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, application.getId(),
        LegalFileType.NOTICE, "legalContentHash");
    LegalFileOverride noticeOverride = tempEntity.newLegalFileOverride("originalHash2", "hash2",
        "overrideContent", ComponentLegalPartStatus.ENABLED, noticeLegalFile.getId());
    ComponentLegalFile licenseLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, application.getId(),
        LegalFileType.LICENSE, "legalContentHash");
    LegalFileOverride licenseOverride = tempEntity.newLegalFileOverride("originalHash3", "hash3",
        "overrideContent", ComponentLegalPartStatus.ENABLED, licenseLegalFile.getId());
    ComponentObligation componentObligation = tempEntity.newComponentObligation(componentIdentifier,
        application.getId(), "name", "comment", ObligationStatus.OPEN, "legalContentHash");
    ComponentObligationAttribution attribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), "name", "content1", "legalContentHash");
    ComponentObligationAttribution additionalAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, application.getId(), null, "content2", "legalContentHash");

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation);

    // Verify that the application report contains the overridden data
    ApiLicenseLegalApplicationReportDTO apiLicenseLegalApplicationReportDTO =
        apiLicenseLegalService.getLicenseLegalApplicationReport(application);
    ApiLicenseLegalComponentDTO apiLicenseLegalComponentDTO =
        apiLicenseLegalApplicationReportDTO.components.stream()
            .filter(c -> c.componentIdentifier.toComponentIdentifier()
                .equals(
                    componentIdentifier))
            .findFirst()
            .get();
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.componentCopyrightId).isEqualTo(componentCopyright.getId());
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.componentCopyrightScopeOwnerId)
        .isEqualTo(application.getId());
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.copyrights).containsExactly(
        new ApiLicenseLegalCopyrightDTO(
            copyrightOverride.getId(),
            copyrightOverride.getContent(),
            copyrightOverride.getOriginalContentHash(),
            copyrightOverride.getStatus()));
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.licenseFiles).containsExactly(
        new ApiLicenseLegalFileDTO(licenseOverride.getId(), null, licenseOverride.getContent(),
            licenseOverride.getOriginalContentHash(), licenseOverride.getStatus()));
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.noticeFiles).containsExactly(
        new ApiLicenseLegalFileDTO(noticeOverride.getId(), null, noticeOverride.getContent(),
            noticeOverride.getOriginalContentHash(), licenseOverride.getStatus()));
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.obligations).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(new ApiLicenseLegalObligationDTO(componentObligation));
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.attributions).containsExactly(
        new ComponentObligationAttributionDTO(attribution),
        new ComponentObligationAttributionDTO(additionalAttribution));
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.highestEffectiveLicenseThreatGroup.licenseThreatGroupLevel)
        .isEqualTo(5);
    assertThat(apiLicenseLegalComponentDTO.licenseLegalData.highestEffectiveLicenseThreatGroup.licenseThreatGroupName)
        .isEqualTo("custom-ltg");
  }

  @Test
  public void testGetAnameComponentLegalComments_reconstructComponentHash() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createAnameCoordinates("aname-test", "", "1.0.0");

    LegalCopyrightDTO copyright = new LegalCopyrightDTO();
    copyright.setContentHash("copyrightHash");
    copyright.setContent("copyrightContent");
    copyright.setAuthor("author");
    copyright.setYear("year");
    LegalCommentDTO comment = new LegalCommentDTO();
    comment.setContent("content");
    comment.setCopyrights(ImmutableSet.of(copyright));

    ComponentLegalCommentDTO resultDTO = new ComponentLegalCommentDTO();
    resultDTO.setComponentIdentifier(componentIdentifier);
    resultDTO.setHash("compHash");
    resultDTO.setComments(ImmutableSet.of(comment));

    Application app = tempEntity.newApplicationWithParent();
    ApplicationComponent appComp =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "compHash", componentIdentifier);
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash", Sets.newHashSet("some/path"));

    doReturn(ImmutableSet.of(resultDTO))
        .when(mockApiLicenseLegalHdsService)
        .getAnameComponentLegalComments(
            any(),
            eq(ImmutableMap.of(componentIdentifier, "compHash")));

    Map<ComponentIdentifier, Set<ComponentLegalCommentDTO>> results =
        apiLicenseLegalService.getAnameComponentLegalComments(ImmutableMap.of(componentIdentifier, "compHash"));

    assertThat(results).hasSize(1).containsKey(componentIdentifier);
    assertThat(results.get(componentIdentifier))
        .extracting("hash", String.class)
        .containsExactly("compHash");
  }

  @Test
  public void testGetComponentLegalComments_reconstructComponentHashForAname() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createAnameCoordinates("aname-test", "", "1.0.0");

    LegalCopyrightDTO copyright = new LegalCopyrightDTO();
    copyright.setContentHash("copyrightHash");
    copyright.setContent("copyrightContent");
    copyright.setAuthor("author");
    copyright.setYear("year");
    LegalCommentDTO comment = new LegalCommentDTO();
    comment.setContent("content");
    comment.setCopyrights(ImmutableSet.of(copyright));

    ComponentLegalCommentDTO resultDTO = new ComponentLegalCommentDTO();
    resultDTO.setComponentIdentifier(componentIdentifier);
    resultDTO.setHash("compHash");
    resultDTO.setComments(ImmutableSet.of(comment));

    Application app = tempEntity.newApplicationWithParent();
    ApplicationComponent appComp =
        tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "compHash", componentIdentifier);
    tempEntity.newAggregateFile(appComp.getId(), "aggregate_file_hash", Sets.newHashSet("some/path"));

    doReturn(ImmutableSet.of(resultDTO))
        .when(mockApiLicenseLegalHdsService)
        .getAnameComponentLegalComments(
            any(),
            eq(ImmutableMap.of(componentIdentifier, "compHash")));

    Set<ComponentLegalCommentDTO> results =
        apiLicenseLegalService.getComponentLegalComments(componentIdentifier, "compHash");

    assertThat(results).hasSize(1);
    assertThat(results).hasSize(1)
        .extracting("hash", String.class)
        .containsExactly("compHash");
  }

  @Test
  public void testGetLicenseLegalComponentReport_WithSavedObligationData() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier c = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    // Set the HDS data
    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(Sets.newHashSet("Beerware"));
    licenseMetadataDTOs.get(0)
        .setLicenseObligations(new LinkedHashSet<>(Arrays
            .asList(new LicenseObligationDTO("name1", Collections.emptySet()),
                new LicenseObligationDTO("name2", Collections.emptySet()),
                new LicenseObligationDTO("name3", Collections.emptySet()),
                new LicenseObligationDTO("name4", Collections.emptySet()))));
    doReturn(licenseMetadataDTOs)
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(any());
    doReturn(new LinkedHashSet<>(Collections.singletonList(createComponentLegalCommentDTO(c))))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalComments(any());
    doReturn(new LinkedHashSet<>(Collections.singletonList(createComponentLegalFileDTO(c))))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(c);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport = apiLicenseLegalService
        .getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), c, null, null, null, null);

    // Without saved obligation data (no obligations or obligation attributions), we get defaults
    List<ApiLicenseLegalObligationDTO> obligationDTOS =
        licenseLegalComponentReport.component.licenseLegalData.obligations;
    assertThat(obligationDTOS).extracting(ApiLicenseLegalObligationDTO::getId)
        .containsExactly(null, null, null, null);
    assertThat(obligationDTOS).extracting(ApiLicenseLegalObligationDTO::getOwnerId)
        .containsExactly(null, null, null, null);
    assertThat(obligationDTOS).extracting(ApiLicenseLegalObligationDTO::getName)
        .containsExactlyInAnyOrder("name1", "name2", "name3", "name4");
    assertThat(obligationDTOS).extracting(ApiLicenseLegalObligationDTO::getStatus)
        .containsExactly(ObligationStatus.OPEN, ObligationStatus.OPEN, ObligationStatus.OPEN, ObligationStatus.OPEN);
    assertThat(obligationDTOS).extracting(ApiLicenseLegalObligationDTO::getComment)
        .containsExactly(null, null, null, null);
    assertThat(licenseLegalComponentReport.component.licenseLegalData.attributions).isEmpty();

    // Save some obligation data
    ComponentObligation componentObligation1 = tempEntity
        .newComponentObligation(c, owner.getId(), "name1", "comment1", ObligationStatus.FULFILLED, "legalContentHash");
    ComponentObligation componentObligation2 = tempEntity
        .newComponentObligation(c, owner.getId(), "name2", "comment2", ObligationStatus.FLAGGED, "legalContentHash");
    // Obligation but no obligation attribution
    ComponentObligation componentObligation3 = tempEntity
        .newComponentObligation(c, owner.getId(), "name3", "comment3", ObligationStatus.IGNORED, "legalContentHash");
    ComponentObligationAttribution componentObligationAttribution1 =
        tempEntity.newComponentObligationAttribution(c, owner.getId(), "name1", "content1", "legalContentHash");
    ComponentObligationAttribution componentObligationAttribution3 =
        tempEntity.newComponentObligationAttribution(c, owner.getId(), "name2", "content3", "legalContentHash");
    // Obligation attribution but no obligation
    ComponentObligationAttribution componentObligationAttribution4 =
        tempEntity.newComponentObligationAttribution(c, owner.getId(), "name4", "content4", "legalContentHash");

    licenseLegalComponentReport = apiLicenseLegalService
        .getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), c, null, null, null, null);

    // With saved obligation data, the corresponding set is as expected
    obligationDTOS =
        licenseLegalComponentReport.component.licenseLegalData.obligations;
    assertThat(obligationDTOS).isNotEmpty();
    ApiLicenseLegalObligationDTO obligation1 = obligationDTOS.stream()
        .filter(o -> o.getName().equals(componentObligation1.getObligationName()))
        .findFirst()
        .orElse(null);
    assertThat(obligation1).isNotNull();
    assertThat(obligation1.getId()).isEqualTo(componentObligation1.getId());
    assertThat(obligation1.getOwnerId()).isEqualTo(componentObligation1.getOwnerId());
    assertThat(obligation1.getStatus()).isEqualTo(componentObligation1.getStatus());
    assertThat(obligation1.getComment()).isEqualTo(componentObligation1.getComment());

    ApiLicenseLegalObligationDTO obligation2 = obligationDTOS.stream()
        .filter(o -> o.getName().equals(componentObligation2.getObligationName()))
        .findFirst()
        .orElse(null);
    assertThat(obligation2).isNotNull();
    assertThat(obligation2.getId()).isEqualTo(componentObligation2.getId());
    assertThat(obligation2.getOwnerId()).isEqualTo(componentObligation2.getOwnerId());
    assertThat(obligation2.getStatus()).isEqualTo(componentObligation2.getStatus());
    assertThat(obligation2.getComment()).isEqualTo(componentObligation2.getComment());

    ApiLicenseLegalObligationDTO obligation3 = obligationDTOS.stream()
        .filter(o -> o.getName().equals(componentObligation3.getObligationName()))
        .findFirst()
        .orElse(null);
    assertThat(obligation3).isNotNull();
    assertThat(obligation3.getId()).isEqualTo(componentObligation3.getId());
    assertThat(obligation3.getOwnerId()).isEqualTo(componentObligation3.getOwnerId());
    assertThat(obligation3.getStatus()).isEqualTo(ObligationStatus.IGNORED);
    assertThat(obligation3.getComment()).isEqualTo(componentObligation3.getComment());

    ApiLicenseLegalObligationDTO obligation4 = obligationDTOS.stream()
        .filter(o -> o.getName().equals(componentObligationAttribution4.getObligationName()))
        .findFirst()
        .orElse(null);
    assertThat(obligation4).isNotNull();
    assertThat(obligation4.getId()).isNull();
    assertThat(obligation4.getOwnerId()).isNull();
    assertThat(obligation4.getStatus()).isEqualTo(ObligationStatus.OPEN);
    assertThat(obligation4.getComment()).isNull();

    assertThat(licenseLegalComponentReport.component.licenseLegalData.attributions)
        .containsExactlyInAnyOrder(
            new ComponentObligationAttributionDTO(componentObligationAttribution1),
            new ComponentObligationAttributionDTO(componentObligationAttribution3),
            new ComponentObligationAttributionDTO(componentObligationAttribution4));
  }

  private void testGetLicenseLegalComponentReport(
      Owner owner,
      NamedComponentDetails namedComponentDetails,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      LicenseOverrideStatus expectedLicenseOverrideStatus) throws Exception
  {
    testGetLicenseLegalComponentReport(owner, namedComponentDetails, componentIdentifier, packageUrl, hash, null, null,
        expectedLicenseOverrideStatus);
  }

  private void testGetLicenseLegalComponentReport(
      Owner owner,
      NamedComponentDetails namedComponentDetails,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      String identificationSource,
      String scanId,
      LicenseOverrideStatus expectedLicenseOverrideStatus) throws Exception
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

    doAnswer(invocationOnMock -> {
      ComponentIdentifier compIdentifier = invocationOnMock.getArgument(0, ComponentIdentifier.class);
      return Sets.newHashSet(new LegalSourceLinkDTO("https://" + compIdentifier), new LegalSourceLinkDTO("contentB"),
          new LegalSourceLinkDTO("contentC"));
    }).when(mockApiLicenseLegalHdsService).getSourceLinksFromComponentIdentifier(any());

    doAnswer(invocationOnMock -> {
      SourceLinkOverride sourceLinkOverride =
          new SourceLinkOverride("content", "content", ComponentLegalPartStatus.ENABLED, "1");
      SourceLinkOverride sourceLinkOverrideExtra =
          new SourceLinkOverride("contentB", ComponentLegalPartStatus.ENABLED, "1");
      return Sets.newHashSet(new LegalSourceLinkDTO(sourceLinkOverride),
          new LegalSourceLinkDTO(sourceLinkOverrideExtra));
    }).when(mockComponentLegalService).getSourceLinksOverridesFromComponentIdentifier(any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            packageUrl, hash, identificationSource, scanId);

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
    assertThat(licenseLegalComponent.displayName).isNotNull()
        .isEqualTo(
            ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier()).toString());
    assertThat(licenseLegalComponent.licenseLegalData).isNotNull();
    assertThat(licenseLegalComponent.licenseLegalData.effectiveLicenseStatus)
        .isEqualTo(expectedLicenseOverrideStatus.getName());
    assertThat(licenseLegalComponent.licenseLegalData.declaredLicenses)
        .containsExactly(namedComponentDetails.getDeclaredLicenseIds().toArray(new String[0]));
    assertThat(licenseLegalComponent.licenseLegalData.observedLicenses)
        .containsExactly(namedComponentDetails.getObservedLicenseIds().toArray(new String[0]));
    Set<String> expectedEffectiveLicenseIds = getExpectedEffectiveLicenseIds(namedComponentDetails);
    assertThat(licenseLegalComponent.licenseLegalData.effectiveLicenses)
        .containsExactlyInAnyOrder(expectedEffectiveLicenseIds.toArray(new String[0]));
    assertThat(licenseLegalComponent.licenseLegalData.copyrights).hasSize(8)
        .allMatch(copyright -> copyright.content.endsWith("content"));
    assertThat(licenseLegalComponent.licenseLegalData.licenseFiles).hasSize(4)
        .allMatch(licenseFile -> licenseFile.content.endsWith("contentLicense"));
    assertThat(licenseLegalComponent.licenseLegalData.noticeFiles).hasSize(4)
        .allMatch(noticeFile -> noticeFile.content.endsWith("contentNotice"));
    assertThat(licenseLegalComponent.licenseLegalData.sourceLinks).hasSize(4)
        .containsExactlyInAnyOrder(
            new LegalSourceLinkDTO("contentC"), new LegalSourceLinkDTO(
                "https://" + licenseLegalComponent.componentIdentifier.toComponentIdentifier()),
            new LegalSourceLinkDTO(new SourceLinkOverride("content", ComponentLegalPartStatus.ENABLED, "1")),
            new LegalSourceLinkDTO(new SourceLinkOverride("contentB", ComponentLegalPartStatus.ENABLED, "1")));

    Map<ApiLicenseDTO, Set<com.sonatype.insight.brain.model.license.License>> multiLicenseToSingleLicense =
        Sets.newHashSet(Iterables.concat(
            licenseLegalComponent.licenseLegalData.effectiveLicenses,
            licenseLegalComponent.licenseLegalData.declaredLicenses,
            licenseLegalComponent.licenseLegalData.observedLicenses))
            .stream()
            .map(multiLicenseDAO::getByIdNotNull)
            .collect(Collectors.toMap(
                multiLicense -> new ApiLicenseDTO(multiLicense.getId(), multiLicense.getShortDisplayName()),
                multiLicense -> multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicense.getId()),
                (prev, next) -> next,
                HashMap::new));

    ApiLicenseLegalMetadataDTO[] expectedLicenseLegalMetadata = legalReportBuilder
        .getLicenseLegalMetadata(
            multiLicenseToSingleLicense,
            expectedLicenseMetadataDTOs.stream()
                .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity())))
        .toArray(new ApiLicenseLegalMetadataDTO[0]);

    assertThat(licenseLegalComponentReport.licenseLegalMetadata).isNotNull()
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(expectedLicenseLegalMetadata);

    if (identificationSource != null && scanId != null) {
      verify(mockThirdPartyComponentDAO).getComponentDetailsByIdentifier(componentIdentifier, owner.getId(), scanId);
      verify(componentInfoServiceSpy, never()).getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    }
    else {
      verify(mockThirdPartyComponentDAO, never()).getComponentDetailsByIdentifier(any(), any(), any());
      verify(componentInfoServiceSpy).getComponentDetailsFromHDS(any(), any(), eq(componentIdentifier), any(), any());
    }
  }

  private Set<String> getExpectedEffectiveLicenseIds(NamedComponentDetails namedComponentDetails) {
    return Sets.newHashSet(Iterables.concat(
        namedComponentDetails.getEffectiveLicenses(),
        namedComponentDetails.getOverriddenLicenses()))
        .stream()
        .map(License::getLicenseId)
        .collect(Collectors.toSet());
  }

  @Test
  public void testGetLicenseLegalComponentReport_OwnerDoesNotExist() {
    String ownerId = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION, ownerId, null,
            null, null, null, null))
        .withMessageContaining("Application with ID " + ownerId + " does not exist.");
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.ORGANIZATION, ownerId, null,
            null, null, null, null))
        .withMessageContaining("Organization with ID " + ownerId + " does not exist.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_NoComponentIdentifier() {
    Application application = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getPublicId(), null, null, "hash", null, null))
        .withMessageContaining("Unable to determine componentIdentifier.");
  }

  @Test
  public void testInitialize_ComponentInfoServiceToolNameSet() {
    verify(componentInfoServiceSpy, atLeastOnce()).setToolName("ci");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifierAndPackageUrl() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getPublicId(), componentIdentifier, packageUrl, null, null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifierAndHash() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getPublicId(), componentIdentifier, "hash", null, null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_PackageUrlAndHash() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getPublicId(), null, packageUrl, "hash", null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifierAndPackageUrlAndHash() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String packageUrl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiLicenseLegalService.getLicenseLegalComponentReport(OwnerType.APPLICATION,
            application.getPublicId(), componentIdentifier, packageUrl, "hash", null, null))
        .withMessageContaining("Only one of componentIdentifier, packageUrl, or hash must be specified.");
  }

  @Test
  public void testGetLicenseLegalComponentReport_Unlicensed() {
    setUnlicensedForAdvancedLegalPack();
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> apiLicenseLegalService
        .getLicenseLegalComponentReport(OwnerType.APPLICATION, "anAppPublicId", null, null, "hash", null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasComponentCopyrightIdAndScope() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, owner.getId(), "legalContentHash");

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentCopyrightId)
        .isEqualTo(componentCopyright.getId());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentCopyrightScopeOwnerId)
        .isEqualTo(owner.getId());
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasComponentLegalFileIdsAndScopes() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    ComponentLegalFile componentLicense =
        tempEntity.newComponentLegalFile(componentIdentifier, org.getId(), LegalFileType.LICENSE, "legalContentHash");
    tempEntity.newLegalFileOverride(null, "hash2", "content2", ComponentLegalPartStatus.ENABLED,
        componentLicense.getId());
    ComponentLegalFile componentNotice =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash");
    tempEntity.newLegalFileOverride(null, "hash1", "content1", ComponentLegalPartStatus.ENABLED,
        componentNotice.getId());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(app.getType(), app.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentLicensesId)
        .isEqualTo(componentLicense.getId());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentLicensesScopeOwnerId)
        .isEqualTo(org.getId());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentNoticesId)
        .isEqualTo(componentNotice.getId());
    assertThat(licenseLegalComponentReport.component.licenseLegalData.componentNoticesScopeOwnerId)
        .isEqualTo(app.getId());
  }

  @Test
  public void testGetLicenseLegalComponentReport_SortsHDSCopyrights() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    LegalCopyrightDTO copyright1 = new LegalCopyrightDTO();
    copyright1.setAuthor("author");
    copyright1.setYear("year");
    copyright1.setContent("z");
    LegalCopyrightDTO copyright2 = new LegalCopyrightDTO();
    copyright2.setAuthor("author");
    copyright2.setYear("year");
    copyright2.setContent("a");
    ComponentLegalCommentDTO componentLegalCommentDTO = new ComponentLegalCommentDTO();
    LegalCommentDTO legalCommentDTO = new LegalCommentDTO();
    legalCommentDTO.setCopyrights(Sets.newLinkedHashSet(Arrays.asList(copyright1, copyright2)));
    componentLegalCommentDTO.setComments(Sets.newHashSet(legalCommentDTO));
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalCommentDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalComments(any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.copyrights).extracting(c -> c.content)
        .containsExactly("a", "z");
  }

  @Test
  public void testGetLicenseLegalComponentReport_SortsCopyrightOverrides() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(componentIdentifier, owner.getId(), "legalContentHash1");
    tempEntity.newCopyrightOverride(null, "hash1", "y", ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    tempEntity.newCopyrightOverride(null, "hash2", "b", ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    tempEntity.newCopyrightOverride("originalHash1", "hash3", "z", ComponentLegalPartStatus.ENABLED,
        componentCopyright.getId());
    tempEntity.newCopyrightOverride("originalHash2", "hash4", "a", ComponentLegalPartStatus.ENABLED,
        componentCopyright.getId());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.copyrights).extracting(c -> c.content)
        .containsExactly("a", "z", "b", "y");
  }

  @Test
  public void testGetLicenseLegalComponentReport_SortsHDSLegalFiles() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    LegalFileDTO legalFile1 = new LegalFileDTO();
    legalFile1.setType("NOTICE");
    legalFile1.setContent("z");
    LegalFileDTO legalFile2 = new LegalFileDTO();
    legalFile2.setType("NOTICE");
    legalFile2.setContent("a");
    LegalFileDTO legalFile3 = new LegalFileDTO();
    legalFile3.setType("LICENSE");
    legalFile3.setContent("y");
    LegalFileDTO legalFile4 = new LegalFileDTO();
    legalFile4.setType("LICENSE");
    legalFile4.setContent("b");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO
        .setLegalFiles(Sets.newLinkedHashSet(Arrays.asList(legalFile1, legalFile2, legalFile3, legalFile4)));
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalFileDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(c -> c.content)
        .containsExactly("a", "z");
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(c -> c.content)
        .containsExactly("b", "y");
  }

  @Test
  public void testGetLicenseLegalComponentReport_SortsLegalFileOverrides() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    ComponentLegalFile noticeLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(), LegalFileType.NOTICE, "legalContentHash1");
    tempEntity.newLegalFileOverride(null,
        "hash1", "y", ComponentLegalPartStatus.ENABLED, noticeLegalFile.getId());
    tempEntity.newLegalFileOverride(null,
        "hash2", "b", ComponentLegalPartStatus.ENABLED, noticeLegalFile.getId());
    tempEntity.newLegalFileOverride("originalHash1",
        "hash3", "z", ComponentLegalPartStatus.ENABLED, noticeLegalFile.getId());
    tempEntity.newLegalFileOverride("originalHash2",
        "hash4", "a", ComponentLegalPartStatus.ENABLED, noticeLegalFile.getId());
    ComponentLegalFile licenseLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(),
        LegalFileType.LICENSE, "legalContentHash1");
    tempEntity.newLegalFileOverride(null,
        "hash1", "y", ComponentLegalPartStatus.ENABLED, licenseLegalFile.getId());
    tempEntity.newLegalFileOverride(null,
        "hash2", "b", ComponentLegalPartStatus.ENABLED, licenseLegalFile.getId());
    tempEntity.newLegalFileOverride("originalHash1",
        "hash3", "z", ComponentLegalPartStatus.ENABLED, licenseLegalFile.getId());
    tempEntity.newLegalFileOverride("originalHash2",
        "hash4", "a", ComponentLegalPartStatus.ENABLED, licenseLegalFile.getId());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(c -> c.content)
        .containsExactly("a", "z", "b", "y");
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(c -> c.content)
        .containsExactly("a", "z", "b", "y");
  }

  @Test
  public void testGetLicenseLegalComponentReport_SingleComponentMatchingRequestedOne() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    LegalFileDTO legalFile1 = new LegalFileDTO();
    legalFile1.setType("NOTICE");
    legalFile1.setContent("a");
    LegalFileDTO legalFile2 = new LegalFileDTO();
    legalFile2.setType("LICENSE");
    legalFile2.setContent("b");
    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"));
    componentLegalFileDTO
        .setLegalFiles(Sets.newLinkedHashSet(Arrays.asList(legalFile1, legalFile2)));
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalFileDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(c -> c.content)
        .containsExactly("a");
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(c -> c.content)
        .containsExactly("b");
  }

  @Test
  public void testGetLicenseLegalComponentReport_DuplicateLegalFilesSameRelPathContentHash() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifierBinary = ComponentIdentifier.createMavenCoordinates("test",
        "some-artifact", "1.0.0", "", ".jar");
    ComponentIdentifier componentIdentifierSources = ComponentIdentifier.createMavenCoordinates("test",
        "some-artifact", "1.0.0", "source", ".jar");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifierBinary);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    LegalFileDTO legalFile1 = new LegalFileDTO();
    legalFile1.setRelPath("/a");
    legalFile1.setContentHash("aaa");
    legalFile1.setContent("a");
    legalFile1.setType("NOTICE");
    LegalFileDTO legalFile2 = new LegalFileDTO();
    legalFile2.setRelPath("/a");
    legalFile2.setContentHash("aaa");
    legalFile2.setContent("a");
    legalFile2.setType("NOTICE");
    LegalFileDTO legalFile3 = new LegalFileDTO();
    legalFile3.setRelPath("/b");
    legalFile3.setContentHash("bbb");
    legalFile3.setContent("b");
    legalFile3.setType("LICENSE");
    LegalFileDTO legalFile4 = new LegalFileDTO();
    legalFile4.setRelPath("/b");
    legalFile4.setContentHash("bbb");
    legalFile4.setType("LICENSE");
    legalFile4.setContent("b");
    ComponentLegalFileDTO componentLegalFileDTOBinary = new ComponentLegalFileDTO();
    componentLegalFileDTOBinary.setHash("1");
    componentLegalFileDTOBinary.setComponentIdentifier(componentIdentifierBinary);
    componentLegalFileDTOBinary
        .setLegalFiles(Sets.newLinkedHashSet(Arrays.asList(legalFile1, legalFile3)));

    ComponentLegalFileDTO componentLegalFileDTOSources = new ComponentLegalFileDTO();
    componentLegalFileDTOSources.setHash("2");
    componentLegalFileDTOSources.setComponentIdentifier(componentIdentifierSources);
    componentLegalFileDTOSources.setLegalFiles(Sets.newLinkedHashSet(Arrays.asList(legalFile2, legalFile4)));

    doReturn(new LinkedHashSet<>(Arrays.asList(componentLegalFileDTOBinary, componentLegalFileDTOSources)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(),
            componentIdentifierBinary, null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(c -> c.content)
        .containsExactly("a");
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(c -> c.content)
        .containsExactly("b");
  }

  @Test
  public void testGetLicenseLegalComponentReport_SingleComponentIdentifierDifferentClassifier() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "sources", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    LegalFileDTO legalFile1 = new LegalFileDTO();
    legalFile1.setType("NOTICE");
    legalFile1.setRelPath("/1");
    legalFile1.setContent("content");
    LegalFileDTO legalFile2 = new LegalFileDTO();
    legalFile2.setType("NOTICE");
    legalFile2.setRelPath("/2");
    legalFile2.setContent("content");
    LegalFileDTO legalFile3 = new LegalFileDTO();
    legalFile3.setType("NOTICE");
    legalFile3.setRelPath("/3");
    legalFile3.setContent("content");

    LegalFileDTO legalFile4 = new LegalFileDTO();
    legalFile4.setType("LICENSE");
    legalFile4.setContent("b");
    legalFile4.setRelPath("/a");
    LegalFileDTO legalFile5 = new LegalFileDTO();
    legalFile5.setType("LICENSE");
    legalFile5.setContent("b");
    legalFile5.setRelPath("/b");

    ComponentLegalFileDTO componentLegalFileDTO = new ComponentLegalFileDTO();
    componentLegalFileDTO
        .setLegalFiles(Sets.newLinkedHashSet(Arrays.asList(legalFile1, legalFile2, legalFile3, legalFile4,
            legalFile5)));
    doReturn(new LinkedHashSet<>(Collections.singletonList(componentLegalFileDTO)))
        .when(mockApiLicenseLegalHdsService)
        .getComponentLegalFiles(any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(),
            ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "e"),
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.noticeFiles).extracting(c -> c.relPath)
        .containsExactlyElementsOf(Arrays.asList("/1", "/2", "/3"));
    assertThat(licenseLegalComponentReport.component.licenseLegalData.licenseFiles).extracting(c -> c.relPath)
        .containsExactlyElementsOf(Arrays.asList("/a", "/b"));
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasHighestEffectiveLicenseThreatGroup() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    ApiLicenseThreatDTOV2 expected = new ApiLicenseThreatDTOV2();
    expected.licenseThreatGroupCategory = "severe";
    expected.licenseThreatGroupName = "Non Standard";
    expected.licenseThreatGroupLevel = 6;
    assertThat(licenseLegalComponentReport.component.licenseLegalData.highestEffectiveLicenseThreatGroup)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  public void testGetLicenseLegalComponentReport_multiLicense_HasHighestEffectiveLicenseThreatGroup() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails(
        Collections.singletonList("CDDL-1.1-GPL-2.0-CPE"),
        Collections.singletonList("MIT"));

    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    ApiLicenseThreatDTOV2 expected = new ApiLicenseThreatDTOV2();
    expected.licenseThreatGroupCategory = "critical";
    expected.licenseThreatGroupName = "Copyleft";
    expected.licenseThreatGroupLevel = 9;
    assertThat(licenseLegalComponentReport.component.licenseLegalData.highestEffectiveLicenseThreatGroup)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  public void testGetLicenseLegalComponentReport_multiLicense_mergeObligations() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails(
        Collections.singletonList("BSD-3-Clause-GPL-2.0"),
        Collections.singletonList("MIT"));

    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    Set<String> licenses = new LinkedHashSet<>(Arrays.asList("MIT", "BSD-3-Clause", "GPL-2.0"));
    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);
    licenseMetadataDTOs.get(0)
        .setLicenseObligations(new LinkedHashSet<>(
            Arrays.asList(
                new LicenseObligationDTO("a", Collections.emptySet()),
                new LicenseObligationDTO("b", Collections.emptySet()),
                new LicenseObligationDTO("c", Collections.emptySet()))));
    licenseMetadataDTOs.get(1)
        .setLicenseObligations(new LinkedHashSet<>(Arrays
            .asList(
                new LicenseObligationDTO("x", Collections.emptySet()),
                new LicenseObligationDTO("y", Collections.emptySet()))));
    licenseMetadataDTOs.get(2).setLicenseObligations(Collections.emptySet());
    doReturn(licenseMetadataDTOs)
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.licenseLegalMetadata).isNotNull().isNotEmpty().hasSize(4);
    assertThat(licenseLegalComponentReport.licenseLegalMetadata)
        .extracting(dto -> dto.licenseId)
        .containsExactlyInAnyOrder("MIT", "GPL-2.0", "BSD-3-Clause", "BSD-3-Clause-GPL-2.0");

    licenseLegalComponentReport.licenseLegalMetadata.stream().map(l -> {
      LicenseObligationDTO[] tmpObligationDTOs = l.obligations
          .toArray(new LicenseObligationDTO[l.obligations.size()]);
      if (l.licenseId.equals("MIT")) {
        assertThat(tmpObligationDTOs).hasSize(3);
        assertThat(tmpObligationDTOs[0].getName()).isEqualTo("a");
        assertThat(tmpObligationDTOs[1].getName()).isEqualTo("b");
        assertThat(tmpObligationDTOs[2].getName()).isEqualTo("c");
      }
      else if (l.licenseId.equals("GPL-2.0")) {
        assertThat(tmpObligationDTOs).isEmpty();
      }
      else if (l.licenseId.equals("BSD-3-Clause")) {
        assertThat(tmpObligationDTOs).hasSize(2);
        assertThat(tmpObligationDTOs[0].getName()).isEqualTo("x");
        assertThat(tmpObligationDTOs[1].getName()).isEqualTo("y");
      }
      else if (l.licenseId.equals("BSD-3-Clause-GPL-2.0")) {
        assertThat(tmpObligationDTOs).isEmpty();
      }
      return l;
    }).collect(Collectors.toSet());
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasHighestEffectiveLicenseThreatGroup_overrides() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();

    LicenseThreatGroup licenseThreatGroup =
        tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID, "Very Bad", 10);
    tempEntity.newLicenseThreatGroupLicense(Organization.ROOT_ORGANIZATION_ID, licenseThreatGroup.getId(),
        "CDDL-1.1");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails(
        Collections.singletonList("CDDL-1.1-GPL-2.0-CPE"),
        Collections.singletonList("MIT"));
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    ApiLicenseThreatDTOV2 expected = new ApiLicenseThreatDTOV2();
    expected.licenseThreatGroupCategory = "critical";
    expected.licenseThreatGroupName = "Very Bad";
    expected.licenseThreatGroupLevel = 10;
    assertThat(licenseLegalComponentReport.component.licenseLegalData.highestEffectiveLicenseThreatGroup)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasNoHighestEffectiveLicenseThreatGroup() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(owner.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GLWTPL");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.highestEffectiveLicenseThreatGroup).isNull();
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasStageScansWithoutEvaluations() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.stageScans)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new ApiLicenseLegalStageScanDTO(StageTypes.SOURCE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.BUILD.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.STAGE_RELEASE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.RELEASE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.OPERATE.getName(), null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_HasStageScansWithEvaluations() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.SOURCE.getId(), namedComponentDetails.getHash(),
        componentIdentifier);
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.BUILD.getId(), namedComponentDetails.getHash(),
        componentIdentifier);
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.STAGE_RELEASE.getId(), namedComponentDetails.getHash(),
        componentIdentifier);
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.RELEASE.getId(), namedComponentDetails.getHash(),
        componentIdentifier);
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.OPERATE.getId(), namedComponentDetails.getHash(),
        componentIdentifier);
    PolicyEvaluation sourceEval =
        tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.SOURCE.getId(), "scanIdSource", new Date(0));
    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.BUILD.getId(), "scanIdBuild",
        new Date(1));
    PolicyEvaluation stageReleaseEval = tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.STAGE_RELEASE.getId(),
        "scanIdStageRelease", new Date(2));
    PolicyEvaluation releaseEval = tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.RELEASE.getId(),
        "scanIdRelease", new Date(3));
    PolicyEvaluation operateEval = tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.OPERATE.getId(),
        "scanIdStageOperate", new Date(4));

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.stageScans)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new ApiLicenseLegalStageScanDTO(StageTypes.SOURCE.getName(), sourceEval.getScanId(), sourceEval.getTime()),
            new ApiLicenseLegalStageScanDTO(StageTypes.BUILD.getName(), buildEval.getScanId(), buildEval.getTime()),
            new ApiLicenseLegalStageScanDTO(StageTypes.STAGE_RELEASE.getName(), stageReleaseEval.getScanId(),
                stageReleaseEval.getTime()),
            new ApiLicenseLegalStageScanDTO(StageTypes.RELEASE.getName(), releaseEval.getScanId(),
                releaseEval.getTime()),
            new ApiLicenseLegalStageScanDTO(StageTypes.OPERATE.getName(), operateEval.getScanId(),
                operateEval.getTime()));
  }

  @Test
  public void testGetLicenseLegalComponentReport_GetsStageScansByPassedHash() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.BUILD.getId(), "otherHash", componentIdentifier);
    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.BUILD.getId(), "scanIdBuild",
        new Date());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), null,
            null, "otherHash", IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.stageScans)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new ApiLicenseLegalStageScanDTO(StageTypes.SOURCE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.BUILD.getName(), buildEval.getScanId(), buildEval.getTime()),
            new ApiLicenseLegalStageScanDTO(StageTypes.STAGE_RELEASE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.RELEASE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.OPERATE.getName(), null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_GetsStageScansByComponentIdentifier() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    tempEntity.newApplicationComponent(owner.getId(), StageTypes.BUILD.getId(), namedComponentDetails.getHash(),
        componentIdentifier);
    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(owner.getId(), StageTypes.BUILD.getId(), "scanIdBuild",
        new Date());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.stageScans)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new ApiLicenseLegalStageScanDTO(StageTypes.SOURCE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.BUILD.getName(), buildEval.getScanId(), buildEval.getTime()),
            new ApiLicenseLegalStageScanDTO(StageTypes.STAGE_RELEASE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.RELEASE.getName(), null, null),
            new ApiLicenseLegalStageScanDTO(StageTypes.OPERATE.getName(), null, null));
  }

  @Test
  public void testGetLicenseLegalComponentReport_SortsObligations() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    List<String> licenses = Collections.singletonList("Beerware");
    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);
    licenseMetadataDTOs.get(0)
        .setLicenseObligations(new LinkedHashSet<>(Arrays
            .asList(
                new LicenseObligationDTO("z", Collections.emptySet()),
                new LicenseObligationDTO("k", Collections.emptySet()),
                new LicenseObligationDTO("a", Collections.emptySet()),
                new LicenseObligationDTO("y", Collections.emptySet()),
                new LicenseObligationDTO("b", Collections.emptySet()),
                new LicenseObligationDTO("x", Collections.emptySet()))));
    doReturn(licenseMetadataDTOs)
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.obligations).extracting(
        ApiLicenseLegalObligationDTO::getName).containsExactly("a", "b", "k", "x", "y", "z");
  }

  @Test
  public void testGetLicenseLegalComponentReport_SortsAttributions() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), "z", "content", "hash");
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), "k", "content", "hash");
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), "a", "content", "hash");
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), null, "content", "hash");
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), "y", "content", "hash");
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), "b", "content", "hash");
    tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), "x", "content", "hash");

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport.component.licenseLegalData.attributions).extracting(
        ComponentObligationAttributionDTO::getObligationName)
        .containsExactly("a", "b", "k", "x", "y", "z", null);
  }

  @Test
  public void testGetLicenseLegalComponentReport_NullHash() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createAnameCoordinates("n", "q", "v");
    NamedComponentDetails namedComponentDetails = createNamedComponentDetails();
    namedComponentDetails.setComponentIdentifier(componentIdentifier);
    namedComponentDetails.setHash(null);
    doReturn(namedComponentDetails)
        .when(componentInfoServiceSpy)
        .getComponentDetailsFromHDS(any(), any(), any(), any(), any());

    ApiLicenseLegalComponentReportDTO licenseLegalComponentReport =
        apiLicenseLegalService.getLicenseLegalComponentReport(owner.getType(), owner.getPublicId(), componentIdentifier,
            null, null, IdentificationSource.SONATYPE.toString(), null);

    assertThat(licenseLegalComponentReport).isNotNull();
    assertThat(licenseLegalComponentReport.component).isNotNull();
    assertThat(licenseLegalComponentReport.component.licenseLegalData).isNotNull();
    assertThat(licenseLegalComponentReport.component.licenseLegalData.copyrights).isEmpty();
  }

  @Test
  public void testGetLicenseLegalMultiApplicationReport_HdsgetLicenseMetadataCalledOnceWhenLicenseRepeated() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    ApiReportRawDataDTOV2 reportDto1 = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 componentDto1 = new ApiReportComponentDTOV2();
    componentDto1.hash = "hash1";
    componentDto1.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createNpmCoordinates("p1", "v1"));
    componentDto1.licenseData = new ApiLicenseDataDTOV2();
    componentDto1.licenseData.effectiveLicenses.add(new ApiLicenseDTO("MIT", "MIT"));
    reportDto1.components.add(componentDto1);

    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    doReturn(reportDto1).when(apiLicenseLegalServiceSpy)
        .getApiReportRawDataForMultiApplicationReport(app1,
            BuildStageType.ID);

    ApiReportRawDataDTOV2 reportDto2 = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 componentDto2 = new ApiReportComponentDTOV2();
    componentDto2.hash = "hash2";
    componentDto2.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createNpmCoordinates("p2", "v2"));
    componentDto2.licenseData = new ApiLicenseDataDTOV2();
    componentDto2.licenseData.effectiveLicenses.add(new ApiLicenseDTO("MIT", "MIT"));
    reportDto2.components.add(componentDto2);

    doReturn(reportDto2).when(apiLicenseLegalServiceSpy)
        .getApiReportRawDataForMultiApplicationReport(app2,
            BuildStageType.ID);

    List<String> licenses = Collections.singletonList("MIT");
    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);

    doReturn(licenseMetadataDTOs).when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));

    Set<Optional<ApiLicenseLegalApplicationReportDTO>> optionalResult =
        apiLicenseLegalServiceSpy.getLicenseLegalMultiApplicationReport(Arrays.asList(app1, app2),
            Arrays.asList(BuildStageType.ID, BuildStageType.ID), false, false);

    assertThat(optionalResult).isNotEmpty();
    verify(mockApiLicenseLegalHdsService).getLicenseMetadata(new HashSet<>(licenses));
  }

  @Test
  public void getLicenseLegalApplicationReport_sendsObfuscatedApplicationTelemetryIfRequired() throws Exception {
    // Toggle advanced reporting to make sure values are being obfuscated accordingly in the telemetry
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation);
    ApiReportRawDataDTOV2 rawReport =
        apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation.getScanId());
    apiLicenseLegalServiceSpy = spy(apiLicenseLegalService);
    testGetLicenseLegalApplicationReport(app, rawReport, "lls-license-metadata.json", EXPECTED_LICENSE_IDS, null,
        false);
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard_Conan() {
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier(ComponentIdentifier.FORMAT_CONAN, Map.of(
        ComponentIdentifier.CONAN_CHANNEL, "",
        ComponentIdentifier.CONAN_OWNER, "",
        ComponentIdentifier.CONAN_NAME, "bzip2",
        ComponentIdentifier.VERSION, "1.0.8"));
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createConanCoordinates("bzip2", "1.0.8", null, null);
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier2, licenseIds, ObligationStatus.FULFILLED,
        ObligationStatus.FULFILLED);

    ApiLicenseLegalComponentDashboardResultDTO resultDto = apiLicenseLegalService.getLicenseLegalComponentsDashboard(
        new LicenseLegalFilterDTO(null, null, null, null, null, null, 1, 3, null));

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.results.get(0).reviewCompletedCount).isEqualTo(2);
    assertThat(resultDto.results.get(0).reviewTotalCount).isEqualTo(2);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_Conan() {
    ComponentIdentifier componentIdentifier1 = new ComponentIdentifier(ComponentIdentifier.FORMAT_CONAN, Map.of(
        ComponentIdentifier.CONAN_CHANNEL, "",
        ComponentIdentifier.CONAN_OWNER, "",
        ComponentIdentifier.CONAN_NAME, "bzip2",
        ComponentIdentifier.VERSION, "1.0.8"));
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createConanCoordinates("bzip2", "1.0.8", null, null);
    List<String> licenseIds = Collections.singletonList("MIT");
    Application app = setupApplicationWithLicenses(componentIdentifier1, licenseIds.get(0)).getLeft();
    app.setId(Organization.ROOT_ORGANIZATION_ID);
    setupLicenseObligations(app, componentIdentifier2, licenseIds, ObligationStatus.FULFILLED,
        ObligationStatus.FULFILLED);

    ApiLicenseLegalApplicationDashboardResultDTO resultDto =
        apiLicenseLegalService.getLicenseLegalApplicationsDashboard(null, null, null, null, null, null, 1, 3);

    assertThat(resultDto.totalResultsCount).isEqualTo(1);
    assertThat(resultDto.results).hasSize(1);
    assertThat(resultDto.results.get(0).componentsReviewedCount).isEqualTo(1);
    assertThat(resultDto.results.get(0).componentsTotalCount).isEqualTo(1);
  }

  private NamedComponentDetails createNamedComponentDetails() {
    return createNamedComponentDetails(
        Arrays.asList("Apache-2.0+", "Apache-2.0-MIT"),
        Arrays.asList("Beerware-Pizzaware", "Beerware"));
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
    legalCopyrightDTO.setContent(TemporaryEntity.uuid() + " content");
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
    final String uuid = TemporaryEntity.uuid();
    licenseLegalFileDTO.setRelPath(uuid + " relPath");
    licenseLegalFileDTO.setContent(uuid + " contentLicense");
    licenseLegalFileDTO.setContentHash(uuid);
    return licenseLegalFileDTO;
  }

  private LegalFileDTO createNoticeLegalFileDTO() {
    LegalFileDTO noticeLegalFileDTO = createLegalFileDTO("NOTICE");
    final String uuid = TemporaryEntity.uuid();
    noticeLegalFileDTO.setRelPath(uuid + " relPath");
    noticeLegalFileDTO.setContent(uuid + " contentNotice");
    noticeLegalFileDTO.setContentHash(uuid);
    return noticeLegalFileDTO;
  }

  private LegalFileDTO createLegalFileDTO(String type) {
    LegalFileDTO legalFileDTO = new LegalFileDTO();
    legalFileDTO.setContentHash(TemporaryEntity.uuid());
    legalFileDTO.setRelPath("relPath");
    legalFileDTO.setType(type);
    return legalFileDTO;
  }

  private void assertLicenseLegalMetadata(
      List<ApiLicenseLegalComponentDTO> components,
      Set<ApiLicenseLegalMetadataDTO> licenseLegalMetadata,
      ApiReportRawDataDTOV2 rawReport,
      String[] expectedLicenseIds,
      boolean includeInnerSource)
  {
    assertThat(components).hasSize((int) rawReport.components.stream()
        .filter(c -> c.componentIdentifier != null)
        .filter(c -> includeInnerSource
            || !c.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER))
        .count());
    List<Collection<String>> licenseIds = licenseIdArgumentCaptor.getAllValues();
    assertThat(licenseIds).hasSize(1);
    assertThat(licenseIds.get(0)).containsExactlyInAnyOrder(expectedLicenseIds);
    Set<String> expectedLicenseLegalMetadataLicenseIds = new LinkedHashSet<>(Arrays.asList(expectedLicenseIds));
    expectedLicenseLegalMetadataLicenseIds.addAll(rawReport.components.stream()
        .filter(c -> c.componentIdentifier != null)
        .filter(c -> !c.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER))
        .flatMap(component -> Stream.concat(Stream.concat(component.licenseData.declaredLicenses.stream(),
            component.licenseData.observedLicenses.stream()),
            component.licenseData.effectiveLicenses.stream()))
        .map(license -> license.licenseId)
        .collect(Collectors.toCollection(LinkedHashSet::new)));
    assertThat(licenseLegalMetadata).hasSize(expectedLicenseLegalMetadataLicenseIds.size());
    assertThat(licenseLegalMetadata)
        .extracting(license -> license.licenseId)
        .containsExactlyInAnyOrder(expectedLicenseLegalMetadataLicenseIds.toArray(new String[0]));
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
    return licenseIds.stream().map(this::createLicenseMetadataDTO).toList();
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
      Set<LicenseObligationDTO> legalLicenseObligations =
          getLicenseObligationByLicenseId(licenseLegalMetadata, lm.getLicenseId());
      lm.getLicenseObligations().forEach(lo -> {
        Optional<LicenseObligationDTO> legalLicenseObligation = legalLicenseObligations.stream()
            .filter(llo -> llo.getName().equals(lo.getName()))
            .findFirst();
        assertThat(legalLicenseObligation.isPresent())
            .withFailMessage("Legal Report Data did not contain License Obligation: " + lo.getName())
            .isTrue();
        assertThat(lo.getObligationTexts())
            .isEqualTo(legalLicenseObligation.get().getObligationTexts());
      });
    });
  }

  private Set<LicenseObligationDTO> getLicenseObligationByLicenseId(
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
            .filter(clc -> LegalComponentIdentifierUtil.removeClassifierAndExtension(clc.getComponentIdentifier())
                .equals(
                    LegalComponentIdentifierUtil
                        .removeClassifierAndExtension(lrc.componentIdentifier.toComponentIdentifier())))
            .flatMap(clc -> clc.getUniqueCopyrights().stream())
            .map(legalCopyrightDTO -> new ApiLicenseLegalCopyrightDTO(null,
                legalCopyrightDTO.getContent(),
                legalCopyrightDTO.getContentHash(),
                ComponentLegalPartStatus.ENABLED))
            .sorted(Comparator.comparing(lc -> lc.content))
            .toArray(ApiLicenseLegalCopyrightDTO[]::new)));
  }

  private void assertComponentLegalFiles(
      List<ApiLicenseLegalComponentDTO> licenseLegalComponents,
      Set<ComponentLegalFileDTO> componentLegalFiles)
  {
    licenseLegalComponents.forEach(lrc -> {
      assertThat(lrc.licenseLegalData.noticeFiles).usingRecursiveFieldByFieldElementComparator()
          .containsExactly(
              componentLegalFiles.stream()
                  .filter(clf -> LegalComponentIdentifierUtil.removeClassifierAndExtension(clf.getComponentIdentifier())
                      .equals(
                          LegalComponentIdentifierUtil
                              .removeClassifierAndExtension(lrc.componentIdentifier.toComponentIdentifier())))
                  .flatMap(clf -> clf.getLegalFiles().stream())
                  .filter(c -> c.getType().equals("NOTICE"))
                  .map(
                      legalFileDTO -> new ApiLicenseLegalFileDTO(null, legalFileDTO.getRelPath(),
                          legalFileDTO.getContent(),
                          legalFileDTO.getContentHash(), ComponentLegalPartStatus.ENABLED))
                  .toArray(ApiLicenseLegalFileDTO[]::new));
      assertThat(lrc.licenseLegalData.licenseFiles).usingRecursiveFieldByFieldElementComparator()
          .containsExactly(
              componentLegalFiles.stream()
                  .filter(clf -> LegalComponentIdentifierUtil.removeClassifierAndExtension(clf.getComponentIdentifier())
                      .equals(
                          LegalComponentIdentifierUtil
                              .removeClassifierAndExtension(lrc.componentIdentifier.toComponentIdentifier())))
                  .flatMap(clf -> clf.getLegalFiles().stream())
                  .filter(c -> c.getType().equals("LICENSE"))
                  .map(
                      legalFileDTO -> new ApiLicenseLegalFileDTO(null, legalFileDTO.getRelPath(),
                          legalFileDTO.getContent(),
                          legalFileDTO.getContentHash(), ComponentLegalPartStatus.ENABLED))
                  .toArray(ApiLicenseLegalFileDTO[]::new));
    });
  }

  private void assertComponentData(
      List<ApiLicenseLegalComponentDTO> components,
      ApiReportRawDataDTOV2 rawReport)
  {
    components.forEach(this::assertValidComponent);

    Map<String, ApiLicenseDataDTOV2> expectedComponentData = rawReport.components.stream()
        .filter(comp -> comp.displayName != null)
        .collect(Collectors.toMap(c -> c.displayName,
            c -> c.licenseData == null ? new ApiLicenseDataDTOV2() : c.licenseData, (l1, l2) -> l1));

    components
        .forEach(comp -> validateLicenseData(comp, comp.licenseLegalData, expectedComponentData.get(comp.displayName)));
  }

  private void assertValidComponent(ApiLicenseLegalComponentDTO component) {
    assertThat(component).satisfiesAnyOf(
        lrc -> assertThat(lrc.componentIdentifier).isNotNull(),
        lrc -> assertThat(lrc.packageUrl).isNotNull(),
        lrc -> assertThat(lrc.hash).isNotNull());
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

  private void assertObligations(
      final List<ApiLicenseLegalComponentDTO> components,
      final LicenseMetadataDTO[] licenseMetadata)
  {
    Map<String, LicenseMetadataDTO> licenseMetadataDTOMap = Arrays.stream(licenseMetadata)
        .collect(Collectors.toMap(LicenseMetadataDTO::getLicenseId, Function.identity()));
    for (ApiLicenseLegalComponentDTO componentDTO : components) {
      Set<String> obligationNames =
          componentDTO.licenseLegalData.obligations.stream()
              .map(ApiLicenseLegalObligationDTO::getName)
              .collect(
                  Collectors.toSet());
      Set<String> expectedObligationNames = componentDTO.licenseLegalData.effectiveLicenses.stream()
          .flatMap(l -> licenseMetadataDTOMap.get(l).getLicenseObligations().stream())
          .map(LicenseObligationDTO::getName)
          .collect(Collectors.toSet());

      assertThat(obligationNames).containsAll(expectedObligationNames);
    }
  }

  private <T> T getContent(String resource, Class<? extends T> type) throws Exception {
    return JsonUtils.parse(IOUtils.toString(getClass().getResource("/" + getClass().getSimpleName() + "/" + resource),
        StandardCharsets.UTF_8), type);
  }

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      ReportHelper.saveMockReport(
          insightWork,
          tempDir,
          "/" + getClass().getSimpleName() + "/report/",
          evaluation.getApplicationId(),
          evaluation.getScanId());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (innerSourceApplicationDAO.getByApplicationId(evaluation.getApplicationId()).isEmpty()) {
      tempEntity.newInnerSourceApplication(InnerSourceUtils.getVersionlessPackageUrl(INNER_SOURCE_COMPONENT_IDENTIFIER)
          .getPackageUrl(), applicationDAO.getById(evaluation.getApplicationId()));
    }
  }

  private void assertApplicationTelemetry(
      Application application,
      ApiReportRawDataDTOV2 rawReport,
      boolean includeInnerSource)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();

    final ApplicationLicenseUsageTelemetry applicationLicenseUsageTelemetry = new ApplicationLicenseUsageTelemetry(
        application.getId(),
        rawReport.components.stream()
            .filter(c -> Objects.isNull(c.componentIdentifier) || includeInnerSource
                || !c.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER))
            .map(component -> component.hash)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        rawReport.components.stream()
            .filter(c -> Objects.isNull(c.componentIdentifier) || includeInnerSource
                || !c.componentIdentifier.toComponentIdentifier().equals(INNER_SOURCE_COMPONENT_IDENTIFIER))
            .filter(component -> component.licenseData != null)
            .map(component -> component.licenseData)
            .flatMap(licenseData -> Stream.concat(
                Stream.concat(licenseData.declaredLicenses.stream(), licenseData.observedLicenses.stream()),
                licenseData.effectiveLicenses.stream()))
            .map(license -> license.licenseId)
            .collect(Collectors.toSet()));

    applicationLicenseUsageTelemetry.setRealApplicationId(application.getId());
    if (!configuration.getAdvanceReportingInsightsEnabled()) {
      applicationLicenseUsageTelemetry.setRealApplicationId(telemetryUtils.obfuscate(application.getId()));
    }

    expectedAttributes.put(ApplicationLicenseUsageTelemetry.ATTRIBUTE_NAME, applicationLicenseUsageTelemetry);

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.APPLICATION_LICENSE_USAGE);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).hasSize(1);
    assertThat(telemetryData.getAttributes().keySet().iterator().next())
        .isEqualTo(expectedAttributes.keySet().iterator().next());
    assertThat((ApplicationLicenseUsageTelemetry) telemetryData.getAttributes().values().iterator().next())
        .usingRecursiveComparison()
        .isEqualTo(expectedAttributes.values().iterator().next());
  }

  private void setUnlicensedForAdvancedLegalPack() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  private Triple<Application, Tag, PolicyEvaluation> setupApplicationDashboardEntities(
      String tagName,
      String stageTypeId)
  {
    return setupApplicationDashboardEntities(tagName, stageTypeId, System.currentTimeMillis());
  }

  private Triple<Application, Tag, PolicyEvaluation> setupApplicationDashboardEntities(
      String tagName,
      String stageTypeId,
      long scanTime)
  {
    Application app = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(app.getOrganizationId(), tagName);
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, TemporaryEntity.uuid(), new Date(scanTime));

    return new ImmutableTriple<>(app, tag, policyEvaluation);
  }

  private void assertLegalLicenseApplicationDashboardDTO(
      Application app,
      Tag tag,
      PolicyEvaluation latestPolicyEvaluation,
      ApiLicenseLegalApplicationDashboardDTO dto)
  {
    assertLegalLicenseApplicationDashboardDTO(app, tag, latestPolicyEvaluation, dto, 0, 0);
  }

  private void assertLegalLicenseApplicationDashboardDTO(
      Application app,
      Tag tag,
      PolicyEvaluation latestPolicyEvaluation,
      ApiLicenseLegalApplicationDashboardDTO dto,
      int componentsReviewedCount,
      int componentsTotalCount)
  {
    assertThat(dto.applicationId).isEqualTo(app.getId());
    assertThat(dto.applicationName).isEqualTo(app.getName());
    assertThat(dto.applicationPublicId).isEqualTo(app.getPublicId());
    assertThat(dto.applicationTagNames).containsExactly(tag.getName());
    assertThat(dto.lastScanTime).isEqualTo(latestPolicyEvaluation.getTime().getTime());
    assertThat(dto.stageTypeId).isEqualTo(latestPolicyEvaluation.getStageTypeId());
    assertThat(dto.stageTypeName).isEqualTo(StageTypes.getById(latestPolicyEvaluation.getStageTypeId()).getName());
    assertThat(dto.componentsReviewedCount).isEqualTo(componentsReviewedCount);
    assertThat(dto.componentsTotalCount).isEqualTo(componentsTotalCount);
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
      int applicationOccurrences,
      int componentsReviewedCount,
      int componentsTotalCount,
      String licenseThreatGroupName)
  {
    assertThat(dto.hash).isEqualTo(hash);
    assertThat(dto.displayName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(dto.licenses).doesNotContainNull();
    assertThat(dto.licenses)
        .flatExtracting(license -> license.licenseThreatGroups)
        .extracting(group -> group.licenseThreatGroupName)
        .containsOnly(licenseThreatGroupName);
    assertThat(dto.licenses).extracting(d -> d.licenseName).containsExactlyElementsOf(licenseNames);
    assertThat(dto.applicationOccurrences).isEqualTo(applicationOccurrences);
    assertThat(dto.reviewCompletedCount).isEqualTo(componentsReviewedCount);
    assertThat(dto.reviewTotalCount).isEqualTo(componentsTotalCount);
  }

  private void setupLicenseObligations(
      Application app,
      ComponentIdentifier componentIdentifier,
      List<String> licenses,
      ObligationStatus... obligationStatuses)
  {
    Set<LicenseObligationDTO> obligationDtos = new LinkedHashSet<>();
    for (int i = 0; i < obligationStatuses.length; i++) {
      if (obligationStatuses[i] != null) {
        tempEntity.newComponentObligation(componentIdentifier, app.getId(), "obligation" + i, "comment",
            obligationStatuses[i], "hash" + i);
        obligationDtos.add(new LicenseObligationDTO("obligation" + i, Collections.emptySet()));
      }
    }

    List<LicenseMetadataDTO> licenseMetadataDTOs = createLicenseMetadataDTOs(licenses);
    for (LicenseMetadataDTO licenseMetadataDTO : licenseMetadataDTOs) {
      licenseMetadataDTO.setLicenseObligations(obligationDtos);
    }

    lenient().doReturn(licenseMetadataDTOs)
        .when(mockApiLicenseLegalHdsService)
        .getLicenseMetadata(argThat(list -> list.containsAll(licenses)));
  }
}
