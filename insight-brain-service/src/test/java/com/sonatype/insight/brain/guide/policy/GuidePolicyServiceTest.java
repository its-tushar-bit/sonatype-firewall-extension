/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuidePolicyServiceTest
{
  private static final String PURL = "pkg:maven/org.example/lib@1.0";

  private static final String MAVEN_PURL = "pkg:maven/org.example/lib@1.0?type=jar";

  private static final String APP_PUBLIC_ID = "sandbox-application";

  private static final String APP_INTERNAL_ID = "internal-app-id";

  @Mock
  private GuidePolicyEvaluator guidePolicyEvaluator;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OwnerDAO ownerDAO;

  @Mock
  private Application application;

  @Mock
  private Owner owner;

  @Mock
  private PermissionService permissionService;

  private GuidePolicyService underTest;

  @Before
  public void setUp() {
    // The detail/MCP gate calls SecurityUtils.getSubject(), so bind a subject for the thread.
    SecurityManager securityManager = mock(SecurityManager.class);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(new Subject.Builder(securityManager).buildSubject());
    // Default: caller HAS EVALUATE_COMPONENT → detail surfaces return the FULL card. Tests that
    // exercise the unauthorized path override this to deny. Lenient: not every test reaches the gate.
    lenient().when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.of(Permission.EVALUATE_COMPONENT));
    underTest = new GuidePolicyService(guidePolicyEvaluator, applicationDAO, ownerDAO, permissionService);
  }

  @After
  public void tearDown() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  // --- MCP: evaluatePolicies / owner resolution ----------------------------------------------------

  @Test
  public void resolvesByApplicationPublicId_projectsSlimCompliance() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL)), eq(APP_INTERNAL_ID), any(Stage.class)))
        .thenReturn(Map.of(PURL, compliance));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), APP_PUBLIC_ID, null);

    McpPolicyCompliance entry = result.get(PURL);
    assertThat(entry).isNotNull();
    assertThat(entry.compliant()).isTrue();
    assertThat(entry.stage()).isEqualTo("release");
    assertThat(entry.ownerId()).isEqualTo("owner-id");
    // Summary is reused verbatim from the API shape; violations are projected to the slim MCP shape.
    assertThat(entry.summary()).isSameAs(compliance.summary());
    assertThat(entry.violations()).isEmpty();
  }

  @Test
  public void multiplePurls_evaluatedInSingleBatch() {
    String purl2 = "pkg:npm/lodash@4.17.21";
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuidePolicyCompliance c1 = compliantOf();
    GuidePolicyCompliance c2 = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL, purl2)), eq(APP_INTERNAL_ID), any(Stage.class)))
        .thenReturn(Map.of(PURL, c1, purl2, c2));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL, purl2), APP_PUBLIC_ID, null);

    assertThat(result).containsOnlyKeys(PURL, purl2);
    // A single batched evaluator call serves the whole batch, not one per PURL.
    verify(guidePolicyEvaluator, times(1)).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void unknownApplication_butKnownOwner_resolvesViaOwnerDAO() {
    String orgId = "some-org-id";
    when(applicationDAO.getByIdOrPublicId(orgId)).thenReturn(null);
    when(ownerDAO.getById(orgId)).thenReturn(owner);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL)), eq(orgId), any(Stage.class)))
        .thenReturn(Map.of(PURL, compliance));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), orgId, null);

    assertThat(result.get(PURL)).isNotNull();
    assertThat(result.get(PURL).compliant()).isTrue();
  }

  @Test
  public void unknownApplicationAndUnknownOwner_returnsEmptySoftFail() {
    when(applicationDAO.getByIdOrPublicId("nonexistent")).thenReturn(null);
    when(ownerDAO.getById("nonexistent")).thenReturn(null);

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), "nonexistent", null);

    assertThat(result).isEmpty();
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void emptyPurls_returnsEmpty_andDoesNotResolveOwner() {
    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(), APP_PUBLIC_ID, null);

    assertThat(result).isEmpty();
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void blankApplicationId_defaultsToRootOrganization() {
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL)), eq(Organization.ROOT_ORGANIZATION_ID), any(Stage.class)))
        .thenReturn(Map.of(PURL, compliance));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), "", null);

    assertThat(result.get(PURL)).isNotNull();
    assertThat(result.get(PURL).compliant()).isTrue();
  }

  @Test
  public void nullApplicationId_defaultsToRootOrganization() {
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL)), eq(Organization.ROOT_ORGANIZATION_ID), any(Stage.class)))
        .thenReturn(Map.of(PURL, compliance));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), null, null);

    assertThat(result.get(PURL)).isNotNull();
  }

  @Test
  public void invalidStage_throwsIllegalArgumentException() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);

    assertThatThrownBy(() -> underTest.evaluatePolicies(List.of(PURL), APP_PUBLIC_ID, "not-a-stage"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not-a-stage");
  }

  @Test
  public void caseInsensitiveStageAccepted() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL)), eq(APP_INTERNAL_ID), any(Stage.class)))
        .thenReturn(Map.of(PURL, compliance));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), APP_PUBLIC_ID, "BUILD");

    assertThat(result.get(PURL)).isNotNull();
  }

  @Test
  public void evaluatorThrows_returnsEmptySoftFail() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    when(guidePolicyEvaluator.evaluate(anyList(), any(), any(Stage.class)))
        .thenThrow(new RuntimeException("boom"));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), APP_PUBLIC_ID, null);

    assertThat(result).isEmpty();
  }

  @Test
  public void evaluatorReturnsEmptyMap_returnsEmpty() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    when(guidePolicyEvaluator.evaluate(anyList(), any(), any(Stage.class))).thenReturn(Map.of());

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), APP_PUBLIC_ID, null);

    assertThat(result).isEmpty();
  }

  @Test
  public void inputPurlNotCanonical_matchesViaCanonicalization() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuidePolicyCompliance compliance = compliantOf();
    // The evaluator keys its result by canonical PURL.
    String canonicalPurl = "pkg:maven/org.example/lib@1.0";
    when(guidePolicyEvaluator.evaluate(anyList(), any(), any(Stage.class)))
        .thenReturn(Map.of(canonicalPurl, compliance));

    // Caller passes a non-canonical PURL (capitalized type); the result is keyed by that exact input,
    // matched to the evaluator's canonical key via canonicalization.
    String nonCanonicalPurl = "pkg:Maven/org.example/lib@1.0";
    Map<String, McpPolicyCompliance> result =
        underTest.evaluatePolicies(List.of(nonCanonicalPurl), APP_PUBLIC_ID, null);

    McpPolicyCompliance entry = result.get(nonCanonicalPurl);
    assertThat(entry).isNotNull();
    assertThat(entry.compliant()).isTrue();
    assertThat(entry.summary()).isSameAs(compliance.summary());
  }

  @Test
  public void evaluatePolicies_withoutEvaluateComponent_returnsBadgeOnly() {
    when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.noneOf(Permission.class));
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(PURL)), eq(APP_INTERNAL_ID), any(Stage.class)))
        .thenReturn(Map.of(PURL, compliance));

    Map<String, McpPolicyCompliance> result = underTest.evaluatePolicies(List.of(PURL), APP_PUBLIC_ID, null);

    McpPolicyCompliance entry = result.get(PURL);
    assertThat(entry).isNotNull();
    // Badge only: compliant + complianceLevel, no card — caller lacks EVALUATE_COMPONENT.
    assertThat(entry.compliant()).isTrue();
    assertThat(entry.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.PASS);
    assertThat(entry.summary()).isNull();
    assertThat(entry.violations()).isNull();
  }

  // --- REST: list surfaces attach slim, detail surfaces attach full --------------------------------

  @Test
  public void enrichComponentSearch_attachesSlimCompliance() {
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    when(guidePolicyEvaluator.evaluate(List.of(MAVEN_PURL))).thenReturn(Map.of(MAVEN_PURL, compliantOf()));

    ApiSearchResponse<ComponentDocument> result = underTest.enrichComponentSearch(upstream);

    GuideComponentDocument enriched = (GuideComponentDocument) result.hits().get(0);
    assertThat(enriched.policyCompliance()).isNotNull();
    assertThat(enriched.policyCompliance().compliant()).isTrue();
    assertThat(enriched.policyCompliance().summary()).isNull();
    assertThat(enriched.policyCompliance().violations()).isNull();
  }

  @Test
  public void enrichComponentDetail_withEvaluateComponent_attachesFullCompliance() {
    // Default setUp grants EVALUATE_COMPONENT → full card.
    GuideComponentDetailDocument upstream = mavenDetail();
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of(MAVEN_PURL))).thenReturn(Map.of(MAVEN_PURL, compliance));

    ComponentDetailDocument result = underTest.enrichComponentDetail(upstream);

    assertThat(((GuideComponentDetailDocument) result).policyCompliance()).isSameAs(compliance);
  }

  @Test
  public void enrichComponentDetail_withoutEvaluateComponent_attachesBadgeOnly() {
    when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.noneOf(Permission.class));
    GuideComponentDetailDocument upstream = mavenDetail();
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(List.of(MAVEN_PURL))).thenReturn(Map.of(MAVEN_PURL, compliance));

    ComponentDetailDocument result = underTest.enrichComponentDetail(upstream);

    GuidePolicyCompliance attached = ((GuideComponentDetailDocument) result).policyCompliance();
    // Detail is gated: a caller without EVALUATE_COMPONENT gets the badge, not the card.
    assertThat(attached.compliant()).isTrue();
    assertThat(attached.complianceLevel()).isEqualTo(GuidePolicyComplianceLevel.PASS);
    assertThat(attached.summary()).isNull();
    assertThat(attached.violations()).isNull();
  }

  // GUIDE-2821: programmatic cap on `limit` for the policy-enriched Guide search endpoints.
  // Implemented as a static helper (rather than @Max on the resource method) because the
  // resources implement contract interfaces from guide-api-contract and Hibernate Validator
  // (HV000151) forbids an overriding method from redefining parameter constraints.

  @Test
  public void requireLimitWithinPolicyEnrichmentCap_overCap_throws400() {
    assertThatThrownBy(() -> GuidePolicyService.requireLimitWithinPolicyEnrichmentCap(26))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("limit must not exceed 25");
  }

  @Test
  public void requireLimitWithinPolicyEnrichmentCap_atCap_allowed() {
    GuidePolicyService.requireLimitWithinPolicyEnrichmentCap(GuidePolicyService.MAX_POLICY_ENRICHED_LIMIT);
    // no exception
  }

  @Test
  public void requireLimitWithinPolicyEnrichmentCap_nullLimit_allowed() {
    // Callers that omit `limit` get the upstream default; the cap only constrains explicit values.
    GuidePolicyService.requireLimitWithinPolicyEnrichmentCap(null);
    // no exception
  }

  @Test
  public void enrichComponentSearch_noGuideHits_skipsEvaluate() {
    // A component with no resolvable PURL yields no purls — the evaluator must not be called.
    GuideComponentDocument noPurl = new GuideComponentDocument(
        null, null, null, "lib", "1.0", null, null, null, null, null, null, null, null, null, null);
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(noPurl), 1L, 0, 20, Map.of());

    ApiSearchResponse<ComponentDocument> result = underTest.enrichComponentSearch(upstream);

    assertThat(result).isSameAs(upstream);
    verify(guidePolicyEvaluator, never()).evaluate(anyList());
  }

  // --- REST: owner/stage-scoped enrichment (GUIDE-3045) --------------------------------------------

  @Test
  public void enrichComponentSearch_scoped_bothBlank_delegatesToUnscoped() {
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    when(guidePolicyEvaluator.evaluate(List.of(MAVEN_PURL))).thenReturn(Map.of(MAVEN_PURL, compliantOf()));

    ApiSearchResponse<ComponentDocument> result = underTest.enrichComponentSearch(upstream, null, null);

    GuideComponentDocument enriched = (GuideComponentDocument) result.hits().get(0);
    assertThat(enriched.policyCompliance()).isNotNull();
    // Byte-identical to the unscoped path: the 2-arg evaluate is used, not the 3-arg scoped one.
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void enrichComponentSearch_scoped_validAppOwner_attachesScopedCompliance() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(MAVEN_PURL)), eq(APP_INTERNAL_ID), any(Stage.class)))
        .thenReturn(Map.of(MAVEN_PURL, compliance));

    ApiSearchResponse<ComponentDocument> result =
        underTest.enrichComponentSearch(upstream, APP_PUBLIC_ID, null);

    GuideComponentDocument enriched = (GuideComponentDocument) result.hits().get(0);
    assertThat(enriched.policyCompliance()).isNotNull();
    assertThat(enriched.policyCompliance().compliant()).isTrue();
  }

  @Test
  public void enrichComponentDetail_scoped_validOrgOwner_attachesFullComplianceWithEffectiveOwnerId() {
    String orgId = "some-org-id";
    when(applicationDAO.getByIdOrPublicId(orgId)).thenReturn(null);
    when(ownerDAO.getById(orgId)).thenReturn(owner);
    when(owner.getType()).thenReturn(OwnerType.ORGANIZATION);
    GuideComponentDetailDocument upstream = mavenDetail();
    GuidePolicyCompliance compliance = compliantOf();
    when(guidePolicyEvaluator.evaluate(eq(List.of(MAVEN_PURL)), eq(orgId), any(Stage.class)))
        .thenReturn(Map.of(MAVEN_PURL, compliance));

    ComponentDetailDocument result = underTest.enrichComponentDetail(upstream, orgId, null);

    // Full card (not badge-only) since resolveScope already gated on EVALUATE_COMPONENT.
    GuidePolicyCompliance attached = ((GuideComponentDetailDocument) result).policyCompliance();
    assertThat(attached).isSameAs(compliance);
    assertThat(attached.ownerId()).isEqualTo("owner-id"); // effective owner, from compliantOf()'s fixture
  }

  @Test
  public void enrichComponentDetail_scoped_unknownOwner_omitsPolicyComplianceSilently() {
    when(applicationDAO.getByIdOrPublicId("nonexistent")).thenReturn(null);
    when(ownerDAO.getById("nonexistent")).thenReturn(null);
    GuideComponentDetailDocument upstream = mavenDetail();

    ComponentDetailDocument result = underTest.enrichComponentDetail(upstream, "nonexistent", null);

    // Mirrors MCP's silent soft-fail: no policyCompliance, no evaluator call — not an HTTP 400.
    assertThat(result).isSameAs(upstream);
    GuideComponentDetailDocument unchanged = (GuideComponentDetailDocument) result;
    assertThat(unchanged.policyCompliance()).isNull();
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void enrichComponentDetail_scoped_noEvaluateComponentOnExplicitOwner_omitsPolicyComplianceSilently() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.noneOf(Permission.class));
    GuideComponentDetailDocument upstream = mavenDetail();

    ComponentDetailDocument result = underTest.enrichComponentDetail(upstream, APP_PUBLIC_ID, null);

    // Since resolveScope already gates on EVALUATE_COMPONENT, a denied permission on an explicit
    // owner must omit the full violation card entirely — not fall back to a COMPLIANT_ONLY badge.
    GuideComponentDetailDocument unchanged = (GuideComponentDetailDocument) result;
    assertThat(unchanged.policyCompliance()).isNull();
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void enrichComponentDetail_scoped_blankOwnerNoEvaluateComponent_fallsBackToBadgeOnly() {
    // Regression for a permission-bypass: resolveScope's blank-ownerId branch skips
    // canSeePolicyDetail (that shortcut exists for the list/search surfaces, which show
    // COMPLIANT_ONLY to everyone regardless of permission). A blank ownerId with only an explicit
    // stage must not inherit that shortcut here — it must still gate the FULL card on permission,
    // same as the fully-unscoped (both blank) path does.
    when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.noneOf(Permission.class));
    GuideComponentDetailDocument upstream = mavenDetail();
    when(guidePolicyEvaluator.evaluate(eq(List.of(MAVEN_PURL)), eq(Organization.ROOT_ORGANIZATION_ID),
        any(Stage.class))).thenReturn(Map.of(MAVEN_PURL, compliantOf()));

    ComponentDetailDocument result = underTest.enrichComponentDetail(upstream, null, "build");

    GuidePolicyCompliance attached = ((GuideComponentDetailDocument) result).policyCompliance();
    assertThat(attached).isNotNull();
    // Badge only — no violations/summary/ownerId/stage, mirroring GuidePolicyResponseEnricher's
    // COMPLIANT_ONLY reduction — NOT the FULL card the bug would have attached unconditionally.
    assertThat(attached.violations()).isNull();
    assertThat(attached.summary()).isNull();
  }

  @Test
  public void enrichComponentSearch_scoped_nonReleaseStage_evaluatesAtThatStage() {
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());
    ArgumentCaptor<Stage> stageCaptor = ArgumentCaptor.forClass(Stage.class);
    when(guidePolicyEvaluator.evaluate(eq(List.of(MAVEN_PURL)), eq(Organization.ROOT_ORGANIZATION_ID),
        stageCaptor.capture())).thenReturn(Map.of(MAVEN_PURL, compliantOf()));

    // No ownerId → root org, but an explicit non-release stage must still reach the evaluator
    // (not silently default to release, which the bare 2-arg evaluate() would do).
    underTest.enrichComponentSearch(upstream, null, "build");

    assertThat(stageCaptor.getValue().getStageTypeId()).isEqualTo("build");
  }

  @Test
  public void enrichComponentSearch_scoped_unknownOwner_omitsPolicyComplianceSilently() {
    when(applicationDAO.getByIdOrPublicId("nonexistent")).thenReturn(null);
    when(ownerDAO.getById("nonexistent")).thenReturn(null);
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());

    ApiSearchResponse<ComponentDocument> result =
        underTest.enrichComponentSearch(upstream, "nonexistent", null);

    // Mirrors MCP's silent soft-fail: 200-equivalent response, no policyCompliance, no evaluator
    // call — not an HTTP 400.
    assertThat(result).isSameAs(upstream);
    GuideComponentDocument unchanged = (GuideComponentDocument) result.hits().get(0);
    assertThat(unchanged.policyCompliance()).isNull();
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void enrichComponentSearch_scoped_noEvaluateComponentOnExplicitOwner_omitsPolicyComplianceSilently() {
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    when(permissionService.validatePermission(any(), any(), any(), any()))
        .thenReturn(EnumSet.noneOf(Permission.class));
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());

    ApiSearchResponse<ComponentDocument> result =
        underTest.enrichComponentSearch(upstream, APP_PUBLIC_ID, null);

    // Stricter than the default no-ownerId path (which still shows a COMPLIANT_ONLY badge to
    // everyone): an explicit owner-scoped request with no permission gets NO enrichment at all.
    GuideComponentDocument unchanged = (GuideComponentDocument) result.hits().get(0);
    assertThat(unchanged.policyCompliance()).isNull();
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void enrichComponentSearch_scoped_invalidStage_throwsGuideApiException400() {
    GuideComponentDocument hit = mavenComponent();
    GuideComponentSearchResponse upstream = new GuideComponentSearchResponse(
        List.of(hit), 1L, 0, 20, Map.of());

    assertThatThrownBy(() -> underTest.enrichComponentSearch(upstream, APP_PUBLIC_ID, "not-a-stage"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("not-a-stage")
        .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    verifyNoInteractions(guidePolicyEvaluator);
  }

  @Test
  public void filterRecommendations_scoped_bothBlank_delegatesToUnscoped() {
    GuideRecommendationResult upstream = recommendationResultWithOneCandidate();
    // Empty compliance map here means "no evaluation data for this candidate", which
    // GuideRecommendationsPolicyFilter treats as compliant (kept) — distinct from an empty
    // purlByVersion (unparseable parent/blank version), which is what actually blocks everything.
    when(guidePolicyEvaluator.evaluate(anyList())).thenReturn(Map.of());

    GuideRecommendationResult result = underTest.filterRecommendations(upstream, PURL, null, null);

    assertThat(result.outcome()).isEqualTo(RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS);
    // Byte-identical to the unscoped path: the 2-arg evaluate is used, not the 4-arg scoped one.
    verify(guidePolicyEvaluator).evaluate(anyList());
    verify(guidePolicyEvaluator, never()).evaluate(anyList(), any(), any(Stage.class));
  }

  @Test
  public void filterRecommendations_scoped_unknownOwner_returnsUpstreamUnfiltered() {
    when(applicationDAO.getByIdOrPublicId("nonexistent")).thenReturn(null);
    when(ownerDAO.getById("nonexistent")).thenReturn(null);
    GuideRecommendationResult upstream = recommendationResultWithOneCandidate();

    GuideRecommendationResult result = underTest.filterRecommendations(upstream, PURL, "nonexistent", null);

    // Must NOT run through GuideRecommendationsPolicyFilter with an empty compliance map — that
    // would wrongly report BLOCKED_BY_POLICY just because the caller asked for owner scoping.
    assertThat(result).isSameAs(upstream);
    verify(guidePolicyEvaluator, never()).evaluate(anyList());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void filterRecommendations_scoped_validOwner_filtersNonCompliantCandidates() {
    // Exercises the actual filtering behavior of the scoped variant — evaluate(purls, ownerId,
    // stage) then GuideRecommendationsPolicyFilter.apply — not just the two soft-fail/delegate
    // paths already covered above.
    when(applicationDAO.getByIdOrPublicId(APP_PUBLIC_ID)).thenReturn(application);
    when(application.getId()).thenReturn(APP_INTERNAL_ID);
    GuideRecommendationResult upstream = recommendationResultWithOneCandidate();
    ArgumentCaptor<List<String>> purlsCaptor = ArgumentCaptor.forClass(List.class);
    when(guidePolicyEvaluator.evaluate(purlsCaptor.capture(), eq(APP_INTERNAL_ID), any(Stage.class)))
        .thenAnswer(invocation -> Map.of(
            purlsCaptor.getValue().get(0), GuidePolicyCompliance.badge(GuidePolicyComplianceLevel.FAIL)));

    GuideRecommendationResult result = underTest.filterRecommendations(upstream, PURL, APP_PUBLIC_ID, null);

    // The upstream result's one candidate is evaluated as non-compliant, so it's dropped — no
    // survivors left means the filter reports BLOCKED_BY_POLICY.
    assertThat(result.outcome()).isEqualTo(RecommendationResponse.Outcome.BLOCKED_BY_POLICY);
    verify(guidePolicyEvaluator).evaluate(anyList(), eq(APP_INTERNAL_ID), any(Stage.class));
  }

  private static GuideRecommendationResult recommendationResultWithOneCandidate() {
    return new GuideRecommendationResult(
        RecommendationResponse.Outcome.FOUND_RECOMMENDATIONS,
        new RecommendedVersionInfo("1.0", "0", Map.of(), Map.of(), Map.of(), List.of(), 85, 10.0, null),
        List.of(new RecommendedVersionInfo("1.1", "0", Map.of(), Map.of(), Map.of(), List.of(), 90, null, null)));
  }

  private static GuideComponentDocument mavenComponent() {
    return new GuideComponentDocument(
        "maven", null, "org.example", "lib", "1.0", null,
        null, null, null, null, null, null, null, null, null);
  }

  private static GuideComponentDetailDocument mavenDetail() {
    return new GuideComponentDetailDocument(
        "maven", null, "org.example", "lib", "1.0", null, null, null,
        null, null, null, null, null, null, null, null, null);
  }

  private static GuidePolicyCompliance compliantOf() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("SECURITY", 0);
    counts.put("LICENSE", 0);
    counts.put("QUALITY", 0);
    counts.put("OTHER", 0);
    return new GuidePolicyCompliance(true, GuidePolicyComplianceLevel.PASS, "release", "owner-id",
        new GuidePolicyComplianceSummary(0, "none", 0, 0, counts), List.of());
  }
}
