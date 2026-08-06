/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Badge, Box, DropdownMenu, Flex, IconButton, Text } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { SearchPanelTab } from 'MainRoot/nosc/search/searchPanelModel';

/**
 * Horizontal tab strip for the suggest panel: All + one tab per entity type,
 * each with a count badge. Tabs that do not fit move into an overflow menu
 * behind an ellipsis button, so a narrow panel never clips a tab.
 *
 * Built from plain buttons in a `tablist` rather than Radix Tabs: the rows the
 * tabs filter live in the panel's shared listbox, outside any tab panel, and a
 * Radix `Tabs.Trigger` without a matching `Tabs.Content` emits an `aria-controls`
 * pointing at an element that does not exist.
 *
 * Widths are measured from the rendered triggers and cached, because a tab that
 * moves into the overflow menu no longer reports an offsetWidth of its own.
 */

/** A count at or above this is rendered as a capped label rather than a number. */
const COUNT_DISPLAY_CAP = 1000;

function formatCount(count: number): string {
  return count >= COUNT_DISPLAY_CAP ? '1,000+' : String(count);
}

/**
 * The tablist's own horizontal spacing: the flex `gap` between adjacent tabs and
 * its combined left/right padding. Read from computed style so the CSS stays the
 * single source of truth. Non-numeric values (jsdom reports `gap: "normal"`)
 * resolve to 0.
 */
function tablistSpacing(tablist: HTMLElement | null): { gap: number; padding: number } {
  if (!tablist || typeof window.getComputedStyle !== 'function') return { gap: 0, padding: 0 };
  const style = window.getComputedStyle(tablist);
  const px = (value: string): number => {
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : 0;
  };
  return {
    gap: px(style.columnGap || style.gap || ''),
    padding: px(style.paddingLeft) + px(style.paddingRight),
  };
}

/** Tab label plus its count badge, shared by the visible strip and the overflow menu. */
function TabLabel({ tab }: { readonly tab: SearchPanelTab }): JSX.Element {
  return (
    <>
      <Text size="2">{tab.label}</Text>
      {typeof tab.count === 'number' && (
        <Badge size="1" variant="soft" color="gray" radius="full">
          {formatCount(tab.count)}
        </Badge>
      )}
    </>
  );
}

/**
 * A tab is disabled only when its count is known to be zero. An undefined count
 * (counts still loading) leaves the tab enabled so it never flickers disabled.
 */
function isDisabled(tab: SearchPanelTab): boolean {
  return typeof tab.count === 'number' && tab.count === 0;
}

export interface SearchPanelTabsProps {
  readonly tabs: readonly SearchPanelTab[];
  readonly activeTab: string;
  readonly onActiveTabChange: (tab: string) => void;
}

