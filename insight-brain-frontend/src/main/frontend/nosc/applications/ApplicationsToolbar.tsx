/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Button, Flex, Select, Text, TextField } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { getApplicationRisksExportUrl } from 'MainRoot/util/CLMLocation';
import { buildApplicationsListExportPayload } from 'MainRoot/nosc/applications/applicationsListExport';
import type { ApplicationsListFilterState } from 'MainRoot/nosc/applications/applicationsListFilters';
import {
  type ApplicationsListOrderBy,
  applicationsListOrderByLabel,
} from 'MainRoot/nosc/applications/applicationsListQuery';

export interface ApplicationsToolbarProps {
  readonly totalCount: number;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly orderBy: ApplicationsListOrderBy;
  readonly onOrderByChange: (orderBy: ApplicationsListOrderBy) => void;
  readonly filters: ApplicationsListFilterState;
}

/**
 * Toolbar row for the Martha V1 Applications page (CLM-42226).
 *
 * Search submits on Enter and drives the list API {@code search} field. Sort toggles between the
 * two validator-supported {@code orderBy} tokens. CSV export posts the active sidebar filters to
 * the Classic {@code /rest/dashboard/export/applicationRisks} endpoint (search is index-only and is
 * not included in the export payload). Stage filters follow Classic matching and may differ from
 * the violation-scoped apps shown in the Martha list.
 */
export default function ApplicationsToolbar({
  totalCount,
  searchValue,
  onSearchSubmit,
  orderBy,
  onOrderByChange,
  filters,
}: ApplicationsToolbarProps): JSX.Element {
  const [draft, setDraft] = useState(searchValue);

  useEffect(() => {
    setDraft(searchValue);
  }, [searchValue]);

  const exportPayloadJson = useMemo(
    () => JSON.stringify(buildApplicationsListExportPayload(filters, orderBy)),
    [filters, orderBy],
  );
  const csvExportTitle = useMemo(() => {
    const caveats: string[] = [
      // Classic PostgreSQL export rejects lastEvaluationTime; payload remaps to TOTAL_RISK.
      'sorted by total risk (not evaluation time)',
    ];
    if (searchValue.trim().length > 0) {
      caveats.push('search term is not included');
    }
    if (filters.stageIds.size > 0) {
      caveats.push('stage filter uses Classic matching and may differ from this list');
    }
    return `CSV export caveat: ${caveats.join('; ')}`;
  }, [searchValue, filters.stageIds]);

  return (
    <Flex
      align="center"
      justify="between"
      gap="3"
      wrap="wrap"
      data-testid="applications-toolbar"
    >
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
            placeholder="Search applications..."
            aria-label="Search applications"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            data-testid="applications-toolbar-search"
            style={{ width: '100%' }}
          >
            <TextField.Slot>
              <ActionIcons.Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </form>

        <Flex align="center" gap="2" data-testid="applications-toolbar-sort">
          <Text size="2" color="gray" as="label" htmlFor="applications-toolbar-sort-select">
            Sort
          </Text>
          <Select.Root
            value={orderBy}
            onValueChange={(value) => onOrderByChange(value as ApplicationsListOrderBy)}
          >
            <Select.Trigger
              id="applications-toolbar-sort-select"
              variant="soft"
              color="gray"
            />
            <Select.Content>
              <Select.Item value="-lastEvaluationTime">
                {applicationsListOrderByLabel('-lastEvaluationTime')}
              </Select.Item>
              <Select.Item value="lastEvaluationTime">
                {applicationsListOrderByLabel('lastEvaluationTime')}
              </Select.Item>
            </Select.Content>
          </Select.Root>
        </Flex>
      </Flex>

      <Flex align="center" gap="3">
        <form
          action={getApplicationRisksExportUrl()}
          method="post"
          encType="multipart/form-data"
          data-testid="applications-toolbar-export-form"
        >
          <input type="hidden" name="filter" value={exportPayloadJson} />
          <Button
            variant="outline"
            color="gray"
            size="2"
            type="submit"
            data-testid="applications-toolbar-csv"
            title={csvExportTitle}
          >
            <ActionIcons.Download size={14} />
            CSV
          </Button>
        </form>
        <Text size="2" color="gray" data-testid="applications-toolbar-count">
          {totalCount} {totalCount === 1 ? 'application' : 'applications'}
        </Text>
      </Flex>
    </Flex>
  );
}
