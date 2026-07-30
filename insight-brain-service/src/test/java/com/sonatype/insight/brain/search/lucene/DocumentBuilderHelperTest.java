/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
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
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.junit.Before;
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
  private com.sonatype.insight.brain.search.ConversionHelper conversionHelper;

  @Inject
  private com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO waiverDAO;

  @Inject
  private PolicyWaiverReasonDAO waiverReasonDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAOMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private IndexingContext indexingContextMock;

  @Before
  public void stubIndexingContextAncestorWalk() {
    // getAncestorOrgIds now encapsulates the OwnerDAO.walkHierarchy walk (memoized per run in the
    // real IndexingContext). The mock has no cache, so delegate to the real ownerDAO walk here so
    // the closure tests exercise the genuine ancestor chain rather than an empty stub result.
    lenient()
        .when(indexingContextMock.getAncestorOrgIds(any()))
        .thenAnswer(inv -> {
          Organization org = inv.getArgument(0);
          if (org == null) {
            return List.of();
          }
          List<String> ids = new ArrayList<>();
          ownerDAO.walkHierarchy(org).forEach(o -> ids.add(o.getId()));
          return ids;
        });
    lenient()
        .when(indexingContextMock.getLicenseNameById())
        .thenReturn(new HashMap<>(Map.of("license-id-1", "")));
  }

  /**
   * The two app rollups are memoized on the real IndexingContext; the mock has no cache, so run the
   * loader inline against the (real or mocked) DAOs, alongside the category-names loader passthrough.
   */
  @Before
  @SuppressWarnings("unchecked")
  public void stubIndexingContextRollupLoaders() {
    lenient()
        .when(indexingContextMock.getLatestEvaluationEpochMsByApp(anySet(), any()))
        .thenAnswer(inv -> {
          Set<String> ids = inv.getArgument(0);
          Function<Set<String>, Map<String, Long>> loader = inv.getArgument(1);
          return loader.apply(ids);
        });
    lenient()
        .when(indexingContextMock.getViolationRollupByApp(anySet(), any()))
        .thenAnswer(inv -> {
          Set<String> ids = inv.getArgument(0);
          Function<Set<String>, Map<String, IndexingContext.ViolationRollup>> loader = inv.getArgument(1);
          return loader.apply(ids);
        });
    // Category names are memoized on the real IndexingContext; the mock has no cache, so route the
    // loader inline (it now calls TagDAO.getByApplicationIdsGrouped) so H2-seeded tags are read back.
    lenient()
        .when(indexingContextMock.getCategoryNamesByApp(anySet(), any()))
        .thenAnswer(inv -> {
          Set<String> ids = inv.getArgument(0);
          Function<Set<String>, Map<String, List<String>>> loader = inv.getArgument(1);
          return loader.apply(ids);
        });
  }

  @Test
  public void testBuildApplicationStageSVDocs_IoExceptionIsSwallowed() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    eval.setOwnerId(app.getId());
    when(policyEvaluationDAOMock.getLastByOwnerIdAndStageId(app.getId(), StageTypes.BUILD.getId())).thenReturn(
        eval);
    LifecycleReport mockLifecycleReport = mock(LifecycleReport.class);
    doThrow(new IOException("IO error")).when(mockLifecycleReport).exists();
    when(reportServiceMock.getReport(anyString(), anyString())).thenReturn(mockLifecycleReport);

    assertThat(documentBuilderHelper.buildApplicationStageSVDocs(
        indexingContextMock, org, app, StageTypes.BUILD, Collections.emptyList())).isEmpty();
  }

  @Test
  public void testBuildApplicationStageSVDocs_NotFoundExceptionIsSwallowed() {
    Application app = tempEntity.newApplicationWithParent();
    Organization org = tempEntity.newOrganization();
    PolicyEvaluation eval = new PolicyEvaluation();
    eval.setScanId("scan-id");
    when(policyEvaluationDAOMock.getLastByOwnerIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
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
    when(policyEvaluationDAOMock.getLastByOwnerIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
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
    when(policyEvaluationDAOMock.getLastByOwnerIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
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
    when(policyEvaluationDAOMock.getLastByOwnerIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
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
    when(policyEvaluationDAOMock.getLastByOwnerIdAndStageId(app.getId(), StageTypes.BUILD.getId()))
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
  public void testBuildApplicationDoc_denormalizesLatestEvaluationTimeAndCategories() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    Tag tag = tempEntity.newTag(org.getId(), "Finance");
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    when(indexingContextMock.getOwner(app.getOrganizationId())).thenReturn(org);

    long buildMs = 1_700_000_000_000L;
    long releaseMs = 1_800_000_000_000L;
    PolicyEvaluation buildEval = new PolicyEvaluation();
    buildEval.setOwnerId(app.getId());
    buildEval.setTime(new Date(buildMs));
    PolicyEvaluation releaseEval = new PolicyEvaluation();
    releaseEval.setOwnerId(app.getId());
    releaseEval.setTime(new Date(releaseMs));
    // The rollup loader batch-loads the per-app latest evaluations in one query and takes the max
    // time per app across the returned rows (release > build).
    when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(eq(Set.of(app.getId())), anySet()))
        .thenReturn(List.of(buildEval, releaseEval));

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(doc).isNotNull();
    // Latest = max across stages (release), not build.
    assertThat(doc.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(String.valueOf(releaseMs));
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).contains("Finance");
  }

  @Test
  public void testBuildApplicationDoc_neverEvaluatedApp_omitsEvaluationFields() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);

    when(indexingContextMock.getOwner(app.getOrganizationId())).thenReturn(org);
    // No evaluations -> the batch load returns an empty list, so the doc omits the field.
    stubNoEvaluationsByDefault(app.getId());

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(doc).isNotNull();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label)).isEmpty();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label)).isEmpty();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();
  }

  @Test
  public void testBuildApplicationDoc_nullId_doesNotThrowAndOmitsRollupFields() {
    Organization org = tempEntity.newOrganization();
    Application persistedApp = tempEntity.newApplicationWithParent(org);
    // A category on the persisted app: if the null-id guard did not skip the id-keyed lookups the
    // category (and Set.of(null)) would surface; with a null id all id-keyed fields must be omitted.
    Tag tag = tempEntity.newTag(org.getId(), "Finance");
    tempEntity.newApplicationTag(persistedApp.getId(), tag.getId());

    // Same app but reporting a null id (the incremental single-app path used to call Set.of(null)).
    Application app = spy(persistedApp);
    when(app.getId()).thenReturn(null);

    when(indexingContextMock.getOwner(app.getOrganizationId())).thenReturn(org);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(doc).isNotNull();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_ID.label)).isEmpty();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label)).isEmpty();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label)).isEmpty();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();
  }

  @Test
  public void testBuildApplicationDoc_rollsUpUnfixedViolationsByStageAndSeverity() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);

    // Two unfixed violations on the BUILD stage: one CRITICAL (level 9), one SEVERE (level 5).
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-rollup", new Date(), "commit-1");
    tempEntity.newPolicyViolation(eval, tempEntity.newPolicy(org.getId(), "crit-policy", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    tempEntity.newPolicyViolation(eval, tempEntity.newPolicy(org.getId(), "sev-policy", 5), 5,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    when(indexingContextMock.getOwner(app.getOrganizationId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(doc).isNotNull();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:critical:1", "build:severe:1");
  }

  @Test
  public void testBuildApplicationDoc_stageSeverityRollupExcludesWaivedViolations() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);

    // Two CRITICAL (level 9) violations on the same BUILD stage/severity bucket: one active, one
    // waived. The pill must count only the active one — a waived violation is not an active threat.
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-waived", new Date(), "commit-1");
    tempEntity.newPolicyViolation(eval, tempEntity.newPolicy(org.getId(), "crit-active", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    PolicyViolation waived = tempEntity.newPolicyViolation(
        eval, tempEntity.newPolicy(org.getId(), "crit-waived", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "2");
    waived.setWaiveTime(new Date());
    tempEntity.updatePolicyViolation(waived);

    when(indexingContextMock.getOwner(app.getOrganizationId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(doc).isNotNull();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactly("build:critical:1");
  }

  private void stubNoEvaluationsByDefault(final String applicationId) {
    lenient()
        .when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(eq(Set.of(applicationId)), anySet()))
        .thenReturn(Collections.emptyList());
  }

  /**
   * Batches two apps in one {@code buildApplicationDocs} run and asserts each app's APPLICATION doc
   * carries its own latest-evaluation time and its own stage:severity:count tokens — proving the
   * cross-app grouping in the batched rollups does not bleed one app's values into another.
   */
  @Test
  public void testBuildApplicationDocs_batchedRollups_noCrossAppBleed() {
    Organization org = tempEntity.newOrganization();
    Application appA = tempEntity.newApplicationWithParent(org);
    Application appB = tempEntity.newApplicationWithParent(org);

    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);

    // appA evaluated at buildMsA, appB at buildMsB (distinct) — the batched evaluation query returns
    // both apps' rows in one list; grouping must map each app to its own max time.
    long msA = 1_700_000_000_000L;
    long msB = 1_650_000_000_000L;
    PolicyEvaluation evalA = new PolicyEvaluation();
    evalA.setOwnerId(appA.getId());
    evalA.setTime(new Date(msA));
    PolicyEvaluation evalB = new PolicyEvaluation();
    evalB.setOwnerId(appB.getId());
    evalB.setTime(new Date(msB));
    when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(anySet(), anySet()))
        .thenReturn(List.of(evalA, evalB));

    // appA gets a CRITICAL BUILD violation; appB gets a SEVERE BUILD violation. The batched
    // violations query is the real DAO (H2), so seed real rows.
    PolicyEvaluation seedEvalA = tempEntity.newPolicyEvaluation(
        appA.getId(), StageTypes.BUILD.getId(), "scan-a", new Date(), "commit-a");
    tempEntity.newPolicyViolation(seedEvalA, tempEntity.newPolicy(org.getId(), "crit-a", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    PolicyEvaluation seedEvalB = tempEntity.newPolicyEvaluation(
        appB.getId(), StageTypes.BUILD.getId(), "scan-b", new Date(), "commit-b");
    tempEntity.newPolicyViolation(seedEvalB, tempEntity.newPolicy(org.getId(), "sev-b", 5), 5,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    List<Document> docs =
        documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(appA, appB));

    Document docA = docs.stream()
        .filter(d -> appA.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();
    Document docB = docs.stream()
        .filter(d -> appB.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    assertThat(docA.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(String.valueOf(msA));
    assertThat(docB.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(String.valueOf(msB));
    assertThat(docA.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:critical:1");
    assertThat(docB.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:severe:1");
  }

  /**
   * The batched category loader (warmed once per {@code buildApplicationDocs} run via
   * {@code TagDAO.getByApplicationIdsGrouped}) must attribute each app only its own category names
   * and must match what the single-app path produces — proving batching preserves semantics and
   * does not bleed one app's category onto another.
   */
  @Test
  public void testBuildApplicationDocs_batchedCategoryNames_noCrossAppBleed() {
    Organization org = tempEntity.newOrganization();
    Application appA = tempEntity.newApplicationWithParent(org);
    Application appB = tempEntity.newApplicationWithParent(org);
    Application appC = tempEntity.newApplicationWithParent(org);
    Tag distributed = tempEntity.newTag(org.getId(), "Distributed");
    Tag finance = tempEntity.newTag(org.getId(), "Finance");
    tempEntity.newApplicationTag(appA.getId(), distributed.getId());
    tempEntity.newApplicationTag(appB.getId(), finance.getId());
    // appC has no category.

    // Real IndexingContext so the genuine batch-memoized category loader runs against H2.
    IndexingContext realContext = new IndexingContext(ownerDAO, conversionHelper)
    {
      @Override
      public void deleteDocuments(final String query) {
      }

      @Override
      public void addDocuments(final List<Document> documents) {
      }
    };
    // No evaluations/violations so those fields are simply omitted; we only assert categories here.
    lenient().when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(anySet(), anySet()))
        .thenReturn(Collections.emptyList());

    List<Document> docs =
        documentBuilderHelper.buildApplicationDocs(realContext, List.of(appA, appB, appC));

    Document docA = docs.stream()
        .filter(d -> appA.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();
    Document docB = docs.stream()
        .filter(d -> appB.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();
    Document docC = docs.stream()
        .filter(d -> appC.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    assertThat(docA.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label))
        .containsExactly("Distributed");
    assertThat(docB.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label))
        .containsExactly("Finance");
    // appC has no category, so the field is omitted entirely (no cross-app bleed from A or B).
    assertThat(docC.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();
  }

  /**
   * The batched category loader must produce the same category names for one app as the single-app
   * (incremental) path — batching is a query-count optimization and must not change the field value.
   */
  @Test
  public void testBuildApplicationDocs_batchedCategoryNames_matchPerAppPath() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    Tag tag = tempEntity.newTag(org.getId(), "Distributed");
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    lenient().when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(anySet(), anySet()))
        .thenReturn(Collections.emptyList());

    Document batchedDoc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();
    Document perAppDoc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(batchedDoc.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label))
        .containsExactly(perAppDoc.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label))
        .containsExactly("Distributed");
  }

  /**
   * {@code applicationCategoryNames} is the shared category-names helper reached from the
   * POLICY_VIOLATION and LEGAL_VIOLATION paths with {@code application.getId()} passed straight
   * through (no per-caller null guard). Its {@code Set.of(applicationId)} rejects a null element, so
   * a null id must short-circuit to an empty list rather than throwing NullPointerException.
   */
  @Test
  public void testApplicationCategoryNames_nullApplicationId_returnsEmpty() throws Exception {
    java.lang.reflect.Method method = DocumentBuilderHelper.class.getDeclaredMethod(
        "applicationCategoryNames", IndexingContext.class, String.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<String> result = (List<String>) method.invoke(documentBuilderHelper, indexingContextMock, null);

    assertThat(result).isEmpty();
    verify(indexingContextMock, never()).getCategoryNamesByApp(anySet(), any());
  }

  @SuppressWarnings("unchecked")
  private List<Document> buildPolicyViolationDocuments(
      final IndexingContext indexingContext,
      final Organization organization,
      final Collection<Organization> parentOrganizations,
      final Application application,
      final List<PolicyViolation> violations) throws Exception
  {
    java.lang.reflect.Method method = DocumentBuilderHelper.class.getDeclaredMethod(
        "buildPolicyViolationDocuments", IndexingContext.class, Organization.class, Collection.class,
        Application.class, StageType.class, String.class, List.class, Map.class);
    method.setAccessible(true);
    return (List<Document>) method.invoke(documentBuilderHelper, indexingContext, organization,
        parentOrganizations, application, StageTypes.BUILD, "scan-id", violations,
        Collections.emptyMap());
  }

  @SuppressWarnings("unchecked")
  private List<Document> buildLegalViolationDocuments(
      final IndexingContext indexingContext,
      final Organization organization,
      final Collection<Organization> parentOrganizations,
      final Application application,
      final Component component) throws Exception
  {
    java.lang.reflect.Method method = DocumentBuilderHelper.class.getDeclaredMethod(
        "buildLegalViolationDocuments", IndexingContext.class, Organization.class, Collection.class,
        Application.class, StageType.class, String.class, Component.class, Map.class);
    method.setAccessible(true);
    return (List<Document>) method.invoke(documentBuilderHelper, indexingContext, organization,
        parentOrganizations, application, StageTypes.BUILD, "scan-id", component,
        Collections.emptyMap());
  }

  /**
   * PR-A denormalizes the application's category names onto POLICY_VIOLATION docs so a category
   * filter matches violations of a tagged app. Build a real POLICY_VIOLATION doc for an app that
   * has a category tag and assert the doc carries the category on the {@code APPLICATION_CATEGORY_NAME}
   * field.
   */
  @Test
  public void testBuildPolicyViolationDocument_carriesApplicationCategoryName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    Tag tag = tempEntity.newTag(org.getId(), "Finance");
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-pv-cat", new Date(), "commit-pv");
    PolicyViolation violation = tempEntity.newPolicyViolation(
        eval, tempEntity.newPolicy(org.getId(), "crit-policy", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    List<Document> docs = buildPolicyViolationDocuments(
        indexingContextMock, org, List.of(org), app, List.of(violation));

    Document doc = docs.stream()
        .filter(d -> ItemType.POLICY_VIOLATION.name().equals(itemTypeOf(d)))
        .findFirst()
        .orElseThrow();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).containsExactly("Finance");
  }

  /**
   * A POLICY_VIOLATION doc for an app with no category tags must omit the category field entirely.
   */
  @Test
  public void testBuildPolicyViolationDocument_noCategory_omitsApplicationCategoryName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-pv-nocat", new Date(), "commit-pv-nocat");
    PolicyViolation violation = tempEntity.newPolicyViolation(
        eval, tempEntity.newPolicy(org.getId(), "crit-policy", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    List<Document> docs = buildPolicyViolationDocuments(
        indexingContextMock, org, List.of(org), app, List.of(violation));

    Document doc = docs.stream()
        .filter(d -> ItemType.POLICY_VIOLATION.name().equals(itemTypeOf(d)))
        .findFirst()
        .orElseThrow();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();
  }

  /**
   * PR-A also denormalizes the category names onto LEGAL_VIOLATION docs. Build a real
   * LEGAL_VIOLATION doc for a tagged app and assert the category is present.
   */
  @Test
  public void testBuildLegalViolationDocument_carriesApplicationCategoryName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    Tag tag = tempEntity.newTag(org.getId(), "Finance");
    tempEntity.newApplicationTag(app.getId(), tag.getId());

    Component component = new Component(
        ComponentIdentifier.createMavenCoordinates("g", "a", "1", null, "jar"));
    component.addDeclaredLicenseId("license-id-1");

    List<Document> docs =
        buildLegalViolationDocuments(indexingContextMock, org, List.of(org), app, component);

    Document doc = docs.stream()
        .filter(d -> ItemType.LEGAL_VIOLATION.name().equals(itemTypeOf(d)))
        .findFirst()
        .orElseThrow();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).containsExactly("Finance");
  }

  /**
   * A LEGAL_VIOLATION doc for an app with no category tags must omit the category field entirely.
   */
  @Test
  public void testBuildLegalViolationDocument_noCategory_omitsApplicationCategoryName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);

    Component component = new Component(
        ComponentIdentifier.createMavenCoordinates("g", "a", "1", null, "jar"));
    component.addDeclaredLicenseId("license-id-1");

    List<Document> docs =
        buildLegalViolationDocuments(indexingContextMock, org, List.of(org), app, component);

    Document doc = docs.stream()
        .filter(d -> ItemType.LEGAL_VIOLATION.name().equals(itemTypeOf(d)))
        .findFirst()
        .orElseThrow();
    assertThat(doc.getFields(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();
  }

  /**
   * The shared {@code applicationCategoryNames} helper reached from the violation-doc builders is
   * fed {@code application.getId()} with no per-caller null guard, so a null app id must not throw
   * (the NPE guard short-circuits to no categories). Build both violation doc types for an app
   * reporting a null id and assert no NPE and no category field.
   */
  @Test
  public void testBuildViolationDocuments_nullApplicationId_noNpeAndNoCategory() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application persistedApp = tempEntity.newApplicationWithParent(org);
    // A real category on the persisted app: were the null-id guard absent, Set.of(null) would throw.
    Tag tag = tempEntity.newTag(org.getId(), "Finance");
    tempEntity.newApplicationTag(persistedApp.getId(), tag.getId());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        persistedApp.getId(), StageTypes.BUILD.getId(), "scan-null-id", new Date(), "commit-null");
    PolicyViolation violation = tempEntity.newPolicyViolation(
        eval, tempEntity.newPolicy(org.getId(), "crit-policy", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    Application app = spy(persistedApp);
    when(app.getId()).thenReturn(null);

    List<Document> policyDocs = buildPolicyViolationDocuments(
        indexingContextMock, org, List.of(org), app, List.of(violation));
    Document policyDoc = policyDocs.stream()
        .filter(d -> ItemType.POLICY_VIOLATION.name().equals(itemTypeOf(d)))
        .findFirst()
        .orElseThrow();
    assertThat(policyDoc.getFields(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();

    Component component = new Component(
        ComponentIdentifier.createMavenCoordinates("g", "a", "1", null, "jar"));
    component.addDeclaredLicenseId("license-id-1");
    List<Document> legalDocs =
        buildLegalViolationDocuments(indexingContextMock, org, List.of(org), app, component);
    Document legalDoc = legalDocs.stream()
        .filter(d -> ItemType.LEGAL_VIOLATION.name().equals(itemTypeOf(d)))
        .findFirst()
        .orElseThrow();
    assertThat(legalDoc.getFields(FieldIdentifier.APPLICATION_CATEGORY_NAME.label)).isEmpty();

    verify(indexingContextMock, never()).getCategoryNamesByApp(anySet(), any());
  }

  /**
   * The INCREMENTAL indexing path (AbstractSearchIndexClient.updateIndexForApplication) shares ONE
   * IndexingContext across a batch and calls {@code buildDocument(ctx, app)} once per app, each with
   * a single-app id set. With a real IndexingContext this exercises the load-on-miss cache: the
   * first app must not "claim" the cache for the whole batch — every subsequent app must still load
   * and emit its own latest-evaluation time and stage:severity:count tokens. Regression for the
   * one-shot cache-gate bug that silently omitted those fields for every app after the first.
   */
  @Test
  public void testBuildDocument_incrementalPath_sharedContext_allAppsCarryRollups() {
    Organization org = tempEntity.newOrganization();
    Application appA = tempEntity.newApplicationWithParent(org);
    Application appB = tempEntity.newApplicationWithParent(org);

    // Real IndexingContext so the genuine load-on-miss cache runs (the mock bypasses it).
    IndexingContext realContext = new IndexingContext(ownerDAO, conversionHelper)
    {
      @Override
      public void deleteDocuments(final String query) {
      }

      @Override
      public void addDocuments(final List<Document> documents) {
      }
    };

    // Each per-app evaluation query (Set.of(appId)) returns only that app's row, mirroring the
    // incremental path where updateIndexForApplication passes one app id at a time.
    long msA = 1_700_000_000_000L;
    long msB = 1_650_000_000_000L;
    PolicyEvaluation evalA = new PolicyEvaluation();
    evalA.setOwnerId(appA.getId());
    evalA.setTime(new Date(msA));
    PolicyEvaluation evalB = new PolicyEvaluation();
    evalB.setOwnerId(appB.getId());
    evalB.setTime(new Date(msB));
    when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(eq(Set.of(appA.getId())), anySet()))
        .thenReturn(List.of(evalA));
    when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(eq(Set.of(appB.getId())), anySet()))
        .thenReturn(List.of(evalB));

    // Real H2-backed violations: appA a CRITICAL BUILD violation, appB a SEVERE BUILD violation.
    PolicyEvaluation seedEvalA = tempEntity.newPolicyEvaluation(
        appA.getId(), StageTypes.BUILD.getId(), "scan-inc-a", new Date(), "commit-inc-a");
    tempEntity.newPolicyViolation(seedEvalA, tempEntity.newPolicy(org.getId(), "crit-inc-a", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    PolicyEvaluation seedEvalB = tempEntity.newPolicyEvaluation(
        appB.getId(), StageTypes.BUILD.getId(), "scan-inc-b", new Date(), "commit-inc-b");
    tempEntity.newPolicyViolation(seedEvalB, tempEntity.newPolicy(org.getId(), "sev-inc-b", 5), 5,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    // Build A then B against the SAME context, exactly as the incremental batch does.
    Document docA = documentBuilderHelper.buildDocument(realContext, appA);
    Document docB = documentBuilderHelper.buildDocument(realContext, appB);

    assertThat(docA.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(String.valueOf(msA));
    assertThat(docA.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:critical:1");

    // The bug: with the one-shot gate, B misses the cache entirely and both fields are OMITTED.
    assertThat(docB.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(String.valueOf(msB));
    assertThat(docB.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:severe:1");
  }

  /**
   * Locks the {@code applicationStageSeverityCount} wire format: encode + decode must round-trip,
   * and the encoded token must be the exact {@code stage:severity:count} shape consumers bind to.
   */
  @Test
  public void testStageSeverityCount_encodeDecodeRoundTrip() {
    String token = DocumentBuilderHelper.encodeStageSeverityCount(
        StageTypes.BUILD.getId(), ThreatLevel.CRITICAL, 7);
    assertThat(token).isEqualTo("build:critical:7");

    String[] parts = DocumentBuilderHelper.decodeStageSeverityCount(token);
    assertThat(parts[0]).isEqualTo(StageTypes.BUILD.getId());
    assertThat(parts[1]).isEqualTo("critical");
    assertThat(Integer.parseInt(parts[2])).isEqualTo(7);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> DocumentBuilderHelper.decodeStageSeverityCount("malformed"));

    // encode rejects a stageId containing the delimiter, which would corrupt the token.
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> DocumentBuilderHelper.encodeStageSeverityCount("sta:ge", ThreatLevel.CRITICAL, 1));
  }

  /**
   * The batched {@code buildApplicationDocs} path and the single-app {@code buildDocument} path must
   * produce the same latest-evaluation time and the same stage:severity:count tokens for one app —
   * batching is a query-count optimization and must not change any indexed field value.
   */
  @Test
  public void testBuildApplicationDocs_batchedMatchesPerAppFieldValues() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);

    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);

    long evalMs = 1_700_000_000_000L;
    PolicyEvaluation latestEval = new PolicyEvaluation();
    latestEval.setOwnerId(app.getId());
    latestEval.setTime(new Date(evalMs));
    when(policyEvaluationDAOMock.getLastByOwnerIdsAndStageIds(anySet(), anySet()))
        .thenReturn(List.of(latestEval));

    PolicyEvaluation seedEval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-match", new Date(), "commit-match");
    tempEntity.newPolicyViolation(seedEval, tempEntity.newPolicy(org.getId(), "crit-match", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    tempEntity.newPolicyViolation(seedEval, tempEntity.newPolicy(org.getId(), "sev-match", 5), 5,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    Document batchedDoc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();
    Document perAppDoc = documentBuilderHelper.buildDocument(indexingContextMock, app);

    assertThat(batchedDoc.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(perAppDoc.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label))
        .isEqualTo(String.valueOf(evalMs));
    assertThat(batchedDoc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder(perAppDoc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:critical:1", "build:severe:1");
  }

  /**
   * Mixed active violations across stages and policy types: A4 max threat = max raw active threat level,
   * A1 stages set = active-violation stages, A2 policy types set = active-violation categories, and the
   * active-only pills still reflect only active rows. A3 states = open (all active).
   */
  @Test
  public void testBuildApplicationDocs_mixedActiveViolations_aggregatesAndPills() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    PolicyEvaluation buildEval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-b", new Date(), "commit-b");
    PolicyEvaluation releaseEval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.RELEASE.getId(), "scan-r", new Date(), "commit-r");
    // BUILD: a CRITICAL(9) SECURITY and a MODERATE(3) LICENSE; RELEASE: a SEVERE(7) QUALITY.
    tempEntity.newPolicyViolation(buildEval, tempEntity.newPolicy(org.getId(), "crit", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    tempEntity.newPolicyViolation(buildEval, tempEntity.newPolicy(org.getId(), "lic", 3), 3,
        PolicyThreatCategory.LICENSE, "g", "a", "2");
    tempEntity.newPolicyViolation(releaseEval, tempEntity.newPolicy(org.getId(), "qual", 7), 7,
        PolicyThreatCategory.QUALITY, "g", "a", "3");

    Document doc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    // A4: max raw active threat level.
    assertThat(doc.get(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label)).isEqualTo("9");
    // A1: stages with active violations.
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label))
        .containsExactlyInAnyOrder(StageTypes.BUILD.getId(), StageTypes.RELEASE.getId());
    // A2: policy types present among active violations (lowercased).
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label))
        .containsExactlyInAnyOrder("security", "license", "quality");
    // A3: all active -> only open.
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STATE.label)).containsExactly("open");
    // A6: worst state ordinal = 0 (Open).
    assertThat(doc.get(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label)).isEqualTo("0");
    // Active-only pills unchanged by the wider fetch.
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactlyInAnyOrder("build:critical:1", "build:moderate:1", "release:severe:1");
  }

  /**
   * A3 correctness with waived + legacy + active mixed: the violation-state set must surface waived and
   * legacy (from the wider unfixed fetch) while the active-only pills, A1/A2/A4 stay active-only. A6 is
   * the worst (min) ordinal across the states present.
   */
  @Test
  public void testBuildApplicationDocs_violationStates_activeWaivedLegacy() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-mixed", new Date(), "commit-mixed");
    Policy activePolicy = tempEntity.newPolicy(org.getId(), "active", 9);
    Policy waivedPolicy = tempEntity.newPolicy(org.getId(), "waived", 8);
    Policy legacyPolicy = tempEntity.newPolicy(org.getId(), "legacy", 10);
    // Active CRITICAL(9); a waived SEVERE(8); a legacy CRITICAL(10). Only the active one feeds A1/A2/A4/pills.
    tempEntity.newPolicyViolation(eval, activePolicy, 9, PolicyThreatCategory.SECURITY, "g", "a", "1");
    PolicyWaiver waiver = tempEntity.newWaiver(waivedPolicy.getId(), org.getId());
    tempEntity.newWaivedPolicyViolation(eval, waivedPolicy, 8, PolicyThreatCategory.LICENSE,
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "2"), "h2", waiver);
    tempEntity.newLegacyPolicyViolation(eval, legacyPolicy);

    Document doc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    // A3: all three states surface from the wider unfixed fetch.
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STATE.label))
        .containsExactlyInAnyOrder("open", "waived", "legacy");
    // A6: worst (min) ordinal is Open = 0.
    assertThat(doc.get(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label)).isEqualTo("0");
    // A4/A1/A2: active-only -> only the active CRITICAL(9) SECURITY BUILD violation counts.
    assertThat(doc.get(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label)).isEqualTo("9");
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label))
        .containsExactly(StageTypes.BUILD.getId());
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label)).containsExactly("security");
    // Active-only pills: only the active CRITICAL, not the waived/legacy rows.
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label))
        .containsExactly("build:critical:1");
  }

  /**
   * A6 ordinal reflects the WORST (min) state when the app has no active (open) violation — only a waived
   * and a legacy row — so the ordinal is Waived(1), not Open. Pills and A4 are absent (no active row).
   */
  @Test
  public void testBuildApplicationDocs_violationStateOrdinal_waivedAndLegacyOnly() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-wl", new Date(), "commit-wl");
    Policy waivedPolicy = tempEntity.newPolicy(org.getId(), "waived-only", 8);
    Policy legacyPolicy = tempEntity.newPolicy(org.getId(), "legacy-only", 10);
    PolicyWaiver waiver = tempEntity.newWaiver(waivedPolicy.getId(), org.getId());
    tempEntity.newWaivedPolicyViolation(eval, waivedPolicy, 8, PolicyThreatCategory.LICENSE,
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "1"), "h1", waiver);
    tempEntity.newLegacyPolicyViolation(eval, legacyPolicy);

    Document doc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STATE.label))
        .containsExactlyInAnyOrder("waived", "legacy");
    // Worst (min) ordinal across {waived=1, legacy=2} = 1.
    assertThat(doc.get(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label)).isEqualTo("1");
    // No active violation -> no max-threat, no active pills/stages/types.
    assertThat(doc.get(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label)).isNull();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label)).isEmpty();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label)).isEmpty();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label)).isEmpty();
  }

  /**
   * An app with no unfixed violation emits NONE of the aggregate fields (sparse), so the RANGE/TERMS
   * filters and the ordinal sort treat it as "no threat / no state" and it sorts last.
   */
  @Test
  public void testBuildApplicationDocs_noViolations_aggregatesAbsent() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    Document doc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    assertThat(doc.get(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label)).isNull();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label)).isEmpty();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label)).isEmpty();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STATE.label)).isEmpty();
    assertThat(doc.get(FieldIdentifier.APPLICATION_VIOLATION_STATE_SORT_ORDINAL.label)).isNull();
  }

  /**
   * A violation on a stage outside the global registry is dropped from A1 and the pills, but its threat
   * level and policy type still feed A4/A2 (they are not stage-scoped) and its state still feeds A3.
   */
  @Test
  public void testBuildApplicationDocs_unknownStage_droppedFromStagesButNotThreat() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    stubNoEvaluationsByDefault(app.getId());

    PolicyEvaluation unknownStageEval = tempEntity.newPolicyEvaluation(
        app.getId(), "not-a-real-stage", "scan-u", new Date(), "commit-u");
    tempEntity.newPolicyViolation(unknownStageEval, tempEntity.newPolicy(org.getId(), "u", 6), 6,
        PolicyThreatCategory.SECURITY, "g", "a", "1");

    Document doc = documentBuilderHelper.buildApplicationDocs(indexingContextMock, List.of(app))
        .stream()
        .filter(d -> app.getId().equals(d.get(FieldIdentifier.APPLICATION_ID.label)))
        .findFirst()
        .orElseThrow();

    // Stage dropped from A1 and pills, but the threat level/type still feed A4/A2 and the state feeds A3.
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STAGE.label)).isEmpty();
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label)).isEmpty();
    assertThat(doc.get(FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label)).isEqualTo("6");
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_POLICY_TYPE.label)).containsExactly("security");
    assertThat(doc.getValues(FieldIdentifier.APPLICATION_VIOLATION_STATE.label)).containsExactly("open");
  }

  /**
   * The wider aggregate fetch must remain ONE bounded query for the whole app batch — no N+1. Asserts the
   * violations DAO is hit exactly once via {@code getUnfixedByOwnerIds} across a two-app batch, and
   * the active-only stage-severity query {@code getActiveByOwnerIds} is NOT used anymore.
   */
  @Test
  public void testBuildApplicationDocs_violationRollup_singleQueryNoNPlusOne() {
    Organization org = tempEntity.newOrganization();
    Application appA = tempEntity.newApplicationWithParent(org);
    Application appB = tempEntity.newApplicationWithParent(org);

    // Real IndexingContext so the genuine per-run memo dedupes: buildApplicationDocs warms the whole
    // batch in ONE loader call, and each per-app buildDocument then hits the cache. The mock context
    // has no cache and would re-run the loader per call, which measures the stub, not the real path.
    IndexingContext realContext = new IndexingContext(ownerDAO, conversionHelper)
    {
      @Override
      public void deleteDocuments(final String query) {
      }

      @Override
      public void addDocuments(final List<Document> documents) {
      }
    };

    PolicyViolationDAO spyViolationDAO = spy(policyViolationDAO);
    DocumentBuilderHelper helperWithSpy = swapViolationDAO(documentBuilderHelper, spyViolationDAO);

    PolicyEvaluation evalA = tempEntity.newPolicyEvaluation(
        appA.getId(), StageTypes.BUILD.getId(), "scan-n1a", new Date(), "commit-n1a");
    tempEntity.newPolicyViolation(evalA, tempEntity.newPolicy(org.getId(), "na", 9), 9,
        PolicyThreatCategory.SECURITY, "g", "a", "1");
    PolicyEvaluation evalB = tempEntity.newPolicyEvaluation(
        appB.getId(), StageTypes.BUILD.getId(), "scan-n1b", new Date(), "commit-n1b");
    tempEntity.newPolicyViolation(evalB, tempEntity.newPolicy(org.getId(), "nb", 5), 5,
        PolicyThreatCategory.LICENSE, "g", "a", "2");

    try {
      helperWithSpy.buildApplicationDocs(realContext, List.of(appA, appB));

      // One widened violations query for the whole two-app batch (no N+1), and the active-only
      // stage-severity query is no longer used — the wider fetch backs both pills and aggregates.
      verify(spyViolationDAO, times(1)).getUnfixedByOwnerIds(anySet());
      verify(spyViolationDAO, never()).getActiveByOwnerIds(anySet());
    }
    finally {
      swapViolationDAO(documentBuilderHelper, policyViolationDAO);
    }
  }

  /** Reflectively swaps the {@code policyViolationDAO} field so the batch fetch can be spied. */
  private static DocumentBuilderHelper swapViolationDAO(
      final DocumentBuilderHelper helper,
      final PolicyViolationDAO dao)
  {
    try {
      java.lang.reflect.Field field = DocumentBuilderHelper.class.getDeclaredField("policyViolationDAO");
      field.setAccessible(true);
      field.set(helper, dao);
      return helper;
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
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
  public void testBuildDocument_SbomVulnerability_UnscoredWritesNoSeverityField() {
    // An unscored SBOM vulnerability (e.g. EPSS-only): severity stays at the primitive-double default
    // 0.0 with no CVSS ratingMethod. Writing 0.0 would put it in the `none` CVSS band, conflating "no
    // score" with a real 0.0 — so no vulnerabilitySeverity field must be written, keeping it out of all
    // severity bands.
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "filename");

    ThirdPartyFileCoordinate fileCoord = new ThirdPartyFileCoordinate();
    fileCoord.setHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    ThirdPartyCoordinateSecurity unscored = new ThirdPartyCoordinateSecurity();
    unscored.setRefId("CVE-2020-UNSCORED");
    unscored.setDescription("no cvss score");
    // severity left at 0.0 default, ratingMethod left null => unscored

    Document doc = documentBuilderHelper.buildDocument(
        organization, application, sbomMetadata, fileCoord, unscored, parentOrgs);

    assertThat(doc).isNotNull();
    assertThat(doc.get(FieldIdentifier.VULNERABILITY_ID.label)).isEqualTo("CVE-2020-UNSCORED");
    assertThat(doc.getFields(FieldIdentifier.VULNERABILITY_SEVERITY.label)).isEmpty();
  }

  @Test
  public void testBuildDocument_SbomVulnerability_ScoredZeroWritesSeverityField() {
    // A genuine CVSS 0.0 (None severity) always carries a ratingMethod, so it IS scored and must be
    // indexed as 0.0 (landing in the `none` band) — distinct from the unscored default above.
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Collection<Organization> parentOrgs = new ArrayList<>();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(application.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "filename");

    ThirdPartyFileCoordinate fileCoord = new ThirdPartyFileCoordinate();
    fileCoord.setHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    ThirdPartyCoordinateSecurity scoredZero = new ThirdPartyCoordinateSecurity();
    scoredZero.setRefId("CVE-2020-SCORED-ZERO");
    scoredZero.setSeverity(0.0d);
    scoredZero.setRatingMethod("CVSSv3");

    Document doc = documentBuilderHelper.buildDocument(
        organization, application, sbomMetadata, fileCoord, scoredZero, parentOrgs);

    assertThat(doc).isNotNull();
    assertThat(doc.getFields(FieldIdentifier.VULNERABILITY_SEVERITY.label)).isNotEmpty();
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
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label)).isEqualTo("false");
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
  public void testBuildDocument_AutoPolicyWaiver_leavesPolicyNameNullAndIndexesSubset() {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(organization.getId(), 9, true, false);
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);

    assertThat(itemTypeOf(doc)).isEqualTo(ItemType.POLICY_WAIVER.name());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)).isEqualTo(waiver.getId());
    // Auto-waivers carry no indexed policy name; the display title is synthesized on the read side
    // (IndexQueryRowMapper) so it is never text-searchable and can change without a reindex.
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isNull();
    assertThat(doc.getField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label).numericValue().intValue())
        .isEqualTo(9);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label)).isEqualTo(organization.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label)).isEqualTo("ORGANIZATION");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label)).isNotBlank();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label)).isEqualTo("true");
    // Auto-waivers carry no policy id, reason, comment, or expiry.
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_REASON.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_COMMENT.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label)).isNull();
  }

  @Test
  public void testBuildDocument_PolicyWaiver_indexesFullParentOrganizationChain() {
    Organization grandparent = tempEntity.newOrganization();
    Organization parent = tempEntity.newOrganization(grandparent);
    Organization child = tempEntity.newOrganization(parent);
    Policy policy = tempEntity.newPolicy(child.getId(), "nested policy", 4);
    PolicyWaiver waiver = tempEntity.newWaiver("hash-nested", policy.getId(), child.getId(), "comment");
    when(indexingContextMock.getOwner(child.getId())).thenReturn(child);
    when(indexingContextMock.getOwner(parent.getId())).thenReturn(parent);
    when(indexingContextMock.getOwner(grandparent.getId())).thenReturn(grandparent);
    when(indexingContextMock.getAncestorOrgIds(child))
        .thenReturn(List.of(child.getId(), parent.getId(), grandparent.getId()));

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);

    assertThat(stringValuesOf(doc, FieldIdentifier.PARENT_ORGANIZATION_NAME))
        .containsExactlyInAnyOrder(child.getName(), parent.getName(), grandparent.getName());
    assertThat(stringValuesOf(doc, FieldIdentifier.PARENT_ORGANIZATION_ID))
        .containsExactlyInAnyOrder(child.getId(), parent.getId(), grandparent.getId());
  }

  @Test
  public void testBuildDocument_appScopedPolicyWaiver_indexesAppOrgAndFullParentChain() {
    Organization grandparent = tempEntity.newOrganization();
    Organization parent = tempEntity.newOrganization(grandparent);
    Organization child = tempEntity.newOrganization(parent);
    Application application = tempEntity.newApplication(child.getId());
    Policy policy = tempEntity.newPolicy(child.getId(), "app nested policy", 4);
    PolicyWaiver waiver = tempEntity.newWaiver("hash-app-nested", policy.getId(), application.getId(), "comment");
    when(indexingContextMock.getOwner(application.getId())).thenReturn(application);
    when(indexingContextMock.getOwner(child.getId())).thenReturn(child);
    when(indexingContextMock.getOwner(parent.getId())).thenReturn(parent);
    when(indexingContextMock.getOwner(grandparent.getId())).thenReturn(grandparent);
    when(indexingContextMock.getAncestorOrgIds(child))
        .thenReturn(List.of(child.getId(), parent.getId(), grandparent.getId()));

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);

    assertThat(doc.get(FieldIdentifier.APPLICATION_ID.label)).isEqualTo(application.getId());
    assertThat(doc.get(FieldIdentifier.APPLICATION_NAME.label)).isEqualTo(application.getName());
    assertThat(doc.get(FieldIdentifier.ORGANIZATION_ID.label)).isEqualTo(child.getId());
    assertThat(doc.get(FieldIdentifier.ORGANIZATION_NAME.label)).isEqualTo(child.getName());
    assertThat(stringValuesOf(doc, FieldIdentifier.PARENT_ORGANIZATION_NAME))
        .containsExactlyInAnyOrder(child.getName(), parent.getName(), grandparent.getName());
    assertThat(stringValuesOf(doc, FieldIdentifier.PARENT_ORGANIZATION_ID))
        .containsExactlyInAnyOrder(child.getId(), parent.getId(), grandparent.getId());
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
    // Auto-waivers index no policy name; the read side synthesizes their display title.
    assertThat(autoDoc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isNull();

    assertThat(waiverDocById(docs, containerWaiver.getId())).isNull();
  }

  private static Document waiverDocById(List<Document> docs, String waiverId) {
    return docs.stream()
        .filter(doc -> waiverId.equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)))
        .findFirst()
        .orElse(null);
  }

  @Test
  public void buildPolicyWaiverRequestDocs_indexesRequestFieldsAndPolicyType() {
    Organization organization = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(organization.getId())).thenReturn(organization);
    Policy policy = tempEntity.newPolicy(organization.getId(), "req policy", 6);
    com.sonatype.insight.brain.model.policy.PolicyWaiverRequest request =
        tempEntity.newPolicyWaiverRequest(new com.sonatype.insight.brain.model.policy.PolicyWaiverRequest(
            policy.getId(), organization.getId(), "please waive")
                .setRequesterName("Alice"));

    List<Document> docs = documentBuilderHelper.buildPolicyWaiverRequestDocs(indexingContextMock);

    Document doc = waiverDocById(docs, request.getId());
    assertThat(doc).isNotNull();
    assertThat(doc.get(FieldIdentifier.ITEM_TYPE.label))
        .isEqualTo(com.sonatype.insight.brain.search.index.ItemType.POLICY_WAIVER_REQUEST.name());
    // Status is indexed lowercased so Lucene exact-match (via the query-time LowerCaseKeywordAnalyzer)
    // and the OpenSearch lowercase keyword normalizer both hit; the RowMapper uppercases it on read.
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label)).isEqualTo("requested");
    assertThat(doc.get(FieldIdentifier.REQUESTER_NAME.label)).isEqualTo("Alice");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("req policy");
    // policyType denormalized from the resolved policy (present on request docs, like waiver docs).
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label)).isNotBlank();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label)).isEqualTo("ORGANIZATION");
  }

  @Test
  public void buildPolicyWaiverRequestDoc_rbacClosureMatchesWaiverClosureForSameOwner() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    when(indexingContextMock.getOwner(app.getId())).thenReturn(app);
    Policy policy = tempEntity.newPolicy(org.getId(), "p", 5);

    PolicyWaiver waiver = tempEntity.newWaiver("h-w", policy.getId(), app.getId(), "w");
    com.sonatype.insight.brain.model.policy.PolicyWaiverRequest request =
        tempEntity.newPolicyWaiverRequest(new com.sonatype.insight.brain.model.policy.PolicyWaiverRequest(
            policy.getId(), app.getId(), "r"));

    Document waiverDoc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);
    Document requestDoc = documentBuilderHelper.buildDocument(indexingContextMock, request);

    // RBAC closure must be byte-for-byte identical for the same owner (MTIQ correctness).
    assertThat(requestDoc.getValues(FieldIdentifier.ALLOWED_CONTEXT_IDS.label))
        .containsExactlyInAnyOrder(waiverDoc.getValues(FieldIdentifier.ALLOWED_CONTEXT_IDS.label));
  }

  @Test
  public void buildPolicyWaiverRequestDoc_approvedRequestIsIndexedWithApprovedStatus() {
    Organization org = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    Policy policy = tempEntity.newPolicy(org.getId(), "p", 5);
    com.sonatype.insight.brain.model.policy.PolicyWaiverRequest request =
        tempEntity.newPolicyWaiverRequest(new com.sonatype.insight.brain.model.policy.PolicyWaiverRequest(
            policy.getId(), org.getId(), "r")
                .setStatus(com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus.APPROVED));

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, request);
    // Approved requests ARE indexed (for completeness); the query-surface waiverStates filter simply
    // never selects the APPROVED status.
    assertThat(doc).isNotNull();
    // Status indexed lowercased (see buildPolicyWaiverRequestDocs_indexesRequestFieldsAndPolicyType).
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label)).isEqualTo("approved");
  }

  @Test
  public void buildPolicyWaiverRequestDoc_nonIndexableOwnerReturnsNull() {
    com.sonatype.insight.brain.model.policy.PolicyWaiverRequest request =
        new com.sonatype.insight.brain.model.policy.PolicyWaiverRequest("p", "repo-owner", "r");
    request.setId("req-1");
    // No owner resolves for the id (repository-family / missing) -> not indexable -> null doc.
    when(indexingContextMock.getOwner("repo-owner")).thenReturn(null);
    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, request)).isNull();
  }

  @Test
  public void buildPolicyWaiverDoc_componentTargetedWaiver_indexesComponentScopeWithOwnerRbac() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    when(indexingContextMock.getOwner(app.getId())).thenReturn(app);
    Policy policy = tempEntity.newPolicy(org.getId(), "p", 5);
    // A component-targeted waiver: carries a hash (EXACT_COMPONENT), owned by an application.
    PolicyWaiver componentWaiver = tempEntity.newWaiver("comp-hash", policy.getId(), app.getId(), "c");

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, componentWaiver);

    // scope granularity is "component" (targets a specific component)...
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE.label)).isEqualTo("component");
    // ...while the RBAC/owner type stays APPLICATION (owner-based permission closure unchanged).
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label)).isEqualTo("APPLICATION");
    // RBAC closure is the owning app + its org ancestors, exactly as a non-component waiver on the app.
    PolicyWaiver ownerWideWaiver = new PolicyWaiver(null, policy.getId(), app.getId(), "owner-wide");
    ownerWideWaiver.setComponentMatchStrategy(
        com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
    ownerWideWaiver.setId("owner-wide-id");
    Document ownerWideDoc = documentBuilderHelper.buildDocument(indexingContextMock, ownerWideWaiver, policy);
    assertThat(doc.getValues(FieldIdentifier.ALLOWED_CONTEXT_IDS.label))
        .containsExactlyInAnyOrder(ownerWideDoc.getValues(FieldIdentifier.ALLOWED_CONTEXT_IDS.label));
    assertThat(ownerWideDoc.get(FieldIdentifier.POLICY_WAIVER_SCOPE.label)).isEqualTo("application");
  }

  @Test
  public void buildPolicyWaiverDoc_orgWideWaiver_indexesOrganizationScope() {
    Organization org = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    Policy policy = tempEntity.newPolicy(org.getId(), "p", 5);
    PolicyWaiver waiver = new PolicyWaiver(null, policy.getId(), org.getId(), "org-wide");
    waiver.setComponentMatchStrategy(
        com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS);
    waiver.setId("org-wide-id");

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver, policy);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE.label)).isEqualTo("organization");
  }

  @Test
  public void buildPolicyWaiverRequestDoc_nullComponentMatchStrategy_notComponentTargeted() {
    // policy_waiver_request.component_match_strategy is nullable with no ALL_COMPONENTS backfill. A
    // null strategy with no hash/purl is owner-wide, not component-targeted, so the scope must be the
    // owner granularity (organization) — not "component".
    Organization org = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    Policy policy = tempEntity.newPolicy(org.getId(), "p", 5);
    com.sonatype.insight.brain.model.policy.PolicyWaiverRequest request =
        new com.sonatype.insight.brain.model.policy.PolicyWaiverRequest(policy.getId(), org.getId(), "r");
    request.setId("req-null-strategy");

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, request);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_SCOPE.label)).isEqualTo("organization");
  }

  @Test
  public void buildPolicyWaiverRequestDoc_dockerComponentRequest_notIndexed() {
    // A docker-component request maps to a committed waiver with isForContainerImageComponent=true,
    // which is excluded from Global Search; the request must be excluded too so the two surfaces match.
    // The container-image guard runs before the owner lookup, so no owner stub is needed.
    com.sonatype.insight.brain.model.policy.PolicyWaiverRequest request =
        new com.sonatype.insight.brain.model.policy.PolicyWaiverRequest("p", "org-1", "r");
    request.setId("req-docker");
    request.setAssociatedPackageUrl("pkg:docker/library/ubuntu@20.04");

    assertThat(documentBuilderHelper.buildDocument(indexingContextMock, request)).isNull();
  }

  @Test
  public void buildPolicyWaiverDoc_denormalizesPolicyTypeOnWaiverDoc() {
    Organization org = tempEntity.newOrganization();
    when(indexingContextMock.getOwner(org.getId())).thenReturn(org);
    Policy policy = tempEntity.newPolicy(org.getId(), "p", 5);
    PolicyWaiver waiver = tempEntity.newWaiver("h-pt", policy.getId(), org.getId(), "w");

    Document doc = documentBuilderHelper.buildDocument(indexingContextMock, waiver);
    // GAP2+3 handoff: policyType is now denormalized onto POLICY_WAIVER docs too.
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label)).isNotBlank();
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

  // ---- Component violation rollup (C2/C3/C4 denormalization) --------------------------------

  @Test
  public void componentViolationRollupByHash_unionsTypes_maxThreat_classifiesStates() {
    PolicyViolation critSecurityOpen = violation("hashA", 10, PolicyThreatCategory.SECURITY, null, null);
    PolicyViolation lowLicenseWaived = violation("hashA", 3, PolicyThreatCategory.LICENSE, new Date(), null);
    PolicyViolation qualityAutoWaived = violation("hashB", 1, PolicyThreatCategory.QUALITY, null, "autoWaiverId");

    Map<String, DocumentBuilderHelper.ComponentViolationRollup> rollup =
        DocumentBuilderHelper.componentViolationRollupByHash(
            List.of(critSecurityOpen, lowLicenseWaived, qualityAutoWaived));

    // hashA: two violations -> union of types (security, license), max threat 10, states {open, waived}.
    assertThat(rollup.get("hashA").policyTypes()).containsExactlyInAnyOrder("security", "license");
    assertThat(rollup.get("hashA").maxThreatLevel()).isEqualTo(10);
    assertThat(rollup.get("hashA").states()).containsExactlyInAnyOrder("open", "waived");
    // hashB: auto-waived classifies as waived (not a separate legacy/auto state on the component field).
    assertThat(rollup.get("hashB").policyTypes()).containsExactly("quality");
    assertThat(rollup.get("hashB").states()).containsExactly("waived");
    assertThat(rollup.get("hashB").maxThreatLevel()).isEqualTo(1);
  }

  @Test
  public void componentViolationState_classifiesOpenLegacyWaivedDistinctly() {
    // active -> open, pure-legacy -> legacy, manually waived -> waived, waived+legacy -> waived
    // (waiver precedence in deriveWaiverStatus). Legacy is a distinct grandfathered-in state.
    PolicyViolation open = violation("hashOpen", 5, PolicyThreatCategory.SECURITY, null, null, false);
    PolicyViolation legacy = violation("hashLegacy", 5, PolicyThreatCategory.SECURITY, null, null, true);
    PolicyViolation waived = violation("hashWaived", 5, PolicyThreatCategory.SECURITY, new Date(), null, false);
    PolicyViolation waivedLegacy =
        violation("hashWaivedLegacy", 5, PolicyThreatCategory.SECURITY, new Date(), null, true);

    Map<String, DocumentBuilderHelper.ComponentViolationRollup> rollup =
        DocumentBuilderHelper.componentViolationRollupByHash(List.of(open, legacy, waived, waivedLegacy));

    assertThat(rollup.get("hashOpen").states()).containsExactly("open");
    assertThat(rollup.get("hashLegacy").states()).containsExactly("legacy");
    assertThat(rollup.get("hashWaived").states()).containsExactly("waived");
    assertThat(rollup.get("hashWaivedLegacy").states()).containsExactly("waived");
  }

  @Test
  public void componentViolationRollupByHash_emptyOrNullHash_areHandled() {
    assertThat(DocumentBuilderHelper.componentViolationRollupByHash(List.of())).isEmpty();
    // A violation with a null hash cannot be attributed to a component row and is skipped.
    PolicyViolation noHash = violation(null, 10, PolicyThreatCategory.SECURITY, null, null);
    assertThat(DocumentBuilderHelper.componentViolationRollupByHash(List.of(noHash))).isEmpty();
  }

  // ---- Vulnerability first-seen (open_time) join --------------------------------------------

  /**
   * A vuln whose refId is referenced by a single policy violation's constraint facts gets a
   * first-seen equal to that violation's open time (= its evaluation time). The join is over real
   * persisted violations + constraint-facts rows read back through the real DAO.
   */
  @Test
  public void firstSeenEpochMsByVulnRefId_singleViolation_usesOpenTime() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    long openMs = 1_700_000_000_000L;
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-fs-1", new Date(openMs));
    Policy policy = tempEntity.newPolicy(org.getId(), "vuln-policy", 9);
    // reason == the vuln refId: the SecurityVulnerabilitySeverity condition seeds a
    // TriggerReference(SECURITY_VULNERABILITY_REFID, reason) into the persisted constraint facts.
    tempEntity.newPolicyViolation(
        eval, policy, com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        "hashV", "CVE-2020-0001");

    Map<String, Long> firstSeen = firstSeenEpochMsByVulnRefId(loadUnfixedViolations(app));

    assertThat(firstSeen).containsEntry("CVE-2020-0001", openMs);
  }

  /**
   * A vuln triggered by violations across two evaluations (stages/policies) takes the EARLIEST open
   * time — the true first-seen — not the latest.
   */
  @Test
  public void firstSeenEpochMsByVulnRefId_multipleViolations_usesMinOpenTime() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    long earlierMs = 1_600_000_000_000L;
    long laterMs = 1_700_000_000_000L;
    Policy policyBuild = tempEntity.newPolicy(org.getId(), "vuln-policy-build", 9);
    Policy policyStage = tempEntity.newPolicy(org.getId(), "vuln-policy-stage", 9);
    // Later violation first, earlier violation second: min must win regardless of insertion order.
    PolicyEvaluation evalLater = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-fs-late", new Date(laterMs));
    tempEntity.newPolicyViolation(
        evalLater, policyBuild,
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        "hashV", "CVE-2020-0002");
    PolicyEvaluation evalEarlier = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.STAGE_RELEASE.getId(), "scan-fs-early", new Date(earlierMs));
    tempEntity.newPolicyViolation(
        evalEarlier, policyStage,
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        "hashV", "CVE-2020-0002");

    List<PolicyViolation> violations = new ArrayList<>(loadUnfixedViolations(app));
    violations.addAll(loadUnfixedViolationsForStage(app, StageTypes.STAGE_RELEASE));
    Map<String, Long> firstSeen = firstSeenEpochMsByVulnRefId(violations);

    assertThat(firstSeen).containsEntry("CVE-2020-0002", earlierMs);
  }

  /**
   * A violation whose constraint facts carry no SECURITY_VULNERABILITY_REFID trigger (a non-vuln
   * policy condition) contributes no first-seen entry — the map is empty, so such a vuln row shows
   * a blank first-seen and no time is fabricated.
   */
  @Test
  public void firstSeenEpochMsByVulnRefId_nonVulnViolation_producesNoEntry() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-fs-nonvuln", new Date(), "commit-nonvuln");
    // This newPolicyViolation overload seeds a ConditionFact WITHOUT a TriggerReference, so no vuln
    // refId is associated — the join finds nothing to attribute an open time to.
    tempEntity.newPolicyViolation(eval, tempEntity.newPolicy(org.getId(), "non-vuln", 5), 5,
        PolicyThreatCategory.QUALITY, "g", "a", "1");

    Map<String, Long> firstSeen = firstSeenEpochMsByVulnRefId(loadUnfixedViolations(app));

    assertThat(firstSeen).isEmpty();
  }

  /**
   * {@code loadConstraintNames} reads the same shared constraint-facts batch that the first-seen join
   * uses; verify it still resolves the persisted constraint name by facts id (output preserved after
   * the load-once refactor).
   */
  @Test
  public void loadConstraintNames_fromSharedFacts_resolvesConstraintName() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-cn-1", new Date());
    tempEntity.newPolicyViolation(
        eval, tempEntity.newPolicy(org.getId(), "cn-policy", 9),
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        "hashCN", "CVE-2020-1000");

    List<PolicyViolation> violations = loadUnfixedViolations(app);
    List<PolicyViolationConstraintFacts> facts = loadConstraintFacts(violations);
    Map<String, String> namesById = loadConstraintNames(facts);

    // Every violation with a constraint-facts id whose facts row carries a name resolves to it.
    String factsId = violations.get(0).getConstraintFactsId();
    assertThat(namesById).containsKey(factsId);
    assertThat(namesById.get(factsId)).isNotBlank();
  }

  /**
   * The load-once refactor: {@code buildApplicationStageSVDocs} loads the constraint-facts batch a
   * single time and shares it between the first-seen join and the constraint-name lookup. Spy the DAO
   * and drive both consumers off one {@code loadConstraintFacts} call; {@code getByIds} must run once,
   * not once per consumer.
   */
  @Test
  public void loadConstraintFacts_sharedBetweenConsumers_getByIdsInvokedOnce() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(org);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(), StageTypes.BUILD.getId(), "scan-once-1", new Date());
    tempEntity.newPolicyViolation(
        eval, tempEntity.newPolicy(org.getId(), "once-policy", 9),
        com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates("g", "a", "1"),
        "hashOnce", "CVE-2020-2000");
    List<PolicyViolation> violations = loadUnfixedViolations(app);

    PolicyViolationConstraintFactsDAO daoSpy = spy(policyViolationConstraintFactsDAO);
    java.lang.reflect.Field field =
        DocumentBuilderHelper.class.getDeclaredField("policyViolationConstraintFactsDAO");
    field.setAccessible(true);
    Object original = field.get(documentBuilderHelper);
    field.set(documentBuilderHelper, daoSpy);
    try {
      List<PolicyViolationConstraintFacts> facts = loadConstraintFacts(violations);
      Map<String, Long> firstSeen = firstSeenEpochMsByVulnRefId(violations, facts);
      Map<String, String> names = loadConstraintNames(facts);

      // Both consumers produced their output from the single shared batch...
      assertThat(firstSeen).isNotEmpty();
      assertThat(names).isNotEmpty();
      // ...and the batch fetch ran exactly once, not once per consumer.
      verify(daoSpy, times(1)).getByIds(any());
    }
    finally {
      field.set(documentBuilderHelper, original);
    }
  }

  private List<PolicyViolation> loadUnfixedViolations(final Application app) {
    return loadUnfixedViolationsForStage(app, StageTypes.BUILD);
  }

  @Inject
  private PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  private List<PolicyViolation> loadUnfixedViolationsForStage(final Application app, final StageType stage) {
    return policyViolationDAO.getUnfixedByOwnerIdAndStageId(app.getId(), stage.getId());
  }

  /**
   * Drives the production code path: load the constraint facts once, then feed the shared list into
   * the first-seen join exactly as {@code buildApplicationStageSVDocs} does.
   */
  private Map<String, Long> firstSeenEpochMsByVulnRefId(final List<PolicyViolation> violations) throws Exception {
    return firstSeenEpochMsByVulnRefId(violations, loadConstraintFacts(violations));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Long> firstSeenEpochMsByVulnRefId(
      final List<PolicyViolation> violations,
      final List<PolicyViolationConstraintFacts> factsList) throws Exception
  {
    java.lang.reflect.Method method =
        DocumentBuilderHelper.class.getDeclaredMethod("firstSeenEpochMsByVulnRefId", List.class, List.class);
    method.setAccessible(true);
    return (Map<String, Long>) method.invoke(documentBuilderHelper, violations, factsList);
  }

  @SuppressWarnings("unchecked")
  private List<PolicyViolationConstraintFacts> loadConstraintFacts(
      final List<PolicyViolation> violations) throws Exception
  {
    java.lang.reflect.Method method =
        DocumentBuilderHelper.class.getDeclaredMethod("loadConstraintFacts", List.class);
    method.setAccessible(true);
    return (List<PolicyViolationConstraintFacts>) method.invoke(documentBuilderHelper, violations);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> loadConstraintNames(
      final List<PolicyViolationConstraintFacts> factsList) throws Exception
  {
    java.lang.reflect.Method method =
        DocumentBuilderHelper.class.getDeclaredMethod("loadConstraintNames", List.class);
    method.setAccessible(true);
    return (Map<String, String>) method.invoke(documentBuilderHelper, factsList);
  }

  private static PolicyViolation violation(
      final String hash,
      final int threatLevel,
      final PolicyThreatCategory category,
      final Date waiveTime,
      final String autoWaiverId)
  {
    return violation(hash, threatLevel, category, waiveTime, autoWaiverId, false);
  }

  private static PolicyViolation violation(
      final String hash,
      final int threatLevel,
      final PolicyThreatCategory category,
      final Date waiveTime,
      final String autoWaiverId,
      final boolean legacy)
  {
    PolicyViolation v = mock(PolicyViolation.class);
    lenient().when(v.getHash()).thenReturn(hash);
    lenient().when(v.getThreatLevel()).thenReturn(threatLevel);
    lenient().when(v.getThreatCategory()).thenReturn(category);
    lenient().when(v.getWaiveTime()).thenReturn(waiveTime);
    lenient().when(v.getAutoPolicyWaiverId()).thenReturn(autoWaiverId);
    lenient().when(v.isLegacyViolation()).thenReturn(legacy);
    return v;
  }

  /**
   * An unknown violation-state token must degrade gracefully (sort last) rather than throw, so a future
   * state or unexpected token does not abort the indexing batch.
   */
  @Test
  public void testStateSortPriority_UnknownTokenSortsLastWithoutThrowing() throws Exception {
    java.lang.reflect.Method method =
        DocumentBuilderHelper.class.getDeclaredMethod("stateSortPriority", String.class);
    method.setAccessible(true);

    assertThat((int) method.invoke(null, "open")).isEqualTo(0);
    assertThat((int) method.invoke(null, "waived")).isEqualTo(1);
    assertThat((int) method.invoke(null, "legacy")).isEqualTo(2);
    assertThat((int) method.invoke(null, "some-future-state")).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void deriveWaiverStatus_active_returnsActive() {
    PolicyViolation violation = new PolicyViolation();
    assertThat(DocumentBuilderHelper.deriveWaiverStatus(violation))
        .isEqualTo(DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_ACTIVE);
  }

  @Test
  public void deriveWaiverStatus_manuallyWaived_returnsWaived() {
    PolicyViolation violation = new PolicyViolation();
    violation.setWaiveTime(new Date());
    assertThat(DocumentBuilderHelper.deriveWaiverStatus(violation))
        .isEqualTo(DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_WAIVED);
  }

  @Test
  public void deriveWaiverStatus_legacyOnly_returnsLegacy() {
    PolicyViolation violation = new PolicyViolation();
    violation.setLegacyViolationTime(new Date());
    assertThat(DocumentBuilderHelper.deriveWaiverStatus(violation))
        .isEqualTo(DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_LEGACY);
  }

  @Test
  public void deriveWaiverStatus_waivedAndLegacy_waiverWins() {
    // Single-valued field: waiver precedence over legacy. A waived+legacy violation indexes as Waived
    // and surfaces under WAIVED, not LEGACY (documented divergence from the SQL multi-membership path).
    PolicyViolation violation = new PolicyViolation();
    violation.setWaiveTime(new Date());
    violation.setLegacyViolationTime(new Date());
    assertThat(DocumentBuilderHelper.deriveWaiverStatus(violation))
        .isEqualTo(DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_WAIVED);
  }

  @Test
  public void deriveWaiverStatus_autoWaivedAndLegacy_autoWaiverWins() {
    PolicyViolation violation = new PolicyViolation();
    violation.setAutoPolicyWaiverId("auto-waiver-1");
    violation.setLegacyViolationTime(new Date());
    assertThat(DocumentBuilderHelper.deriveWaiverStatus(violation))
        .isEqualTo(DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_AUTO_WAIVED);
  }
}
