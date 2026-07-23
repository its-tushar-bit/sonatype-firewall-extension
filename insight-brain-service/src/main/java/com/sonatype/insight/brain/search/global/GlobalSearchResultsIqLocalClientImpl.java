/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.sonatype.insight.brain.search.global.IqLocalSearchService.IqLocalSearchResponse;
import com.sonatype.insight.brain.search.global.IqLocalSearchService.SearchInputs;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;

/**
 * Concrete Lucene-backed {@link GlobalSearchResultsIqLocalClient} for Global Search {@code /results}.
 * Adapts {@link IqLocalSearchService} (the real Lucene query executor) to the SPI shape consumed
 * by {@link ResultsService}.
 *
 * <p>
 * Responsibilities:
 * <ol>
 * <li>Map the request {@link Tab} to its native {@link ItemType}(s). {@link Tab#VIOLATION} maps
 * to {@code {POLICY_VIOLATION, LEGAL_VIOLATION}} (merged at the API surface).</li>
 * <li>Hand off to {@link IqLocalSearchService#search(SearchInputs)} which runs the AST-compiled
 * query and returns typed rows plus warnings.</li>
 * <li>Map each returned {@link SearchResultItemDTO} to a {@link ResultRow} via a per-tab row
 * builder.</li>
 * </ol>
 *
 * <p>
 * Cursor validation is handled by the outer {@link ResultsService} before dispatch; this adapter
 * only unwraps {@code searchAfter} bytes from the caller-supplied cursor for the inner service.
 */
