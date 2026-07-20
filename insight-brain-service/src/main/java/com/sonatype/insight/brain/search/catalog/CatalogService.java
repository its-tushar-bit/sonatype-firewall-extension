/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  /**
   * Local facet spec: each entry maps a catalog row-field (whose page values seed the bucket list)
   * to the IQ-local index field the whole-corpus count queries. Kept ordered so the returned facet
   * map is stable.
   */
  private static final Map<CatalogEntityType, List<LocalFacet>> LOCAL_FACET_FIELDS = Map.of(
      CatalogEntityType.COMPONENT,
      // Only fields a NON_VULNERABLE_COMPONENT doc actually carries: componentFormat and
      // organizationName. Effective-license / license-threat-group live on LEGAL_VIOLATION docs, never
      // on component docs, so a licenseThreatGroup facet here would always be empty in production.
      List.of(
          new LocalFacet(CatalogRowMapper.LOCAL_FIELD_ECOSYSTEM, "componentFormat"),
          // organizationName is rewritten to parentOrganizationName by the metric layer, so the count
          // includes the org and its descendants (consistent with the organizations filter clause).
          new LocalFacet(CatalogRowMapper.LOCAL_FIELD_ORGANIZATION, "organizationName")),
      CatalogEntityType.VULNERABILITY,
      List.of(new LocalFacet(CatalogRowMapper.LOCAL_FIELD_STATUS, "vulnerabilityStatus")));

  /**
   * Cap on distinct values counted per facet field. Facet values come from the current page's rows,
   * so this is bounded by page size in practice; the cap is a hard ceiling on {@code count()} calls
   * per field to keep per-field fan-out well below page size (see {@link #MAX_FACET_COUNT_QUERIES}).
   */
  static final int MAX_FACET_BUCKETS_PER_FIELD = 20;

  /**
   * Overall ceiling on whole-corpus facet {@code count()} calls issued per request across all facet
   * fields. Each bucket count is one RBAC-scoped index query; bounding total fan-out keeps a single
   * {@code includeFacets} request from firing dozens of counts under load (p95 &lt; 300ms target).
   * Once the budget is exhausted the remaining buckets are omitted and a truncation warning is added.
   */
  static final int MAX_FACET_COUNT_QUERIES = 40;

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

  private record LocalFacet(String rowField, String indexField)
  {
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

    // Pin the next-page cursor to the backend that actually served this page, so a cross-backend
    // switch on the follow-up request is rejected as stale rather than silently mis-paginated.
    final GlobalSearchCursor next = iqLocalSearchService.mintNextCursor(
        tab, sortKey, pageSize, result.nextSearchAfter(), result.servingBackendId());

    final List<String> warnings = new ArrayList<>(result.warnings());
    warnings.addAll(local.warnings());
    if (!GlobalSearchSortAllowlist.RELEVANCE.equals(sortKey) && !IqLocalSearchService.isFieldSortEnabled()) {
      warnings.add("sort is relevance-only until field sort is enabled");
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
        buckets.add(new CatalogFacetBucket(value, searchIndexClient.count(query)));
        budget--;
      }
      out.put(facet.rowField(), buckets);
    }
    if (truncated) {
      warnings.add(CatalogWarnings.FACET_COUNTS_TRUNCATED);
    }
    return out;
  }

  /**
   * RBAC-scoped facet-count base: {@code itemType:<localType> AND <structured filter clauses>}. The
   * RBAC filter is applied inside {@link SearchIndexClient#count(String)} (fail-closed), so a caller
   * with no readable contexts counts 0 rather than an unscoped total.
   */
  private static String baseMetricQuery(final CatalogEntityType entityType, final LocalQuery local) {
    final StringBuilder q = new StringBuilder();
    final Set<ItemType> types = entityType.localItemTypes();
    // The catalog entity types each map to a single local item type; join defensively if that changes.
    final List<String> typeClauses = new ArrayList<>(types.size());
    for (ItemType type : types) {
      typeClauses.add("itemType:" + type.searchFieldName());
    }
    q.append(typeClauses.size() == 1 ? typeClauses.get(0) : "(" + String.join(" OR ", typeClauses) + ")");
    for (String clause : local.fieldClauses()) {
      q.append(" AND ").append(clause);
    }
    return q.toString();
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

    private CatalogWarnings() {
    }
  }
}
