/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.catalog.CatalogLocalRequestBuilder.LocalQuery;
import com.sonatype.insight.brain.search.catalog.CatalogResponse.CatalogFacetBucket;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.IqLocalSearchService;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalRow;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.MetricAggregationResult;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper.POLICY_VIOLATION_WAIVER_STATUS_ACTIVE;

@Named
@Singleton
public class CatalogService
{
  private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

  /**
   * Page-size ceiling for the catalog (HDS/Guide) source. Independent of the IQ-local index cap
   * {@link IqLocalSearchService#MAX_PAGE_SIZE}; the catalog backend paginates its own store.
   */
  static final int MAX_CATALOG_PAGE_SIZE = 200;

  /** Upper bound on {@code page} to reject absurd values before they reach offset arithmetic. */
  static final int MAX_PAGE = 10_000;

  /**
   * Default per-page size for the catalog source when the caller omits {@code pageSize}. Deliberately
   * a catalog-owned constant (rather than borrowing {@link IqLocalSearchService#DEFAULT_PER_TYPE_PAGE_SIZE},
   * which the local index owns) so a future change to the local default cannot silently move the
   * catalog default. Kept numerically aligned with the local default today.
   */
  static final int DEFAULT_CATALOG_PAGE_SIZE = 50;

  // Raw stored-field labels used as composite keys for the distinct-count aggregations. These are
  // Lucene field labels (validated against FieldIdentifier in the client), not the FieldMap query
  // field names; applicationId is stored on component and vuln docs even though the FieldMap only
  // exposes it as a filter on APPLICATION docs.
  private static final String FIELD_APPLICATION_ID = "applicationId";

  private static final String FIELD_COMPONENT_HASH = "componentHash";

  private static final String FIELD_VULNERABILITY_ID = "vulnerabilityId";

  /** Threat-level field on POLICY_VIOLATION docs, bucketed into severity bands for the C1 counts. */
  private static final String FIELD_POLICY_VIOLATION_THREAT_LEVEL = "policyViolationThreatLevel";

  /** Distinct-entity key for the C1 per-severity counts (dedups a violation across per-stage docs). */
  private static final String FIELD_POLICY_VIOLATION_ID = "policyViolationId";

  /**
   * Maps a {@link com.sonatype.insight.brain.utils.ThreatLevel} severity band ({@code low}/
   * {@code moderate}/{@code severe}/{@code critical}) to the Components-leg per-severity row field the
   * prototype card renders ({@code latest_critical_count}/{@code latest_high_count}/
   * {@code latest_medium_count}/{@code latest_low_count}). severe&rarr;high and moderate&rarr;medium
   * follow the prototype's mapping (components/page.tsx). Ordered for stable iteration.
   */
  private static final Map<String, String> SEVERITY_BAND_TO_ROW_FIELD;

  static {
    Map<String, String> bandToRowField = new LinkedHashMap<>();
    bandToRowField.put("critical", "latest_critical_count");
    bandToRowField.put("severe", "latest_high_count");
    bandToRowField.put("moderate", "latest_medium_count");
    bandToRowField.put("low", "latest_low_count");
    SEVERITY_BAND_TO_ROW_FIELD = Collections.unmodifiableMap(bandToRowField);
  }

  /** Item-type clause selecting POLICY_VIOLATION docs (source of the C1 per-severity counts). */
  private static final String POLICY_VIOLATION_ITEM_TYPE_QUERY =
      "itemType:" + ItemType.POLICY_VIOLATION.searchFieldName();

  /** Row key + facet name for the CVSS severity-band facet on the local Vulnerabilities leg. */
  static final String SEVERITY_FACET_KEY = "severity";

