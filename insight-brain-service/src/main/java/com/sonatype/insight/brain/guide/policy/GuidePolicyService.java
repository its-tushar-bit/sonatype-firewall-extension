/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.RecommendedVersionInfo;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.purl.GuidePurlAssembler;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;
import com.sonatype.insight.brain.guide.mcp.policy.McpStageResolver;
import com.sonatype.insight.brain.guide.mcp.policy.McpPolicyAnnotator;
import com.sonatype.insight.brain.guide.policy.GuidePolicyResponseEnricher.PolicyDetail;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single entry point for Guide policy compliance. Presentation layers — the JAX-RS resources and
 * the MCP servlet — call this and do no policy work themselves: they hand it an upstream search/detail
 * payload (or a PURL + scope) and get back the policy-enriched/filtered equivalent.
 *
 * <p>
 * It owns the orchestration (collect PURLs &rarr; evaluate &rarr; attach/filter) and the MCP
 * owner-resolution, and delegates the specifics to helpers: {@link GuidePolicyEvaluator} (the HDS +
 * Drools engine), {@link GuidePolicyResponseEnricher} (per-element attach), {@link
 * GuideRecommendationsPolicyFilter} (candidate filtering), and {@link McpStageResolver} (stage parsing).
 *
 * <p>
 * List surfaces attach only {@code policyCompliance.compliant}; single-component detail surfaces attach
 * the full shape (see {@link PolicyDetail}).
 */
