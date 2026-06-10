/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useRef, useState, useLayoutEffect } from 'react';
import { createPortal } from 'react-dom';
import { Box, TextField, Theme } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { useGlobalSearch } from 'MainRoot/nosc/search/useGlobalSearch';
import {
  ItemType,
  SearchResultItemDTO,
  reactKeyFor,
} from 'MainRoot/nosc/search/searchTypes';
import { clickHrefFor, enterSearchHref } from 'MainRoot/nosc/search/searchClickTargets';
import { activateOnKey } from 'MainRoot/nosc/keyboardActivate';
import { SearchResultRow, SearchResultRowSkeleton } from 'MainRoot/nosc/search/SearchResultRow';
import 'MainRoot/nosc/search/SearchOmnibar.css';

/**
 * P1-F13 / CLM-39549. Multi-entity global search omnibar for the Preview UI.
 *
 * Sits in the TopNav's search-slot. Debounced typeahead over IQ's existing
 * OpenSearch index (via GET /api/v2/search/advanced), surfacing 6 entity
 * types: Applications, Organizations, Components, Vulnerabilities,
 * Policies, SBOM Metadata.
 *
 * Mirrors:
 *   - Sonatype Guide's SearchWithSuggestions (debounce, keyboard nav,
 *     ARIA combobox/listbox, Enter goes to full results page)
 *   - Sonatype Repo's SearchSuggestions grouped-by-type layout (best-match
 *     on top, then per-type sections each capped to a small N)
 *   - nexusone-ux-prototype's GuideApplicationFilters/ResultCard design
 *     language (calm, polished rows; no heavy chrome)
 *
 * Behavior:
 *   - Type 2+ chars → debounced fetch → dropdown opens with grouped results
 *   - Click a row → navigate to that entity's detail page (Classic deep
 *     link or Coming Soon stub per searchClickTargets.ts)
 *   - Press Enter without selecting a row → navigate to the full
 *     /preview/search?q=... results page
 *   - Press Enter WITH a row highlighted → activate that row
 *   - Esc → clear & close
 *   - Click outside → close (state preserved for re-focus)
 *
 * The dropdown renders via a React portal into document.body so no
 * ancestor's overflow / containing-block / transform can clip it.
 */
const PLACEHOLDER = 'Search apps, components, CVEs, policies...';

/**
 * Per-type cap in the typeahead. Keeps the dropdown to ~10 rows total
 * even if there are dozens of matches per type. Mirrors Repo's
 * SearchSuggestions caps. The "Press Enter for all results" footer takes
 * the user to the full /preview/search page when they want more.
 */
const TYPEAHEAD_CAPS: Record<ItemType, number> = {
  APPLICATION: 2,
  ORGANIZATION: 2,
  NON_VULNERABLE_COMPONENT: 3,
  SECURITY_VULNERABILITY: 2,
  APPLICATION_CATEGORY: 0, // not rendered
  COMPONENT_LABEL: 0, // not rendered
  POLICY: 1,
  // SBOM_METADATA is intentionally not rendered in F13. The bucket is
  // omitted from useGlobalSearch's fanout — see ENTITY_BUCKETS for the
  // rationale. The cap is left at 0 here so any stray document that
  // somehow reaches the omnibar (e.g. from a future free-text query
  // path) is silently dropped instead of producing an empty section.
  SBOM_METADATA: 0,
};

/**
 * Display order of entity-type sections in the dropdown. SBOM_METADATA
 * is not in this list — see TYPEAHEAD_CAPS comment.
 */
const SECTION_ORDER: readonly ItemType[] = [
  'SECURITY_VULNERABILITY',
  'NON_VULNERABLE_COMPONENT',
  'APPLICATION',
  'ORGANIZATION',
  'POLICY',
];

/** Section heading label per entity type. */
const SECTION_LABEL: Record<ItemType, string> = {
  APPLICATION: 'Applications',
  ORGANIZATION: 'Organizations',
  NON_VULNERABLE_COMPONENT: 'Components',
  SECURITY_VULNERABILITY: 'Vulnerabilities',
  APPLICATION_CATEGORY: '',
  COMPONENT_LABEL: '',
  POLICY: 'Policies',
  SBOM_METADATA: '',
};

/**
 * Group results by ItemType, apply per-type cap, return a flat ordered
 * list of {section header} + {rows}. Best-match (resultIndex === 0) is
 * extracted to a "BEST MATCH" section at the top so the most relevant
 * result is always above-the-fold.
 */
interface RenderItem {
  readonly kind: 'section' | 'row';
  /** Section label (when kind === 'section'). */
  readonly label?: string;
  /** Result row (when kind === 'row'). */
  readonly result?: SearchResultItemDTO;
  /** Stable key for React. */
  readonly key: string;
}