  /**
   * Local facet spec: each entry maps a catalog row-field (whose page values seed the bucket list)
   * to the IQ-local index field the whole-corpus count queries. Kept ordered so the returned facet
   * map is stable.
   */
  private static final Map<CatalogEntityType, List<LocalFacet>> LOCAL_FACET_FIELDS = Map.of(
      CatalogEntityType.COMPONENT,
      // Fields a NON_VULNERABLE_COMPONENT doc carries: componentFormat, organizationName, applicationName.
      // Effective-license / license-threat-group live on LEGAL_VIOLATION docs, never on component docs,
      // so a licenseThreatGroup facet here would always be empty in production. There is NO severities
      // facet: component docs carry no threat/severity field (a known deferred data gap), so it is
      // omitted rather than fabricated from an absent field.
      List.of(
          LocalFacet.value(CatalogRowMapper.LOCAL_FIELD_ECOSYSTEM, "componentFormat"),
          // organizationName is rewritten to parentOrganizationName by the metric layer, so the count
          // includes the org and its descendants (consistent with the organizations filter clause).
          LocalFacet.value(CatalogRowMapper.LOCAL_FIELD_ORGANIZATION, "organizationName"),
          // applicationName is queryable on component docs (PR-A widening). A component recurs once per
          // (app, stage), so the bucket count is distinct componentHash — the number of distinct
          // components in that app, not raw per-stage docs. Bucketed by name (applicationId is not a
          // queryable filter on component docs).
          LocalFacet.distinct(
              CatalogRowMapper.LOCAL_FIELD_APPLICATION, "applicationName", FIELD_COMPONENT_HASH)),
      CatalogEntityType.VULNERABILITY,
      // vulnerabilityStatus (triage status) and componentFormat (affected ecosystems) are term-valued
      // on SECURITY_VULNERABILITY docs, but those docs are per-(app, stage), so a raw count() over a
      // bucket over-counts a CVE once per (app, stage). Every VULNERABILITY facet therefore declares an
      // explicit distinctKey=vulnerabilityId so the bucket count is distinct CVEs (consistent with the
      // orgs/apps affected-CVE counts). organizationName + applicationName are queryable on vuln docs
      // (PR-A widening). The severity-BAND facet ("severity") is NOT a page-value facet in this list: its
      // buckets are the five fixed CVSS bands, not values discovered from the page, so it is computed
      // separately in localFacets() via distinct-CVE counting over half-open float ranges on
      // vulnerabilitySeverity (see severityBandFacet()).
      List.of(
          LocalFacet.distinct(CatalogRowMapper.LOCAL_FIELD_STATUS, "vulnerabilityStatus", FIELD_VULNERABILITY_ID),
          LocalFacet.distinct(CatalogRowMapper.LOCAL_FIELD_ECOSYSTEM, "componentFormat", FIELD_VULNERABILITY_ID),
          LocalFacet.distinct(
              CatalogRowMapper.LOCAL_FIELD_ORGANIZATION, "organizationName", FIELD_VULNERABILITY_ID),
          LocalFacet.distinct(
              CatalogRowMapper.LOCAL_FIELD_APPLICATION, "applicationName", FIELD_VULNERABILITY_ID)));

  /**
   * Cap on distinct values counted per facet field. Facet values come from the current page's rows,
   * so this is bounded by page size in practice; the cap is a hard ceiling on {@code count()} calls
   * per field to keep per-field fan-out well below page size (see {@link #MAX_FACET_COUNT_QUERIES}).
   */
  static final int MAX_FACET_BUCKETS_PER_FIELD = 20;

  /**
   * Overall ceiling on whole-corpus facet {@code count()} calls issued per request across the
   * per-value facet fields (orgs/apps). Each bucket count is one RBAC-scoped index query; bounding
   * total fan-out keeps a single {@code includeFacets} request from firing dozens of counts under
   * load (p95 &lt; 300ms target). Once the budget is exhausted the remaining buckets are omitted and
   * a truncation warning is added.
   * <p>
   * This bounds the whole per-request aggregation fan-out to a small constant: at most this many
   * per-value facet counts, plus ONE aggregation pass for the CVSS severity-band facet (a single
   * {@link SearchIndexClient#aggregateCountByFloatField} call over all bands, not one count per
   * band), plus the small fixed number of grouped distinct-count reads in {@link #enrichLocalCounts}
   * (one for components, two for vulns). So the realistic ceiling is roughly {@code 40 + 1 + 2}
   * aggregation queries per request.
   */
  static final int MAX_FACET_COUNT_QUERIES = 40;

  /**
   * Affected-app / affected-component counts for the whole page are computed with ONE grouped
   * distinct-count index read for components (distinct applicationId grouped by componentHash) and
   * TWO for vulns (distinct applicationId and distinct componentHash, each grouped by
   * vulnerabilityId), regardless of page size. This bounds the aggregation fan-out to a small
   * constant per request rather than one distinct-count query per row (which blew the budget on a
   * full page and opened one fresh reader per row). Each read is RBAC-scoped and fails closed.
   */

  private final IqLocalSearchService iqLocalSearchService;

  private final SearchApiClient searchApiClient;

  private final SearchIndexClient searchIndexClient;

  private final ProductLicense productLicense;

  private final TenantUtil tenantUtil;

  @Inject
  public CatalogService(
      final IqLocalSearchService iqLocalSearchService,
      final SearchApiClient searchApiClient,
      final SearchIndexClient searchIndexClient,
      final ProductLicense productLicense,
      final TenantUtil tenantUtil)
  {
    this.iqLocalSearchService = iqLocalSearchService;
    this.searchApiClient = searchApiClient;
    this.searchIndexClient = searchIndexClient;
    this.productLicense = productLicense;
    this.tenantUtil = tenantUtil;
  }

  /**
   * One local facet.
   *
   * @param rowField page row field whose distinct values seed the bucket list.
   * @param indexField raw IQ index field the whole-corpus count queries (a FieldIdentifier label,
   *          not the FieldMap token). {@code organizationName} is rewritten to
   *          {@code parentOrganizationName} by the metric layer so the count spans the org hierarchy.
   * @param distinctKey when non-null, the bucket count is a {@code countDistinct} over this composite
   *          key rather than a raw document {@code count}. Required whenever the docs behind a bucket
   *          are per-app-per-stage on the counted dimension (e.g. an apps facet over component docs,
   *          where a component recurs once per (app, stage) — distinct componentHash yields the number
   *          of distinct components, not raw docs). Null means a plain document count.
   */
  private record LocalFacet(String rowField, String indexField, String distinctKey)
  {
    static LocalFacet value(final String rowField, final String indexField) {
      return new LocalFacet(rowField, indexField, null);
    }

