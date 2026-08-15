/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Facet-rail → query-string round-trip for the /search results page.
 *
 * The backend /rest/search/results endpoint returns per-tab facet buckets keyed
 * by a facet key (e.g. `organizations`, `states`, `policyTypes`). Selecting a
 * bucket must narrow the SAME free-text query the rail was computed from, so the
 * rail rewrites the `q=` string by appending / removing a `field:value`
 * predicate and re-navigating. There is no separate structured-filter channel:
 * the query grammar IS the filter channel (mirrors the omnibar FilterBar).
 *
 * A facet key does not always equal the query-grammar field it filters on:
 *   - VALUE facets round-trip as `<field>:"<bucketValue>"` where field is the
 *     grammar field name. For the id-carrying entity facets
 *     (organizations/applications/applicationCategories/policy), `value` is
 *     the entity's opaque id and `displayName` is the resolved name (rendering
 *     already handles this generically — see {@link ../search/searchTypes}'s
 *     doc comment and SearchResultsFilters' `bucketLabel`), and each
 *     descriptor's `field` is that entity's id grammar field, which is also the
 *     index field the facet aggregates on: organizations →
 *     `parentOrganizationId`, applications → `applicationId`,
 *     applicationCategories → `applicationCategoryId`, policy →
 *     `policyWaiverPolicyId`. Each of these is a known FieldMap grammar entry, so
 *     the appended predicate is parsed by the same QueryParser + FieldMap pipeline
 *     the omnibar uses (see ResultsFacetQueryBridge / IqLocalSearchService) without
 *     any field-name translation. Note these are not all FILTER_TREE leaves:
 *     FILTER_TREE offers `organizationId` and `policyId`, whereas a facet has to
 *     use the field its aggregation groups on — `parentOrganizationId` for the
 *     hierarchical org closure, `policyWaiverPolicyId` for a waiver's policy — or
 *     own-clause removal would not find the selection and the rail would collapse
 *     to it.
 *   - Fixed-vocabulary facets (states / waiverType / status) have bucket values
 *     that are lifecycle keys (OPEN / WAIVED / AUTO / ACTIVE …), not raw field
 *     values, so each key maps to a specific grammar predicate.
 *   - NUMERIC facets (threatLevel) filter with a range `field:[lo TO hi]` and are
 *     driven by the rail's slider rather than by buckets, using the active tab's
 *     threat-level field (see THREAT_LEVEL_FIELD_BY_TAB).
 *
 * The predicate a checkbox toggles and the whole-token match used to detect the
 * "already selected" state both come from {@link facetPredicate}, so add + remove
 * always agree on the exact token.
 */

import { FILTER_TREE } from 'MainRoot/nosc/search/searchFilterTree';

/** How a facet key's buckets translate into a query predicate. */
export type FacetKind = 'value' | 'fixed';

export interface FacetDescriptor {
  /** Human section label matching the prototype rail (e.g. "Violation State"). */
  readonly label: string;
  /**
   * `value`  — bucket value round-trips as `<field>:"<value>"`.
   * `fixed`  — bucket value is a lifecycle key mapped via {@link fixedTokens}.
   */
  readonly kind: FacetKind;
  /** Grammar field for `value` kind (unused for `fixed`). */
  readonly field?: string;
  /** Lifecycle-key → full predicate token for `fixed` kind. */
  readonly fixedTokens?: Readonly<Record<string, string>>;
}

/**
 * Per-facet-key descriptors. Keys mirror the backend FACET_FIELDS map; the fixed
 * tokens mirror the grammar's accepted values for the corresponding lifecycle
 * filter. For every VALUE facet, `field` is the grammar field that matches the
 * index field the backend aggregates on — for the id-carrying entity facets
 * (organizations/applications/applicationCategories/policy) that field
 * carries the entity's id (see the module doc comment above); for every other
 * VALUE facet it carries the bucket value the backend emits directly
 * (denormalized keyword sets, not ids).
 *
 * Shared across tabs by key: `organizations`/`applications`/`applicationCategories`/
 * `policyTypes`/`stages` appear on multiple tabs with the same round-trip, so one
 * descriptor per key covers every tab that emits it.
 */
