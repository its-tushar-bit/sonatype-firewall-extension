/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mock;

public class DocumentBuilderHelperTest
    extends AbstractComponentTest
{
  @Inject
  private DocumentBuilderHelper documentBuilderHelper;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAOMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private IndexingContext indexingContextMock;

  @Test
  public void testBuildApplicationStageSVDocs_IoExceptionIsSwallowed() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    eval.setApplicationId(app.getId());
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId())).thenReturn(
        eval);
    ApplicationReport mockApplicationReport = mock(ApplicationReport.class);
    doThrow(new IOException("IO error")).when(mockApplicationReport).exists();
    when(reportServiceMock.getReport(anyString(), anyString())).thenReturn(mockApplicationReport);

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_NotFoundExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new NotFoundException("Not found"));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_UncheckedIoExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new UncheckedIOException(new IOException("IO error")));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_WrappedIoExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new RuntimeException("Wrapped", new IOException("IO error")));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_WrappedNotFoundExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new RuntimeException("Wrapped", new NotFoundException("Not found")));

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_NonIoExceptionIsRethrown() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByApplicationIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
        .thenReturn(eval);
    when(reportServiceMock.getReport(anyString(), anyString()))
        .thenThrow(new IllegalStateException("Unexpected error"));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> documentBuilderHelper.buildApplicationStageSVDocs(
            indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList()))
        .withMessage("Unexpected error");
  }

  @Test
  public void testBuildOrganizationDocs_EmptyWhenMissingData() {
    assertThat(documentBuilderHelper.buildOrganizationDocs(indexingContextMock, null)).isEmpty();
    assertThat(documentBuilderHelper.buildOrganizationDocs(indexingContextMock, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationDocs_EmptyWhenMissingData() {
    assertThat(documentBuilderHelper.buildApplicationDocs(indexingContextMock, null)).isEmpty();
    assertThat(documentBuilderHelper.buildApplicationDocs(indexingContextMock, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildDocument_NullWhenMissingData() {
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (Organization) null)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (Application) null)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (Tag) null)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (Label) null)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (Policy) null)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (ThirdPartySbomMetadata) null)).isNull();

    // indexingContextMock will return null for owner lookups
    assertThat(
        documentBuilderHelper.buildDocument(indexingContextMock, tempEntity.newApplicationWithParent())).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock,
        tempEntity.newTag(tempEntity.newOrganization().getId()))).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock,
        tempEntity.newLabel(tempEntity.newOrganization().getId()))).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock,
        tempEntity.newPolicy(tempEntity.newOrganization().getId()))).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock,
        tempEntity.newThirdPartySbomMetadata(tempEntity.newApplicationWithParent().getId(),
            ThirdPartySbomMetadataStatus.ACTIVE, "filename"))).isNull();
  }

  @Test
  public void testBuildApplicationSVDocs_EmptyWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Map<Organization, Collection<Organization>> parentOrgsMap = new HashMap<>();

    assertThat(
        documentBuilderHelper.buildApplicationSVDocs(indexingContextMock, null, application, parentOrgsMap)).isEmpty();
    assertThat(
        documentBuilderHelper.buildApplicationSVDocs(indexingContextMock, organization, null, parentOrgsMap)).isEmpty();
    assertThat(
        documentBuilderHelper.buildApplicationSVDocs(indexingContextMock, organization, application, null)).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_EmptyWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();

    assertThat(
        documentBuilderHelper.buildApplicationStageSVDocs(indexingContextMock, null, application, StageTypes.BUILD,
            parentOrgs)).isEmpty();
    assertThat(
        documentBuilderHelper.buildApplicationStageSVDocs(indexingContextMock, organization, null, StageTypes.BUILD,
            parentOrgs)).isEmpty();
    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(indexingContextMock, organization, application,
        StageTypes.BUILD, null)).isEmpty();
  }

  @Test
  public void testBuildApplicationComponentVulnerabilityDocuments_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();

    assertThat(documentBuilderHelper.buildApplicationComponentVulnerabilityDocuments(indexingContextMock, null,
        parentOrgs, application, StageTypes.BUILD, "scan-id", mock(Component.class))).isEmpty();
    assertThat(documentBuilderHelper.buildApplicationComponentVulnerabilityDocuments(indexingContextMock, organization,
        null, application, StageTypes.BUILD, "scan-id", mock(Component.class))).isEmpty();
    assertThat(documentBuilderHelper.buildApplicationComponentVulnerabilityDocuments(indexingContextMock, organization,
        parentOrgs, null, StageTypes.BUILD, "scan-id", mock(Component.class))).isEmpty();
    assertThat(documentBuilderHelper.buildApplicationComponentVulnerabilityDocuments(indexingContextMock, organization,
        parentOrgs, application, StageTypes.BUILD, "scan-id", null)).isEmpty();
  }

  @Test
  public void testBuildSbomSVDocs_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Map<Organization, Collection<Organization>> parentOrgsMap = new HashMap<>();

    assertThat(
        documentBuilderHelper.buildSbomSVDocs(null, application, parentOrgsMap)).isEmpty();
    assertThat(
        documentBuilderHelper.buildSbomSVDocs(organization, null, parentOrgsMap)).isEmpty();
    assertThat(
        documentBuilderHelper.buildSbomSVDocs(organization, application, null)).isEmpty();
  }

  @Test
  public void testBuildSbomVersionSVDocs_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "filename");

    assertThat(documentBuilderHelper.buildSbomVersionSVDocs(null, application, sbomMetadata, parentOrgs)).isEmpty();
    assertThat(documentBuilderHelper.buildSbomVersionSVDocs(organization, null, sbomMetadata, parentOrgs)).isEmpty();
    assertThat(documentBuilderHelper.buildSbomVersionSVDocs(organization, application, null, parentOrgs)).isEmpty();
    assertThat(documentBuilderHelper.buildSbomVersionSVDocs(organization, application, sbomMetadata, null)).isEmpty();
  }

  @Test
  public void testBuildSbomFileCoordinateSVDocs_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "filename");

    assertThat(documentBuilderHelper.buildSbomFileCoordinateSVDocs(null, application, sbomMetadata, parentOrgs,
        mock(ThirdPartyFileCoordinate.class))).isEmpty();
    assertThat(documentBuilderHelper.buildSbomFileCoordinateSVDocs(organization, null, sbomMetadata, parentOrgs,
        mock(ThirdPartyFileCoordinate.class))).isEmpty();
    assertThat(documentBuilderHelper.buildSbomFileCoordinateSVDocs(organization, application, null, parentOrgs,
        mock(ThirdPartyFileCoordinate.class))).isEmpty();
    assertThat(documentBuilderHelper.buildSbomFileCoordinateSVDocs(organization, application, sbomMetadata, null,
        mock(ThirdPartyFileCoordinate.class))).isEmpty();
    assertThat(
        documentBuilderHelper.buildSbomFileCoordinateSVDocs(organization, application, sbomMetadata, parentOrgs, null))
            .isEmpty();
  }

  @Test
  public void testBuildDocument_ComponentWithStageType_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();

    assertThat(documentBuilderHelper.buildDocument(null, parentOrgs, application, StageTypes.BUILD, "scan-id",
        mock(Component.class))).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, null, application, StageTypes.BUILD, "scan-id",
        mock(Component.class))).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, parentOrgs, null, StageTypes.BUILD, "scan-id",
        mock(Component.class))).isNull();
    assertThat(
        documentBuilderHelper.buildDocument(organization, parentOrgs, application, StageTypes.BUILD, "scan-id", null))
            .isNull();
  }

  @Test
  public void testBuildDocument_SbomComponent_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "filename");

    assertThat(documentBuilderHelper.buildDocument(null, application, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, null, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, null,
        mock(ThirdPartyFileCoordinate.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, sbomMetadata, null, parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), null)).isNull();
  }

  @Test
  public void testBuildDocument_SbomComponentWithVulnerability_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "filename");

    assertThat(documentBuilderHelper.buildDocument(null, application, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), mock(ThirdPartyCoordinateSecurity.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, null, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), mock(ThirdPartyCoordinateSecurity.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, null,
        mock(ThirdPartyFileCoordinate.class), mock(ThirdPartyCoordinateSecurity.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, sbomMetadata, null,
        mock(ThirdPartyCoordinateSecurity.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), null, parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(organization, application, sbomMetadata,
        mock(ThirdPartyFileCoordinate.class), mock(ThirdPartyCoordinateSecurity.class), null)).isNull();
  }

  @Test
  public void testBuildDocument_ComponentWithVulnerability_NullWhenMissingData() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();

    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, null, application, StageTypes.BUILD, "scan-id",
        mock(Component.class), mock(SecurityVulnerability.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, organization, null, StageTypes.BUILD, "scan-id",
        mock(Component.class), mock(SecurityVulnerability.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, organization, application, StageTypes.BUILD,
        "scan-id", null, mock(SecurityVulnerability.class), parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, organization, application, StageTypes.BUILD,
        "scan-id", mock(Component.class), null, parentOrgs)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, organization, application, StageTypes.BUILD,
        "scan-id", mock(Component.class), mock(SecurityVulnerability.class), null)).isNull();
  }

  @Test
  public void testBuildDocument_SbomComponentWithVulnerability_RoundsFloatSeverity() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = Collections.singletonList(organization);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "test.json");

    ThirdPartyFileCoordinate fileCoordinate = mock(ThirdPartyFileCoordinate.class);
    when(fileCoordinate.getPackageUrl()).thenReturn("pkg:maven/org.example/test@1.0.0");
    when(fileCoordinate.getHash()).thenReturn("someHash");

    // Use a severity value that requires rounding (would have thrown with RoundingMode.UNNECESSARY)
    ThirdPartyCoordinateSecurity coordinateSecurity = mock(ThirdPartyCoordinateSecurity.class);
    when(coordinateSecurity.getRefId()).thenReturn("CVE-2024-12345");
    when(coordinateSecurity.getSeverity()).thenReturn(7.5555); // This requires rounding to 2 decimal places
    when(coordinateSecurity.getDescription()).thenReturn("Test vulnerability description");

    // This should not throw an exception and should properly round the severity
    assertThat(documentBuilderHelper.buildDocument(organization, application, sbomMetadata, fileCoordinate,
        coordinateSecurity, parentOrgs)).isNotNull();
  }
}