    static LocalFacet distinct(final String rowField, final String indexField, final String distinctKey) {
      return new LocalFacet(rowField, indexField, distinctKey);
    }
  }

  public CatalogResponse search(
      final CatalogEntityType entityType,
      final SearchSource source,
      final CatalogRequest request)
  {
    final int page = page(request.getPage());
    // Page-size caps differ per source (the catalog backend has no relation to the local index
    // cap), so validate once the source is known rather than borrowing one cap for both.
    final int pageSize = pageSize(request.getPageSize(), source);
    if (source == SearchSource.CATALOG) {
      return searchCatalog(entityType, request, page, pageSize);
    }
    return searchLocal(entityType, request, page, pageSize);
  }

  private CatalogResponse searchCatalog(
      final CatalogEntityType entityType,
      final CatalogRequest request,
      final int page,
      final int pageSize)
  {
    // The catalog source is unscoped, shared public data (Guide/HDS), not tenant-filtered, so the
    // coarse read-on-any-context gate at the resource is intentional here.
    final List<String> warnings = catalogWarnings(request);
    // widen to long before multiply to avoid int overflow at large page numbers
    final int offset = (int) Math.min((long) (page - 1) * pageSize, Integer.MAX_VALUE);
    // Validate/build the backend request FIRST, before any availability/entitlement short-circuit,
    // so a malformed filter is a consistent 400 regardless of flag or license state (rather than a
    // degraded-200 when off but a 400 when on).
    final GuideComponentSearchRequest componentRequest = entityType == CatalogEntityType.COMPONENT
        ? CatalogRequestBuilder.component(request.getFilters(), offset, pageSize)
        : null;
    final GuideVulnerabilitySearchRequest vulnerabilityRequest = entityType == CatalogEntityType.VULNERABILITY
        ? CatalogRequestBuilder.vulnerability(request.getFilters(), offset, pageSize)
        : null;
    // Availability + entitlement gate. A feature flag is not an entitlement: mirror the rule
    // SearchLicenseFilter enforces on the Guide API (deny multi-tenant, require GUIDE_SEARCH) so the
    // catalog leg cannot expose HDS data to an unentitled or MTIQ caller. Denials degrade to the
    // same catalog-unavailable body the flag-off path returns rather than leaking a 403 mid-response.
    if (!SystemConfigurationPropertyFeature.CATALOG_FEDERATION.isEnabled() || !catalogEntitled()) {
      return CatalogResponse.catalogUnavailable(entityType, page, pageSize, warnings);
    }
    try {
      return switch (entityType) {
        case COMPONENT -> catalogComponents(componentRequest, request.isIncludeFacets(), page, pageSize, warnings);
        case VULNERABILITY -> catalogVulnerabilities(vulnerabilityRequest, request.isIncludeFacets(), page, pageSize,
            warnings);
      };
    }
    catch (RuntimeException e) {
      // Any backend failure (Guide API error, license-unavailable, transport, or a malformed
      // payload NPE) degrades to catalog-unavailable rather than a 500. No cross-source
      // fall-through: catalog-unavailable returns degraded, never local rows.
      log.warn("Catalog source unavailable for entityType {}: {}", entityType, e.getMessage(), e);
      return CatalogResponse.catalogUnavailable(entityType, page, pageSize, warnings);
    }
  }