export const FACET_DESCRIPTORS: Readonly<Record<string, FacetDescriptor>> = {
  states: {
    label: 'Violation State',
    kind: 'fixed',
    fixedTokens: {
      OPEN: 'policyViolationWaiverStatus:Active',
      WAIVED: 'policyViolationWaiverStatus:Waived',
    },
  },
  // MANUAL carries no token. The grammar's waiver-status vocabulary distinguishes only AutoWaived
  // from Waived, so a "manual waiver" predicate would be the very same
  // `policyViolationWaiverStatus:Waived` that states.WAIVED emits: ticking either control would show
  // both as checked and unticking either would clear both. AUTO has its own value and stays
  // filterable, so this section renders as counts with only the Auto bucket toggleable.
  //
  // TODO(CLM-42453): make Manual filterable by adding a value to the grammar's waiver-status
  // vocabulary that names a manually-created waiver distinctly from the generic Waived state.
  waiverType: {
    label: 'Waiver Type',
    kind: 'fixed',
    fixedTokens: {
      AUTO: 'policyViolationWaiverStatus:AutoWaived',
    },
  },
  // `status` is emitted only on the WAIVER tab (IndexQueryService.FACET_FIELDS) and its bucket values
  // are the lowercase keys the backend counts under.
  //
  // No bucket carries a predicate, so the section renders as read-only counts. Each bucket is counted
  // against a base that strips the caller's own expires-at narrowing, which keeps the buckets
  // independent of each other; the only fields expressible from here (the denormalized expiry-status
  // keyword and the auto discriminator) are not stripped from that base, so selecting one would
  // collapse its siblings' counts. `active` and `expiring` additionally depend on a server-relative
  // "now" that a client-side predicate cannot pin.
  //
  // TODO(CLM-42453): make Status filterable by having the count base drop expiry-status / auto
  // clauses as well, then map the buckets to those fields.
  status: {
    label: 'Status',
    kind: 'fixed',
    fixedTokens: {},
  },
  // Every tab emits its owner facets under `organizations` / `applications`, keyed by the entity's opaque
  // id (see the module doc comment above).
  organizations: { label: 'Organizations', kind: 'value', field: 'parentOrganizationId' },
  applications: { label: 'Applications', kind: 'value', field: 'applicationId' },
  applicationCategories: { label: 'Categories', kind: 'value', field: 'applicationCategoryId' },
  stages: { label: 'Stages', kind: 'value', field: 'policyEvaluationStage' },
  policyTypes: { label: 'Policy Types', kind: 'value', field: 'policyViolationThreatCategory' },
  policyType: { label: 'Policy Type', kind: 'value', field: 'policyWaiverPolicyType' },
  scope: { label: 'Scope', kind: 'value', field: 'policyWaiverScope' },
  auto: { label: 'Auto', kind: 'value', field: 'policyWaiverAuto' },
  // The WAIVER tab's policy facet. Bucket value is the policy id, and policyWaiverPolicyId is the grammar
  // field it aggregates on -- distinct from `policyId`, which is a Policy-tab field, not a waiver's policy.
  policy: { label: 'Policy Name', kind: 'value', field: 'policyWaiverPolicyId' },
};

/**
 * Facet keys rendered as a 0–10 range slider rather than checkbox buckets. A key
 * listed here has no FACET_DESCRIPTORS entry: the slider owns the round-trip via
 * THREAT_LEVEL_FIELD_BY_TAB, and rendering buckets for it too would repeat the
 * same "Policy Threat Level" heading on one rail.
 */
export const THREAT_LEVEL_FACET_KEYS: readonly string[] = ['threatLevel'];