function buildRenderItems(results: readonly SearchResultItemDTO[]): RenderItem[] {
  if (results.length === 0) return [];

  const items: RenderItem[] = [];
  let bestMatch: SearchResultItemDTO | null = null;
  if (results[0]?.resultIndex === 0) {
    bestMatch = results[0];
    items.push({ kind: 'section', label: 'Best match', key: 'sec:best' });
    items.push({ kind: 'row', result: bestMatch, key: `row:best:${reactKeyFor(bestMatch)}` });
  }

  // Group remaining by type, applying per-type cap.
  const grouped = new Map<ItemType, SearchResultItemDTO[]>();
  for (const r of results) {
    if (r === bestMatch) continue;
    const cap = TYPEAHEAD_CAPS[r.itemType] ?? 0;
    if (cap === 0) continue;
    const bucket = grouped.get(r.itemType) ?? [];
    if (bucket.length < cap) {
      bucket.push(r);
      grouped.set(r.itemType, bucket);
    }
  }

  for (const type of SECTION_ORDER) {
    const bucket = grouped.get(type);
    if (!bucket || bucket.length === 0) continue;
    items.push({
      kind: 'section',
      label: `${SECTION_LABEL[type]} (${bucket.length})`,
      key: `sec:${type}`,
    });
    for (const r of bucket) {
      items.push({ kind: 'row', result: r, key: `row:${type}:${reactKeyFor(r)}` });
    }
  }

  return items;
}

