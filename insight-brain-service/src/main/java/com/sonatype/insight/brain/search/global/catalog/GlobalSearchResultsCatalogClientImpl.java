/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sonatype.guide.api.dto.ComponentLicense;
import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.telemetry.GuideOperationType;
import com.sonatype.insight.brain.guide.telemetry.GuideUsageEvent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.global.GlobalSearchCursor;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.global.GlobalSearchTenancy;
import com.sonatype.insight.brain.search.global.ResultRow;
import com.sonatype.insight.brain.search.global.ResultsRequest;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.SectionResult;
import com.sonatype.insight.brain.search.global.Tab;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;

/**
 * Live catalog leg for the Global Search results endpoint. Serves the catalog-backed tabs
 * ({@link Tab#COMPONENT}, {@link Tab#VULNERABILITY}) by calling the HDS global-search path through the
 * dedicated {@link GlobalSearchCatalogHdsClient} (its own connection pool, sub-second timeout, no
 * retries) and mapping the response documents to {@link ResultRow}s whose {@code fields} bag mirrors the
 * catalog list endpoint's row shape.
 *
 * <h3>Entitlement</h3>
 *
 * <p>
 * {@link #isEnabled()} and {@link #searchResults(ResultsRequest)} apply the same gate the catalog list
 * endpoint uses: deny on multi-tenant (MTIQ) deployments and require the
 * {@link LicensedFeature#GUIDE_SEARCH} feature. When not entitled the client degrades — it never reaches
 * HDS and never fails the response.
 *
 * <h3>Failure handling</h3>
 *
 * <p>
 * Every upstream failure mode (5xx, 429, timeout, license-unavailable, network, malformed payload)
 * collapses to a degraded empty {@link SectionResult} carrying a warning, so the {@code /results}
 * dispatcher degrades the catalog section only and never returns a 500.
 *
 * <h3>Href policy</h3>
 *
 * <p>
 * Rows carry NO href. The catalog leg does not emit Guide-outbound links; rows stay within Lifecycle.
 * A row is never dropped for lacking an href.
 */
