/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.telemetry.GuideOperationType;
import com.sonatype.insight.brain.guide.telemetry.GuideUsageEvent;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.SuggestItemType;
import com.sonatype.insight.brain.search.global.SuggestRow;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;

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
 * Live catalog leg for the Global Search suggest endpoint. Serves the catalog-backed suggest types
 * ({@link SuggestItemType#COMPONENT}, {@link SuggestItemType#VULNERABILITY}) by calling the HDS
 * global-search path through the dedicated {@link GlobalSearchCatalogHdsClient} (own connection pool,
 * sub-second timeout, no retries) and mapping the response documents to {@link SuggestRow}s.
 *
 * <h3>Entitlement</h3>
 *
 * <p>
 * Catalog federation is base Nexus One functionality: it is available with any valid IQ license, on
 * both single-tenant and multi-tenant (MTIQ) deployments. IQ does not serve requests without a valid
 * product license, so no license-feature or tenancy gate is applied here.
 * <p>
 * The only gate on this leg is the {@code PREVIEW_NEXUS_ONE_UI} feature, enforced upstream by
 * {@code GlobalSearchResource#verifyPreviewUiEnabled()}. The admin {@code CATALOG_FEDERATION} toggle
 * is NOT consulted: it defaults to off ({@code enabledWhenAbsent = false}) and gates only the catalog
 * browse endpoint ({@code CatalogService#searchCatalog}), so reading it here would leave global search's
 * catalog suggestions dark on every deployment that has not explicitly switched it on.
 *
 * <h3>Failure handling</h3>
 *
 * <p>
 * Every upstream failure mode (5xx, 429, timeout, license-unavailable, network, malformed payload)
 * collapses to {@link CatalogSuggestResult#unavailable()} so the suggest service degrades the catalog
 * groups only and never returns a 500. A bare empty response is a distinct state
 * ({@link CatalogSuggestResult#available(List)} with an empty row list).
 *
 * <h3>Href policy</h3>
 *
 * <p>
 * Rows carry NO href. The catalog leg does not emit catalog-outbound links; rows stay within
 * Lifecycle. A row is never dropped for lacking an href.
 */
@Named
@Primary
@Singleton
public class GlobalSearchSuggestCatalogClientImpl
    implements GlobalSearchSuggestCatalogClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchSuggestCatalogClientImpl.class);

  private static final String GLOBAL_SEARCH_PATH = "rest/search/global";

  private final GlobalSearchCatalogHdsClient hdsClient;

  @Inject
  public GlobalSearchSuggestCatalogClientImpl(final GlobalSearchCatalogHdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  @Override
  public boolean isEnabled() {
    return entitled();
  }

  @Override
  public CatalogSuggestResult suggest(final CatalogSuggestRequest request) {
    if (!entitled()) {
      return CatalogSuggestResult.unavailable();
    }

    try {
      // Row mapping (toRows) stays inside this try so a mapping RuntimeException degrades to
      // unavailable here rather than escaping suggest() — the no-throw guarantee is self-contained.
      final GuideGlobalSearchResponse response = callCatalogGlobalSearch(request);
      if (response == null) {
        log.warn("Catalog suggest returned null response (degrading catalog groups)");
        return CatalogSuggestResult.unavailable();
      }
      if (response.hits() == null || response.hits().isEmpty()) {
        return CatalogSuggestResult.available(List.of());
      }
      return CatalogSuggestResult.available(toRows(response.hits()));
    }
    catch (NotFoundException nfe) {
      // 404 from HDS means no hits, not a broken integration; matches the results leg
      // (GlobalSearchResultsCatalogClientImpl) so both treat HDS status codes the same way.
      log.debug("Catalog suggest returned 404 (treating as empty available result)");
      return CatalogSuggestResult.available(List.of());
    }
    catch (PaymentRequiredException | GuideApiException | GatewayTimeoutException
        | BadGatewayException | InternalServerErrorException upstream)
    {
      log.warn("Catalog suggest failed (degrading catalog groups): {}", upstream.getMessage());
      return CatalogSuggestResult.unavailable();
    }
    catch (RuntimeException unexpected) {
      log.warn("Catalog suggest failed with unexpected exception (degrading catalog groups)", unexpected);
      return CatalogSuggestResult.unavailable();
    }
  }

  /**
   * Performs the raw HDS call. Carries the {@link GuideUsageEvent} annotation so the AspectJ advice
   * only sees the request DTO (not the raw query) and only records on actual success — exceptions
   * propagate to {@link #suggest(CatalogSuggestRequest)} for degrade handling.
   * <p>
   * Reported as {@link GuideOperationType#CATALOG_FEDERATION_SEARCH}, not
   * {@link GuideOperationType#GLOBAL_SEARCH}: this leg is base functionality reachable with any valid IQ
   * license, so downstream usage analytics must be able to tell it apart from Guide-licensed traffic.
   */
  @GuideUsageEvent(operationType = GuideOperationType.CATALOG_FEDERATION_SEARCH)
  GuideGlobalSearchResponse callCatalogGlobalSearch(final CatalogSuggestRequest request) {
    final Multimap<String, String> params = ArrayListMultimap.create();
    params.put("query", request.query());
    params.put("limit", String.valueOf(request.limit()));
    return hdsClient.getWithMultimap(GuideGlobalSearchResponse.class, GLOBAL_SEARCH_PATH, params);
  }

  /**
   * Always true: catalog federation is base Nexus One functionality available with any valid IQ license
   * on both single-tenant and MTIQ deployments, so no license-feature or tenancy check applies.
   *
   * <p>
   * Entitlement is not the gate that decides whether the catalog source is offered. That is the
   * default-off {@code CATALOG_FEDERATION} flag, enforced on {@code ?source=catalog} by
   * {@code GlobalSearchResource.verifyCatalogSourceAllowed}; this leg is additionally killed by
   * {@code PREVIEW_NEXUS_ONE_UI} in the same resource. Both live at the request boundary, so this
   * method answering "licensed?" rather than "offered?" is the whole split.
   */
  private boolean entitled() {
    return true;
  }

  private static List<SuggestRow> toRows(final List<SearchResult> hits) {
    final List<SuggestRow> rows = new ArrayList<>(hits.size());
    for (SearchResult hit : hits) {
      final SuggestRow row = toRow(hit);
      if (row != null) {
        rows.add(row);
      }
    }
    return rows;
  }

  private static SuggestRow toRow(final SearchResult hit) {
    if (hit instanceof GuideComponentDocument component) {
      return componentRow(component);
    }
    if (hit instanceof GuideVulnerabilityDocument vuln) {
      return vulnerabilityRow(vuln);
    }
    // Defensive only: GuideGlobalSearchResponse registers exactly two DEDUCTION subtypes
    // (GuideComponentDocument, GuideVulnerabilityDocument), so Jackson cannot hand us a third type.
    // If that registration ever grows, drop the unmapped row rather than fail the whole response.
    log.debug("Catalog suggest returned unrecognized hit type {}",
        hit == null ? "null" : hit.getClass().getName());
    return null;
  }

  private static SuggestRow componentRow(final GuideComponentDocument c) {
    final String coordinate = CatalogCoordinates.coordinateOf(c);
    if (coordinate == null) {
      return null;
    }
    final String title = (c.name() != null && !c.name().isBlank()) ? c.name() : coordinate;
    return new SuggestRow(
        coordinate,
        SuggestItemType.COMPONENT,
        SearchSource.CATALOG,
        title,
        c.version(),
        /* href */ null);
  }

  private static SuggestRow vulnerabilityRow(final GuideVulnerabilityDocument v) {
    final String refid = v.refid();
    if (refid == null || refid.isBlank()) {
      return null;
    }
    return new SuggestRow(
        refid,
        SuggestItemType.VULNERABILITY,
        SearchSource.CATALOG,
        refid,
        v.summary(),
        /* href */ null);
  }

}