export function SearchOmnibar(): JSX.Element {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState<number>(-1);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const inputWrapperRef = useRef<HTMLDivElement | null>(null);
  const [anchorRect, setAnchorRect] = useState<DOMRect | null>(null);
  // The portal escapes the Preview shell's <Theme> tree, so the dropdown
  // would otherwise render outside any Radix Theme scope — every
  // var(--color-panel-solid), var(--shadow-4), var(--gray-N) reference
  // in SearchOmnibar.css would resolve to an empty string, producing a
  // transparent dropdown with no shadow. We wrap the portal contents in
  // a Theme of our own so CSS vars resolve correctly. The appearance
  // mirrors the TopNav toggle so light/dark mode flips with the user's
  // choice.
  const { effectiveTheme } = useNoscTheme();

  const { loading, loadError, results, totalHits } = useGlobalSearch(query, { pageSize: 12 });

  // Derive what to render (sections + rows interleaved).
  const renderItems = useMemo(() => buildRenderItems(results), [results]);
  // Indices of rows (not sections) — used for keyboard nav across the flat list.
  const rowIndices = useMemo(
    () => renderItems.map((it, i) => (it.kind === 'row' ? i : -1)).filter((i) => i >= 0),
    [renderItems],
  );

  const trimmedQuery = query.trim();
  const shouldShowDropdown = open && trimmedQuery.length >= 2;
  // While loading, show the skeleton state in the dropdown — Guide does
  // the same so the dropdown doesn't pop into view AFTER the fetch
  // resolves; it opens immediately on the second keystroke.
  const showLoading = shouldShowDropdown && loading && results.length === 0;
  const showNoResults = shouldShowDropdown && !loading && !loadError && results.length === 0;
  const showResults = shouldShowDropdown && results.length > 0;
  const showError = shouldShowDropdown && !!loadError && results.length === 0;
  const dropdownVisible = showResults || showNoResults || showError || showLoading;

  // Reset highlighted row whenever the result set changes.
  useEffect(() => {
    setActiveIndex(-1);
  }, [results, shouldShowDropdown]);

  // Compute the dropdown's screen position from the input's bounding box.
  // Rerun on resize and whenever visibility flips on so first paint is correct.
  useLayoutEffect(() => {
    if (!dropdownVisible) return;
    const update = (): void => {
      if (inputWrapperRef.current) {
        setAnchorRect(inputWrapperRef.current.getBoundingClientRect());
      }
    };
    update();
    window.addEventListener('resize', update);
    window.addEventListener('scroll', update, true);
    return () => {
      window.removeEventListener('resize', update);
      window.removeEventListener('scroll', update, true);
    };
  }, [dropdownVisible]);

  // Click outside closes the dropdown. Account for clicks on the portaled
  // dropdown by checking both the in-tree container AND the portal node.
  useEffect(() => {
    const onClick = (event: MouseEvent): void => {
      const target = event.target as Node;
      if (containerRef.current && containerRef.current.contains(target)) return;
      const portal = document.getElementById('nosc-search-omnibar-dropdown');
      if (portal && portal.contains(target)) return;
      setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const navigateToResult = (result: SearchResultItemDTO): void => {
    setQuery('');
    setOpen(false);
    window.location.assign(clickHrefFor(result));
  };

  const navigateToFullResults = (): void => {
    const href = enterSearchHref(query);
    setQuery('');
    setOpen(false);
    window.location.assign(href);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>): void => {
    if (e.key === 'Escape') {
      setQuery('');
      setOpen(false);
      setActiveIndex(-1);
      return;
    }
    if (e.key === 'Enter') {
      e.preventDefault();
      if (showResults && activeIndex >= 0 && activeIndex < rowIndices.length) {
        const item = renderItems[rowIndices[activeIndex]];
        if (item?.result) {
          navigateToResult(item.result);
          return;
        }
      }
      // Bare Enter (no row selected) → full results page.
      // Only reached when activeIndex < 0 OR the active item has no result
      // (defensive — section headers are filtered out of rowIndices).
      navigateToFullResults();
      return;
    }
    if (!showResults || rowIndices.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex((prev) => (prev < rowIndices.length - 1 ? prev + 1 : 0));
      return;
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : rowIndices.length - 1));
      return;
    }
  };

  const dropdown =
    dropdownVisible &&
    anchorRect &&
    typeof document !== 'undefined' &&
    createPortal(
      <Theme
        appearance={effectiveTheme}
        accentColor={BRAND_ACCENT}
        grayColor="slate"
        radius="medium"
        scaling="100%"
        hasBackground={false}
      >
        <div
          id="nosc-search-omnibar-dropdown"
          className="nosc-search-omnibar-dropdown"
          style={{
            position: 'fixed',
            top: anchorRect.bottom + 6,
            left: anchorRect.left,
            width: anchorRect.width,
          }}
        >
        <ul id="nosc-search-omnibar-listbox" role="listbox" aria-label="Search results">
          {showError && (
            <li>
              <div className="nosc-search-empty">
                <span className="nosc-search-empty-title">Search unavailable</span>
                <span className="nosc-search-empty-subtitle">{loadError ?? 'Try again in a moment.'}</span>
              </div>
            </li>
          )}
          {showNoResults && (
            <li>
              <div className="nosc-search-empty">
                <span className="nosc-search-empty-title">No matches</span>
                <span className="nosc-search-empty-subtitle">
                  Nothing found for &ldquo;{trimmedQuery}&rdquo;.
                </span>
              </div>
            </li>
          )}
          {showLoading &&
            [0, 1, 2, 3, 4].map((i) => (
              <li key={`skeleton-${i}`} className="nosc-search-row nosc-search-row--skeleton">
                <SearchResultRowSkeleton />
              </li>
            ))}
          {showResults &&
            renderItems.map((item) => {
              if (item.kind === 'section') {
                return (
                  <li
                    key={item.key}
                    aria-hidden="true"
                    className="nosc-search-section-eyebrow"
                  >
                    <span className="nosc-search-section-eyebrow-text">{item.label}</span>
                  </li>
                );
              }
              if (item.kind === 'row' && item.result) {
                const flatRowIndex = rowIndices.indexOf(renderItems.indexOf(item));
                const isActive = flatRowIndex === activeIndex;
                return (
                  <li
                    key={item.key}
                    id={`nosc-search-row-${flatRowIndex}`}
                    role="option"
                    aria-selected={isActive}
                    data-selected={isActive || undefined}
                    className="nosc-search-row"
                    onClick={() => navigateToResult(item.result!)}
                    onMouseEnter={() => setActiveIndex(flatRowIndex)}
                  >
                    <SearchResultRow result={item.result} />
                  </li>
                );
              }
              return null;
            })}
        </ul>
        {showResults && (
          <div
            className="nosc-search-see-all"
            role="button"
            tabIndex={0}
            onClick={navigateToFullResults}
            onKeyDown={activateOnKey(navigateToFullResults)}
            data-testid="nosc-search-see-all"
          >
            <span className="nosc-search-see-all-primary">
              See all {totalHits.toLocaleString()} results
            </span>
            <span className="nosc-search-see-all-hint">
              Press <kbd className="nosc-search-see-all-kbd">Enter</kbd> &rarr;
            </span>
          </div>
        )}
        </div>
      </Theme>,
      document.body,
    );

  return (
    <Box
      ref={containerRef}
      style={{ position: 'relative', width: '100%', maxWidth: 560 }}
      data-testid="nosc-search-omnibar"
    >
      <div ref={inputWrapperRef}>
        <TextField.Root
          placeholder={PLACEHOLDER}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          size="2"
          aria-label="Search apps, components, vulnerabilities, and policies"
          aria-autocomplete="list"
          aria-expanded={dropdownVisible}
          aria-controls="nosc-search-omnibar-listbox"
          aria-activedescendant={
            activeIndex >= 0 && activeIndex < rowIndices.length
              ? `nosc-search-row-${activeIndex}`
              : undefined
          }
          role="combobox"
        >
          <TextField.Slot>
            <ActionIcons.Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </div>
      {dropdown}
    </Box>
  );
}
