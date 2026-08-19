/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Flex, IconButton, Select, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { NOTICE_STRIP_HEIGHT_CSS_VAR, TOP_NAV_HEIGHT_PX } from 'MainRoot/nosc/shell/previewShellLayout';
import { useGlobalSearch } from 'MainRoot/nosc/search/useGlobalSearch';
import { isSearchEntityType, SearchEntityType, SearchRow, SearchSource } from 'MainRoot/nosc/search/searchTypes';
import {
  DEFAULT_SEARCH_SOURCE,
  SEARCH_SOURCE_LABEL,
  isTypeVisibleForSource,
} from 'MainRoot/nosc/search/searchDataSource';
import { clickHrefFor, searchResultsStateParams } from 'MainRoot/nosc/search/searchClickTargets';
import { FilterBar, FilterInsertRequest } from 'MainRoot/nosc/search/FilterBar';
import { computeFilterInsert, useFocusInputWithCaret } from 'MainRoot/nosc/search/searchFilterInsert';
import router from 'MainRoot/router/routerInstance';
import { CatalogScopeHint, PanelFooter } from 'MainRoot/nosc/search/SearchPanelParts';
import {
  LEAD_LISTBOX_ID,
  PLACEHOLDER_OPTION_ID,
  PlaceholderView,
  RecentSearchesView,
  ResultsView,
  ROWS_LISTBOX_ID,
  SHOW_RESULTS_OPTION_ID,
  VIEW_MORE_OPTION_ID,
  recentOptionId,
  rowOptionId,
} from 'MainRoot/nosc/search/SearchPanelViews';
import {
  ALL_TAB_ID,
  SearchPanelState,
  buildPanelTabs,
  derivePanelState,
  flattenSuggestRows,
  itemTypeTokens,
  selectTabRows,
} from 'MainRoot/nosc/search/searchPanelModel';
import { useRecentSearches } from 'MainRoot/nosc/search/useRecentSearches';
import 'MainRoot/nosc/search/SearchOmnibar.css';

/**
 * Global search omnibar for the Preview (Nexus One) UI.
 *
 * Structure follows the Nexus One prototype's expand-in-place model rather than
 * an anchored dropdown:
 *
 *   - CLOSED, the omnibar is the search field alone — magnifier, placeholder,
 *     and the "/" + Cmd-K shortcut chips. No data-source select, no filter
 *     toggle.
 *   - FOCUSED, the same surface expands into a floating card centered on the
 *     field. The card's header row carries the data-source select, the input,
 *     and the filter toggle; the body below renders one of the panel views.
 *
 * Which view the body shows is decided solely by derivePanelState (see
 * searchPanelModel): recent searches while the query is empty or too short, a
 * placeholder while fetching or when nothing matched, and the results view with
 * a horizontal tab strip plus a mixed relevance-ranked row list once loaded.
 *
 * Keyboard follows the prototype's pan-panel model: arrows drive the listbox
 * composite from anywhere inside the card (the input is the only Tab stop, via
 * aria-activedescendant), Enter activates the highlighted option only when focus
 * is on the input, and Tab moves in natural document order.
 */
const PLACEHOLDER = 'Search applications, components, violations, and vulnerabilities';

/** Search-syntax docs target for the panel footer. */
const SYNTAX_DOCS_URL = 'https://links.sonatype.com/products/nxiq/doc/advanced-search';

/**
 * Tallest the expanded card may grow. Measured from the viewport's dynamic height
 * minus the fixed top nav the card sits under, the live notice strip height
 * (`NOTICE_STRIP_HEIGHT_CSS_VAR` — 0 when no notice is visible, resolved by
 * the browser at layout time so this needs no React state), and a bottom
 * gutter, so a short viewport shrinks the card instead of letting it run
 * under the nav or a visible notice. `dvh` rather than `vh` so mobile browser
 * chrome is accounted for.
 */
const PANEL_MAX_HEIGHT = `min(640px, calc(100dvh - ${
  TOP_NAV_HEIGHT_PX + 24
}px - var(${NOTICE_STRIP_HEIGHT_CSS_VAR}, 0) * 1px))`;

/**
 * Shortcut chip styling. Inline rather than in CSS because the chips must resolve
 * Radix theme vars inside the field's own slot, matching the prototype exactly.
 */
