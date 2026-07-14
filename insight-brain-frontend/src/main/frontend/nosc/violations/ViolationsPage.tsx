/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Button, Flex, Heading, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { ViolationRow, ViolationsListFacets } from 'MainRoot/nosc/violations/violationListTypes';
import ViolationsFilterRail from 'MainRoot/nosc/violations/ViolationsFilterRail';
import ViolationsToolbar from 'MainRoot/nosc/violations/ViolationsToolbar';
import ViolationCardGrid from 'MainRoot/nosc/violations/ViolationCardGrid';

import './violationsPageLayout.css';

export interface ViolationsPageProps {
  readonly violations: ReadonlyArray<ViolationRow>;
  readonly facets?: ViolationsListFacets;
  readonly labels?: {
    readonly organizations: Readonly<Record<string, string>>;
    readonly applications: Readonly<Record<string, string>>;
  };
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly onRetry?: () => void;
  /** Total matching violations across all pages (for the toolbar count + pagination). */
  readonly totalCount: number;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  /** 1-based current page. */
  readonly page: number;
  readonly pageSize: number;
  readonly onPageChange: (nextPage: number) => void;
}

/**
 * Martha V1 Violations page shell (CLM-42257): filter rail + toolbar + card list + pagination inside
 * the Nexus One Preview shell. Data is wired to POST /rest/dashboard/violations/list by
 * {@link ViolationsList}; filter interactions land in CLM-42258 and CSV export in CLM-42260.
 */
export default function ViolationsPage({
  violations,
  facets,
  labels,
  loading = false,
  error = null,
  onRetry,
  totalCount,
  searchValue,
  onSearchSubmit,
  page,
  pageSize,
  onPageChange,
}: ViolationsPageProps): JSX.Element {
  const offsets = usePreviewShellOffsets();
  // TODO: when interactive filters land, revisit pagination visibility for filter-only empty pages
  // (e.g. zero rows on the current page while facet counts still show matches elsewhere).
  const showPagination = totalCount > pageSize || page > 1;

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
      <main data-testid="preview-violations-page">
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" gap="3">
            <DomainIcons.Vulnerability size={28} color="var(--accent-9)" />
            <Heading size="6">Violations</Heading>
          </Flex>
          <Text size="2" color="gray">
            Policy violations across every application visible to your account, highest threat first.
          </Text>
        </Flex>

        <Flex gap="4" align="start" wrap="wrap" data-testid="violations-page-layout">
          <ViolationsFilterRail facets={facets} labels={labels} />
          <Box className="violations-page__content" data-testid="violations-page-content">
            <Flex direction="column" gap="4">
              <ViolationsToolbar
                totalCount={totalCount}
                searchValue={searchValue}
                onSearchSubmit={onSearchSubmit}
              />

              <AsyncPageState
                loading={loading}
                error={error}
                onRetry={onRetry}
                loadingTestId="violations-list-loading"
                errorTestId="violations-list-error"
                errorTitle="Failed to load violations"
                errorVariant="banner"
              >
                {violations.length === 0 ? (
                  <Flex
                    direction="column"
                    align="center"
                    gap="2"
                    py="8"
                    data-testid="violations-list-empty"
                  >
                    <DomainIcons.Vulnerability size={32} color="var(--gray-9)" />
                    <Text size="3" color="gray">
                      {searchValue ? 'No violations match your search.' : 'No violations to display.'}
                    </Text>
                    <Text size="2" color="gray">
                      {searchValue
                        ? 'Try a different search term or clear the search.'
                        : 'Violations visible to your account will appear here once data is loaded.'}
                    </Text>
                    {/* TODO: when interactive filters land, add a "Reset filters" action for filter-only
                        empties and tailor the copy for search vs filter causes. For now, clearing the
                        committed search is the only reset available in V1. */}
                    {searchValue && (
                      <Button
                        variant="soft"
                        size="2"
                        mt="1"
                        onClick={() => onSearchSubmit('')}
                        data-testid="violations-empty-clear-search"
                      >
                        <ActionIcons.Refresh size={14} aria-hidden />
                        Clear search
                      </Button>
                    )}
                  </Flex>
                ) : (
                  <ViolationCardGrid violations={violations} />
                )}

                {/* Rendered independent of the empty/grid branch so an out-of-range page (page > 1
                    with an empty result after a shrunk total) can still page back. Both this control
                    and the empty state sit inside AsyncPageState, which returns only the skeleton while
                    loading — so the initial (totalCount=0) fetch shows no empty-state or pagination
                    flash before data arrives. */}
                {showPagination && (
                  <Pagination
                    page={page}
                    pageSize={pageSize}
                    totalItems={totalCount}
                    onPageChange={onPageChange}
                    data-testid="violations-pagination"
                  />
                )}
              </AsyncPageState>
            </Flex>
          </Box>
        </Flex>
      </main>
    </Box>
  );
}