export function SearchPanelTabs({ tabs, activeTab, onActiveTabChange }: SearchPanelTabsProps): JSX.Element {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const tablistRef = useRef<HTMLDivElement | null>(null);
  const overflowButtonRef = useRef<HTMLButtonElement | null>(null);
  const triggerRefs = useRef<Map<string, HTMLElement>>(new Map());
  const cachedWidths = useRef<Map<string, number>>(new Map());
  const [overflowIds, setOverflowIds] = useState<readonly string[]>([]);

  const recompute = useCallback((): void => {
    const container = containerRef.current;
    if (!container) return;

    for (const [id, element] of triggerRefs.current.entries()) {
      if (element && element.offsetWidth > 0) cachedWidths.current.set(id, element.offsetWidth);
    }

    const containerWidth = container.clientWidth;
    if (containerWidth === 0) return;
    // Wait until every tab has been measured at least once, otherwise the first
    // pass would push un-measured tabs into the overflow menu.
    if (!tabs.every((tab) => cachedWidths.current.has(tab.id))) return;

    // The tablist lays tabs out with a flex gap inside horizontal padding, none of
    // which is part of any tab's offsetWidth. Counting only the tabs would let a
    // strip that really overflows look like it fits, and it would be clipped by the
    // parent's overflow:hidden before the overflow menu ever appeared.
    const { gap, padding } = tablistSpacing(tablistRef.current);
    const spacingFor = (tabCount: number): number =>
      padding + Math.max(0, tabCount - 1) * gap;

    const tabsWidth = tabs.reduce((sum, tab) => sum + (cachedWidths.current.get(tab.id) ?? 0), 0);
    if (tabsWidth + spacingFor(tabs.length) <= containerWidth) {
      setOverflowIds((previous) => (previous.length === 0 ? previous : []));
      return;
    }

    const budget = containerWidth - (overflowButtonRef.current?.offsetWidth ?? 36) - 4;
    const overflow: string[] = [];
    let used = 0;
    let visibleCount = 0;
    tabs.forEach((tab, index) => {
      const width = cachedWidths.current.get(tab.id) ?? 0;
      // Always keep the first tab visible so the strip is never empty.
      const wouldUse = used + width + spacingFor(visibleCount + 1);
      if (index > 0 && wouldUse > budget) overflow.push(tab.id);
      else {
        used += width;
        visibleCount += 1;
      }
    });
    setOverflowIds((previous) =>
      previous.length === overflow.length && previous.every((id, i) => id === overflow[i]) ? previous : overflow
    );
  }, [tabs]);

  // Deliberately dependency-free: widths must be re-measured after every render,
  // including content changes (a count badge growing) that shift layout without
  // changing `tabs`. `recompute` keeps the previous array identity when the
  // overflow set is unchanged, so this cannot loop.
  useLayoutEffect(() => {
    recompute();
  });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const observer = new ResizeObserver(() => recompute());
    observer.observe(container);
    return () => observer.disconnect();
  }, [recompute]);

  const overflowSet = useMemo(() => new Set(overflowIds), [overflowIds]);

  const { visibleTabs, overflowTabs } = useMemo(() => {
    const visible = tabs.filter((tab) => !overflowSet.has(tab.id));
    let overflow = tabs.filter((tab) => overflowSet.has(tab.id));

    // The active tab must stay visible so its selection indicator renders;
    // swap it with the last visible tab when it would have overflowed.
    if (overflowSet.has(activeTab) && visible.length > 0) {
      const displaced = visible[visible.length - 1];
      const activeDef = tabs.find((tab) => tab.id === activeTab);
      if (activeDef && displaced) {
        visible[visible.length - 1] = activeDef;
        overflow = [displaced, ...overflow.filter((tab) => tab.id !== activeTab)];
      }
    }
    return { visibleTabs: visible, overflowTabs: overflow };
  }, [activeTab, overflowSet, tabs]);

  return (
    <>
      <Flex ref={containerRef} align="center" data-testid="nosc-search-panel-tabs">
        <Box flexGrow="1" minWidth="0" overflow="hidden">
          <Flex
            ref={tablistRef}
            role="tablist"
            aria-label="Result types"
            align="center"
            className="nosc-search-tablist"
          >
            {visibleTabs.map((tab) => {
              const selected = tab.id === activeTab;
              return (
                <button
                  key={tab.id}
                  type="button"
                  role="tab"
                  aria-selected={selected}
                  // Roving tabindex: only the selected tab is a Tab stop, and
                  // left/right arrows move within the strip.
                  tabIndex={selected ? 0 : -1}
                  disabled={isDisabled(tab)}
                  className="nosc-search-tab"
                  data-selected={selected || undefined}
                  data-testid={`nosc-search-panel-tab-${tab.id}`}
                  ref={(element: HTMLButtonElement | null) => {
                    if (element) triggerRefs.current.set(tab.id, element);
                    else triggerRefs.current.delete(tab.id);
                  }}
                  onClick={() => onActiveTabChange(tab.id)}
                  onKeyDown={(event) => {
                    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
                    event.preventDefault();
                    const enabled = visibleTabs.filter((candidate) => !isDisabled(candidate));
                    const current = enabled.findIndex((candidate) => candidate.id === tab.id);
                    if (current < 0 || enabled.length === 0) return;
                    const step = event.key === 'ArrowRight' ? 1 : -1;
                    const next = enabled[(current + step + enabled.length) % enabled.length];
                    if (!next) return;
                    onActiveTabChange(next.id);
                    triggerRefs.current.get(next.id)?.focus();
                  }}
                >
                  <Flex align="center" gap="2">
                    <TabLabel tab={tab} />
                  </Flex>
                </button>
              );
            })}
          </Flex>
        </Box>
        {overflowTabs.length > 0 && (
          <Box flexShrink="0" px="1">
            {/* modal={false} keeps body pointer-events intact so an outside
                click focuses its target normally instead of racing the panel's
                blur handler and closing the whole omnibar. */}
            <DropdownMenu.Root modal={false}>
              <DropdownMenu.Trigger>
                <IconButton
                  ref={overflowButtonRef}
                  variant="outline"
                  color="gray"
                  size="2"
                  data-testid="nosc-search-panel-tabs-overflow"
                  aria-label={`${overflowTabs.length} more tab${overflowTabs.length === 1 ? '' : 's'}`}
                >
                  <ActionIcons.MoreVertical size={16} />
                </IconButton>
              </DropdownMenu.Trigger>
              <DropdownMenu.Content
                align="end"
                size="2"
                // Don't shove focus back to the trigger on close; the panel
                // returns focus to the input itself.
                onCloseAutoFocus={(event) => event.preventDefault()}
              >
                {overflowTabs.map((tab) => (
                  <DropdownMenu.Item
                    key={tab.id}
                    disabled={isDisabled(tab)}
                    data-testid={`nosc-search-panel-tab-overflow-${tab.id}`}
                    onSelect={() => onActiveTabChange(tab.id)}
                  >
                    <Flex justify="between" align="center" gap="4" width="100%" minWidth="160px">
                      <TabLabel tab={tab} />
                    </Flex>
                  </DropdownMenu.Item>
                ))}
              </DropdownMenu.Content>
            </DropdownMenu.Root>
          </Box>
        )}
      </Flex>
    </>
  );
}
