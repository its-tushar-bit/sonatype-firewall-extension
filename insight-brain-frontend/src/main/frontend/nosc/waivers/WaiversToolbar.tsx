/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Button, Flex, Select, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import type { AnaWaiverRow } from 'MainRoot/nosc/waivers/waiversListTypes';
import {
  WaiversListOrderBy,
  waiversListOrderByLabel,
} from 'MainRoot/nosc/waivers/waiversListQuery';
import { downloadWaiversCsv } from 'MainRoot/nosc/waivers/waiversListExport';

export interface WaiversToolbarProps {
  readonly totalCount: number;
  readonly exactTotalEstimate: boolean;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly orderBy: WaiversListOrderBy;
  readonly onOrderByChange: (orderBy: WaiversListOrderBy) => void;
  /** Rows currently rendered on the page — used by the page-scope CSV export. */
  readonly currentPageWaivers: ReadonlyArray<AnaWaiverRow>;
}

const SORT_OPTIONS: ReadonlyArray<WaiversListOrderBy> = [
  '-policyWaiverCreatedAt',
  'policyWaiverCreatedAt',
  '-policyWaiverThreatLevel',
  'policyWaiverThreatLevel',
  'expiration',
];

function formatTotalLabel(totalCount: number, exact: boolean): string {
  const suffix = totalCount === 1 ? 'waiver' : 'waivers';
  if (!exact) return `${totalCount}+ ${suffix}`;
  return `${totalCount} ${suffix}`;
}

/**
 * Toolbar row for the Ana Waivers list (CLM-43204). Search submits on Enter and drives the
 * index-query {@code query} filter; sort switches the wire {@code sort} token (default:
 * newest first). CSV exports the CURRENT page rows only — there is no backend export path.
 */
export default function WaiversToolbar({
  totalCount,
  exactTotalEstimate,
  searchValue,
  onSearchSubmit,
  orderBy,
  onOrderByChange,
  currentPageWaivers,
}: WaiversToolbarProps): JSX.Element {
  const [draft, setDraft] = useState(searchValue);

  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  const csvTitle = 'Exports the waivers currently on this page. Sort or paginate to change the range.';

  return (
    <Flex align="center" justify="between" gap="3" wrap="wrap" data-testid="waivers-toolbar">
      <Flex align="center" gap="3" flexGrow="1" minWidth="240px">
        <form
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            onSearchSubmit(draft.trim());
          }}
          style={{ flex: 1, maxWidth: '360px' }}
        >
          <TextField.Root
            placeholder="Search waivers..."
            aria-label="Search waivers"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            data-testid="waivers-toolbar-search"
            style={{ width: '100%' }}
          >
            <TextField.Slot>
              <ActionIcons.Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </form>

        <Flex align="center" gap="2" data-testid="waivers-toolbar-sort">
          <Text size="2" color="gray" as="label" htmlFor="waivers-toolbar-sort-select">
            Sort
          </Text>
          <Select.Root
            value={orderBy}
            onValueChange={(value) => onOrderByChange(value as WaiversListOrderBy)}
          >
            <Select.Trigger
              id="waivers-toolbar-sort-select"
              variant="soft"
              color="gray"
            />
            {/* Popper: item-aligned Select collapses to ~1 option inside this page's overflow shell. */}
            <Select.Content position="popper">
              {SORT_OPTIONS.map((option) => (
                <Select.Item key={option} value={option}>
                  {waiversListOrderByLabel(option)}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
        </Flex>
      </Flex>

      <Flex align="center" gap="3">
        <Button
          variant="outline"
          color="gray"
          size="2"
          type="button"
          onClick={() => downloadWaiversCsv(currentPageWaivers)}
          data-testid="waivers-toolbar-csv"
          title={csvTitle}
          disabled={currentPageWaivers.length === 0}
        >
          <ActionIcons.Download size={14} />
          CSV
        </Button>
        <Text size="2" color="gray" data-testid="waivers-toolbar-count">
          {formatTotalLabel(totalCount, exactTotalEstimate)}
        </Text>
      </Flex>
    </Flex>
  );
}
