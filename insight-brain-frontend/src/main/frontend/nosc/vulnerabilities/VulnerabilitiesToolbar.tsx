/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Button, Flex, Select, Tabs, Text, TextField, VisuallyHidden } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { getVulnerabilitiesExportUrl } from 'MainRoot/util/CLMLocation';
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListOrderBy,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import { hasActiveVulnerabilityFilters } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import { vulnerabilitiesListOrderByLabel } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListQuery';
import { buildVulnerabilitiesExportPayload } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListExport';

export interface VulnerabilitiesToolbarProps {
  readonly tab: VulnerabilitiesTab;
  readonly onTabChange: (tab: VulnerabilitiesTab) => void;
  readonly totalCount: number;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly orderBy: VulnerabilitiesListOrderBy;
  readonly onOrderByChange: (orderBy: VulnerabilitiesListOrderBy) => void;
  readonly filters: VulnerabilitiesFilterState;
}

type CsvExportState = 'catalog' | 'empty' | 'filtered' | 'ready';

/**
 * Header toolbar for Martha V1 Vulnerabilities: tabs, count, search, CVSS sort, and My Scan Data CSV.
 */
export default function VulnerabilitiesToolbar({
  tab,
  onTabChange,
  totalCount,
  searchValue,
  onSearchSubmit,
  orderBy,
  onOrderByChange,
  filters,
}: VulnerabilitiesToolbarProps): JSX.Element {
  const [draft, setDraft] = useState(searchValue);

  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  const exportPayloadJson = useMemo(
    () => JSON.stringify(buildVulnerabilitiesExportPayload({ search: searchValue, orderBy, filters })),
    [searchValue, orderBy, filters],
  );

  const isCatalog = tab === 'catalog';
  const canExport = !isCatalog && totalCount > 0;
  const hasActiveFilters =
    hasActiveVulnerabilityFilters(filters, tab) || searchValue.trim().length > 0;
  const csvState: CsvExportState = isCatalog
    ? 'catalog'
    : !canExport
      ? 'empty'
      : hasActiveFilters
        ? 'filtered'
        : 'ready';
  const csvExportTitle =
    csvState === 'catalog'
      ? 'Sonatype Catalog export is not available'
      : csvState === 'empty'
        ? 'No vulnerabilities to export'
        : csvState === 'filtered'
          ? 'CSV export uses the current search and filters'
          : undefined;
  const csvHintId =
    csvState === 'catalog'
      ? 'vulnerabilities-toolbar-csv-catalog-hint'
      : csvState === 'empty'
        ? 'vulnerabilities-toolbar-csv-empty-hint'
        : csvState === 'filtered'
          ? 'vulnerabilities-toolbar-csv-filters-hint'
          : undefined;

  const countLabel = isCatalog
    ? `${totalCount.toLocaleString()} catalog vulnerabilit${totalCount === 1 ? 'y' : 'ies'}`
    : `${totalCount.toLocaleString()} vulnerabilit${totalCount === 1 ? 'y' : 'ies'}`;

  return (
    <Flex direction="column" gap="3" data-testid="vulnerabilities-toolbar">
      <Tabs.Root
        value={tab}
        onValueChange={(next) => {
          if (next === 'myScanData' || next === 'catalog') {
            onTabChange(next);
          }
        }}
        aria-label="Vulnerability data source"
      >
        <Tabs.List>
          <Tabs.Trigger value="myScanData" data-testid="vulnerabilities-tab-my-scan-data">
            My Scan Data
          </Tabs.Trigger>
          <Tabs.Trigger value="catalog" data-testid="vulnerabilities-tab-catalog">
            Sonatype Catalog
          </Tabs.Trigger>
        </Tabs.List>
      </Tabs.Root>

      <Flex align="center" justify="between" gap="3" wrap="wrap">
        <Text size="2" color="gray" data-testid="vulnerabilities-toolbar-count">
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
              placeholder="Search vulnerabilities…"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              data-testid="vulnerabilities-toolbar-search"
              aria-label="Search vulnerabilities"
            >
              <TextField.Slot>
                <ActionIcons.Search size={14} aria-hidden />
              </TextField.Slot>
            </TextField.Root>
          </form>

          <Flex align="center" gap="2" data-testid="vulnerabilities-toolbar-sort">
            <Text size="2" color="gray" as="label" htmlFor="vulnerabilities-toolbar-sort-select">
              Sort
            </Text>
            <Select.Root
              value={orderBy}
              onValueChange={(value) => onOrderByChange(value as VulnerabilitiesListOrderBy)}
            >
              <Select.Trigger
                id="vulnerabilities-toolbar-sort-select"
                variant="soft"
                color="gray"
              />
              {/* Popper: item-aligned Select collapses to ~1 option inside this page's overflow shell. */}
              <Select.Content position="popper">
                <Select.Item value="-cvssScore">
                  {vulnerabilitiesListOrderByLabel('-cvssScore')}
                </Select.Item>
                <Select.Item value="cvssScore">
                  {vulnerabilitiesListOrderByLabel('cvssScore')}
                </Select.Item>
              </Select.Content>
            </Select.Root>
          </Flex>

          <form
            action={getVulnerabilitiesExportUrl()}
            method="post"
            encType="multipart/form-data"
            data-testid="vulnerabilities-toolbar-export-form"
          >
            <input type="hidden" name="filter" value={exportPayloadJson} />
            <Button
              variant="outline"
              color="gray"
              size="2"
              type="submit"
              disabled={!canExport}
              title={csvExportTitle}
              aria-describedby={csvHintId}
              data-testid="vulnerabilities-toolbar-csv"
            >
              <ActionIcons.Download size={14} />
              CSV
            </Button>
          </form>
          {csvState === 'catalog' && (
            <VisuallyHidden id="vulnerabilities-toolbar-csv-catalog-hint">
              Sonatype Catalog export is not available. Export is available for My Scan Data only.
            </VisuallyHidden>
          )}
          {csvState === 'empty' && (
            <VisuallyHidden id="vulnerabilities-toolbar-csv-empty-hint">
              CSV export is unavailable when there are no vulnerabilities.
            </VisuallyHidden>
          )}
          {csvState === 'filtered' && (
            <VisuallyHidden id="vulnerabilities-toolbar-csv-filters-hint">
              CSV export uses the current search, severity, CVSS range, and ecosystem filters.
            </VisuallyHidden>
          )}
        </Flex>
      </Flex>
    </Flex>
  );
}