  /**
   * Mirrors {@link com.sonatype.insight.brain.security.SearchLicenseFilter}'s Guide-API gate: deny on
   * multi-tenant (MTIQ) deployments and require the {@link LicensedFeature#GUIDE_SEARCH} feature. The
   * catalog leg calls the same HDS-backed store the filter guards, so it applies the identical rule.
   */
  private boolean catalogEntitled() {
    return !tenantUtil.isMultiTenant() && productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH);
  }

  private CatalogResponse catalogComponents(
      final GuideComponentSearchRequest componentRequest,
      final boolean includeFacets,
      final int page,
      final int pageSize,
      final List<String> warnings)
  {
    final ApiSearchResponse<ComponentDocument> response = searchApiClient.searchComponents(componentRequest);
    final List<CatalogRow> rows = collect(response.hits(), GuideComponentDocument.class,
        CatalogRowMapper::catalogComponent, CatalogEntityType.COMPONENT);
    return catalogResponse(CatalogEntityType.COMPONENT, page, pageSize, response.total(), rows,
        includeFacets ? aggregationFacets(response.aggregations()) : null, warnings);
  }

  private CatalogResponse catalogVulnerabilities(
      final GuideVulnerabilitySearchRequest vulnerabilityRequest,
      final boolean includeFacets,
      final int page,
      final int pageSize,
      final List<String> warnings)
  {
    final ApiSearchResponse<VulnerabilityDocument> response =
        searchApiClient.searchVulnerabilities(vulnerabilityRequest);
    final List<CatalogRow> rows = collect(response.hits(), GuideVulnerabilityDocument.class,
        CatalogRowMapper::catalogVulnerability, CatalogEntityType.VULNERABILITY);
    return catalogResponse(CatalogEntityType.VULNERABILITY, page, pageSize, response.total(), rows,
        includeFacets ? aggregationFacets(response.aggregations()) : null, warnings);
  }

  /**
   * Map backend hits to catalog rows: keep only documents of {@code docType}, apply {@code mapper},
   * drop rows the mapper rejects (null, e.g. missing id), and WARN with the dropped counts. A hit of
   * an unexpected type (schema drift or a wrong-shaped backend response) is counted separately so it
   * is discoverable rather than silently skipped. Treats a null {@code hits} list (malformed backend
   * payload) as empty.
   */
  private static <D, T extends D> List<CatalogRow> collect(
      final List<D> hits,
      final Class<T> docType,
      final Function<T, CatalogRow> mapper,
      final CatalogEntityType entityType)
  {
    if (hits == null) {
      return List.of();
    }
    final List<CatalogRow> rows = new ArrayList<>(hits.size());
    int dropped = 0;
    int typeMismatch = 0;
    for (D doc : hits) {
      if (docType.isInstance(doc)) {
        final CatalogRow row = mapper.apply(docType.cast(doc));
        if (row != null) {
          rows.add(row);
        }
        else {
          dropped++;
        }
      }
      else {
        typeMismatch++;
      }
    }
    logDroppedRows(entityType, dropped, typeMismatch);
    return rows;
  }

  private static CatalogResponse catalogResponse(
      final CatalogEntityType entityType,
      final int page,
      final int pageSize,
      final long total,
      final List<CatalogRow> rows,
      final Map<String, List<CatalogFacetBucket>> facets,
      final List<String> warnings)
  {
    final long capped = AbstractSearchIndexClient.capTotalHitsForGlobalSearch(total);
    // At the cap the real count may be higher, so exact-at-cap is treated as inexact.
    final boolean exact = capped == total && capped < AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP;
    return new CatalogResponse(
        entityType.name(),
        SearchSource.CATALOG,
        true,
        page,
        pageSize,
        capped,
        exact,
        rows,
        facets,
        null,
        warnings);
  }

  /**
   * The catalog source paginates by {@code page} only; {@code sort} and {@code searchAfter} have no
   * effect there, so they are surfaced as warnings rather than dropped silently.
   */
  private static List<String> catalogWarnings(final CatalogRequest request) {
    final List<String> warnings = new ArrayList<>();
    if (StringUtils.isNotBlank(request.getSort())) {
      warnings.add(CatalogWarnings.SORT_NOT_APPLIED);
    }
    if (StringUtils.isNotBlank(request.getSearchAfter())) {
      warnings.add(CatalogWarnings.SEARCH_AFTER_NOT_APPLIED);
    }
    return warnings;
  }

  private static void logDroppedRows(final CatalogEntityType entityType, final int dropped) {
    logDroppedRows(entityType, dropped, 0);
  }

  private static void logDroppedRows(final CatalogEntityType entityType, final int dropped, final int typeMismatch) {
    if (dropped > 0 || typeMismatch > 0) {
      // Generic counts only (no ids/PII); mirrors the index-query endpoint's dropped-row WARN. A
      // non-zero typeMismatch means the backend returned an unexpected doc type (schema drift).
      log.warn("Dropped {} {} catalog rows with missing id, {} of an unexpected type",
          dropped, entityType, typeMismatch);
    }
  }

  private CatalogResponse searchLocal(
      final CatalogEntityType entityType,
      final CatalogRequest request,
      final int page,
      final int pageSize)
  {
    final LocalQuery local = CatalogLocalRequestBuilder.build(entityType, request.getFilters());
    final Tab tab = entityType.tab();
    final String rawSearchAfter = StringUtils.isBlank(request.getSearchAfter()) ? null : request.getSearchAfter();
    // Local paging is cursor-only (searchAfter). Mirror IndexQueryService.validatePage against the RAW
    // requested page (null-aware) so the cursor/page combination is consistent with the index-query
    // endpoint: a first-page request must not carry a stale cursor, and a later page must carry one.
    validateLocalCursor(request.getPage(), rawSearchAfter);
    // Sort is validated once, inside IqLocalSearchService.search (requireAllowed); do not
    // pre-validate here to avoid two allowlist sources of truth. The service echoes the
    // validated key back in the response for cursor minting and warnings.
    //
    // The 6-arg SearchInputs leaves isSbomManagerMode false: this endpoint is Lifecycle-only, and an
    // SBOM-Manager-only tenant is rejected by the service's mode check and mapped to 404 above.
    final SearchInputs inputs = new SearchInputs(
        local.q(), tab, entityType.localItemTypes(), pageSize, request.getSort(), rawSearchAfter);
    final IqLocalSearchResponse result;
    try {
      result = iqLocalSearchService.search(inputs);
    }
    catch (InvalidLicenseException e) {
      // Lifecycle-only endpoint: an SBOM-Manager-only tenant fails the license/mode check. Surface
      // the same "unavailable for this tenant" 404 the flag-off path uses rather than a raw 500.
      throw new NotFoundException("Not Found");
    }
    catch (IllegalArgumentException e) {
      // The service's sort allowlist (requireAllowed) rejects unknown sort keys defensively.
      throw new BadRequestException(e.getMessage());
    }
    final String sortKey = result.sortKey();

    final List<CatalogRow> rows = new ArrayList<>(result.rows().size());
    int dropped = 0;
    for (IqLocalRow tagged : result.rows()) {
      final CatalogRow row = entityType == CatalogEntityType.COMPONENT
          ? CatalogRowMapper.localComponent(tagged.row())
          : CatalogRowMapper.localVulnerability(tagged.row());
      if (row != null) {
        rows.add(row);
      }
      else {
        dropped++;
      }
    }
    logDroppedRows(entityType, dropped);

    // Query-time affected-app / affected-component aggregations: these cannot be a stored field
    // because component/vuln docs are per-app-per-stage (no single global doc). One grouped,
    // RBAC-scoped (fail-closed) index read per metric covers the whole page. Enrichment mutates the
    // row list in place.
    enrichLocalCounts(entityType, rows);

    // C1: per-severity active-policy-violation counts on each component row, page-bounded. Query-time
    // (component docs carry no severity field) via one grouped distinct-count per severity band over
    // the POLICY_VIOLATION docs of the page's component hashes.
    if (entityType == CatalogEntityType.COMPONENT) {
      enrichComponentSeverityCounts(rows);
    }

    // Pin the next-page cursor to the backend that actually served this page, so a cross-backend
    // switch on the follow-up request is rejected as stale rather than silently mis-paginated.
    final GlobalSearchCursor next = iqLocalSearchService.mintNextCursor(
        tab, sortKey, pageSize, result.nextSearchAfter(), result.servingBackendId());

    final List<String> warnings = new ArrayList<>(result.warnings());
    warnings.addAll(local.warnings());
    if (!GlobalSearchSortAllowlist.RELEVANCE.equals(sortKey) && !IqLocalSearchService.isFieldSortEnabled()) {
      warnings.add("sort is relevance-only until field sort is enabled");
    }
    // SBOM-sourced vulnerability docs carry no policyEvaluationStage, so a stages filter cannot
    // scope them and silently excludes every one. Surface that as a warning (mirrors the catalog
    // filter-rejection warnings) rather than dropping them without a signal.
    if (entityType == CatalogEntityType.VULNERABILITY && hasStagesFilter(request.getFilters())) {
      warnings.add(CatalogWarnings.STAGES_EXCLUDE_SBOM_VULNS);
    }

    // Facet VALUES come from the returned page, but each bucket COUNT is a whole-corpus,
    // RBAC-scoped count over the same active filters + item type (not page-only).
    final Map<String, List<CatalogFacetBucket>> facets =
        request.isIncludeFacets() ? localFacets(entityType, local, rows, warnings) : null;

    return new CatalogResponse(
        entityType.name(),
        SearchSource.LOCAL,
        true,
        page,
        pageSize,
        result.total(),
        result.exactTotalHits(),
        rows,
        facets,
        next == null ? null : next.encode(),
        warnings);
  }

  /**
   * Cursor/page consistency for the cursor-only local source, matching
   * {@link com.sonatype.insight.brain.search.indexquery.IndexQueryService}'s {@code validatePage}:
   * page 1 (or absent) must not carry a {@code searchAfter} cursor, and page &gt; 1 must carry one.
   * Runs against the raw requested page so a cursor sent with no page is rejected, not treated as
   * page 1. Static, input-free messages.
   */
  private static void validateLocalCursor(final Integer requestedPage, final String searchAfter) {
    final boolean firstPage = requestedPage == null || requestedPage <= 1;
    if (firstPage && searchAfter != null) {
      throw new BadRequestException("page 1 must not carry a searchAfter cursor");
    }
    if (!firstPage && searchAfter == null) {
      throw new BadRequestException("page > 1 requires a searchAfter cursor");
    }
  }

  private static int page(final Integer requested) {
    if (requested == null) {
      return 1;
    }
    if (requested < 1) {
      throw new BadRequestException("page must be >= 1");
    }
    if (requested > MAX_PAGE) {
      throw new BadRequestException("page exceeds the maximum");
    }
    return requested;
  }

  private static int pageSize(final Integer requested, final SearchSource source) {
    final int max = source == SearchSource.CATALOG ? MAX_CATALOG_PAGE_SIZE : IqLocalSearchService.MAX_PAGE_SIZE;
    if (requested == null) {
      return source == SearchSource.CATALOG
          ? DEFAULT_CATALOG_PAGE_SIZE
          : IqLocalSearchService.DEFAULT_PER_TYPE_PAGE_SIZE;
    }
    if (requested < 1 || requested > max) {
      throw new BadRequestException("pageSize must be in [1, " + max + "]");
    }
    return requested;
  }

  private static Map<String, List<CatalogFacetBucket>> aggregationFacets(
      final Map<String, Map<String, Long>> aggregations)
  {
    if (aggregations == null || aggregations.isEmpty()) {
      return null;
    }
    final Map<String, List<CatalogFacetBucket>> out = new LinkedHashMap<>();
    aggregations.forEach((field, buckets) -> {
      if (buckets == null) {
        return;
      }
      final List<CatalogFacetBucket> list = new ArrayList<>(buckets.size());
      buckets.forEach((value, count) -> list.add(new CatalogFacetBucket(value, count == null ? 0L : count)));
      out.put(field, list);
    });
    return out;
  }

  /**
   * Whole-corpus local facets. Values are discovered from the current page's rows (capped at
   * {@link #MAX_FACET_BUCKETS_PER_FIELD} per field); each bucket count is a fresh RBAC-scoped,
   * fail-closed {@link SearchIndexClient#count(String)} over the same active filters + item type AND
   * {@code indexField=value}, so it reflects the full filtered result set rather than the page.
   * <p>
   * The count base intentionally omits the free-text {@code query} refinement (the metric parser
   * defaults bare terms to the vulnerabilityId field); facet counts therefore reflect the structured
   * filters + item type. Full free-text-consistent aggregate facets are a follow-up.
   */
  private Map<String, List<CatalogFacetBucket>> localFacets(
      final CatalogEntityType entityType,
      final LocalQuery local,
      final List<CatalogRow> rows,
      final List<String> warnings)
  {
    final List<LocalFacet> facetFields = LOCAL_FACET_FIELDS.getOrDefault(entityType, List.of());
    if (facetFields.isEmpty()) {
      return new LinkedHashMap<>();
    }
    final String baseQuery = baseMetricQuery(entityType, local);
    final Map<String, List<CatalogFacetBucket>> out = new LinkedHashMap<>();
    // Bound total count() fan-out per request: each bucket is one RBAC-scoped index query, and the
    // p95 < 300ms target cannot absorb dozens of them under concurrency. Fields and values are
    // processed in their existing stable order so truncation is deterministic.
    int budget = MAX_FACET_COUNT_QUERIES;
    boolean truncated = false;
    for (LocalFacet facet : facetFields) {
      final Set<String> values = new LinkedHashSet<>();
      for (CatalogRow row : rows) {
        if (values.size() >= MAX_FACET_BUCKETS_PER_FIELD) {
          break;
        }
        final Object value = row.getFields().get(facet.rowField());
        if (value != null) {
          values.add(String.valueOf(value));
        }
      }
      final List<CatalogFacetBucket> buckets = new ArrayList<>(values.size());
      for (String value : values) {
        if (budget <= 0) {
          truncated = true;
          break;
        }
        final String query = baseQuery + " AND " + facet.indexField() + ":" + quote(value);
        buckets.add(new CatalogFacetBucket(value, facetBucketCount(facet, query)));
        budget--;
      }
      out.put(facet.rowField(), buckets);
    }
    // The severity-band facet is not seeded from page values: its buckets are the five fixed CVSS bands.
    // It is computed in a SINGLE aggregation pass (one aggregateCountByFloatField call over all bands,
    // not one distinct-CVE count per band), so it costs one query against the per-request budget and
    // cannot blow the p95 target under concurrency.
    if (entityType == CatalogEntityType.VULNERABILITY && !truncated) {
      if (budget >= 1) {
        // Filter-scoped: baseQuery carries the caller's active org/app/stage clauses, so these
        // per-band distinct-CVE counts reflect the filtered corpus — consistent with the other
        // localFacets here, and unlike enrichLocalCounts (global-reach via itemTypeQuery).
        out.put(SEVERITY_FACET_KEY, severityBandFacet(baseQuery));
        budget--;
      }
      else {
        truncated = true;
      }
    }
    if (truncated) {
      warnings.add(CatalogWarnings.FACET_COUNTS_TRUNCATED);
    }
    return out;
  }

  /**
   * CVSS severity-band facet for the local Vulnerabilities leg: five fixed bands
   * ({@code none}/{@code low}/{@code medium}/{@code high}/{@code critical}) with distinct-CVE counts.
   * Each band count is the number of distinct {@code vulnerabilityId} values whose
   * {@code vulnerabilitySeverity} falls in that band's CVSS range, so a CVE that recurs across
   * per-app-per-stage docs counts once in its band (consistent with the orgs/apps vuln facets), not once
   * per doc. RBAC-scoped and fail-closed.
   * <p>
   * Computed in a <em>single</em> aggregation pass via
   * {@link SearchIndexClient#aggregateCountByFloatField(String, String, Map, String)} with
   * {@code distinctField = vulnerabilityId} and {@code ranges = }{@link CvssV3Severity#halfOpenScoreBands()},
   * rather than one {@code countDistinct} query per band. The band boundaries come from that single source
   * of truth, half-open {@code [minInclusive, maxExclusive)}, so a boundary value lands in exactly one band:
   * {@code 4.0} is Medium (not Low), {@code 7.0} is High (not Medium), {@code 9.0} is Critical (not High).
   * {@code none} is the single point {@code 0.0}; {@code critical}'s upper is the inclusive top of the scale
   * ({@code 10.0}). The bounds are passed to the primitive as programmatic {@code float[]} ranges (never
   * string-interpolated into a re-parsed query), so no boundary-rendering footgun exists.
   */
  private List<CatalogFacetBucket> severityBandFacet(final String baseQuery) {
    final Map<String, float[]> bands = CvssV3Severity.halfOpenScoreBands();
    final MetricAggregationResult result = searchIndexClient.aggregateCountByFloatField(
        baseQuery, FieldIdentifier.VULNERABILITY_SEVERITY.label, bands, FIELD_VULNERABILITY_ID);
    final List<CatalogFacetBucket> buckets = new ArrayList<>(bands.size());
    for (String band : bands.keySet()) {
      buckets.add(new CatalogFacetBucket(band, result.buckets.getOrDefault(band, 0L)));
    }
    return buckets;
  }

  /**
   * Adds query-time aggregation counts to the local rows in place, using a single grouped
   * distinct-count read per metric for the whole page. Components get {@code affectedApps} (distinct
   * applicationId grouped by componentHash); vulns get {@code affectedApps} (distinct applicationId)
   * and {@code affectedComponents} (distinct componentHash), each grouped by vulnerabilityId. A group
   * with no matching documents is absent from the result map and treated as zero.
   * <p>
   * These are <em>global-reach</em> counts: the base query carries only the item type (and the RBAC
   * filter applied inside the client, fail-closed) and deliberately DROPS the caller's active
   * app/stage/org filter clauses. "Affected apps" therefore reports the item's true reach across the
   * readable estate, not a count re-scoped to whatever the caller is currently filtering by (which
   * would collapse to 1 under an {@code applications:[X]} filter and read as misleading).
   */
  private void enrichLocalCounts(
      final CatalogEntityType entityType,
      final List<CatalogRow> rows)
  {
    if (rows.isEmpty()) {
      return;
    }
    // Global-reach base: item type + RBAC only, no caller filter clauses (see method javadoc).
    final String baseQuery = itemTypeQuery(entityType);
    if (entityType == CatalogEntityType.COMPONENT) {
      final Set<String> hashes = groupValues(rows, CatalogRowMapper.LOCAL_FIELD_COMPONENT_HASH);
      if (hashes.isEmpty()) {
        return;
      }
      final Map<String, Long> affectedAppsByHash = searchIndexClient.countDistinctGroupedBy(
          baseQuery, FIELD_COMPONENT_HASH, FIELD_APPLICATION_ID, hashes);
      for (int i = 0; i < rows.size(); i++) {
        final CatalogRow row = rows.get(i);
        final Object hash = row.getFields().get(CatalogRowMapper.LOCAL_FIELD_COMPONENT_HASH);
        if (hash == null) {
          continue;
        }
        // countDistinctGroupedBy keys its result map by the lowercased group value (keyword fields
        // carry a lowercase normalizer), so look up with the lowercased key on both backends.
        final long affectedApps = affectedAppsByHash.getOrDefault(groupLookupKey(hash), 0L);
        rows.set(i, row.toBuilder().field("affectedApps", affectedApps).build());
      }
    }
    else {
      final Set<String> vulnIds = groupValues(rows, CatalogRowMapper.LOCAL_FIELD_REFERENCE);
      if (vulnIds.isEmpty()) {
        return;
      }
      final Map<String, Long> affectedAppsByVuln = searchIndexClient.countDistinctGroupedBy(
          baseQuery, FIELD_VULNERABILITY_ID, FIELD_APPLICATION_ID, vulnIds);
      final Map<String, Long> affectedComponentsByVuln = searchIndexClient.countDistinctGroupedBy(
          baseQuery, FIELD_VULNERABILITY_ID, FIELD_COMPONENT_HASH, vulnIds);
      for (int i = 0; i < rows.size(); i++) {
        final CatalogRow row = rows.get(i);
        final Object vulnId = row.getFields().get(CatalogRowMapper.LOCAL_FIELD_REFERENCE);
        if (vulnId == null) {
          continue;
        }
        final String key = groupLookupKey(vulnId);
        rows.set(i, row.toBuilder()
            .field("affectedApps", affectedAppsByVuln.getOrDefault(key, 0L))
            .field("affectedComponents", affectedComponentsByVuln.getOrDefault(key, 0L))
            .build());
      }
    }
  }

  /**
   * Adds the C1 per-severity active-policy-violation counts to each component row, in place. For the
   * page's component hashes, counts distinct active policy violations grouped by component hash within
   * each threat-level severity band, in one RBAC-scoped grouped index read per band (a small constant,
   * not one query per row). Active-only (waiverStatus=Active) so a waived violation is not counted as
   * an active threat, matching the evaluation-card semantics. Distinct policyViolationId dedups a
   * violation re-indexed across per-(app, stage) docs. A component with no active violation is absent
   * from the result and reads zero for every band. The counts are global-reach (item type + RBAC only,
   * no caller filter clauses), like the affected-app count, so they report the component's true threat
   * across the readable estate rather than a filter-rescoped subset.
   *
   * <p>
   * {@code rows} must be a mutable list: like {@link #enrichLocalCounts} this replaces each row in
   * place via {@code rows.set(i, ...)}. Callers pass the {@code new ArrayList<>()} built above; an
   * unmodifiable list would throw {@link UnsupportedOperationException}.
   */
  private void enrichComponentSeverityCounts(final List<CatalogRow> rows) {
    if (rows.isEmpty()) {
      return;
    }
    final Set<String> hashes = groupValues(rows, CatalogRowMapper.LOCAL_FIELD_COMPONENT_HASH);
    if (hashes.isEmpty()) {
      return;
    }
    final String baseQuery = POLICY_VIOLATION_ITEM_TYPE_QUERY + " AND "
        + FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label + ":\""
        + POLICY_VIOLATION_WAIVER_STATUS_ACTIVE + "\"";
    final Map<String, Map<String, Long>> countsByHash = searchIndexClient.countDistinctGroupedByBands(
        baseQuery, FIELD_COMPONENT_HASH, FIELD_POLICY_VIOLATION_ID, hashes,
        FIELD_POLICY_VIOLATION_THREAT_LEVEL, ThreatLevel.searchAggregationBands());
    for (int i = 0; i < rows.size(); i++) {
      final CatalogRow row = rows.get(i);
      final Object hash = row.getFields().get(CatalogRowMapper.LOCAL_FIELD_COMPONENT_HASH);
      if (hash == null) {
        continue;
      }
      final Map<String, Long> bands = countsByHash.getOrDefault(groupLookupKey(hash), Map.of());
      final CatalogRow.Builder builder = row.toBuilder();
      // Emit all four counts (zero when absent) so the row always carries the full breakdown.
      SEVERITY_BAND_TO_ROW_FIELD.forEach((band, rowField) -> builder.field(rowField, bands.getOrDefault(band, 0L)));
      rows.set(i, builder.build());
    }
  }

  /**
   * Key for looking up a grouped-count result by group value. {@code countDistinctGroupedBy(Bands)}
   * keys its result map by the lowercased group value (keyword fields carry a lowercase normalizer),
   * so callers must look up with the lowercased key on both backends.
   */
  private static String groupLookupKey(final Object groupValue) {
    return String.valueOf(groupValue).toLowerCase(Locale.ROOT);
  }

  /** Distinct non-null values of {@code rowField} across the page, in row order. */
  private static Set<String> groupValues(final List<CatalogRow> rows, final String rowField) {
    final Set<String> values = new LinkedHashSet<>();
    for (CatalogRow row : rows) {
      final Object value = row.getFields().get(rowField);
      if (value != null) {
        values.add(String.valueOf(value));
      }
    }
    return values;
  }

  /**
   * Facet bucket count, RBAC-scoped and fail-closed. When the facet declares a
   * {@link LocalFacet#distinctKey()} the count is a {@code countDistinct} over that key, so a bucket
   * whose docs are per-app-per-stage reports the number of distinct entities (e.g. distinct CVEs for a
   * vuln facet, distinct components for a component apps facet) rather than raw docs. Every VULNERABILITY
   * facet declares distinctKey=vulnerabilityId, so distinct-CVE counting is driven purely by the facet
   * spec rather than a per-entity-type fallthrough. When no distinct key is declared a plain document
   * count is used.
   */
  private long facetBucketCount(final LocalFacet facet, final String query) {
    if (facet.distinctKey() != null) {
      return searchIndexClient.countDistinct(query, List.of(facet.distinctKey()));
    }
    return searchIndexClient.count(query);
  }

  /**
   * RBAC-scoped facet-count base: {@code itemType:<localType> AND <structured filter clauses>}. The
   * RBAC filter is applied inside {@link SearchIndexClient#count(String)} (fail-closed), so a caller
   * with no readable contexts counts 0 rather than an unscoped total.
   */
  private static String baseMetricQuery(final CatalogEntityType entityType, final LocalQuery local) {
    final StringBuilder q = new StringBuilder(itemTypeQuery(entityType));
    for (String clause : local.fieldClauses()) {
      q.append(" AND ").append(clause);
    }
    return q.toString();
  }

  /**
   * Item-type-only metric query ({@code itemType:<localType>}), without any caller filter clauses.
   * The RBAC filter is applied inside the client (fail-closed). Used as the global-reach base for the
   * affected-app/component counts so they report the item's reach across the whole readable estate
   * rather than a filter-scoped subset.
   */
  private static String itemTypeQuery(final CatalogEntityType entityType) {
    final Set<ItemType> types = entityType.localItemTypes();
    // The catalog entity types each map to a single local item type; join defensively if that changes.
    final List<String> typeClauses = new ArrayList<>(types.size());
    for (ItemType type : types) {
      typeClauses.add("itemType:" + type.searchFieldName());
    }
    return typeClauses.size() == 1 ? typeClauses.get(0) : "(" + String.join(" OR ", typeClauses) + ")";
  }

  /** True when the caller applied a non-empty {@code stages} terms filter. */
  private static boolean hasStagesFilter(final Map<String, Object> filters) {
    if (filters == null) {
      return false;
    }
    final Object stages = filters.get("stages");
    return stages instanceof List<?> list && !list.isEmpty();
  }

  private static String quote(final String value) {
    // Keyword fields are matched as a single lowercased token; escape embedded quotes/backslashes so a
    // value cannot break out of the phrase. Not injection defense (server-built), just query shaping.
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  /** Catalog warning strings in one place so wording is asserted from a single source in tests. */
  public static final class CatalogWarnings
  {
    public static final String SORT_NOT_APPLIED = "sort is not applied for the catalog source";

    public static final String SEARCH_AFTER_NOT_APPLIED = "searchAfter is not applied for the catalog source";

    public static final String CATALOG_UNAVAILABLE = "catalog source is unavailable";

    public static final String FACET_COUNTS_TRUNCATED =
        "some facet counts were omitted to stay within the per-request query budget";

    public static final String STAGES_EXCLUDE_SBOM_VULNS =
        "the stages filter excludes SBOM-sourced vulnerabilities, which are not scoped to a policy evaluation stage";

    private CatalogWarnings() {
    }
  }
}
