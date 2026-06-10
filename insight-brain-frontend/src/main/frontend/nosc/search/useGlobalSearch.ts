/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useState } from 'react';
import axios from 'axios';
import { getNoscGlobalSearchUrl } from 'MainRoot/util/CLMLocation';
import {
  GroupingByDTO,
  SearchResultDTO,
  SearchResultItemDTO,
  isRenderedType,
  reactKeyFor,
} from 'MainRoot/nosc/search/searchTypes';

/**
 * P1-F13 / CLM-39549: debounced multi-entity global search hook.
 *
 * Backed by IQ's existing OpenSearch index via
 * GET /api/v2/search/advanced. Uses a per-entity-bucket fanout pattern
 * rather than a single omnibus query — see ENTITY_BUCKETS below for why.
 *
 * No new backend endpoint required. The endpoint is permission-scoped on
 * the server, so we don't need any client-side authorization filtering.
 *
 * Failure modes:
 *   - Empty query → returns empty state, no fetch
 *   - Query shorter than MIN_QUERY_LENGTH → empty state, no fetch
 *   - All buckets fail (network down, 5xx everywhere) → loadError
 *     populated, results empty
 *   - Some buckets fail (e.g. SBOM_METADATA returns nothing in dev) →
 *     other buckets still render. Partial failure is silent because the
 *     user just sees fewer rows.
 *
 * Cancellation: a fresh fetch always wins. We use a `cancelled` flag
 * tied to the React effect's cleanup so a slow earlier batch can never
 * overwrite a more recent one. AbortController would be cleaner but
 * doesn't compose well with Promise.all + axios-mock-adapter in tests.
 */
const DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;

/**
 * Per-entity-type buckets we fan out across in parallel. The single
 * omnibus query approach (one giant OR across all fields) was provably
 * broken: Lucene ranks by term-frequency, so a query like "apple" returns
 * the 583 components mentioning Apple before any of the actual
 * applicationName="Apple - Java" rows ever surface. With a pageSize cap
 * of ~12 for typeahead, apps and orgs simply fall off page 1.
 *
 * Each bucket pins the search to one itemType using
 * `itemType:VALUE AND (field:*term* OR ...)` so the backend's grouping
 * logic produces clean per-entity rows. Verified live: with this pattern,
 * "apple" → an APPLICATION row "Apple - Java" appears every time.
 *
 * The fanout is in parallel so total latency is the slowest single bucket
 * (typically <100ms), not the sum.
 */
const ENTITY_BUCKETS: ReadonlyArray<{
  readonly itemType: string;
  readonly fields: readonly string[];
  /**
   * How many top results to keep from this bucket. The omnibar shows up
   * to ~12 rows total, so we keep buckets balanced — apps/orgs/policies
   * are valuable signals even when components dominate raw match counts.
   */
  readonly limit: number;
}> = [
  { itemType: 'APPLICATION', fields: ['applicationName', 'applicationPublicId'], limit: 5 },
  { itemType: 'ORGANIZATION', fields: ['organizationName'], limit: 3 },
  {
    itemType: 'SECURITY_VULNERABILITY',
    fields: ['vulnerabilityId', 'vulnerabilityDescription'],
    limit: 5,
  },
  // Components are searchable only by their GAV identifier
  // (componentName = "groupId : artifactId : version") and componentCoordinate.
  // The IQ index does NOT carry a separate component "display name" field.
  { itemType: 'NON_VULNERABLE_COMPONENT', fields: ['componentName', 'componentCoordinate'], limit: 5 },
  { itemType: 'POLICY', fields: ['policyName'], limit: 3 },
  // NOTE: SBOM_METADATA is intentionally omitted from the omnibar in F13.
  // The indexed fields are limited to sbomSpecification ("CycloneDX 1.6",
  // "SPDX 2.3") + sbomVersion — neither is a useful free-text typeahead
  // signal. The bucket also covers ONLY third-party SBOMs uploaded via
  // SBOM Manager (ThirdPartySbomMetadataDAO); SBOMs IQ generates from
  // scans aren't indexed at all. Components and CVEs *inside* third-party
  // SBOMs already surface as regular component / vulnerability rows
  // because the indexer emits them with their normal ItemType. If a real
  // SBOM-Manager-specific search use case emerges (e.g. "find SBOMs from
  // supplier X"), it deserves a Phase 2 backend story with proper fields.
];

