/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Box, Button, Dialog, Flex, Heading, Text, TextField } from '@radix-ui/themes';
import { AsyncPageState, AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import './FilteredListLayout.css';

/** Icon component type (Lucide icons). */
type IconComponent = React.ComponentType<{ size?: number; color?: string }>;

/** Singular/plural nouns for the "N items" count label. */
export interface CountNoun {
  readonly singular: string;
  readonly plural: string;
}

/**
 * Shared skeleton for filtered list pages (Applications, Violations, Vulnerabilities, Waivers).
 * CLM-42562 — Reduces each page from ~100 lines to a declarative config.
 *
 * Owns:
 * - Page header (title, description, icon)
 * - Filter rail slot (left sidebar)
 * - Toolbar (search bar, count)
 * - AsyncPageState (loading, error, empty)
 * - Pagination
 *
 * The `slug` (kebab-case) drives every data-testid and is kept separate from the
 * human-readable `title`; `countNoun` supplies the singular/plural label for the count
 * so irregular plurals (vulnerability/vulnerabilities) render correctly.
 */

export interface FilteredListLayoutProps<TItem> {
  // Identity
  /** Human-readable page title (e.g. "Vulnerabilities"). */
  readonly title: string;
  /** kebab-case identifier for data-testids (e.g. "vulnerabilities"). */
  readonly slug: string;
  /**
   * Root `<main>` data-testid. Defaults to `preview-{slug}-page`. Lets a page that reuses another
   * page's slug-namespaced slots (e.g. Legal reusing the Violations slots) still get a distinct
   * root id (`preview-legal-page`) while inner testids stay in the shared `{slug}-*` namespace.
   */
  readonly pageTestId?: string;
  readonly description: string;
  readonly icon: IconComponent;
  /** Singular/plural nouns for the count label (e.g. { singular: 'vulnerability', plural: 'vulnerabilities' }). */
  readonly countNoun: CountNoun;

  // Data
  readonly items: ReadonlyArray<TItem>;
  readonly totalCount: number;
  readonly loading: boolean;
  readonly error: string | null;
  /** Error banner title. Defaults to "Failed to load {countNoun.plural}". */
  readonly errorTitle?: string;
  /** Informational panel (e.g. 409 index-building) shown in place of the list, with optional retry. */
  readonly info?: AsyncPageStateInfoProps | null;
  readonly onRetry?: () => void;

  // Search
  /** When false, the in-toolbar search form is not rendered (e.g. query comes from a global omnibar). Default true. */
  readonly searchable?: boolean;
  /** Current search term. Optional when `searchable` is false. */
  readonly searchValue?: string;
  /** Submit handler for the search form. Required only when `searchable`. */
  readonly onSearchSubmit?: (term: string) => void;
  readonly searchPlaceholder?: string;

  // Count
  /** Override the toolbar count label (e.g. "Showing X of Y matches"). Falls back to `{count} {noun}`. */
  readonly renderCount?: () => React.ReactNode;
  /** When false, the below-tabs toolbar (search + count) is not rendered. Default true. */
  readonly showToolbar?: boolean;
  /** Replaces the default search + count toolbar content (e.g. a page with a CSV export button). */
  readonly renderToolbar?: () => React.ReactNode;

  // Header
  /** Replaces the default title/description/icon header. Rendered above the tabs. */
  readonly renderHeader?: () => React.ReactNode;

  // Pagination
  /** When false, pagination is never rendered (e.g. "load more" deferred). Default true. */
  readonly paginated?: boolean;
  readonly page?: number;
  readonly pageSize?: number;
  readonly onPageChange?: (page: number) => void;
  /** Force pagination visible even when the current page is within pageSize (server says more pages exist). */
  readonly hasNextPage?: boolean;

  // Tabs (optional scope strip between header and content, e.g. "My Scan Data / Sonatype Catalog")
  readonly renderTabs?: () => React.ReactNode;

  // Filter rail
  readonly hasActiveFilters?: boolean;
  readonly renderFilterRail?: () => React.ReactNode;
  readonly renderMobileFilterDrawer?: () => React.ReactNode;
  /** Mobile filter-drawer helper text. Defaults to "Narrow results by {countNoun.plural}." */
  readonly filterDrawerDescription?: string;

  /**
   * Reset all filters to their default state. Used by the empty-state
   * "Reset filters" button when hasActiveFilters is true.
   */
  readonly onResetFilters?: () => void;

  // Render slots
  readonly renderCardGrid: (items: ReadonlyArray<TItem>) => React.ReactNode;
  readonly emptyIcon?: IconComponent;
  readonly emptyTitle?: string;
  readonly emptyHint?: string;
  /** Replaces the default empty state (e.g. to add filter/search recovery actions). */
  readonly renderEmpty?: () => React.ReactNode;
}

/** Default icon for empty state when search is active. */
const SearchIcon = ActionIcons.Search;

export function FilteredListLayout<TItem>({
  title,
  slug,
  pageTestId,
  description,
  icon: Icon,
  countNoun,
  items,
  totalCount,
  loading,
  error,
  errorTitle,
  info = null,
  onRetry,
  searchable = true,
  searchValue = '',
  onSearchSubmit,
  searchPlaceholder = 'Search...',
  renderCount,
  showToolbar = true,
  renderToolbar,
  renderHeader,
  paginated = true,
  page = 1,
  pageSize = 0,
  onPageChange,
  hasNextPage = false,
  renderTabs,
  hasActiveFilters = false,
  renderFilterRail,
  renderMobileFilterDrawer,
  filterDrawerDescription,
  onResetFilters,
  renderCardGrid,
  emptyIcon,
  emptyTitle,
  emptyHint,
  renderEmpty,
}: FilteredListLayoutProps<TItem>): JSX.Element {
  const offsets = usePreviewShellOffsets();
  // `pageSize > 0` guards a footgun: an adopter that paginates but omits pageSize (default 0)
  // would otherwise render <Pagination pageSize={0}> → Math.ceil(total / 0) = Infinity pages.
  const showPagination =
    paginated && Boolean(onPageChange) && pageSize > 0 && (totalCount > pageSize || page > 1 || hasNextPage);
  const hasSearch = searchable && searchValue.trim().length > 0;
  const hasItems = items.length > 0;
  const hasFilterRail = Boolean(renderFilterRail);

  // Local draft for the search input
  const [draft, setDraft] = useState(searchValue);
  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  // Mobile filter drawer state
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  const isNarrowing = hasSearch || hasActiveFilters;

  // Default empty messages based on search/filter state
  const resolvedEmptyTitle =
    emptyTitle ?? (isNarrowing ? 'No results match your filters.' : `No ${countNoun.plural} to display.`);
  const resolvedEmptyHint =
    emptyHint ??
    (isNarrowing ? 'Try adjusting your search or filters.' : `${title} will appear here once data is loaded.`);
  // Search glyph only reads right when the emptiness comes from narrowing; the pure data-empty
  // state ("No X to display.") uses the page's own icon so it doesn't imply a failed search.
  const EmptyIcon = emptyIcon ?? (isNarrowing ? SearchIcon : Icon);

  const countLabel = `${totalCount} ${totalCount === 1 ? countNoun.singular : countNoun.plural}`;

  return (
    <Box
      asChild
      p="6"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <main data-testid={pageTestId ?? `preview-${slug}-page`}>
        {/* Header — custom override or default title/description/icon */}
        {renderHeader ? (
          <Box mb="5" data-testid={`${slug}-header`}>
            {renderHeader()}
          </Box>
        ) : (
          <Flex direction="column" gap="2" mb="5">
            <Flex align="center" gap="3">
              <Icon size={28} color="var(--accent-9)" />
              <Heading size="6">{title}</Heading>
            </Flex>
            <Text size="2" color="gray">
              {description}
            </Text>
          </Flex>
        )}

        {/* Optional scope tabs (e.g. "My Scan Data / Sonatype Catalog") — full width above content */}
        {renderTabs && (
          <Box mb="4" data-testid={`${slug}-tabs`}>
            {renderTabs()}
          </Box>
        )}

        {/* Content with optional filter rail */}
        <Flex gap="4" align="start" wrap="wrap" data-testid={`${slug}-page-layout`}>
          {/* Desktop filter rail — flexShrink:0 so the rail keeps its width instead of
              compressing when the card grid is wide. */}
          {hasFilterRail && (
            <Box display={{ initial: 'none', sm: 'block' }} style={{ flexShrink: 0 }}>
              {renderFilterRail?.()}
            </Box>
          )}

          <Box className="filtered-list-layout__content" data-testid={`${slug}-page-content`}>
            <Flex direction="column" gap="4">
              {/* Mobile filter trigger — rendered independently of the toolbar so pages that hide
                  the toolbar (e.g. the search results page, showToolbar={false}) still expose the
                  filter rail on small screens, where the desktop rail is display:none. */}
              {hasFilterRail && renderMobileFilterDrawer && (
                <Box display={{ initial: 'block', sm: 'none' }}>
                  <Dialog.Root open={mobileFiltersOpen} onOpenChange={setMobileFiltersOpen}>
                    <Dialog.Trigger>
                      <Button
                        variant="outline"
                        color="gray"
                        size="2"
                        data-testid={`${slug}-filters-mobile-trigger`}
                        aria-label={hasActiveFilters ? 'Filters (active)' : 'Filters'}
                      >
                        <ActionIcons.Filter size={14} aria-hidden />
                        Filters
                        {hasActiveFilters && (
                          <Box
                            data-testid={`${slug}-filters-mobile-active-dot`}
                            style={{
                              width: 8,
                              height: 8,
                              borderRadius: '50%',
                              backgroundColor: 'var(--accent-9)',
                            }}
                          />
                        )}
                      </Button>
                    </Dialog.Trigger>
                    <Dialog.Content maxWidth="360px" data-testid={`${slug}-filters-mobile-drawer`}>
                      <Dialog.Title size="3">Filters</Dialog.Title>
                      <Dialog.Description size="1" color="gray" mb="3">
                        {filterDrawerDescription ?? `Narrow results by ${countNoun.plural}.`}
                      </Dialog.Description>
                      {renderMobileFilterDrawer()}
                      <Flex justify="end" mt="4">
                        <Dialog.Close>
                          <Button size="2" data-testid={`${slug}-filters-mobile-apply`}>
                            Done
                          </Button>
                        </Dialog.Close>
                      </Flex>
                    </Dialog.Content>
                  </Dialog.Root>
                </Box>
              )}

              {/* Toolbar */}
              {showToolbar && (
                <Flex align="center" justify="between" gap="3" wrap="wrap" data-testid={`${slug}-toolbar`}>
                  {renderToolbar ? (
                    <Box style={{ flex: 1 }}>{renderToolbar()}</Box>
                  ) : (
                    <>
                      <Flex align="center" gap="3" style={{ flex: 1, minWidth: '240px' }}>
                        {/* Search */}
                        {searchable && (
                          <form
                            role="search"
                            onSubmit={(event) => {
                              event.preventDefault();
                              onSearchSubmit?.(draft.trim());
                            }}
                            style={{ flex: 1 }}
                          >
                            <TextField.Root
                              placeholder={searchPlaceholder}
                              aria-label={`Search ${countNoun.plural}`}
                              value={draft}
                              onChange={(event) => setDraft(event.target.value)}
                              data-testid={`${slug}-toolbar-search`}
                              style={{ width: '100%' }}
                            >
                              <TextField.Slot>
                                <ActionIcons.Search size={16} />
                              </TextField.Slot>
                            </TextField.Root>
                          </form>
                        )}
                      </Flex>

                      <Box data-testid={`${slug}-toolbar-count`}>
                        {renderCount ? (
                          renderCount()
                        ) : (
                          <Text size="2" color="gray">
                            {countLabel}
                          </Text>
                        )}
                      </Box>
                    </>
                  )}
                </Flex>
              )}

              {/* Content states */}
              <AsyncPageState
                loading={loading}
                error={error}
                info={info}
                onRetry={onRetry}
                loadingTestId={`${slug}-list-loading`}
                errorTestId={`${slug}-list-error`}
                errorTitle={errorTitle ?? `Failed to load ${countNoun.plural}`}
                errorVariant="banner"
                infoVariant="banner"
              >
                {hasItems ? (
                  renderCardGrid(items)
                ) : renderEmpty ? (
                  renderEmpty()
                ) : (
                  <Flex direction="column" align="center" gap="2" py="8" data-testid={`${slug}-list-empty`}>
                    <EmptyIcon size={32} color="var(--gray-9)" />
                    <Text size="3" color="gray">
                      {resolvedEmptyTitle}
                    </Text>
                    <Text size="2" color="gray">
                      {resolvedEmptyHint}
                    </Text>
                    {(hasSearch || hasActiveFilters) && (
                      <Flex gap="2" mt="1">
                        {hasSearch && (
                          <Button
                            variant="soft"
                            size="2"
                            onClick={() => onSearchSubmit?.('')}
                            data-testid={`${slug}-empty-clear-search`}
                          >
                            <ActionIcons.Refresh size={14} aria-hidden />
                            Clear search
                          </Button>
                        )}
                        {hasActiveFilters && onResetFilters && (
                          <Button
                            variant="soft"
                            size="2"
                            onClick={onResetFilters}
                            data-testid={`${slug}-empty-reset-filters`}
                          >
                            <ActionIcons.Refresh size={14} aria-hidden />
                            Reset filters
                          </Button>
                        )}
                      </Flex>
                    )}
                  </Flex>
                )}
              </AsyncPageState>

              {/* Pagination lives OUTSIDE AsyncPageState so it survives a loading refetch
                  (AsyncPageState early-returns its banner during loading/error/info, which would
                  otherwise unmount + remount the pager on every page change). Hidden on error/info
                  since no list is shown then. `onPageChange &&` narrows the type — showPagination
                  already guarantees it, but the compiler can't see that here. */}
              {showPagination && !error && !info && onPageChange && (
                <Pagination
                  page={page}
                  pageSize={pageSize}
                  totalItems={totalCount}
                  hasNextPage={hasNextPage}
                  onPageChange={onPageChange}
                  data-testid={`${slug}-pagination`}
                />
              )}
            </Flex>
          </Box>
        </Flex>
      </main>
    </Box>
  );
}
