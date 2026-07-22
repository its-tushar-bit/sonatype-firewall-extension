/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Button, Flex, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { buildComponentsListExportPayload } from 'MainRoot/nosc/componentsList/componentsListExport';
import type { ComponentsListFilterState } from 'MainRoot/nosc/componentsList/componentsListFilters';
import type { ComponentsTab } from 'MainRoot/nosc/componentsList/componentsRoute';
import { getComponentRisksExportUrl } from 'MainRoot/util/CLMLocation';

export const COMPONENTS_CSV_FILTER_CAVEAT =
  'CSV export caveat: search, organization, and ecosystem filters are not applied to Classic export';

export interface ComponentsToolbarProps {
  readonly tab: ComponentsTab;
  readonly onTabChange: (tab: ComponentsTab) => void;
  readonly totalCount: number;
  readonly exactTotalEstimate?: boolean;
  readonly catalogAvailable?: boolean;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly filters: ComponentsListFilterState;
}

/**
 * Header toolbar for Martha V1 Components: My Scan Data / Sonatype Catalog tabs, search, CSV.
 */
export default function ComponentsToolbar({
  tab,
  onTabChange,
  totalCount,
  exactTotalEstimate = true,
  catalogAvailable = true,
  searchValue,
  onSearchSubmit,
  filters,
}: ComponentsToolbarProps): JSX.Element {
  const [draft, setDraft] = useState(searchValue);

  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  const exportPayloadJson = useMemo(
    () => JSON.stringify(buildComponentsListExportPayload(filters)),
    [filters],
  );

  const isCatalog = tab === 'catalog';
  const canExport = !isCatalog && totalCount > 0;
  const hasCatalogOnlyFilters =
    searchValue.trim().length > 0 ||
    filters.organizations.size > 0 ||
    filters.ecosystems.size > 0;
  const showFilterCaveat = !isCatalog && canExport && hasCatalogOnlyFilters;
  const csvExportTitle = isCatalog
    ? 'Sonatype Catalog export is not available'
    : !canExport
      ? 'No components to export'
      : showFilterCaveat
        ? COMPONENTS_CSV_FILTER_CAVEAT
        : undefined;
  const csvHintId = isCatalog
    ? 'components-toolbar-csv-catalog-hint'
    : !canExport
      ? 'components-toolbar-csv-empty-hint'
      : showFilterCaveat
        ? 'components-toolbar-csv-filter-hint'
        : undefined;

  const countLabel = (() => {
    if (isCatalog && !catalogAvailable) {
      return 'Catalog unavailable';
    }
    const suffix = exactTotalEstimate ? '' : '+';
    const noun = isCatalog
      ? `catalog component${totalCount === 1 && exactTotalEstimate ? '' : 's'}`
      : `component${totalCount === 1 && exactTotalEstimate ? '' : 's'}`;
    return `${totalCount.toLocaleString()}${suffix} ${noun}`;
  })();

  return (
    <Flex direction="column" gap="3" data-testid="components-toolbar">
      <Flex align="center" gap="2" role="tablist" aria-label="Component data source">
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'myScanData'}
          data-testid="components-tab-my-scan-data"
          onClick={() => onTabChange('myScanData')}
          style={{
            border: 'none',
            background: tab === 'myScanData' ? 'var(--accent-3)' : 'transparent',
            color: 'inherit',
            padding: '6px 12px',
            borderRadius: 6,
            cursor: 'pointer',
            fontWeight: tab === 'myScanData' ? 600 : 400,
          }}
        >
          My Scan Data
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'catalog'}
          data-testid="components-tab-catalog"
          onClick={() => onTabChange('catalog')}
          style={{
            border: 'none',
            background: tab === 'catalog' ? 'var(--accent-3)' : 'transparent',
            color: 'inherit',
            padding: '6px 12px',
            borderRadius: 6,
            cursor: 'pointer',
            fontWeight: tab === 'catalog' ? 600 : 400,
          }}
        >
          Sonatype Catalog
        </button>
      </Flex>

      <Flex align="center" justify="between" gap="3" wrap="wrap">
        <Text size="2" color="gray" data-testid="components-toolbar-count">
          {countLabel}
        </Text>
        <Flex align="center" gap="3" wrap="wrap" style={{ flex: 1, justifyContent: 'flex-end' }}>
          <form
            role="search"
            onSubmit={(event) => {
              event.preventDefault();
              onSearchSubmit(draft.trim());
            }}
            style={{ flex: 1, minWidth: 200, maxWidth: 360 }}
          >
            <TextField.Root
              placeholder="Search components…"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              data-testid="components-toolbar-search"
              aria-label="Search components"
            >
              <TextField.Slot>
                <ActionIcons.Search size={16} />
              </TextField.Slot>
            </TextField.Root>
          </form>

          <form
            action={getComponentRisksExportUrl()}
            method="post"
            encType="multipart/form-data"
            data-testid="components-toolbar-export-form"
          >
            <input type="hidden" name="filter" value={exportPayloadJson} />
            <Button
              variant="outline"
              color="gray"
              size="2"
              type="submit"
              disabled={!canExport}
              data-testid="components-toolbar-csv"
              title={csvExportTitle}
              aria-describedby={csvHintId}
            >
              <ActionIcons.Download size={14} />
              CSV
            </Button>
          </form>
        </Flex>
      </Flex>

      {isCatalog && (
        <Text size="1" color="gray" id="components-toolbar-csv-catalog-hint">
          Sonatype Catalog export is not available. Export is available for My Scan Data only.
        </Text>
      )}
      {!isCatalog && !canExport && (
        <Text size="1" color="gray" id="components-toolbar-csv-empty-hint">
          No components to export.
        </Text>
      )}
      {showFilterCaveat && (
        <Text
          size="1"
          color="gray"
          id="components-toolbar-csv-filter-hint"
          data-testid="components-toolbar-csv-filter-hint"
        >
          {COMPONENTS_CSV_FILTER_CAVEAT}.
        </Text>
      )}
    </Flex>
  );
}