/**
 * Single-character Lucene reserved chars that must be escaped in user input.
 * Per Lucene QueryParser the full set is: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
 *
 * NB: a lone `&`, `|`, or `/` is a literal in Lucene — only the `&&` and `||`
 * boolean OPERATORS are special. They are handled separately (below) so we don't
 * mangle legitimate queries like "Q&A" or "org/apache/log4j".
 */
const LUCENE_SPECIAL_CHARS_RE = /[+\-!(){}[\]^"~*?:\\]/g;

function escapeLuceneTerm(input: string): string {
  // Escape the single-char specials first (this also escapes any backslash), then
  // neutralize the && / || operators. Order matters: doing the char-class pass last
  // would re-escape the backslashes we insert for && / ||.
  return input
    .replace(LUCENE_SPECIAL_CHARS_RE, (ch) => `\\${ch}`)
    .replace(/&&/g, '\\&\\&')
    .replace(/\|\|/g, '\\|\\|');
}

/**
 * Build a per-bucket query that pins itemType and matches the user's
 * input against that bucket's name/identifier fields.
 * Example: bucket=APPLICATION, input="log4j" →
 *   "(itemType:APPLICATION AND (applicationName:*log4j* OR applicationPublicId:*log4j*))"
 */
export function buildBucketQuery(itemType: string, fields: readonly string[], userInput: string): string {
  const trimmed = userInput.trim();
  if (!trimmed) return '';
  const safe = escapeLuceneTerm(trimmed);
  const fieldClauses = fields.map((f) => `${f}:*${safe}*`).join(' OR ');
  return `(itemType:${itemType} AND (${fieldClauses}))`;
}

/**
 * Build a single multi-field disjunction (legacy fallback used by the
 * full results page where balanced bucketing matters less). Kept as an
 * exported helper so tests can lock the wrapping shape independently.
 */
export function buildBackendQuery(userInput: string): string {
  const trimmed = userInput.trim();
  if (!trimmed) return '';
  const safe = escapeLuceneTerm(trimmed);
  const allFields = ENTITY_BUCKETS.flatMap((b) => b.fields);
  const clauses = Array.from(new Set(allFields)).map((f) => `${f}:*${safe}*`);
  return `(${clauses.join(' OR ')})`;
}

/**
 * Flatten the backend's grouped response into a deduped ordered list,
 * optionally capped at `limit` rendered items.
 */
function flattenGroups(
  groups: readonly GroupingByDTO[] | undefined,
  topLevelItems: readonly SearchResultItemDTO[] | undefined,
  seen: Set<string>,
  limit: number,
): SearchResultItemDTO[] {
  const out: SearchResultItemDTO[] = [];
  const consume = (items: readonly SearchResultItemDTO[] | undefined): void => {
    if (!items) return;
    for (const item of items) {
      if (out.length >= limit) return;
      if (!isRenderedType(item)) continue;
      const key = reactKeyFor(item);
      if (seen.has(key)) continue;
      seen.add(key);
      out.push(item);
    }
  };
  for (const g of groups ?? []) {
    if (out.length >= limit) break;
    consume(g.searchResultItemDTOS);
  }
  if (out.length < limit) consume(topLevelItems);
  return out;
}

export interface UseGlobalSearchOptions {
  /**
   * Maximum number of merged results to return. Defaults to 10 (typeahead);
   * the full results page passes a larger value (e.g. 50). This caps the final
   * de-duped, merged list — per-bucket backend limits still apply first.
   *
   * NB: backend page-offset pagination is intentionally NOT wired up. The
   * omnibar shows a fixed top-N and the full results page uses a "load more"
   * (Phase 1.5) follow-up, so a `page` option would be dead API surface today.
   */
  readonly pageSize?: number;
}

export interface GlobalSearchState {
  readonly loading: boolean;
  readonly loadError: string | null;
  readonly results: readonly SearchResultItemDTO[];
  readonly totalHits: number;
  readonly isExactTotal: boolean;
}

const EMPTY_STATE: GlobalSearchState = {
  loading: false,
  loadError: null,
  results: [],
  totalHits: 0,
  isExactTotal: true,
};

export function useGlobalSearch(query: string, opts?: UseGlobalSearchOptions): GlobalSearchState {
  const trimmed = query.trim();
  const pageSize = opts?.pageSize ?? 10;
  const [state, setState] = useState<GlobalSearchState>(EMPTY_STATE);

  useEffect(() => {
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setState(EMPTY_STATE);
      return;
    }

    // We use a `cancelled` flag (not AbortController.signal) for two reasons:
    //   1. axios-mock-adapter resolves promises synchronously, but React's
    //      effect cleanup also runs synchronously. If the cleanup aborts the
    //      controller before the .then microtask runs, the response is
    //      dropped even when no newer fetch is in flight. The flag avoids
    //      that race because we set it in cleanup and check it in .then.
    //   2. Unit tests are simpler — no need to mock AbortController behavior.
    let cancelled = false;
    const handle = setTimeout(() => {
      // Clear the prior query's results when the new fetch begins so stale suggestions are not
      // shown (or clickable) under the loading state — the skeleton renders while results is empty.
      setState((prev) => ({ ...prev, loading: true, loadError: null, results: [], totalHits: 0 }));

      // Fan out one request per entity bucket in parallel. The single
      // omnibus query was provably broken — Lucene ranking lets one
      // dominant entity type starve all others off page 1 (e.g. "apple"
      // returns 583 component-context hits before the actual
      // applicationName="Apple - Java" row ever surfaces). Per-bucket
      // queries pin itemType so each entity type gets its own slot.
      const bucketRequests = ENTITY_BUCKETS.map((bucket) => {
        const q = buildBucketQuery(bucket.itemType, bucket.fields, trimmed);
        // pageSize matches the bucket's display limit so the backend
        // doesn't waste work returning rows we'd discard anyway.
        return axios
          .get<SearchResultDTO>(getNoscGlobalSearchUrl(q, 0, bucket.limit))
          .then((response) => ({ bucket, data: response.data, error: null as unknown }))
          .catch((error: unknown) => ({ bucket, data: null as SearchResultDTO | null, error }));
      });

      Promise.all(bucketRequests).then((bucketResults) => {
        if (cancelled) return;

        const seen = new Set<string>();
        const merged: SearchResultItemDTO[] = [];
        let totalHits = 0;
        // We only treat a search as failed if EVERY bucket failed.
        // Partial failure (e.g. SBOM_METADATA index empty in dev) still
        // shows the buckets that worked.
        let firstError: unknown = null;
        let successCount = 0;

        for (const { bucket, data, error } of bucketResults) {
          if (error) {
            if (firstError === null) firstError = error;
            continue;
          }
          successCount += 1;
          if (!data) continue;
          totalHits += typeof data.totalNumberOfHits === 'number' ? data.totalNumberOfHits : 0;
          const items = flattenGroups(
            data.groupingByDTOS,
            data.searchResultItemDTOS,
            seen,
            bucket.limit,
          );
          merged.push(...items);
        }

        if (successCount === 0 && firstError !== null) {
          const message = firstError instanceof Error ? firstError.message : 'Search failed';
          setState({
            loading: false,
            loadError: message,
            results: [],
            totalHits: 0,
            isExactTotal: true,
          });
          return;
        }

        setState({
          loading: false,
          loadError: null,
          // Cap the merged list to the caller's requested pageSize (omnibar top-N,
          // results page page-size) so the option is actually honored.
          results: merged.slice(0, pageSize),
          totalHits,
          // totalHits is the sum of each bucket's independent count, and cross-bucket
          // de-duplication can drop overlaps, so it is an approximation — not an exact
          // total. The omnibar renders it as "Showing N results", so mark it non-exact.
          isExactTotal: false,
        });
      })
      .catch((err: unknown) => {
        // Guard the .then() callback: if it throws (e.g. during setState or flattenGroups),
        // without this the state would be stuck at loading:true forever (eternal spinner).
        if (cancelled) return;
        setState({
          loading: false,
          loadError: err instanceof Error ? err.message : 'Search failed',
          results: [],
          totalHits: 0,
          isExactTotal: true,
        });
      });
    }, DEBOUNCE_MS);

    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [trimmed, pageSize]);

  return state;
}
