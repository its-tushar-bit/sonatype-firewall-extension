/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.junit.Test;
import org.mockito.Mock;

public class DocumentBuilderHelperTest
    extends AbstractComponentTest
{
  @Inject
  private DocumentBuilderHelper documentBuilderHelper;

  @Inject
  private com.sonatype.insight.brain.dataaccess.OwnerDAO ownerDAO;

  @Inject
  private com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO waiverDAO;

  @Inject
  private PolicyWaiverReasonDAO waiverReasonDAO;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAOMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private IndexingContext indexingContextMock;

  @org.junit.Before
  public void stubIndexingContextAncestorWalk() {
    // getAncestorOrgIds now encapsulates the OwnerDAO.walkHierarchy walk (memoized per run in the
    // real IndexingContext). The mock has no cache, so delegate to the real ownerDAO walk here so
    // the closure tests exercise the genuine ancestor chain rather than an empty stub result.
    org.mockito.Mockito.lenient()
        .when(indexingContextMock.getAncestorOrgIds(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(inv -> {
          com.sonatype.insight.brain.model.Organization org = inv.getArgument(0);
          if (org == null) {
            return java.util.List.of();
          }
          java.util.List<String> ids = new java.util.ArrayList<>();
          ownerDAO.walkHierarchy(org).forEach(o -> ids.add(o.getId()));
          return ids;
        });
  }

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
  public void allowedContextIds_rootOrganization_omitsRootSentinel() {
    Organization root = tempEntity.newOrganization();

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, root);

    assertThat(itemTypeOf(doc)).isEqualTo(ItemType.ORGANIZATION.name());
    // Root sentinel is omitted from the closure; callers with READ on root bypass permission
    // filtering entirely via null-filter in AbstractSearchIndexClient.buildAllowedContextIdsLuceneFilter.
    assertThat(stringValuesOf(doc, FieldIdentifier.ALLOWED_CONTEXT_IDS))
        .containsOnly(root.getId())
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void allowedContextIds_nestedOrganization_includesAncestorsButOmitsRootSentinel() {
    Organization grandparent = tempEntity.newOrganization();
    Organization parent = tempEntity.newOrganization(grandparent);
    Organization child = tempEntity.newOrganization(parent);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, child);

    assertThat(stringValuesOf(doc, FieldIdentifier.ALLOWED_CONTEXT_IDS))
        .containsOnly(child.getId(), parent.getId(), grandparent.getId())
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void allowedContextIds_applicationOwned_includesAppIdAndOrgAncestorsButOmitsRootSentinel() {
    Organization parent = tempEntity.newOrganization();
    Organization child = tempEntity.newOrganization(parent);
    Application app = tempEntity.newApplicationWithParent(child);

    // Wire the indexing-context lookup so buildDocument can resolve the owner org.
    when(indexingContextMock.getOwner(child.getId())).thenReturn(child);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(itemTypeOf(doc)).isEqualTo(ItemType.APPLICATION.name());
    assertThat(stringValuesOf(doc, FieldIdentifier.ALLOWED_CONTEXT_IDS))
        .containsOnly(app.getId(), child.getId(), parent.getId())
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void allowedContextIds_policyViolation_includesAppIdAndPrecomputedAncestorChain() {
    Organization root = tempEntity.newOrganization();
    Organization child = tempEntity.newOrganization(root);
    Application app = tempEntity.newApplicationWithParent(child);

    // The PV / SV / NV / LV build paths receive a precomputed ancestor chain; this is the
    // shape produced by OwnerDAO.walkHierarchy(Organization) in AbstractSearchIndexClient.
    // `root` here is a regular test org (random UUID), NOT the ROOT_ORGANIZATION_ID sentinel, so
    // including root.getId() in the expected closure is correct (sentinel stripping is covered by
    // allowedContextIds_precomputedAncestorChain_stripsSentinelIds).
    List<Organization> ancestorChain = Arrays.asList(child, root);

    List<String> closure = documentBuilderHelper.computeAllowedContextIds(ancestorChain, app.getId());

    assertThat(closure).containsExactly(app.getId(), child.getId(), root.getId());
  }

  @Test
  public void allowedContextIds_precomputedAncestorChain_stripsSentinelIds() {
    // The Collection overload must apply the same sentinel-exclusion as the Organization-walk path:
    // an ancestor chain that includes the ROOT_ORGANIZATION_ID / GLOBAL_CONTEXT_ID sentinels must
    // have them stripped, since holders of either bypass permission filtering entirely.
    Organization realOrg = tempEntity.newOrganization();

    Organization rootSentinel = new Organization("root-sentinel");
    rootSentinel.setId(Organization.ROOT_ORGANIZATION_ID);

    Organization globalSentinel = new Organization("global-sentinel");
    globalSentinel.setId(MembershipMapping.GLOBAL_CONTEXT_ID);

    List<Organization> ancestorChain = Arrays.asList(realOrg, rootSentinel, globalSentinel);

    List<String> closure = documentBuilderHelper.computeAllowedContextIds(ancestorChain, "app-id");

    assertThat(closure)
        .containsExactly("app-id", realOrg.getId())
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID, MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void allowedContextIds_helper_handlesNullsGracefully() {
    // (org, app) variants: both fields nullable. Null org short-circuits before touching the
    // indexing context's ancestor cache.
    assertThat(documentBuilderHelper.computeAllowedContextIds(indexingContextMock, null, null)).isEmpty();
    assertThat(documentBuilderHelper.computeAllowedContextIds(indexingContextMock, null, "app-id"))
        .containsExactly("app-id");

    // (ancestorOrgs, app) variant
    assertThat(documentBuilderHelper.computeAllowedContextIds((Collection<Organization>) null, null)).isEmpty();
    assertThat(documentBuilderHelper.computeAllowedContextIds((Collection<Organization>) null, "app-id"))
        .containsExactly("app-id");
  }

  @Test
  public void buildDocument_applicationOwnerIsNonOrganization_returnsNull() {
    Application app = tempEntity.newApplicationWithParent();
    when(indexingContextMock.getOwner(app.getOrganizationId())).thenReturn(mock(Repository.class));

    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, app)).isNull();
  }

  @Test
  public void computeAllowedContextIdsForOwner_orphanApp_failsClosedWithEmptyClosureAndWarnsOnce() {
    // An orphan app (owning org unresolvable) fails closed: the closure is empty, so the doc is
    // suppressed from permission-filtered results rather than left app-only-visible. Both
    // invocations use the same app id; the WARN is emitted at most once and the dedupe does not
    // change the (empty) closure semantics.
    Application orphan = tempEntity.newApplicationWithParent();
    when(indexingContextMock.getOwner(orphan.getOrganizationId())).thenReturn(null);

    List<String> firstClosure = documentBuilderHelper.computeAllowedContextIdsForOwner(indexingContextMock, orphan);
    List<String> secondClosure = documentBuilderHelper.computeAllowedContextIdsForOwner(indexingContextMock, orphan);

    assertThat(firstClosure).isEmpty();
    assertThat(secondClosure).isEmpty();
  }

  @Test
  public void computeAllowedContextIdsForOwner_unsupportedOwnerType_returnsEmpty() {
    Owner unsupported = mock(Owner.class);

    assertThat(documentBuilderHelper.computeAllowedContextIdsForOwner(indexingContextMock, unsupported))
        .isEmpty();
  }

  @Test
  public void allowedContextIds_precomputedMap_producesSameClosureAsWalkHierarchyPath() {
    // The precomputed-map path must produce the same closure as the on-demand walkHierarchy path.
    // We assert closure equivalence rather than DAO call counts (spying ownerDAO isn't clean here).
    Organization root = tempEntity.newOrganization();
    Organization child = tempEntity.newOrganization(root);
    Application app = tempEntity.newApplicationWithParent(child);

    when(indexingContextMock.getOwner(child.getId())).thenReturn(child);

    Map<Organization, Collection<Organization>> parents = new HashMap<>();
    parents.put(root, List.of(root));
    parents.put(child, List.of(child, root));

    Document orgDocMapPath = documentBuilderHelper.buildDocument(indexingContextMock, child, parents);
    Document orgDocWalkPath = documentBuilderHelper.buildDocument(indexingContextMock, child, null);
    Document appDocMapPath = documentBuilderHelper.buildDocument(indexingContextMock, app, parents);
    Document appDocWalkPath = documentBuilderHelper.buildDocument(indexingContextMock, app, null);

    assertThat(stringValuesOf(orgDocMapPath, FieldIdentifier.ALLOWED_CONTEXT_IDS))
        .isEqualTo(stringValuesOf(orgDocWalkPath, FieldIdentifier.ALLOWED_CONTEXT_IDS));
    assertThat(stringValuesOf(appDocMapPath, FieldIdentifier.ALLOWED_CONTEXT_IDS))
        .isEqualTo(stringValuesOf(appDocWalkPath, FieldIdentifier.ALLOWED_CONTEXT_IDS));
  }

  @Test
  public void allowedContextIds_orgScopedPolicy_includesOrgAncestorsButOmitsRootSentinel() {
    Organization root = tempEntity.newOrganization();
    Organization child = tempEntity.newOrganization(root);
    Policy policy = tempEntity.newPolicy(child.getId());

    when(indexingContextMock.getOwner(child.getId())).thenReturn(child);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, policy);

    assertThat(itemTypeOf(doc)).isEqualTo(ItemType.POLICY.name());
    assertThat(stringValuesOf(doc, FieldIdentifier.ALLOWED_CONTEXT_IDS))
        .containsOnly(child.getId(), root.getId())
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID);
  }

  private static String itemTypeOf(Document doc) {
    return doc.get(FieldIdentifier.ITEM_TYPE.label);
  }

  private static Set<String> stringValuesOf(Document doc, FieldIdentifier field) {
    IndexableField[] fields = doc.getFields(field.label);
    return Arrays.stream(fields)
        .map(IndexableField::stringValue)
        .filter(v -> v != null)
        .collect(Collectors.toSet());
  }

  @Test
  public void testBuildDocument_PolicyWaiver_NullWhenMissingData() {
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (PolicyWaiver) null)).isNull();
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, (AutoPolicyWaiver) null)).isNull();
  }

  @Test
  public void testBuildDocument_PolicyWaiver_ownerNotResolvable_returnsNull() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(null);

    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, waiver)).isNull();
  }

  @Test
  public void testBuildDocument_ManualPolicyWaiver_populatesFullFieldSet() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), "my policy", 8);
    PolicyWaiver waiver = tempEntity.newWaiverWithReason("hash", policy.getId(), organization.getId(),
        Collections.emptyList(), "my comment", "type", "my reason");
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);

    assertThat(itemTypeOf(doc)).isEqualTo(ItemType.POLICY_WAIVER.name());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)).isEqualTo(waiver.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("my policy");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label)).isEqualTo(policy.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_REASON.label)).isEqualTo("my reason");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_COMMENT.label)).isEqualTo("my comment");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label)).isEqualTo(organization.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label)).isEqualTo("ORGANIZATION");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_WAIVED_BY.label)).isEqualTo(waiver.getCreatorName());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label)).isNotBlank();
    assertThat(doc.getField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label).numericValue().intValue())
        .isEqualTo(8);
  }

  @Test
  public void testBuildDocument_ManualPolicyWaiverWithPreloadedPolicy_usesPassedPolicyNotDbLookup() {
    Organization organization = tempEntity.newOrganization();
    Policy dbPolicy = tempEntity.newPolicy(organization.getId(), "db name", 3);
    PolicyWaiver waiver = tempEntity.newWaiver("hash", dbPolicy.getId(), organization.getId(), "comment");
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);

    // A detached Policy with the same id but a different name/threat. If the preloaded-policy overload
    // re-fetched by id it would surface the DB values; instead the doc must reflect this passed policy,
    // proving the redundant per-waiver policyDAO.getById lookup is gone.
    Policy preloaded = new Policy();
    preloaded.setId(dbPolicy.getId());
    preloaded.setName("preloaded name");
    preloaded.setThreatLevel(9);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver, preloaded);

    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("preloaded name");
    assertThat(doc.getField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label).numericValue().intValue())
        .isEqualTo(9);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label)).isEqualTo(dbPolicy.getId());
  }

  @Test
  public void testBuildDocument_ContainerImageWaiver_returnsNull() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");
    waiver.setForContainerImage(true);

    // Container-image filtering runs before owner resolution, so getOwner is intentionally not stubbed.
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, waiver)).isNull();

    waiver.setForContainerImage(false);
    waiver.setForContainerImageComponent(true);
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, waiver)).isNull();
  }

  @Test
  public void testBuildDocument_RepositoryOwnerWaiver_returnsNull() {
    // v1 indexes only app/org-scoped waivers; a repository-owner waiver has no allowedContextIds
    // closure, so it is excluded rather than indexed with empty (invisible) permissions.
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(mock(Repository.class));

    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, waiver)).isNull();

    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(organization.getId(), 5, true, false);
    when(indexingContextMock.getOwner(autoWaiver.getOwnerId())).thenReturn(mock(Repository.class));
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, autoWaiver)).isNull();
  }

  @Test
  public void testBuildDocument_AutoPolicyWaiver_populatesSyntheticTitleAndSubset() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(organization.getId(), 9, true, false);
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);

    assertThat(itemTypeOf(doc)).isEqualTo(ItemType.POLICY_WAIVER.name());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)).isEqualTo(waiver.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).contains("Auto-waiver").contains("9");
    assertThat(doc.getField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label).numericValue().intValue())
        .isEqualTo(9);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label)).isEqualTo(organization.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label)).isEqualTo("ORGANIZATION");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label)).isNotBlank();
    // Auto-waivers carry no policy id, reason, comment, or expiry.
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_REASON.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_COMMENT.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label)).isNull();
  }

  @Test
  public void buildPolicyWaiverDocs_indexesBothKindsAndExcludesContainerImageWaivers() {
    Organization organization = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);
    Policy policy = tempEntity.newPolicy(organization.getId(), "resolvable policy", 6);
    PolicyWaiver manualWaiver =
        tempEntity.newWaiver("h-manual", policy.getId(), organization.getId(), "manual");
    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(organization.getId(), 7, true, false);
    // Container-image waiver: excluded up front, so it must not appear in the reindex docs.
    PolicyWaiver containerWaiver =
        tempEntity.newWaiver("h-container", policy.getId(), organization.getId(), "container");
    containerWaiver.setForContainerImage(true);
    try (var tx = waiverDAO.createTransactionContext()) {
      tx.begin();
      waiverDAO.updateForRenewal(tx, containerWaiver);
      tx.commit();
    }

    List<Document> docs = documentBuilderHelper.buildPolicyWaiverDocs(indexingContextMock);

    Document manualDoc = waiverDocById(docs, manualWaiver.getId());
    assertThat(manualDoc).isNotNull();
    assertThat(manualDoc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("resolvable policy");

    Document autoDoc = waiverDocById(docs, autoWaiver.getId());
    assertThat(autoDoc).isNotNull();
    assertThat(autoDoc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).contains("Auto-waiver");

    assertThat(waiverDocById(docs, containerWaiver.getId())).isNull();
  }

  private static Document waiverDocById(List<Document> docs, String waiverId) {
    return docs.stream()
        .filter(doc -> waiverId.equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)))
        .findFirst()
        .orElse(null);
  }

  @Test
  public void buildPolicyWaiverDocsForPolicy_preservesReasonAndPolicyFieldsAndExcludesContainerImage() {
    Organization organization = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);
    Policy policy = tempEntity.newPolicy(organization.getId(), "db name", 3);
    PolicyWaiver waiverA = tempEntity.newWaiverWithReason("h-a", policy.getId(), organization.getId(),
        Collections.emptyList(), "comment a", "type", "reason a");
    PolicyWaiver waiverB = tempEntity.newWaiverWithReason("h-b", policy.getId(), organization.getId(),
        Collections.emptyList(), "comment b", "type", "reason b");
    PolicyWaiver containerWaiver =
        tempEntity.newWaiver("h-container", policy.getId(), organization.getId(), "container");
    containerWaiver.setForContainerImage(true);

    // A detached policy with a different name/threat: the rebuilt docs must reflect this passed
    // policy, proving the per-waiver policyDAO.getById lookup is not re-run.
    Policy preloaded = new Policy();
    preloaded.setId(policy.getId());
    preloaded.setName("preloaded name");
    preloaded.setThreatLevel(9);

    List<Document> docs = documentBuilderHelper.buildPolicyWaiverDocsForPolicy(
        indexingContextMock, List.of(waiverA, waiverB, containerWaiver), preloaded);

    Document docA = waiverDocById(docs, waiverA.getId());
    Document docB = waiverDocById(docs, waiverB.getId());
    assertThat(docA).isNotNull();
    assertThat(docB).isNotNull();
    assertThat(docA.get(FieldIdentifier.POLICY_WAIVER_REASON.label)).isEqualTo("reason a");
    assertThat(docB.get(FieldIdentifier.POLICY_WAIVER_REASON.label)).isEqualTo("reason b");
    assertThat(docA.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("preloaded name");
    assertThat(docA.getField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label).numericValue().intValue())
        .isEqualTo(9);
    // Container-image waivers are never indexed on the Global Search surface.
    assertThat(waiverDocById(docs, containerWaiver.getId())).isNull();
  }

  @Test
  public void buildPolicyWaiverDocsForPolicy_batchLoadsReasonsOnceInsteadOfPerWaiver() {
    Organization organization = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);
    Policy policy = tempEntity.newPolicy(organization.getId(), "policy", 4);
    PolicyWaiver waiverA = tempEntity.newWaiverWithReason("h-a", policy.getId(), organization.getId(),
        Collections.emptyList(), "comment a", "type", "reason a");
    PolicyWaiver waiverB = tempEntity.newWaiverWithReason("h-b", policy.getId(), organization.getId(),
        Collections.emptyList(), "comment b", "type", "reason b");
    PolicyWaiver waiverC = tempEntity.newWaiverWithReason("h-c", policy.getId(), organization.getId(),
        Collections.emptyList(), "comment c", "type", "reason c");

    // Spy the reason DAO so the reason resolution strategy is observable: the batch path must issue
    // one getAllByIds and never a per-waiver getById, which is the whole point of the N+1 fix.
    PolicyWaiverReasonDAO spyReasonDAO = spy(waiverReasonDAO);
    org.springframework.test.util.ReflectionTestUtils.setField(
        documentBuilderHelper, "policyWaiverReasonDAO", spyReasonDAO);
    try {
      List<Document> docs = documentBuilderHelper.buildPolicyWaiverDocsForPolicy(
          indexingContextMock, List.of(waiverA, waiverB, waiverC), policy);

      assertThat(docs).hasSize(3);
      verify(spyReasonDAO, times(1)).getAllByIds(anyList());
      verify(spyReasonDAO, never()).getById(anyString());
    }
    finally {
      org.springframework.test.util.ReflectionTestUtils.setField(
          documentBuilderHelper, "policyWaiverReasonDAO", waiverReasonDAO);
    }
  }

  @Test
  public void testBuildDocument_PolicyWaiverCreatedAt_sortsChronologicallyAsString() {
    // Fixed-width millis form: a whole-second time and a sub-second time must order chronologically
    // as strings (Instant.toString() dropped the .000 on whole seconds, breaking lexicographic sort).
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId());
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);

    PolicyWaiver wholeSecond = tempEntity.newWaiver("hash1", policy.getId(), organization.getId(), "c1");
    wholeSecond.setCreateTime(new java.util.Date(1_000L));
    PolicyWaiver subSecond = tempEntity.newWaiver("hash2", policy.getId(), organization.getId(), "c2");
    subSecond.setCreateTime(new java.util.Date(1_001L));

    String earlier = documentBuilderHelper.buildDocument(indexingContextMock, wholeSecond)
        .get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label);
    String later = documentBuilderHelper.buildDocument(indexingContextMock, subSecond)
        .get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label);

    assertThat(earlier).isEqualTo("1970-01-01T00:00:01.000Z");
    assertThat(later).isEqualTo("1970-01-01T00:00:01.001Z");
    assertThat(earlier.compareTo(later)).isLessThan(0);
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