/**
 * The grammar field the threat-level range slider writes to, per tab. Each tab
 * indexes the threat level under its own field and the compiler scopes a field to
 * its allowed entity types — querying the wrong one compiles to a match-nothing
 * clause with no warning, silently zeroing every result. A tab absent from this
 * map has no threat-level field, so the slider is not offered there.
 */
export const THREAT_LEVEL_FIELD_BY_TAB: Readonly<Record<string, string>> = {
  APPLICATION: 'applicationMaxPolicyThreatLevel',
  VIOLATION: 'policyViolationThreatLevel',
  COMPONENT: 'componentMaxPolicyThreatLevel',
  WAIVER: 'policyWaiverThreatLevel',
};

/** True when the given tab offers the threat-level slider. */
export function hasThreatLevelField(tab: string): boolean {
  return tab in THREAT_LEVEL_FIELD_BY_TAB;
}

/** The threat-level grammar field for a tab, or null when the tab has none. */
export function threatLevelFieldForTab(tab: string): string | null {
  return THREAT_LEVEL_FIELD_BY_TAB[tab] ?? null;
}

/** Section label for the threat-level slider (not a bucket-backed section). */
export const THREAT_LEVEL_LABEL = 'Policy Threat Level';

/** Minimum / maximum of the threat-level slider range. */
export const THREAT_LEVEL_MIN = 0;
export const THREAT_LEVEL_MAX = 10;

/**
 * Wrap a value in double quotes when it contains whitespace or a quote, matching the omnibar grammar's
 * quoting so the value stays a single predicate. Owner bucket values are opaque ids and so are normally
 * left bare, like the FilterBar leaves; the quoting matters for fixed-vocabulary values that carry spaces
 * and as a guard for any id shape that would otherwise be split.
 */
export function quoteValue(value: string): string {
  // Brackets are range syntax in the grammar, so a bucket value containing one
  // must be quoted too or it would be parsed as a range rather than a literal.
  //
  // Backslashes and quotes are passed through as-is rather than escaped: the grammar's
  // quote reader consumes characters up to the first `"` with no escape handling at all,
  // so neither a doubled backslash nor a `\"` sequence round-trips. A value carrying
  // either is not expressible at all — see isQuotableValue, which keeps such a bucket
  // read-only rather than emitting a predicate that cannot match.
  if (/[\s":[\]]/.test(value)) {
    return `"${value.replace(/"/g, '\\"')}"`;
  }
  return value;
}

/**
 * True when a facet bucket value can be expressed as a grammar predicate.
 *
 * The grammar's quote reader has no escape handling: it consumes raw characters up
 * to the first `"`. A value containing a backslash or a double quote therefore cannot
 * round-trip — the reader ends the quoted span at that `"` (either the escaped one or
 * the value's own), so the predicate written into the query stops matching the token
 * the round-trip lookup searches for, the checkbox reads unchecked immediately after
 * being ticked, and a second click appends a duplicate while the trailing fragment is
 * parsed as unrelated free text. Org and application names are user-controlled, so
 * both cases are reachable.
 */
export function isQuotableValue(value: string): boolean {
  return !value.includes('\\') && !value.includes('"');
}

/**
 * The exact `field:value` (or fixed) predicate token a bucket contributes. Used
 * both to append the token and to detect whether it is already present, so the
 * two never disagree. Returns null when a facet key / bucket value has no known
 * round-trip (rendered read-only rather than toggling a broken filter).
 */
export function facetPredicate(facetKey: string, bucketValue: string): string | null {
  const descriptor = FACET_DESCRIPTORS[facetKey];
  if (!descriptor) return null;
  if (descriptor.kind === 'fixed') {
    return descriptor.fixedTokens?.[bucketValue] ?? null;
  }
  if (!descriptor.field) return null;
  if (!isQuotableValue(bucketValue)) return null;
  return `${descriptor.field}:${quoteValue(bucketValue)}`;
}

/** Range predicate for the threat-level slider on `tab`, e.g. `field:[lo TO hi]`. */
export function threatLevelPredicate(tab: string, lo: number, hi: number): string | null {
  const field = threatLevelFieldForTab(tab);
  return field ? `${field}:[${lo} TO ${hi}]` : null;
}

/**
 * Regex matching any threat-level range predicate for `tab`, so a re-drag replaces
 * the old one instead of stacking a second range.
 */
function threatLevelRangeRe(field: string): RegExp {
  return new RegExp(`^${field}:\\[\\s*(\\d+)\\s+TO\\s+(\\d+)\\s*\\]$`);
}

/** True when a whole token is a threat-level range predicate for `field`. */
function isThreatLevelRangeToken(token: string, field: string): boolean {
  return threatLevelRangeRe(field).test(token);
}

/**
 * Split a query into whitespace-delimited tokens, keeping these as ONE token:
 *   - a `field:"quoted value"` predicate (a quoted value with spaces must not
 *     split, or the round-trip match would break);
 *   - a bracketed range span such as `age:[* TO 7]`, whose ` TO ` separator is
 *     unquoted whitespace — without this it splits into three tokens and no
 *     round-trip lookup can ever match the predicate it was built from;
 *   - a parenthesised group such as `(field:a OR field:b)`, for the same reason.
 * A token otherwise runs from a non-whitespace start through any number of quoted
 * spans and bare runs until the next unquoted whitespace.
 *
 * Bracket and paren spans are flat, not nested: the grammar only ever emits
 * well-formed ranges, so a hand-typed malformed one like `field:[1 TO [5]` splits
 * into bare tokens instead of matching as a range.
 */
export function tokenize(query: string): string[] {
  const tokens: string[] = [];
  const re = /(?:"(?:[^"\\]|\\.)*"|\[[^[\]]*\]|\([^()]*\)|[^\s"[\]()]+)+/g;
  let match: RegExpExecArray | null;
  while ((match = re.exec(query)) !== null) {
    tokens.push(match[0]);
  }
  return tokens;
}