@Named
@Primary
@Singleton
public class GlobalSearchResultsIqLocalClientImpl
    implements GlobalSearchResultsIqLocalClient
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchResultsIqLocalClientImpl.class);

  /** Tab → native ItemType set. Some tabs union multiple item types (VIOLATION merges POLICY+LEGAL). */
  private static final Map<Tab, Set<ItemType>> NATIVE_TYPES_BY_TAB;

  static {
    EnumMap<Tab, Set<ItemType>> nativeTypes = new EnumMap<>(Tab.class);
    nativeTypes.put(Tab.APPLICATION, Set.of(ItemType.APPLICATION));
    // Merged VIOLATION tab unions both policy and legal violation item types.
    nativeTypes.put(Tab.VIOLATION, Set.of(ItemType.POLICY_VIOLATION, ItemType.LEGAL_VIOLATION));
    nativeTypes.put(Tab.WAIVER, Set.of(ItemType.POLICY_WAIVER));
    // The local (My Scan Data) source includes component and vulnerability item types from the IQ
    // index; the catalog source serves those tabs from HDS instead.
    nativeTypes.put(Tab.COMPONENT, Set.of(ItemType.NON_VULNERABLE_COMPONENT));
    nativeTypes.put(Tab.VULNERABILITY, Set.of(ItemType.SECURITY_VULNERABILITY));
    NATIVE_TYPES_BY_TAB = Map.copyOf(nativeTypes);
  }

  private final IqLocalSearchService iqLocalSearchService;

  @Inject
  public GlobalSearchResultsIqLocalClientImpl(final IqLocalSearchService iqLocalSearchService) {
    this.iqLocalSearchService = Objects.requireNonNull(iqLocalSearchService, "iqLocalSearchService");
  }

  @Override
  public Optional<SectionResult> searchNative(final ResultsRequest request) {
    final Set<ItemType> types = NATIVE_TYPES_BY_TAB.get(request.getTab());
    if (types == null) {
      // ALL never reaches searchNative (the packer composes sections). COMPONENT/VULNERABILITY DO
      // reach here on the local source and map from the IQ index; on the catalog source the catalog
      // leg serves them instead.
      return Optional.empty();
    }
    return Optional.of(runSearch(request, types));
  }

  private SectionResult runSearch(final ResultsRequest request, final Set<ItemType> types) {
    final int pageSize = request.getPageSize();
    final String sortKey = effectiveSortKey(request.getSort());

    // Thread the caller-supplied searchAfter cursor into the inner service so page 2+ works.
    // The outer /results endpoint has already validated the generation-token binding; the inner
    // IqLocalSearchService decodes the raw cursor string against its own preimage.
    final String cursor = request.getSearchAfter() == null || request.getSearchAfter().isBlank()
        ? null
        : request.getSearchAfter();

    final SearchInputs inputs = new SearchInputs(
        request.getQ(),
        request.getTab(),
        types,
        pageSize,
        sortKey,
        cursor);

    final IqLocalSearchResponse response = iqLocalSearchService.search(inputs);

    final Function<SearchResultItemDTO, ResultRow> mapper = rowMapperFor(request.getTab());
    final List<ResultRow> rows = new ArrayList<>(response.rows().size());
    int droppedCount = 0;
    for (IqLocalSearchService.IqLocalRow raw : response.rows()) {
      final ResultRow mapped = mapper.apply(raw.row());
      if (mapped == null) {
        droppedCount++;
        continue;
      }
      rows.add(mapped);
    }
    if (droppedCount > 0) {
      log.debug("Dropped {} malformed rows for tab {} (missing id or title)",
          droppedCount, request.getTab());
    }

    // Mint the next-page cursor through IqLocalSearchService.mintNextCursor — the same authoritative
    // path the list/catalog endpoints use — pinned to the backend that actually served the page. This
    // guarantees the mint preimage matches what IqLocalSearchService.search validates on the follow-up
    // request, so page 2+ decodes cleanly instead of tripping the stale-cursor 410 guard.
    final GlobalSearchCursor next = iqLocalSearchService.mintNextCursor(
        request.getTab(), sortKey, pageSize, response.nextSearchAfter(), response.servingBackendId());
    final String nextCursor = next == null ? null : next.encode();

    // Thread parser + compiler warnings up so ResultsService can surface them on X-Search-Warnings.
    return new SectionResult(request.getTab(), rows, response.total(), nextCursor, true, response.warnings());
  }

  private static String effectiveSortKey(final String sortKey) {
    return sortKey == null || sortKey.isBlank() ? GlobalSearchSortAllowlist.RELEVANCE : sortKey;
  }

  /** Return the per-tab {@link SearchResultItemDTO} → {@link ResultRow} mapper. */
  static Function<SearchResultItemDTO, ResultRow> rowMapperFor(final Tab tab) {
    return switch (tab) {
      case APPLICATION -> GlobalSearchResultsIqLocalClientImpl::mapApplication;
      case COMPONENT -> GlobalSearchResultsIqLocalClientImpl::mapComponent;
      case VULNERABILITY -> GlobalSearchResultsIqLocalClientImpl::mapVulnerability;
      case VIOLATION -> GlobalSearchResultsIqLocalClientImpl::mapViolation;
      case WAIVER -> GlobalSearchResultsIqLocalClientImpl::mapWaiver;
      case ALL -> throw new IllegalStateException(
          "Tab.ALL is packed at ResultsService; rowMapperFor must not be invoked with ALL");
    };
  }

  private static ResultRow mapApplication(final SearchResultItemDTO doc) {
    if (doc.applicationId == null || doc.applicationName == null) {
      return null;
    }
    return ResultRow.builder()
        .type(Tab.APPLICATION.name())
        .source(SearchSource.LOCAL.value())
        .id(doc.applicationId)
        .title(doc.applicationName)
        .subtitle(doc.applicationPublicId)
        .field("applicationPublicId", doc.applicationPublicId)
        .field("organizationId", doc.organizationId)
        .field("organizationName", doc.organizationName)
        .build();
  }

  private static ResultRow mapComponent(final SearchResultItemDTO doc) {
    if (doc.componentHash == null && doc.componentName == null) {
      return null;
    }
    final String id = doc.componentHash != null ? doc.componentHash : doc.componentName;
    final String title = doc.componentName != null ? doc.componentName : doc.componentHash;
    return ResultRow.builder()
        .type(Tab.COMPONENT.name())
        .source(SearchSource.LOCAL.value())
        .id(id)
        .title(title)
        .field("componentHash", doc.componentHash)
        .field("componentName", doc.componentName)
        .build();
  }

  private static ResultRow mapVulnerability(final SearchResultItemDTO doc) {
    if (doc.vulnerabilityId == null) {
      return null;
    }
    return ResultRow.builder()
        .type(Tab.VULNERABILITY.name())
        .source(SearchSource.LOCAL.value())
        .id(doc.vulnerabilityId)
        .title(doc.vulnerabilityId)
        .subtitle(doc.vulnerabilityDescription)
        .field("vulnerabilityId", doc.vulnerabilityId)
        .field("status", doc.vulnerabilityStatus)
        .build();
  }

  private static ResultRow mapViolation(final SearchResultItemDTO doc) {
    // Merged VIOLATION tab covers both POLICY_VIOLATION and LEGAL_VIOLATION docs - both carry the
    // same policyViolation* fields, so a single mapper handles both.
    if (doc.policyViolationId == null || doc.policyViolationPolicyName == null) {
      return null;
    }
    return ResultRow.builder()
        .type(Tab.VIOLATION.name())
        .source(SearchSource.LOCAL.value())
        .id(doc.policyViolationId)
        .title(doc.policyViolationPolicyName)
        .subtitle(doc.applicationPublicId)
        .field("applicationPublicId", doc.applicationPublicId)
        .field("applicationName", doc.applicationName)
        .field("organizationId", doc.organizationId)
        .field("threatCategory", doc.policyViolationThreatCategory)
        .field("threatLevel", doc.policyViolationThreatLevel)
        .field("waiverStatus", doc.policyViolationWaiverStatus)
        .field("constraintName", doc.policyViolationConstraintName)
        .field("policyId", doc.policyViolationPolicyId)
        .build();
  }

  private static ResultRow mapWaiver(final SearchResultItemDTO doc) {
    // Waiver row shape: id/title/policyId/ownerId only. Free-form fields (reason, comment,
    // waivedBy) are intentionally omitted until length caps and frontend escaping are in place.
    //
    // Auto-waivers carry a synthetic policyName from indexing, so a null name is not expected; if it
    // is ever blank we still keep the row and fall back to the waiver id as the title rather than
    // dropping it. No Guide-outbound href is emitted: waiver rows stay within Lifecycle.
    if (doc.policyWaiverId == null) {
      return null;
    }
    final String title = doc.policyWaiverPolicyName != null && !doc.policyWaiverPolicyName.isBlank()
        ? doc.policyWaiverPolicyName
        : doc.policyWaiverId;
    return ResultRow.builder()
        .type(Tab.WAIVER.name())
        .source(SearchSource.LOCAL.value())
        .id(doc.policyWaiverId)
        .title(title)
        .field("policyId", doc.policyWaiverPolicyId)
        .field("ownerId", doc.policyWaiverScopeOwnerId)
        .build();
  }
}