@Named
@Singleton
public class GuidePolicyService
    implements McpPolicyAnnotator
{
  private static final Logger log = LoggerFactory.getLogger(GuidePolicyService.class);

  private static final PolicyDetail COMPLIANT_ONLY = PolicyDetail.COMPLIANT_ONLY;

  private static final PolicyDetail FULL = PolicyDetail.FULL;

  /**
   * Stop-gap cap on the {@code limit} query parameter for Guide search endpoints whose hits feed
   * policy enrichment. Each returned hit incurs one HDS fetch + one Drools session on the shared
   * request thread; an unbounded {@code limit} would let an authenticated caller force a ~10k
   * Drools run (OpenSearch's default {@code index.max_result_window}). 25 is deliberately
   * conservative — raise based on customer feedback rather than start high. Kept here (next to the
   * enrichment cost it bounds) rather than on the transport layer.
   */
  public static final int MAX_POLICY_ENRICHED_LIMIT = 25;

  /**
   * Reject {@code limit > 25} with HTTP 400. Done programmatically rather than via
   * {@code @Max(25)} on the resource method because these resources implement contract interfaces
   * from {@code guide-api-contract} and Hibernate Validator (HV000151) forbids an overriding
   * method from redefining the parameter-constraint configuration of its supertype. Callers that
   * omit {@code limit} are unaffected — null short-circuits. Uses the Sonatype
   * {@link BadRequestException} (mapped via {@code @HttpStatusCode(400)}) for body-shape
   * consistency with the rest of IQ's 400 responses.
   */
  public static void requireLimitWithinPolicyEnrichmentCap(Integer limit) {
    if (limit != null && limit > MAX_POLICY_ENRICHED_LIMIT) {
      // INFO (not WARN): caller-supplied value out of range, not a server problem. Logged so we
      // can see frequency in prod — the cap's intentional stop-gap framing means we'll raise it
      // if legitimate clients keep hitting it.
      log.info("Guide search rejected: limit={} exceeds policy-enrichment cap of {}", limit, MAX_POLICY_ENRICHED_LIMIT);
      throw new BadRequestException(
          "limit must not exceed " + MAX_POLICY_ENRICHED_LIMIT + " for policy-enriched Guide search endpoints");
    }
  }

  /**
   * Validates {@code stage} before the resource method makes its upstream call, so an invalid
   * value fails fast with a 400 instead of wasting a search/HDS round trip that {@link
   * #resolveScope} would only reject afterward. {@link #resolveScope} re-validates {@code stage}
   * itself (it needs the resolved {@link Stage} for evaluation) — that duplication is intentional
   * and cheap; this method exists purely for the resource layer to call up front.
   *
   * @throws GuideApiException 400 if {@code stage} is non-blank and not a valid stage id.
   */
  public static void requireValidStage(String stage) {
    try {
      McpStageResolver.resolve(stage);
    }
    catch (IllegalArgumentException e) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Invalid stage: " + stage);
    }
  }

  private final GuidePolicyEvaluator guidePolicyEvaluator;

  private final ApplicationDAO applicationDAO;

  private final OwnerDAO ownerDAO;

  private final PermissionService permissionService;

  @Inject
  public GuidePolicyService(
      GuidePolicyEvaluator guidePolicyEvaluator,
      ApplicationDAO applicationDAO,
      OwnerDAO ownerDAO,
      PermissionService permissionService)
  {
    this.guidePolicyEvaluator = guidePolicyEvaluator;
    this.applicationDAO = applicationDAO;
    this.ownerDAO = ownerDAO;
    this.permissionService = permissionService;
  }

  // --- REST: search/list surfaces (slim — compliant flag only) -------------------------------------

  public ApiSearchResponse<ComponentDocument> enrichComponentSearch(
      ApiSearchResponse<ComponentDocument> upstream)
  {
    return enrich(upstream, GuideComponentDocument.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichComponent(g, c, COMPLIANT_ONLY),
        hits -> new GuideComponentSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()));
  }

  /**
   * Owner/stage-scoped variant of {@link #enrichComponentSearch(ApiSearchResponse)}. Both blank
   * delegates straight to the unscoped overload — byte-identical to the GUIDE-2745 default behavior.
   * Otherwise evaluates against the resolved owner/stage, or leaves {@code upstream} untouched
   * ({@code policyCompliance} omitted) when the owner is unknown or the caller lacks {@link
   * Permission#EVALUATE_COMPONENT} on it — see {@link #resolveScope}.
   */
  public ApiSearchResponse<ComponentDocument> enrichComponentSearch(
      ApiSearchResponse<ComponentDocument> upstream,
      String ownerId,
      String stage)
  {
    if (isBlank(ownerId) && isBlank(stage)) {
      return enrichComponentSearch(upstream);
    }
    ScopedEvaluation scope = resolveScope(ownerId, stage);
    if (!scope.enrichmentAllowed()) {
      return upstream;
    }
    return enrich(upstream, GuideComponentDocument.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichComponent(g, c, COMPLIANT_ONLY),
        hits -> new GuideComponentSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()),
        scope.ownerId(), scope.stage());
  }

  public ApiSearchResponse<ComponentDetailDocument> enrichComponentDetailSearch(
      ApiSearchResponse<ComponentDetailDocument> upstream)
  {
    return enrich(upstream, GuideComponentDetailDocument.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichDetail(g, c, COMPLIANT_ONLY),
        hits -> new GuideComponentDetailSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()));
  }

  /** Owner/stage-scoped variant of {@link #enrichComponentDetailSearch(ApiSearchResponse)}. */
  public ApiSearchResponse<ComponentDetailDocument> enrichComponentDetailSearch(
      ApiSearchResponse<ComponentDetailDocument> upstream,
      String ownerId,
      String stage)
  {
    if (isBlank(ownerId) && isBlank(stage)) {
      return enrichComponentDetailSearch(upstream);
    }
    ScopedEvaluation scope = resolveScope(ownerId, stage);
    if (!scope.enrichmentAllowed()) {
      return upstream;
    }
    return enrich(upstream, GuideComponentDetailDocument.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichDetail(g, c, COMPLIANT_ONLY),
        hits -> new GuideComponentDetailSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()),
        scope.ownerId(), scope.stage());
  }

  public ApiSearchResponse<SearchResult> enrichGlobalSearch(ApiSearchResponse<SearchResult> upstream) {
    // Global-search hits are GuideComponentDocument instances (SearchApiClient.globalSearch returns a
    // GuideGlobalSearchResponse of those), so GuideComponentDocument is the type discriminator here.
    // If a future SearchResult subtype isn't a GuideComponentDocument it's simply left un-enriched
    // rather than mis-handled — the enrich() type filter skips non-matching hits.
    return enrich(upstream, GuideComponentDocument.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichComponent(g, c, COMPLIANT_ONLY),
        hits -> new GuideGlobalSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()));
  }

  /** Owner/stage-scoped variant of {@link #enrichGlobalSearch(ApiSearchResponse)}. */
  public ApiSearchResponse<SearchResult> enrichGlobalSearch(
      ApiSearchResponse<SearchResult> upstream,
      String ownerId,
      String stage)
  {
    if (isBlank(ownerId) && isBlank(stage)) {
      return enrichGlobalSearch(upstream);
    }
    ScopedEvaluation scope = resolveScope(ownerId, stage);
    if (!scope.enrichmentAllowed()) {
      return upstream;
    }
    return enrich(upstream, GuideComponentDocument.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichComponent(g, c, COMPLIANT_ONLY),
        hits -> new GuideGlobalSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()),
        scope.ownerId(), scope.stage());
  }

  public ApiSearchResponse<AffectedComponentVersion> enrichAffectedSearch(
      ApiSearchResponse<AffectedComponentVersion> upstream)
  {
    return enrich(upstream, GuideAffectedComponentVersion.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichAffected(g, c, COMPLIANT_ONLY),
        hits -> new GuideAffectedComponentVersionSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()));
  }

  /** Owner/stage-scoped variant of {@link #enrichAffectedSearch(ApiSearchResponse)}. */
  public ApiSearchResponse<AffectedComponentVersion> enrichAffectedSearch(
      ApiSearchResponse<AffectedComponentVersion> upstream,
      String ownerId,
      String stage)
  {
    if (isBlank(ownerId) && isBlank(stage)) {
      return enrichAffectedSearch(upstream);
    }
    ScopedEvaluation scope = resolveScope(ownerId, stage);
    if (!scope.enrichmentAllowed()) {
      return upstream;
    }
    return enrich(upstream, GuideAffectedComponentVersion.class,
        GuidePurlAssembler::purlFor,
        (g, c) -> GuidePolicyResponseEnricher.enrichAffected(g, c, COMPLIANT_ONLY),
        hits -> new GuideAffectedComponentVersionSearchResponse(
            hits, upstream.total(), upstream.offset(), upstream.limit(), upstream.aggregations()),
        scope.ownerId(), scope.stage());
  }

  // --- REST: single-component detail surfaces (full shape) -----------------------------------------

  public ComponentDetailDocument enrichComponentDetail(ComponentDetailDocument upstream) {
    if (!(upstream instanceof GuideComponentDetailDocument detail)) {
      return upstream;
    }
    String purl = GuidePurlAssembler.purlFor(detail);
    if (purl == null) {
      return upstream;
    }
    Map<String, GuidePolicyCompliance> compliance = guidePolicyEvaluator.evaluate(List.of(purl));
    // The full violation/threat/waiver card requires EVALUATE_COMPONENT on the (root) org — the same
    // permission the MCP path and IQ's component-eval API require. Callers without it still get the
    // badge (compliant + complianceLevel), just not the card. The slim search surfaces are badge-only
    // for everyone, so this single-component detail surface is the only one that needs gating.
    PolicyDetail policyDetail =
        canSeePolicyDetail(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID) ? FULL : COMPLIANT_ONLY;
    return GuidePolicyResponseEnricher.enrichDetail(detail, compliance, policyDetail);
  }

  /**
   * Owner/stage-scoped variant of {@link #enrichComponentDetail(ComponentDetailDocument)}. An explicit
   * {@code ownerId} is all-or-nothing: {@link #resolveScope} already applies the {@code
   * EVALUATE_COMPONENT} gate for that specific owner, so once enrichment is allowed here the full card
   * is attached unconditionally. But a <b>blank</b> {@code ownerId} (only {@code stage} given) resolves
   * to the root org in {@code resolveScope} without a permission check — that shortcut exists for the
   * list/search surfaces, which show the {@code COMPLIANT_ONLY} badge to everyone regardless of
   * permission. This method is the one surface that must not inherit that shortcut, so it re-applies
   * the same {@code canSeePolicyDetail} gate the unscoped path uses whenever {@code ownerId} is blank.
   */
  public ComponentDetailDocument enrichComponentDetail(
      ComponentDetailDocument upstream,
      String ownerId,
      String stage)
  {
    if (isBlank(ownerId) && isBlank(stage)) {
      return enrichComponentDetail(upstream);
    }
    if (!(upstream instanceof GuideComponentDetailDocument detail)) {
      return upstream;
    }
    String purl = GuidePurlAssembler.purlFor(detail);
    if (purl == null) {
      return upstream;
    }
    ScopedEvaluation scope = resolveScope(ownerId, stage);
    if (!scope.enrichmentAllowed()) {
      return upstream;
    }
    PolicyDetail policyDetail = isBlank(ownerId)
        ? (canSeePolicyDetail(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID) ? FULL : COMPLIANT_ONLY)
        : FULL;
    Map<String, GuidePolicyCompliance> compliance =
        guidePolicyEvaluator.evaluate(List.of(purl), scope.ownerId(), scope.stage());
    return GuidePolicyResponseEnricher.enrichDetail(detail, compliance, policyDetail);
  }

  // --- REST: recommendations (filter non-compliant candidates) -------------------------------------

  public GuideRecommendationResult filterRecommendations(GuideRecommendationResult upstream, String parentPurl) {
    if (upstream == null) {
      return upstream;
    }
    // Build the candidate evaluation PURLs once, keyed by version. The filter looks each candidate's
    // PURL up by version in this same map rather than rebuilding it — the two PURL strings must be
    // byte-identical or the compliance-map lookup misses, so they're produced in exactly one place.
    Map<String, String> purlByVersion = candidatePurlsByVersion(parentPurl, upstream);
    Map<String, GuidePolicyCompliance> compliance = purlByVersion.isEmpty()
        ? Map.of()
        : guidePolicyEvaluator.evaluate(new ArrayList<>(purlByVersion.values()));
    return GuideRecommendationsPolicyFilter.apply(upstream, purlByVersion, compliance);
  }

  /**
   * Owner/stage-scoped variant of {@link #filterRecommendations(GuideRecommendationResult, String)}.
   * When the owner is unknown or unauthorized, {@code upstream} is returned <b>unfiltered</b> rather
   * than run through {@link GuideRecommendationsPolicyFilter#apply}: that filter treats a candidate
   * absent from the compliance map as {@code BLOCKED_BY_POLICY} (the correct call for a genuine
   * HDS/Drools failure), but applying that here would wrongly block every recommendation just because
   * the caller requested owner scoping it isn't authorized for.
   */
  public GuideRecommendationResult filterRecommendations(
      GuideRecommendationResult upstream,
      String parentPurl,
      String ownerId,
      String stage)
  {
    if (upstream == null) {
      return upstream;
    }
    if (isBlank(ownerId) && isBlank(stage)) {
      return filterRecommendations(upstream, parentPurl);
    }
    ScopedEvaluation scope = resolveScope(ownerId, stage);
    if (!scope.enrichmentAllowed()) {
      return upstream;
    }
    Map<String, String> purlByVersion = candidatePurlsByVersion(parentPurl, upstream);
    Map<String, GuidePolicyCompliance> compliance = purlByVersion.isEmpty()
        ? Map.of()
        : guidePolicyEvaluator.evaluate(new ArrayList<>(purlByVersion.values()), scope.ownerId(), scope.stage());
    return GuideRecommendationsPolicyFilter.apply(upstream, purlByVersion, compliance);
  }

  // --- MCP: McpPolicyAnnotator -------------------------------------------------------------------------

  @Override
  public Map<String, McpPolicyCompliance> evaluatePolicies(
      List<String> purls,
      String applicationId,
      String stage)
  {
    if (purls == null || purls.isEmpty()) {
      return Map.of();
    }
    ResolvedOwner owner = resolveOwner(applicationId);
    if (owner == null) {
      // DEBUG, not INFO/WARN: an unknown applicationId is a caller-supplied soft-fail (per the
      // evaluatePolicies contract), not a server problem, so it shouldn't spam logs when callers
      // probe with arbitrary or misconfigured ids.
      log.debug("MCP policy evaluation skipped: applicationId={} not found as application or owner", applicationId);
      return Map.of();
    }
    Stage resolvedStage = McpStageResolver.resolve(stage);

    // Single batched evaluation for the whole request: the evaluator does one HDS fetch + one Drools
    // session for all PURLs (owner/stage are request-level), rather than once per PURL. Evaluation
    // itself is not permission-gated — the badge (compliant + complianceLevel) is shown to any
    // authenticated, licensed caller; only the detail card is gated below.
    Map<String, GuidePolicyCompliance> byCanonicalPurl;
    try {
      byCanonicalPurl = guidePolicyEvaluator.evaluate(purls, owner.id(), resolvedStage);
    }
    catch (Exception e) {
      log.warn("MCP policy evaluation failed for {} purls, owner={}, stage={}: {}",
          purls.size(), owner.id(), resolvedStage.getStageTypeId(), e.getMessage());
      return Map.of();
    }

    // The full violation/threat/waiver card requires EVALUATE_COMPONENT on the resolved owner; a
    // caller without it gets the badge only. Same gate (and permission) the REST detail surface uses.
    boolean detail = canSeePolicyDetail(owner.type(), owner.id());

    // The evaluator keys its map by canonical PURL; re-key by the caller's exact input PURL (matched
    // via the same canonicalization) so callers look up by the string they passed.
    Map<String, McpPolicyCompliance> out = new HashMap<>();
    for (String purl : purls) {
      GuidePolicyCompliance compliance = byCanonicalPurl.get(canonicalize(purl));
      if (compliance != null) {
        out.put(purl, detail ? McpPolicyCompliance.from(compliance) : McpPolicyCompliance.badge(compliance));
      }
    }
    return out;
  }

  /**
   * Non-throwing check: does the current subject hold {@link Permission#EVALUATE_COMPONENT} on the
   * given owner? Gates disclosure of the full policy card (violations, threat levels, waiver detail);
   * the badge ({@code compliant} + {@code complianceLevel}) is unconditional. Mirrors the permission
   * IQ's component-evaluation APIs require, applied uniformly to the REST detail and MCP surfaces.
   */
  private boolean canSeePolicyDetail(OwnerType ownerType, String ownerId) {
    return permissionService
        .validatePermission(SecurityUtils.getSubject(), ownerType, ownerId, EnumSet.of(Permission.EVALUATE_COMPONENT))
        .contains(Permission.EVALUATE_COMPONENT);
  }

  private static String canonicalize(String purl) {
    try {
      return new PackageURL(purl).canonicalize();
    }
    catch (MalformedPackageURLException e) {
      return purl;
    }
  }

  // --- internals ------------------------------------------------------------------------------------

  /**
   * Shared list-enrichment skeleton: attach compliance to every {@code guideType} hit, leave other
   * hits untouched, rebuild the concrete response. Returns {@code upstream} unchanged when it is empty
   * or carries no Guide-typed hits (so no {@code evaluate([])} call is made).
   */
  private <T, G extends T> ApiSearchResponse<T> enrich(
      ApiSearchResponse<T> upstream,
      Class<G> guideType,
      Function<G, String> purlOf,
      BiFunction<G, Map<String, GuidePolicyCompliance>, G> enrichOne,
      Function<List<T>, ApiSearchResponse<T>> rebuild)
  {
    return enrich(upstream, guideType, purlOf, enrichOne, rebuild, null, null);
  }

  /** Owner/stage-scoped variant: {@code ownerId}/{@code stage} null means root-org/release. */
  private <T, G extends T> ApiSearchResponse<T> enrich(
      ApiSearchResponse<T> upstream,
      Class<G> guideType,
      Function<G, String> purlOf,
      BiFunction<G, Map<String, GuidePolicyCompliance>, G> enrichOne,
      Function<List<T>, ApiSearchResponse<T>> rebuild,
      String ownerId,
      Stage stage)
  {
    if (upstream == null || upstream.hits().isEmpty()) {
      return upstream;
    }
    List<String> purls = upstream.hits()
        .stream()
        .filter(guideType::isInstance)
        .map(guideType::cast)
        .map(purlOf)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (purls.isEmpty()) {
      return upstream;
    }
    Map<String, GuidePolicyCompliance> compliance = ownerId == null
        ? guidePolicyEvaluator.evaluate(purls)
        : guidePolicyEvaluator.evaluate(purls, ownerId, stage);
    List<T> enriched = upstream.hits()
        .stream()
        .map(h -> guideType.isInstance(h) ? enrichOne.apply(guideType.cast(h), compliance) : h)
        .toList();
    return rebuild.apply(enriched);
  }

  /**
   * Build each candidate's policy-evaluation PURL once, keyed by its version. A blank-version
   * candidate is skipped; an unparseable parent PURL yields an empty map, in which case every
   * candidate is absent and the filter drops them all (&rarr; {@code BLOCKED_BY_POLICY}, matching
   * SaaS). Returning the map (rather than a flat list) lets {@link GuideRecommendationsPolicyFilter}
   * resolve each candidate's PURL by version instead of rebuilding it, so the build and lookup sides
   * cannot drift.
   */
  private static Map<String, String> candidatePurlsByVersion(
      String parentPurl,
      GuideRecommendationResult upstream)
  {
    if (upstream.toVersions() == null || upstream.toVersions().isEmpty()) {
      return Map.of();
    }
    Map<String, String> purlByVersion = new LinkedHashMap<>();
    try {
      PackageURL parent = new PackageURL(parentPurl);
      Map<String, String> parentQualifiers = parent.getQualifiers();
      for (RecommendedVersionInfo c : upstream.toVersions()) {
        if (c.version() == null || c.version().isBlank()) {
          continue;
        }
        // Reuse parent's qualifiers and let buildPurlForPolicyEval fill format defaults
        // (e.g. maven type=jar) so the downstream evaluator's ensureCompleteIdentifier check
        // doesn't reject bare maven candidates.
        purlByVersion.put(c.version(), GuidePurlAssembler.buildPurlForPolicyEval(
            parent.getType(), parent.getNamespace(), parent.getName(), c.version(), parentQualifiers));
      }
    }
    catch (MalformedPackageURLException | GuideApiException e) {
      return Map.of();
    }
    return purlByVersion;
  }

  private ResolvedOwner resolveOwner(String applicationId) {
    if (applicationId == null || applicationId.isBlank()) {
      return new ResolvedOwner(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    }
    Application application = applicationDAO.getByIdOrPublicId(applicationId);
    if (application != null) {
      return new ResolvedOwner(OwnerType.APPLICATION, application.getId());
    }
    Owner owner = ownerDAO.getById(applicationId);
    if (owner != null) {
      return new ResolvedOwner(owner.getType(), applicationId);
    }
    return null;
  }

  /** A resolved evaluation owner: its {@link OwnerType} and internal id, for the authz context. */
  private record ResolvedOwner(OwnerType type, String id)
  {
  }

  /**
   * Resolved scope for a REST caller's optional {@code ownerId}/{@code stage} query params.
   * {@code ownerId} here is null exactly when {@code enrichmentAllowed} is false — callers must check
   * {@code enrichmentAllowed} before using it.
   */
  private record ScopedEvaluation(String ownerId, Stage stage, boolean enrichmentAllowed)
  {
  }

  /**
   * Resolves the effective owner/stage for a scoped Guide REST request. Reuses {@link
   * #resolveOwner(String)} (the same lookup the MCP path uses) and {@link McpStageResolver#resolve}
   * so MCP and REST owner/stage resolution never drift apart.
   *
   * <p>
   * Both an unresolvable {@code ownerId} and a resolvable owner the caller lacks {@link
   * Permission#EVALUATE_COMPONENT} on come back as {@code enrichmentAllowed = false} — deliberately
   * indistinguishable to the caller, mirroring the MCP path's existing silent soft-fail
   * (evaluatePolicies returns no data for an unresolvable applicationId; it never falls back to the
   * root organization). Per GUIDE-3045's acceptance criteria: an unknown owner is a silent 200,
   * not an HTTP 400.
   *
   * @throws GuideApiException 400 if {@code stage} is non-blank and not a valid stage id.
   */
  private ScopedEvaluation resolveScope(String ownerId, String stage) {
    Stage resolvedStage;
    try {
      resolvedStage = McpStageResolver.resolve(stage);
    }
    catch (IllegalArgumentException e) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Invalid stage: " + stage);
    }
    if (ownerId == null || ownerId.isBlank()) {
      return new ScopedEvaluation(Organization.ROOT_ORGANIZATION_ID, resolvedStage, true);
    }
    ResolvedOwner owner = resolveOwner(ownerId);
    if (owner == null || !canSeePolicyDetail(owner.type(), owner.id())) {
      return new ScopedEvaluation(null, resolvedStage, false);
    }
    return new ScopedEvaluation(owner.id(), resolvedStage, true);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