/**
 * Grammar fields a predicate token may name. Derived from the filter tree the
 * omnibar offers, the facet descriptors' own fields and fixed tokens, and the
 * per-tab threat-level fields, so it tracks the real vocabulary instead of a
 * hand-maintained copy.
 *
 * Matching on a shape like `word:` instead would misread ordinary free text that
 * happens to contain a colon: a Maven coordinate such as
 * `org.apache.logging.log4j:log4j-core` is a search term, not a filter, and
 * treating it as one lets a reset delete the user's query.
 */
const KNOWN_PREDICATE_FIELDS: ReadonlySet<string> = (() => {
  const fields = new Set<string>();
  const addFromToken = (token: string): void => {
    const colon = token.indexOf(':');
    if (colon > 0) fields.add(token.slice(0, colon));
  };
  for (const node of FILTER_TREE) {
    for (const leaf of node.leaves ?? []) addFromToken(leaf.syntax);
    for (const group of node.groups ?? []) {
      for (const leaf of group.leaves) addFromToken(leaf.syntax);
    }
  }
  for (const descriptor of Object.values(FACET_DESCRIPTORS)) {
    if (descriptor.field) fields.add(descriptor.field);
    for (const token of Object.values(descriptor.fixedTokens ?? {})) addFromToken(token);
  }
  for (const field of Object.values(THREAT_LEVEL_FIELD_BY_TAB)) fields.add(field);
  return fields;
})();

/**
 * True when a token names a known grammar field, i.e. it is a `field:value` /
 * `field:[range]` predicate rather than free text that merely contains a colon.
 */
export function isPredicateToken(token: string): boolean {
  // A parenthesised group is a predicate when its first field is a known one; the
  // rail only ever emits such a group as a single facet's OR-expansion.
  const inner = token.startsWith('(') && token.endsWith(')') ? token.slice(1, -1) : token;
  const colon = inner.indexOf(':');
  return colon > 0 && KNOWN_PREDICATE_FIELDS.has(inner.slice(0, colon));
}

