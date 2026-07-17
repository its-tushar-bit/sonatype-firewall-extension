/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.guide.api.dto.AppSummary;
import com.sonatype.insight.brain.guide.api.dto.ApiOrgAppsResponse;
import com.sonatype.insight.brain.guide.api.dto.OrgSummary;
import com.sonatype.insight.brain.guide.api.dto.OwnerPathEntry;
import com.sonatype.insight.brain.guide.api.dto.ApiOwnerSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.OwnerSummary;
import com.sonatype.insight.brain.guide.api.dto.ApiTopOrgsResponse;
import com.sonatype.insight.brain.integration.ApplicationSummaryService;
import com.sonatype.insight.brain.integration.OrganizationSummaryService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * Backing service for the {@code /api/v2/policy-context/owners/*} endpoints used by the
 * Guide SPA owner picker. Every endpoint permission-filters through the same
 * {@code @AuthzFilter}-gated summary services that Guide policy evaluation uses:
 * {@link OrganizationSummaryService#getOrganizationsForEvaluateApplication} for orgs
 * ({@code EVALUATE_APPLICATION}), and
 * {@link ApplicationSummaryService#getApplicationsForEvaluateComponent} for apps
 * ({@code EVALUATE_COMPONENT}), so the picker never surfaces owners the caller cannot
 * actually evaluate against.
 *
 * <p>
 * All "no permission" and "not found" outcomes are surfaced as {@link NotFoundException}
 * indistinguishably; callers cannot enumerate the existence of owners they cannot access.
 */
@Named
public class PolicyContextOwnersService
{
  private static final String TYPE_ORGANIZATION = "organization";

  private static final String TYPE_APPLICATION = "application";

  /**
   * Chunk size for permission-filtering apps by public id. {@link ApplicationDAO#getByPublicIds}
   * builds a single un-chunked IN clause, so at 40k+ scale we must partition the input before
   * calling into {@code getApplicationsForEvaluateComponent(null, publicIds)} to stay under
   * PostgreSQL's 65535 bind-parameter limit. 1000 keeps each round-trip small enough to
   * amortize while leaving comfortable headroom for the parameter cap.
   */
  private static final int PERMISSION_FILTER_CHUNK_SIZE = 1000;

  private final OrganizationSummaryService organizationSummaryService;

  private final ApplicationSummaryService applicationSummaryService;

  private final OwnerDAO ownerDAO;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public PolicyContextOwnersService(
      OrganizationSummaryService organizationSummaryService,
      ApplicationSummaryService applicationSummaryService,
      OwnerDAO ownerDAO,
      OrganizationDAO organizationDAO,
      ApplicationDAO applicationDAO)
  {
    this.organizationSummaryService = organizationSummaryService;
    this.applicationSummaryService = applicationSummaryService;
    this.ownerDAO = ownerDAO;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
  }

  /**
   * Returns the top {@code limit} organizations the caller has {@code EVALUATE_APPLICATION}
   * on, sorted alphabetically. Includes orgs at any depth of nesting.
   */
  public ApiTopOrgsResponse getTopOrgs(int limit) {
    List<Organization> permittedOrgs = organizationSummaryService.getOrganizationsForEvaluateApplication();
    long totalOrgCount = permittedOrgs.size();

    List<Organization> limitedOrgs = permittedOrgs.stream()
        .sorted(Comparator.comparing(
            Organization::getName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
        .limit(limit)
        .toList();

    Set<String> limitedOrgIds = limitedOrgs.stream().map(Organization::getId).collect(Collectors.toSet());
    Map<String, Long> appCountsByOrgId = permittedAppCountsByOrgId(limitedOrgIds);

    List<OrgSummary> orgSummaries = limitedOrgs.stream()
        .map(org -> new OrgSummary(
            org.getId(),
            org.getId(),
            org.getName(),
            TYPE_ORGANIZATION,
            buildAncestorPath(org.getId(), OwnerType.ORGANIZATION),
            appCountsByOrgId.getOrDefault(org.getId(), 0L)))
        .toList();

    return new ApiTopOrgsResponse(orgSummaries, totalOrgCount);
  }

  /**
   * Returns applications directly under {@code orgId} that the caller has
   * {@code EVALUATE_COMPONENT} on. 404s indistinguishably when the org doesn't exist,
   * when the caller lacks {@code EVALUATE_APPLICATION} on it, or (implicitly) when the
   * caller has no permitted apps in it.
   * <p>
   * Note: This method fetches all permitted apps for the org into memory before truncating
   * to the limit (max 500). This is intentional for the endpoint's design (sorted alphabetically,
   * capped at 500) and bounded by the max limit. The alternative (SQL-level pagination with
   * permission filtering) would require a more complex query pattern.
   */
  public ApiOrgAppsResponse getOrgApps(String orgId, int limit) {
    Organization org = findPermittedOrg(orgId)
        .orElseThrow(() -> new NotFoundException("Organization not found: " + orgId));

    List<Application> apps = applicationSummaryService.getApplicationsForEvaluateComponent(orgId, null);
    boolean truncated = apps.size() > limit;

    // Build the ancestor path once: org's grandparents + the org itself (the app's direct parent)
    // Immutable to prevent aliasing bugs if downstream code mutates the list
    List<OwnerPathEntry> appAncestorPath = List.copyOf(
        Stream.concat(
            buildAncestorPath(org.getId(), OwnerType.ORGANIZATION).stream(),
            Stream.of(new OwnerPathEntry(org.getId(), org.getName(), TYPE_ORGANIZATION)))
            .toList());

    List<AppSummary> appSummaries = apps.stream()
        .sorted(Comparator.comparing(
            Application::getName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
        .limit(limit)
        .map(app -> new AppSummary(
            app.getId(),
            app.getPublicId(),
            app.getName(),
            TYPE_APPLICATION,
            appAncestorPath))
        .toList();

    return new ApiOrgAppsResponse(appSummaries, truncated);
  }

  /**
   * Substring-searches orgs and/or apps by name. Returns them as separate arrays so the
   * frontend can render All / Orgs / Apps tabs. {@code type} accepts {@code null}, {@code
   * "all"}, {@code "org"}, or {@code "app"}.
   */
  public ApiOwnerSearchResponse searchOwners(String query, String type, int limit) {
    // Treat null, empty, or blank as "all" (user-friendly default)
    boolean includeOrgs =
        type == null || type.isBlank() || "all".equalsIgnoreCase(type) || "org".equalsIgnoreCase(type);
    boolean includeApps =
        type == null || type.isBlank() || "all".equalsIgnoreCase(type) || "app".equalsIgnoreCase(type);

    List<OrgSummary> orgs = List.of();
    boolean orgsTruncated = false;
    if (includeOrgs) {
      SearchResult<Organization> orgResult = searchOrganizations(query, limit);
      orgsTruncated = orgResult.truncated;
      Set<String> matchedOrgIds = orgResult.results.stream().map(Organization::getId).collect(Collectors.toSet());
      Map<String, Long> appCountsByOrgId = permittedAppCountsByOrgId(matchedOrgIds);
      orgs = orgResult.results.stream()
          .map(org -> new OrgSummary(
              org.getId(),
              org.getId(),
              org.getName(),
              TYPE_ORGANIZATION,
              buildAncestorPath(org.getId(), OwnerType.ORGANIZATION),
              appCountsByOrgId.getOrDefault(org.getId(), 0L)))
          .toList();
    }

    List<AppSummary> apps = List.of();
    boolean appsTruncated = false;
    if (includeApps) {
      SearchResult<Application> appResult = searchApplications(query, limit);
      appsTruncated = appResult.truncated;
      apps = appResult.results.stream()
          .map(app -> new AppSummary(
              app.getId(),
              app.getPublicId(),
              app.getName(),
              TYPE_APPLICATION,
              buildAncestorPath(app.getId(), OwnerType.APPLICATION)))
          .toList();
    }

    return new ApiOwnerSearchResponse(orgs, orgsTruncated, apps, appsTruncated);
  }

  /**
   * Resolves a single owner by id (or, for applications, by public id) so the picker can
   * rehydrate a persisted localStorage selection. 404s indistinguishably for "not found"
   * and "no permission" ({@code IdUtils.getOwnerNotNull} bypasses authz, so we permission-
   * gate every path via the summary services).
   */
  public OwnerSummary resolveOwner(String ownerId) {
    Optional<Organization> permittedOrg = findPermittedOrg(ownerId);
    if (permittedOrg.isPresent()) {
      Organization org = permittedOrg.get();
      long appCount = permittedAppCount(org.getId());
      return new OrgSummary(
          org.getId(),
          org.getId(),
          org.getName(),
          TYPE_ORGANIZATION,
          buildAncestorPath(org.getId(), OwnerType.ORGANIZATION),
          appCount);
    }

    Application app = applicationDAO.getByIdOrPublicId(ownerId);
    if (app == null || !isAppPermittedForEvaluateComponent(app.getPublicId())) {
      throw new NotFoundException("Owner not found: " + ownerId);
    }
    return new AppSummary(
        app.getId(),
        app.getPublicId(),
        app.getName(),
        TYPE_APPLICATION,
        buildAncestorPath(app.getId(), OwnerType.APPLICATION));
  }

  /**
   * Counts the apps directly under {@code orgId} that the caller has
   * {@code EVALUATE_COMPONENT} on. Uses {@link #permittedAppCountsByOrgId} internally so all
   * three call sites (top-orgs, search, resolve-owner) share the same permission-scoping
   * logic.
   */
  private long permittedAppCount(String orgId) {
    return permittedAppCountsByOrgId(Set.of(orgId)).getOrDefault(orgId, 0L);
  }

  /**
   * Returns a map of {@code orgId → permitted app count} for the supplied orgs in a batched
   * pass:
   *
   * <ol>
   * <li>Fetch every app directly under any of the requested orgs via
   * {@link ApplicationDAO#getByOrganizationIds} (one IN-clause query, chunked automatically).
   * <li>Permission-filter those apps through
   * {@code getApplicationsForEvaluateComponent(null, publicIds)} so the {@code @AuthzFilter}
   * runs over the hit set, not every app in the system. The public-id set is partitioned
   * into {@link #PERMISSION_FILTER_CHUNK_SIZE}-sized chunks before each call: the summary
   * service's underlying DAO ({@link ApplicationDAO#getByPublicIds}) builds a single
   * un-chunked IN clause, which at 40k+ scale can exceed PostgreSQL's 65535 bind-parameter
   * limit for {@code /top-orgs} with {@code limit=100}.
   * <li>Group the permitted apps by {@code organizationId} and count.
   * </ol>
   *
   * <p>
   * Orgs with zero permitted apps are absent from the returned map; callers use
   * {@code getOrDefault(orgId, 0L)}.
   */
  private Map<String, Long> permittedAppCountsByOrgId(Set<String> orgIds) {
    if (orgIds == null || orgIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Application> appsUnderOrgs = applicationDAO.getByOrganizationIds(orgIds);
    if (appsUnderOrgs.isEmpty()) {
      return Collections.emptyMap();
    }
    List<String> appPublicIds = appsUnderOrgs.stream()
        .map(Application::getPublicId)
        .toList();
    Map<String, Long> countsByOrgId = new HashMap<>();
    for (Set<String> chunk : chunk(appPublicIds, PERMISSION_FILTER_CHUNK_SIZE)) {
      List<Application> permittedChunk =
          applicationSummaryService.getApplicationsForEvaluateComponent(null, chunk);
      for (Application app : permittedChunk) {
        countsByOrgId.merge(app.getOrganizationId(), 1L, Long::sum);
      }
    }
    return countsByOrgId;
  }

  /**
   * Splits {@code items} into fixed-size Sets. Does not preserve per-chunk ordering; input
   * items within a chunk are deduplicated into a Set. The last chunk may be smaller. Used
   * to keep IN-clause parameter counts under database limits when calling downstream
   * services that don't chunk internally. If insertion order within a chunk ever matters,
   * switch to LinkedHashSet.
   */
  private static List<Set<String>> chunk(List<String> items, int chunkSize) {
    if (items.isEmpty()) {
      return List.of();
    }
    List<Set<String>> chunks = new ArrayList<>((items.size() + chunkSize - 1) / chunkSize);
    Iterator<String> it = items.iterator();
    while (it.hasNext()) {
      Set<String> current = new HashSet<>(Math.min(chunkSize, items.size()));
      for (int i = 0; i < chunkSize && it.hasNext(); i++) {
        current.add(it.next());
      }
      chunks.add(current);
    }
    return chunks;
  }

  // --- Private helpers ---

  /**
   * Substring-search on orgs, then permission-filter by intersecting the ≤ limit+1 hits with
   * the caller's {@code EVALUATE_APPLICATION}-permitted set. {@code truncated} reflects
   * whether the underlying substring match exceeded the limit BEFORE permission filtering,
   * which is what the spec's tab-count semantics require ("Tab counts reflect returned
   * (limit-capped) results, not true totals").
   *
   * <p>
   * <b>Known limitation — permission filter is applied AFTER the name-match limit.</b>
   * We fetch the alphabetically-first {@code limit+1} name matches from the DAO first, then
   * intersect with the caller's permitted set. If those first name-matches are owners the
   * caller cannot evaluate against, the response can be sparse or even empty for a query
   * that would match many permitted owners further down the alphabet. The picker treats this
   * as "no more results" — pushing permission scoping into the DAO query (id-narrowed
   * substring search) would eliminate this, but requires a new query path that intersects
   * the permitted-id set with the trigram search inside the database. Deferred to a
   * follow-up; the {@code Zeta-} prefix in test fixtures papers over it, but real deployments
   * with heterogeneous permissions will hit it.
   *
   * <p>
   * Note on scalability: {@code getOrganizationsForEvaluateApplication()} returns every
   * org the caller can see (bounded by the caller's actual permissions, not by total orgs).
   * For typical deployments this is fine; if it becomes a hotspot, add an id-narrowing
   * overload to {@link OrganizationSummaryService}.
   */
  private SearchResult<Organization> searchOrganizations(String query, int limit) {
    List<Organization> matching = organizationDAO.searchByNameSubstring(query, limit + 1);
    boolean truncated = matching.size() > limit;
    List<Organization> capped = matching.stream().limit(limit).toList();

    if (capped.isEmpty()) {
      return new SearchResult<>(List.of(), truncated);
    }

    Set<String> permittedOrgIds = organizationSummaryService.getOrganizationsForEvaluateApplication()
        .stream()
        .map(Organization::getId)
        .collect(Collectors.toSet());
    List<Organization> filtered = capped.stream()
        .filter(org -> permittedOrgIds.contains(org.getId()))
        .toList();

    return new SearchResult<>(filtered, truncated);
  }

  /**
   * Substring-search on apps, then permission-filter by re-fetching JUST the search hits
   * through {@code getApplicationsForEvaluateComponent(null, hitPublicIds)}. Passing the
   * hit public IDs narrows the DAO fetch inside the summary service to those apps only,
   * so {@code @AuthzFilter} runs over ≤ limit rows instead of every app in the system.
   *
   * <p>
   * <b>Known limitation — permission filter is applied AFTER the name-match limit.</b>
   * Same trade-off as {@link #searchOrganizations}: we take the alphabetically-first
   * {@code limit+1} name matches, then intersect with the caller's permitted set. If those
   * first matches are apps the caller cannot evaluate against, the response can be sparse
   * or empty even when many permitted matches exist further down the alphabet. Pushing
   * permission scoping into the DAO query would eliminate this; deferred to a follow-up.
   *
   * <p>
   * <b>Do not</b> call {@code getApplicationsForEvaluateComponent(null, null)} for
   * permission filtering here — that null-null branch falls through to
   * {@code applicationDAO.getAllWithoutRelatedRepositories()}, defeating the pg_trgm index
   * at 40k-app scale.
   */
  private SearchResult<Application> searchApplications(String query, int limit) {
    List<Application> matching = applicationDAO.searchByNameSubstring(query, limit + 1);
    boolean truncated = matching.size() > limit;
    List<Application> capped = matching.stream().limit(limit).toList();

    if (capped.isEmpty()) {
      return new SearchResult<>(List.of(), truncated);
    }

    Set<String> hitPublicIds = capped.stream().map(Application::getPublicId).collect(Collectors.toSet());
    Set<String> permittedAppIds = applicationSummaryService
        .getApplicationsForEvaluateComponent(null, hitPublicIds)
        .stream()
        .map(Application::getId)
        .collect(Collectors.toSet());
    List<Application> filtered = capped.stream()
        .filter(app -> permittedAppIds.contains(app.getId()))
        .toList();

    return new SearchResult<>(filtered, truncated);
  }

  private Optional<Organization> findPermittedOrg(String orgId) {
    return organizationSummaryService.getOrganizationsForEvaluateApplication()
        .stream()
        .filter(org -> org.getId().equals(orgId))
        .findFirst();
  }

  private boolean isAppPermittedForEvaluateComponent(String appPublicId) {
    return !applicationSummaryService
        .getApplicationsForEvaluateComponent(null, Set.of(appPublicId))
        .isEmpty();
  }

  /**
   * Builds the breadcrumb path {@code [top-level-org, ..., immediateParent]} — every
   * ancestor from top-level org down to, but not including, the target owner itself, and
   * excluding the synthetic ROOT_ORGANIZATION.
   *
   * <p>
   * <b>Performance consideration:</b> This method is called once per owner in the result set.
   * For {@code /top-orgs} with default limit 20, this results in 20 hierarchy walks. Each walk
   * iterates from target to root (typically 2-5 levels deep in most deployments). At max limit
   * of 100 orgs, this is still acceptable (~100 walks × ~5 iterations = ~500 iterations total).
   * If this becomes a bottleneck at scale, batch hierarchy fetching would be an optimization.
   *
   * <p>
   * Uses {@link OwnerDAO#walkHierarchy(String, OwnerType)} directly rather than
   * {@link com.sonatype.insight.brain.owner.OwnerService#getHierarchyForRead}: the picker's
   * permission model is {@code EVALUATE_APPLICATION} / {@code EVALUATE_COMPONENT}
   * (already enforced by the summary services above), not {@code READ}, so imposing the
   * hierarchy service's {@code @Authorize(READ)} would surface 403 for users who can
   * legitimately evaluate against the owner but lack READ.
   *
   * <p>
   * <b>Disclosure note:</b> the returned path names every ancestor of a permitted owner,
   * including ancestors the caller lacks {@code READ} on. This is intentional — the
   * picker's permission model is {@code EVALUATE_*}, and filtering ancestors by
   * {@code READ} would produce holed breadcrumbs. The disclosure is scoped: it only
   * surfaces on responses for owners the caller can already evaluate against, and it does
   * not enumerate owners outside the caller's evaluate scope.
   *
   * <p>
   * {@code walkHierarchy} returns {@code [target, immediateParent, ..., top-level-org,
   * ROOT_ORGANIZATION]}. We drop the target (index 0) and ROOT_ORGANIZATION, then reverse
   * so the breadcrumb starts at the topmost org.
   */
  private List<OwnerPathEntry> buildAncestorPath(String ownerId, OwnerType type) {
    List<OwnerPathEntry> reversed = new ArrayList<>();
    boolean first = true;
    for (Owner owner : ownerDAO.walkHierarchy(ownerId, type)) {
      if (first) {
        // Skip the target itself — the path is ancestors only, per the spec.
        first = false;
        continue;
      }
      if (Organization.ROOT_ORGANIZATION_ID.equals(owner.getId())) {
        // Skip the synthetic root; every top-level org has it as a parent.
        continue;
      }
      reversed.add(new OwnerPathEntry(
          owner.getId(),
          owner.getName(),
          owner.getType().name().toLowerCase()));
    }
    // walkHierarchy yields nearest-parent-first; the picker wants root-first.
    Collections.reverse(reversed);
    return reversed;
  }

  private record SearchResult<T>(List<T> results, boolean truncated)
  {
  }
}