const SHORTCUT_CHIP_STYLE: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  minWidth: '1.25rem',
  padding: '0 0.35rem',
  height: '1.25rem',
  fontSize: 11,
  fontFamily: 'var(--code-font-family)',
  lineHeight: 1,
  color: 'var(--gray-11)',
  background: 'var(--gray-3)',
  border: '1px solid var(--gray-6)',
  borderRadius: 'var(--radius-1)',
  boxShadow: '0 1px 0 var(--gray-5)',
};

/** One entry in the panel's keyboard-navigation composite. */
type MenuItem =
  | { readonly kind: 'show-all'; readonly id: string }
  | { readonly kind: 'placeholder'; readonly id: string }
  | { readonly kind: 'row'; readonly id: string; readonly rowIndex: number }
  | { readonly kind: 'view-more'; readonly id: string }
  | { readonly kind: 'recent'; readonly id: string; readonly recentIndex: number };

export function SearchOmnibar(): JSX.Element {
  const [query, setQuery] = useState('');
  const [source, setSource] = useState<SearchSource>(DEFAULT_SEARCH_SOURCE);
  const [panelOpen, setPanelOpen] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [activeTabId, setActiveTabId] = useState<string>(ALL_TAB_ID);
  const [highlight, setHighlight] = useState(0);

  const containerRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  /**
   * Set while Escape is closing the panel. Escape returns focus to the input,
   * and the input's focus handler would otherwise reopen the panel immediately,
   * so a single Escape would appear to do nothing.
   */
  const suppressReopenRef = useRef(false);

  const { entries: recentEntries, record: recordRecent } = useRecentSearches();

  // No warnings destructured: /rest/search/suggest carries none, so the panel has
  // nothing to surface. The full results page renders warnings from /rest/search/results.
  const { loading, loadError, bestMatch, groups } = useGlobalSearch(query, {
    mode: 'typeahead',
    source,
  });

  const trimmedQuery = query.trim();

  const rows = useMemo(() => flattenSuggestRows(bestMatch, groups, source), [bestMatch, groups, source]);

  const panelState: SearchPanelState = derivePanelState({
    panelOpen,
    trimmedQuery,
    loading,
    rowCount: rows.length,
  });

  // Counts come from the suggest groups, which are capped per type, so they are a
  // lower bound rather than a true total. They still drive the badges and the
  // "more results exist" decision, both of which only need "at least this many".
  const countsByType = useMemo(() => {
    const counts: Partial<Record<SearchEntityType, number>> = {};
    for (const group of groups) {
      if (!isTypeVisibleForSource(group.type, source)) continue;
      counts[group.type] = group.rows.length;
    }
    return counts;
  }, [groups, source]);

  const tabs = useMemo(() => buildPanelTabs(source, countsByType, rows.length), [source, countsByType, rows.length]);

  // A query carrying itemType: tokens has already narrowed by type, so the strip
  // is hidden; a single token also selects its tab.
  const tokens = useMemo(() => itemTypeTokens(query), [query]);
  // A token naming a type the active source cannot serve (itemType:APPLICATION
  // against the catalog) has no tab to select, so it is dropped: the tab falls back
  // to All and the strip stays visible rather than hiding it to advertise a
  // narrowing that was not applied.
  const servableTokens = useMemo(() => tokens.filter((token) => isTypeVisibleForSource(token, source)), [
    tokens,
    source,
  ]);
  const hideTabs = servableTokens.length > 0;
  const effectiveActiveTab = useMemo(() => {
    if (servableTokens.length === 1) return servableTokens[0];
    if (servableTokens.length > 1) return ALL_TAB_ID;
    return activeTabId;
  }, [servableTokens, activeTabId]);

  // Drop back to All whenever the active tab is not offered by the current data
  // source (switching to catalog hides the IQ-local tabs).
  useEffect(() => {
    if (!tabs.some((tab) => tab.id === activeTabId)) setActiveTabId(ALL_TAB_ID);
  }, [tabs, activeTabId]);

  const visibleRows = useMemo(() => (panelState === 'loaded' ? selectTabRows(rows, effectiveActiveTab, tabs) : []), [
    panelState,
    rows,
    effectiveActiveTab,
    tabs,
  ]);

  const activeTabCount = isSearchEntityType(effectiveActiveTab) ? countsByType[effectiveActiveTab] : rows.length;
  const showViewMore = (activeTabCount ?? visibleRows.length) > visibleRows.length;

  // -------------------------------------------------------------------------
  // Keyboard composite: the single source of truth for what arrows traverse.
  // -------------------------------------------------------------------------
  const menuItems = useMemo<MenuItem[]>(() => {
    // Exhaustive over SearchPanelState so adding a state without giving it a
    // composite becomes a compile error rather than a silently unnavigable panel.
    switch (panelState) {
      case 'closed':
        return [];
      case 'focused-empty':
      case 'focused-short':
        return recentEntries.map((_, index) => ({
          kind: 'recent' as const,
          id: recentOptionId(index),
          recentIndex: index,
        }));
      case 'loading':
      case 'loaded-empty':
        return [{ kind: 'placeholder' as const, id: PLACEHOLDER_OPTION_ID }];
      case 'loaded': {
        const items: MenuItem[] = [{ kind: 'show-all', id: SHOW_RESULTS_OPTION_ID }];
        visibleRows.forEach((_, index) => items.push({ kind: 'row', id: rowOptionId(index), rowIndex: index }));
        if (showViewMore) items.push({ kind: 'view-more', id: VIEW_MORE_OPTION_ID });
        return items;
      }
      default: {
        const exhaustive: never = panelState;
        return exhaustive;
      }
    }
  }, [panelState, recentEntries, visibleRows, showViewMore]);

  const highlightCount = menuItems.length;
  const activeDescendantId = menuItems[highlight]?.id;

  /**
   * The listbox ids the input controls. role="combobox" requires aria-controls,
   * so the lead listbox is always named — every open view renders it, empty if
   * it has nothing to list. The loaded view adds the rows listbox, so AT walking
   * aria-controls reaches the result rows and "View more", not just the lead option.
   */
  const controlledListboxIds = useMemo<string>(
    () => (panelState === 'loaded' ? `${LEAD_LISTBOX_ID} ${ROWS_LISTBOX_ID}` : LEAD_LISTBOX_ID),
    [panelState]
  );

  // Reset the highlight whenever the composite changes shape. In `loaded` the
  // "Show results for" row sits at index 0, so a bare Enter still falls back to
  // the full results page.
  useEffect(() => {
    setHighlight(0);
  }, [panelState, effectiveActiveTab, trimmedQuery, source]);

  const handleHighlightById = useCallback(
    (id: string): void => {
      const index = menuItems.findIndex((item) => item.id === id);
      if (index >= 0) setHighlight(index);
    },
    [menuItems]
  );

  // Closing the panel always closes the filter bar too, otherwise the toggle can
  // be left stuck in its active state with no panel beneath it.
  const closePanel = useCallback((): void => {
    setPanelOpen(false);
    setFiltersOpen(false);
  }, []);

  // The results page is an in-app UI-Router state, so it is entered through the
  // router. That keeps the transition lifecycle intact and lets the state's dynamic
  // params update in place instead of the full-page reload location.assign forces.
  const goToResultsPage = useCallback(
    (rawQuery: string): void => {
      const trimmed = rawQuery.trim();
      if (!trimmed) return;
      recordRecent(trimmed);
      closePanel();
      router.stateService.go('nexusOneSearch', searchResultsStateParams(trimmed, source));
    },
    [closePanel, source, recordRecent]
  );

  // A row activation jumps to an entity rather than performing a search, so the
  // typed fragment is not recorded as a recent search. Row destinations can be
  // Classic-bundle URLs, so they navigate by href rather than through the router.
  const goToRow = useCallback(
    (row: SearchRow): void => {
      const href = clickHrefFor(row);
      closePanel();
      setQuery('');
      window.location.assign(href);
    },
    [closePanel]
  );

  const openPanel = useCallback((): void => {
    if (suppressReopenRef.current) return;
    setPanelOpen(true);
  }, []);

  // -------------------------------------------------------------------------
  // Document-level keyboard handler (pan-panel model).
  //
  // Capture phase so we run before Radix's own handlers: that lets us stop
  // propagation on arrows to keep a DropdownMenu.Trigger from opening its menu,
  // since arrows are reserved for the listbox. We bail entirely inside a Radix
  // popper, which owns its own keyboard.
  // -------------------------------------------------------------------------
  useEffect(() => {
    function onDocumentKeyDown(event: KeyboardEvent): void {
      const target = event.target as HTMLElement | null;
      const active = document.activeElement as HTMLElement | null;
      const isOnInput = active === inputRef.current;
      const inEditableField =
        !!target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable);

      // A Radix popper (filter menus, tab overflow menu, data-source select) owns
      // its own keyboard entirely, shortcuts included: focusing the input from
      // inside one would dismiss it on focus loss mid-interaction.
      if (target?.closest('[data-radix-popper-content-wrapper]')) return;

      // Global focus shortcuts work from anywhere on the page.
      const isCmdK = (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k';
      const isSlash = event.key === '/' && !inEditableField;
      if (isCmdK || isSlash) {
        event.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
        setPanelOpen(true);
        return;
      }

      // Arrow-down on a focused-but-closed input reopens the panel.
      if (!panelOpen && isOnInput && event.key === 'ArrowDown') {
        event.preventDefault();
        setPanelOpen(true);
        return;
      }

      if (!panelOpen) return;

      if (event.key === 'Escape') {
        event.stopPropagation();
        closePanel();
        suppressReopenRef.current = true;
        inputRef.current?.focus();
        suppressReopenRef.current = false;
        return;
      }

      if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp' && event.key !== 'Enter') return;

      // In scope when focus is inside the card, or has drifted to body/null after
      // a portaled menu closed.
      const isInsideContainer = active != null && !!containerRef.current?.contains(active);
      const isOnBodyOrNothing = active == null || active === document.body;
      if (!isInsideContainer && !isOnBodyOrNothing) return;

      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        event.stopPropagation();
        const step = event.key === 'ArrowDown' ? 1 : -1;
        setHighlight((current) => (highlightCount === 0 ? 0 : (current + step + highlightCount) % highlightCount));
        if (!isOnInput) inputRef.current?.focus();
        return;
      }

      // Enter must defer to native activation on real interactive elements (the
      // clear button, filter toggle, filter buttons, tab triggers, footer links)
      // so those keep working; only the input dispatches listbox activation.
      if (!isOnInput && !isOnBodyOrNothing) return;
      event.preventDefault();
      const item = menuItems[highlight];
      if (!item) {
        goToResultsPage(trimmedQuery);
        return;
      }
      switch (item.kind) {
        case 'show-all':
        case 'view-more':
        case 'placeholder':
          goToResultsPage(trimmedQuery);
          return;
        case 'row': {
          const row = visibleRows[item.rowIndex];
          if (row) goToRow(row);
          return;
        }
        case 'recent': {
          const entry = recentEntries[item.recentIndex];
          if (entry) {
            setQuery(entry.q);
            goToResultsPage(entry.q);
          }
          return;
        }
      }
    }

    document.addEventListener('keydown', onDocumentKeyDown, true);
    return () => document.removeEventListener('keydown', onDocumentKeyDown, true);
  }, [
    panelOpen,
    closePanel,
    highlight,
    highlightCount,
    menuItems,
    goToResultsPage,
    goToRow,
    recentEntries,
    trimmedQuery,
    visibleRows,
  ]);

  // Click-outside closes the panel. Radix menus (filter dropdowns, the tab
  // overflow menu, the data-source select) are portaled to the body, so a click
  // inside one looks "outside" to a naive containment check; treat any click in a
  // popper wrapper as belonging to the panel.
  useEffect(() => {
    function onMouseDown(event: MouseEvent): void {
      const target = event.target as Element | null;
      if (!target) return;
      if (target.closest?.('[data-radix-popper-content-wrapper]')) return;
      if (containerRef.current && !containerRef.current.contains(target)) closePanel();
    }
    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [closePanel]);

  // Owns cancellation of a pending focus retry, so a retry loop cannot keep chasing
  // a detached input after unmount.
  const focusWithCaret = useFocusInputWithCaret();

  const handleFilterInsert = useCallback(
    (request: FilterInsertRequest): void => {
      const { value, caretAt } = computeFilterInsert(query, request.syntax);
      setQuery(value);
      setPanelOpen(true);
      focusWithCaret(inputRef.current, caretAt);
    },
    [query, focusWithCaret]
  );

  /** Returns focus to the input when a filter-category menu closes by dismissal. */
  const handleFilterMenuClose = useCallback((): void => {
    inputRef.current?.focus();
  }, []);

  /**
   * Handle for the pending blur-close check. Cleared on unmount and before each
   * replacement so the callback cannot fire against a torn-down panel — navigating
   * away right after a blur is the common case.
   */
  const blurCloseTimerRef = useRef<number | null>(null);
  useEffect(
    () => () => {
      if (blurCloseTimerRef.current !== null) window.clearTimeout(blurCloseTimerRef.current);
    },
    []
  );

  /**
   * Blur handling: re-check focus after a delay instead of closing immediately.
   * Picking an item in a portaled menu moves focus out of the input and back on a
   * later frame, so an immediate close would race those menus and tear the panel
   * down mid-interaction. The document mousedown handler above remains the
   * authoritative outside-click detector.
   */
  const handleInputBlur = useCallback(
    (event: React.FocusEvent<HTMLInputElement>): void => {
      if (blurCloseTimerRef.current !== null) {
        window.clearTimeout(blurCloseTimerRef.current);
        blurCloseTimerRef.current = null;
      }
      const next = event.relatedTarget as Node | null;
      if (next && containerRef.current?.contains(next)) return;
      if (next instanceof Element && next.closest('[data-radix-popper-content-wrapper]')) return;
      blurCloseTimerRef.current = window.setTimeout(() => {
        blurCloseTimerRef.current = null;
        const active = document.activeElement;
        if (containerRef.current && active && containerRef.current.contains(active)) return;
        // Any open popper means the user is mid-interaction with a menu we own;
        // its own close handlers will tear it down.
        if (document.querySelector('[data-radix-popper-content-wrapper]')) return;
        if (active instanceof Element && active.closest('[data-radix-popper-content-wrapper]')) return;
        // Focus fell to body / nothing: defer to the mousedown handler, which has
        // already decided whether the click was truly outside.
        if (!active || active === document.body) return;
        closePanel();
      }, 150);
    },
    [closePanel]
  );

  const open = panelOpen || filtersOpen;

  return (
    <Box
      ref={containerRef}
      className="nosc-search-omnibar"
      data-testid="nosc-search-omnibar"
      style={{
        flex: '1 1 0',
        maxWidth: 500,
        minWidth: 200,
        position: 'relative',
        display: 'flex',
        justifyContent: 'center',
      }}
    >
      {/* Layout placeholder so the nav row reserves the same space whether the
          card is closed or expanded. */}
      <Box aria-hidden="true" width="100%" height="32px" style={{ pointerEvents: 'none' }} />

      {/* The interactive surface. Closed, it covers the placeholder exactly;
          open, it expands into a floating card centered on the field.

          Deliberately role-less: this is the combobox's popup surface, not a
          dialog. The combobox pattern names the input and its listboxes, and a
          `dialog` role here would have AT announce a dialog for a non-modal
          surface with no focus trap. */}
      <Box
        data-testid="nosc-search-omnibar-surface"
        data-open={open || undefined}
        className="nosc-search-omnibar-surface"
        style={
          open
            ? {
                position: 'absolute',
                top: 'calc(-1 * var(--space-3))',
                left: '50%',
                transform: 'translateX(-50%)',
                width: 'min(790px, calc(100vw - 32px))',
                background: 'var(--color-panel-solid)',
                border: '1px solid var(--gray-6)',
                borderRadius: 'var(--radius-3)',
                boxShadow: 'var(--shadow-5)',
                maxHeight: PANEL_MAX_HEIGHT,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                zIndex: 60,
              }
            : { position: 'absolute', top: 0, left: 0, right: 0, zIndex: 1 }
        }
      >
        {/* Header row. The data-source select and filter toggle belong to the
            expanded card only — closed, this row is the field alone. */}
        <Flex gap="2" width="100%" flexShrink="0" pt={open ? '3' : '0'} px={open ? '3' : '0'}>
          {open && (
            <Select.Root
              value={source}
              size="2"
              onValueChange={(value) => {
                // Narrow instead of casting so a Select.Item added later without
                // updating SearchSource cannot reach the backend.
                if (value !== 'local' && value !== 'catalog') return;
                setSource(value);
                setHighlight(0);
                // Keep the panel open so switching source re-queries in place.
                setPanelOpen(true);
              }}
            >
              <Select.Trigger
                variant="surface"
                color="gray"
                aria-label="Search data source"
                className="nosc-search-datasource-trigger"
                data-testid="nosc-search-datasource"
              />
              <Select.Content data-testid="nosc-search-datasource-content" position="popper">
                <Select.Item value="local" data-testid="nosc-search-datasource-local">
                  {SEARCH_SOURCE_LABEL.local}
                </Select.Item>
                <Select.Item value="catalog" data-testid="nosc-search-datasource-catalog">
                  {SEARCH_SOURCE_LABEL.catalog}
                </Select.Item>
              </Select.Content>
            </Select.Root>
          )}

          <TextField.Root
            ref={inputRef}
            placeholder={PLACEHOLDER}
            size="2"
            style={{ flex: 1 }}
            value={query}
            role="combobox"
            aria-expanded={panelOpen}
            // Names every listbox in the DOM for the current state, so the ids
            // always resolve and AT reaches the result rows, not just the lead option.
            aria-controls={controlledListboxIds}
            aria-autocomplete="list"
            aria-activedescendant={activeDescendantId}
            aria-label={PLACEHOLDER}
            data-testid="nosc-search-input"
            onChange={(event) => {
              setQuery(event.target.value);
              setPanelOpen(true);
            }}
            onFocus={openPanel}
            onBlur={handleInputBlur}
          >
            <TextField.Slot>
              <ActionIcons.Search size={16} />
            </TextField.Slot>
            {/* Closed, the field advertises its shortcuts. Open, it offers a
                clear button instead (only once there is something to clear). */}
            {open ? (
              query ? (
                <TextField.Slot>
                  <IconButton
                    variant="ghost"
                    size="1"
                    onClick={() => {
                      setQuery('');
                      setHighlight(0);
                      inputRef.current?.focus();
                    }}
                    aria-label="Clear search"
                    data-testid="nosc-search-clear"
                  >
                    <ActionIcons.Cancel size={14} />
                  </IconButton>
                </TextField.Slot>
              ) : null
            ) : (
              <TextField.Slot side="right">
                <Flex
                  align="center"
                  gap="1"
                  aria-hidden="true"
                  style={{ flexShrink: 0, pointerEvents: 'none' }}
                  data-testid="nosc-search-shortcut-hints"
                >
                  <kbd style={SHORTCUT_CHIP_STYLE}>/</kbd>
                  <kbd style={SHORTCUT_CHIP_STYLE}>&#8984; K</kbd>
                </Flex>
              </TextField.Slot>
            )}
          </TextField.Root>

          {open && (
            <IconButton
              size="2"
              variant={filtersOpen ? 'solid' : 'outline'}
              color="gray"
              highContrast={filtersOpen}
              // Disclosure, not a toggle button: aria-controls + aria-expanded name
              // the filter bar this button shows and hides.
              aria-expanded={filtersOpen}
              aria-controls="nosc-search-filter-bar"
              aria-label={filtersOpen ? 'Hide filters' : 'Show filters'}
              data-testid="nosc-search-filter-toggle"
              onClick={() => setFiltersOpen((value) => !value)}
            >
              <ActionIcons.FilterList size={16} />
            </IconButton>
          )}
        </Flex>

        {/* Panel body. Mounted only while open so the closed omnibar renders
            nothing but the field. */}
        {open && (
          <Box flexGrow="1" minHeight="0" overflow="auto" data-testid="nosc-search-panel-body">
            {filtersOpen && (
              <FilterBar
                id="nosc-search-filter-bar"
                hideTriggerIcons
                onInsert={handleFilterInsert}
                onMenuClose={handleFilterMenuClose}
              />
            )}

            {panelOpen && (
              <>
                {(panelState === 'focused-empty' || panelState === 'focused-short') && (
                  <RecentSearchesView
                    entries={recentEntries}
                    highlightedItemId={activeDescendantId}
                    onHighlight={handleHighlightById}
                    onActivate={(recentQuery) => {
                      setQuery(recentQuery);
                      goToResultsPage(recentQuery);
                    }}
                  />
                )}

                {(panelState === 'loading' || panelState === 'loaded-empty') && (
                  <PlaceholderView
                    query={trimmedQuery}
                    variant={panelState === 'loading' ? 'loading' : loadError ? 'error' : 'empty'}
                    errorMessage={loadError}
                    highlightedItemId={activeDescendantId}
                    onHighlight={handleHighlightById}
                    onActivate={() => goToResultsPage(trimmedQuery)}
                  />
                )}

                {panelState === 'loaded' && (
                  <ResultsView
                    query={trimmedQuery}
                    tabs={tabs}
                    activeTab={effectiveActiveTab}
                    onActiveTabChange={setActiveTabId}
                    hideTabs={hideTabs}
                    rows={visibleRows}
                    highlightedItemId={activeDescendantId}
                    onHighlight={handleHighlightById}
                    onActivateRow={goToRow}
                    onShowAll={() => goToResultsPage(trimmedQuery)}
                    showViewMore={showViewMore}
                  />
                )}

                {/* Catalog narrows the tab set, so say why once results (or the
                    lack of them) are on screen. */}
                {source === 'catalog' && (panelState === 'loaded' || panelState === 'loaded-empty') && (
                  <CatalogScopeHint />
                )}

                <PanelFooter syntaxDocsUrl={SYNTAX_DOCS_URL} />
              </>
            )}
          </Box>
        )}
      </Box>
    </Box>
  );
}