/**
 * Strip every structured predicate from a query, keeping the free-text terms.
 * Tokenizing first is what keeps quoted and bracketed predicates whole: splitting
 * on raw whitespace would shatter `parentOrganizationId:"legacy org id"` and
 * `policyViolationThreatLevel:[3 TO 7]`, leaving fragments behind as free text.
 */
export function stripPredicates(query: string): string {
  return tokenize(query)
    .filter((t) => !isPredicateToken(t))
    .join(' ')
    .trim();
}

/** True when `query` already contains the exact predicate token. */
export function hasPredicate(query: string, predicate: string): boolean {
  return tokenize(query).includes(predicate);
}

/** Append `predicate` to `query` with single-space separation (no-op if present). */
export function addPredicate(query: string, predicate: string): string {
  if (hasPredicate(query, predicate)) return query;
  const trimmed = query.trimEnd();
  return trimmed.length > 0 ? `${trimmed} ${predicate}` : predicate;
}

/** Remove every exact occurrence of `predicate` from `query`. */
export function removePredicate(query: string, predicate: string): string {
  return tokenize(query)
    .filter((t) => t !== predicate)
    .join(' ')
    .trim();
}

/** Toggle `predicate` in `query`: add when absent, remove when present. */
export function togglePredicate(query: string, predicate: string): string {
  return hasPredicate(query, predicate) ? removePredicate(query, predicate) : addPredicate(query, predicate);
}

/**
 * Replace any existing threat-level range for `tab` with the given [lo, hi].
 * Dragging the slider back to the full 0–10 span removes the predicate entirely
 * (no filter). A tab without a threat-level field leaves the query untouched.
 */
export function setThreatLevelRange(query: string, tab: string, lo: number, hi: number): string {
  const field = threatLevelFieldForTab(tab);
  if (!field) return query;
  // Cleared token-wise rather than by a raw regex replace over the whole string: a
  // quoted value that happens to contain the field's range syntax must not be
  // rewritten from inside its quotes.
  const cleared = tokenize(query)
    .filter((t) => !isThreatLevelRangeToken(t, field))
    .join(' ')
    .trim();
  if (lo <= THREAT_LEVEL_MIN && hi >= THREAT_LEVEL_MAX) {
    return cleared;
  }
  const predicate = threatLevelPredicate(tab, lo, hi);
  return predicate ? addPredicate(cleared, predicate) : cleared;
}

/**
 * Read the current threat-level range for `tab` from a query, defaulting to the
 * full span when the tab has no range predicate (or no threat-level field).
 */
export function readThreatLevelRange(query: string, tab: string): [number, number] {
  const field = threatLevelFieldForTab(tab);
  if (!field) return [THREAT_LEVEL_MIN, THREAT_LEVEL_MAX];
  // Matched per token so a quoted value containing the range syntax is not read as
  // the current selection.
  const re = threatLevelRangeRe(field);
  for (const token of tokenize(query)) {
    const match = re.exec(token);
    if (match) return [parseInt(match[1], 10), parseInt(match[2], 10)];
  }
  return [THREAT_LEVEL_MIN, THREAT_LEVEL_MAX];
}

/** Order facet keys within a tab to match the prototype rail top-to-bottom. */
const FACET_ORDER: readonly string[] = [
  'states',
  'waiverType',
  'status',
  'scope',
  'policyTypes',
  'policyType',
  'stages',
  'organizations',
  'applications',
  'applicationCategories',
  'policy',
  'auto',
];

/** Sort the facet keys present in a response into the prototype's rail order. */
export function orderedFacetKeys(keys: readonly string[]): string[] {
  return [...keys].sort((a, b) => {
    const ia = FACET_ORDER.indexOf(a);
    const ib = FACET_ORDER.indexOf(b);
    return (ia === -1 ? Number.MAX_SAFE_INTEGER : ia) - (ib === -1 ? Number.MAX_SAFE_INTEGER : ib);
  });
}