@Named
@Primary
@Singleton
public class GlobalSearchResultsCatalogClientImpl
    implements GlobalSearchResultsCatalogClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchResultsCatalogClientImpl.class);

  private static final String GLOBAL_SEARCH_PATH = "rest/search/global";

  /**
   * Backend id folded into the catalog cursor's generation-token pin. Must match the backend id
   * {@code ResultsService} validates the incoming catalog cursor against.
   */
  public static final String BACKEND_CATALOG = "catalog";

  public static final String WARNING_UNAVAILABLE = "catalog source is unavailable";

  private final GlobalSearchCatalogHdsClient hdsClient;

  private final ProductLicense productLicense;

  private final TenantUtil tenantUtil;

  @Inject
  public GlobalSearchResultsCatalogClientImpl(
      final GlobalSearchCatalogHdsClient hdsClient,
      final ProductLicense productLicense,
      final TenantUtil tenantUtil)
  {
    this.hdsClient = hdsClient;
    this.productLicense = productLicense;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public boolean isEnabled() {
    return entitled();
  }

  @Override
  public Optional<SectionResult> searchResults(final ResultsRequest request) {
    final Tab tab = request.getTab();
    if (tab != Tab.COMPONENT && tab != Tab.VULNERABILITY) {
      // The catalog leg only serves COMPONENT and VULNERABILITY. Any other tab is empty here.
      return Optional.of(SectionResult.empty(tab, entitled()));
    }
    if (!entitled()) {
      // Not entitled (MTIQ or missing GUIDE_SEARCH): degrade, never reach HDS, never 500.
      return Optional.of(degraded(tab));
    }

    final long offset = offsetOf(request);
    final GuideGlobalSearchResponse response;
    try {
      response = callCatalogGlobalSearch(request, offset);
    }
    catch (NotFoundException nfe) {
      // 404 from HDS — the catalog answered with no hits. Available but empty.
      log.debug("Catalog global search returned 404 for tab {} (treating as empty available result)", tab);
      return Optional.of(new SectionResult(tab, List.of(), 0L, null, true));
    }
    catch (PaymentRequiredException | GuideApiException | GatewayTimeoutException
        | BadGatewayException | InternalServerErrorException upstream)
    {
      log.warn("Catalog global search failed for tab {} (degrading catalog section): {}",
          tab, upstream.getMessage());
      return Optional.of(degraded(tab));
    }
    catch (RuntimeException unexpected) {
      log.warn("Catalog global search failed for tab {} with unexpected exception (degrading catalog section)",
          tab, unexpected);
      return Optional.of(degraded(tab));
    }

    if (response == null) {
      log.warn("Catalog global search returned null response for tab {} (degrading catalog section)", tab);
      return Optional.of(degraded(tab));
    }
    final List<ResultRow> rows = toRows(tab, response.hits());
    // HDS rest/search/global returns a polymorphic mixed stream (components AND vulnerabilities); toRows
    // keeps only this tab's subtype. Paging therefore advances by HITS CONSUMED, not mapped rows: a page
    // dominated by the other subtype yields few or zero rows yet must keep advancing through the stream.
    // total is the mixed global total straight from HDS. A type-scoped HDS path would let the leg page
    // by mapped rows and report a type-scoped total; TODO(CLM-41642) revisit if HDS gains one.
    final long total = response.total();
    final int hitsConsumed = response.hits().size();
    final String nextCursor = nextCatalogCursor(request, offset, hitsConsumed, total);
    return Optional.of(new SectionResult(tab, rows, total, nextCursor, true));
  }

  /**
   * Resolves the zero-based offset for this page. A caller-supplied catalog cursor carries the offset as
   * its single opaque tuple value; absent a cursor the offset is derived from {@code page * pageSize}.
   * {@code ResultsService} has already validated the cursor's generation token (HTTP 410 on drift) before
   * this leg runs, so decoding here cannot resurrect a stale cursor.
   */
  private static long offsetOf(final ResultsRequest request) {
    if (!request.usesCursor()) {
      return request.offset();
    }
    GlobalSearchCursor cursor = GlobalSearchCursor.decode(request.getSearchAfter(), catalogToken(request));
    List<String> values = cursor.sortValues();
    if (values.isEmpty()) {
      return 0L;
    }
    try {
      long parsed = Long.parseLong(values.get(0));
      return parsed < 0 ? 0L : parsed;
    }
    catch (NumberFormatException e) {
      // Server-minted cursors always carry a numeric offset; a non-numeric value means a tampered
      // payload that survived token validation only in a test. Restart at page 1 rather than fail.
      return 0L;
    }
  }

  /**
   * Mints the next-page offset cursor, or {@code null} when the mixed stream is drained. The offset
   * advances by the number of HDS HITS CONSUMED this page (not the mapped-row count), so paging keeps
   * walking the polymorphic component+vulnerability stream even when this page mapped few or zero rows
   * of the requested subtype. Stops when the consumed offset reaches the reported total, or when HDS
   * returned zero hits (no forward progress possible) to avoid walking empty pages forever.
   */
  private static String nextCatalogCursor(
      final ResultsRequest request,
      final long offset,
      final int hitsConsumed,
      final long total)
  {
    long nextOffset = offset + hitsConsumed;
    if (hitsConsumed == 0 || nextOffset >= total) {
      return null;
    }
    return GlobalSearchCursor
        .newCursor(catalogToken(request), List.of(Long.toString(nextOffset)))
        .encode();
  }

  /**
   * Computes the catalog cursor's generation token. Mirrors {@code ResultsService.expectedTokenFor} for
   * the catalog backend so a cursor this leg mints round-trips through the dispatcher's decode/validate.
   */
  private static String catalogToken(final ResultsRequest request) {
    String sortKey = request.getSort() == null || request.getSort().isBlank()
        ? GlobalSearchSortAllowlist.RELEVANCE
        : request.getSort();
    return GlobalSearchCursor.computeGenerationToken(
        GlobalSearchCursor.currentGenerationToken(),
        request.getTab().name(),
        sortKey,
        request.getPageSize(),
        request.getSource().value() + ":" + BACKEND_CATALOG,
        GlobalSearchTenancy.currentTenantId());
  }

  /**
   * Performs the raw HDS call. Carries the {@link GuideUsageEvent} annotation so the AspectJ advice
   * only sees the request DTO (not the raw query) and only records on actual success — exceptions
   * propagate to {@link #searchResults(ResultsRequest)} for degrade handling.
   */
  @GuideUsageEvent(operationType = GuideOperationType.GLOBAL_SEARCH)
  GuideGlobalSearchResponse callCatalogGlobalSearch(final ResultsRequest request, final long offset) {
    final Multimap<String, String> params = ArrayListMultimap.create();
    params.put("query", request.getQ());
    params.put("limit", String.valueOf(request.getPageSize()));
    params.put("offset", String.valueOf(offset));
    return hdsClient.getWithMultimap(GuideGlobalSearchResponse.class, GLOBAL_SEARCH_PATH, params);
  }

  /**
   * Mirrors the catalog list endpoint gate ({@code SearchLicenseFilter}'s Guide-API rule): deny on
   * multi-tenant deployments and require the {@link LicensedFeature#GUIDE_SEARCH} feature.
   */
  private boolean entitled() {
    return !tenantUtil.isMultiTenant() && productLicense.hasFeature(LicensedFeature.GUIDE_SEARCH);
  }

  private static SectionResult degraded(final Tab tab) {
    return new SectionResult(tab, List.of(), 0L, null, false, List.of(WARNING_UNAVAILABLE));
  }

  private static List<ResultRow> toRows(final Tab tab, final List<SearchResult> hits) {
    if (hits == null || hits.isEmpty()) {
      return List.of();
    }
    final List<ResultRow> rows = new ArrayList<>(hits.size());
    for (SearchResult hit : hits) {
      final ResultRow row = toRow(tab, hit);
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  private static ResultRow toRow(final Tab tab, final SearchResult hit) {
    if (tab == Tab.COMPONENT && hit instanceof GuideComponentDocument component) {
      return componentRow(component);
    }
    if (tab == Tab.VULNERABILITY && hit instanceof GuideVulnerabilityDocument vuln) {
      return vulnerabilityRow(vuln);
    }
    // The requested tab and the hit subtype disagree, or the payload is an unrecognized subtype. Drop
    // the row rather than fail the whole response.
    log.debug("Catalog global search returned unexpected hit type {} for tab {}",
        hit == null ? "null" : hit.getClass().getName(), tab);
    return null;
  }

  private static ResultRow componentRow(final GuideComponentDocument c) {
    final String coordinate = CatalogCoordinates.coordinateOf(c);
    if (coordinate == null) {
      return null;
    }
    return ResultRow.builder()
        .type(Tab.COMPONENT.name())
        .source(SearchSource.CATALOG.value())
        .id(coordinate)
        .title(c.name() != null ? c.name() : coordinate)
        .subtitle(c.version())
        .field("ecosystem", c.format())
        .field("name", c.name())
        .field("namespace", c.namespace())
        .field("latest", c.version())
        .field("licenses", licenseNames(c.licenses()))
        .field("categories", c.categories())
        .field("latestStable", c.latestStable())
        .field("versionScore", c.versionScore())
        .field("latestMaxCvss", c.maxCvss())
        .field("publishedDate", c.publishedDate() == null ? null : c.publishedDate().toString())
        .field("malware", c.isMalware())
        .build();
  }

  private static ResultRow vulnerabilityRow(final GuideVulnerabilityDocument v) {
    final String refid = v.refid();
    if (refid == null || refid.isBlank()) {
      return null;
    }
    return ResultRow.builder()
        .type(Tab.VULNERABILITY.name())
        .source(SearchSource.CATALOG.value())
        .id(refid)
        .title(refid)
        .subtitle(v.summary())
        .field("reference", refid)
        .field("aliases", v.aliases())
        .field("vulnerabilitySource", v.source())
        .field("severity", v.cvssSeverity())
        .field("sonatypeSeverity", v.sonatypeCvssSeverity())
        .field("cwe", v.cwes())
        .field("affectedEcosystems", v.affectedEcosystems())
        .field("isKev", v.kev())
        .field("epssScore", v.epss())
        .field("isMalware", v.isMalware())
        .field("researchType", v.researchType())
        .field("publishedAt", v.publishedAt() == null ? null : v.publishedAt().toString())
        .build();
  }

  private static List<String> licenseNames(final List<? extends ComponentLicense> licenses) {
    if (licenses == null || licenses.isEmpty()) {
      return null;
    }
    final List<String> names = new ArrayList<>(licenses.size());
    for (ComponentLicense l : licenses) {
      if (l != null && l.licenseName() != null) {
        names.add(l.licenseName());
      }
    }
    return names.isEmpty() ? null : names;
  }
}
