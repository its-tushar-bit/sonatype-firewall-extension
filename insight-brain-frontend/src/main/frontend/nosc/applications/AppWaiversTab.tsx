/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useState } from 'react';
import { Box, Button, Flex, Link as RadixLink, Text } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import WaiversTable from 'MainRoot/nosc/waivers/WaiversTable';
import { useWaiversList } from 'MainRoot/nosc/waivers/useWaivers';
import { DEFAULT_WAIVERS_PAGE_SIZE } from 'MainRoot/nosc/waivers/noscWaiversSlice';
import { classicHref } from './applicationDetailUtils';

/**
 * Waivers tab inside the Application Detail page. Reads live waivers
 * from POST /rest/dashboard/policy/policyWaivers?includeAutoWaivers=true
 * with `applicationIds=[applicationInternalId]`, which restricts the
 * dashboard query to waivers whose scope is this application or any
 * ancestor org/root that applies.
 *
 * Server-side pagination (Goldman V1) — no index changes required.
 */
interface AppWaiversTabProps {
  applicationInternalId: string | undefined;
  publicId: string;
}

export function AppWaiversTab({
  applicationInternalId,
  publicId,
}: AppWaiversTabProps): JSX.Element {
  const [page, setPage] = useState(0);

  const { loading, error, waivers, hasNextPage, refetch } = useWaiversList({
    applicationInternalId,
    includeAutoWaivers: true,
    page,
    pageSize: DEFAULT_WAIVERS_PAGE_SIZE,
  });

  const totalItemsOnPage = waivers.length;
  const showPagination = page > 0 || hasNextPage;
  const showPaginationBar = showPagination && !error && (totalItemsOnPage > 0 || loading);
  const firstShown = totalItemsOnPage === 0 ? 0 : page * DEFAULT_WAIVERS_PAGE_SIZE + 1;
  const lastShown = page * DEFAULT_WAIVERS_PAGE_SIZE + totalItemsOnPage;
  const paginationBusy = loading && totalItemsOnPage === 0;

  return (
    <Box pt="3" data-testid="nosc-app-detail-waivers-tab">
      <Flex justify="between" align="center" mb="3" wrap="wrap" gap="3">
        <Text size="2" color="gray">
          Active waivers that apply to <strong>{publicId}</strong> — including
          waivers inherited from parent organizations and the root.
        </Text>
        <RadixLink
          size="2"
          href={classicHref(`/management/view/application/${encodeURIComponent(publicId)}/waivers`)}
          data-testid="nosc-app-detail-waivers-classic-link"
        >
          Manage in Classic →
        </RadixLink>
      </Flex>
      <WaiversTable
        waivers={waivers}
        loading={loading || !applicationInternalId}
        error={error}
        onRetry={refetch}
        emptyMessage="No waivers apply to this application"
        emptySubMessage="Waivers created on this application or any parent organization will appear here. Use 'Manage in Classic' to add a new one from a violation."
        testId="nosc-app-detail-waivers-table"
      />
      {showPaginationBar && (
        <Flex align="center" justify="between" p="3" data-testid="nosc-app-detail-waivers-pagination">
          <Text size="2" color="gray">
            {paginationBusy
              ? 'Loading waivers…'
              : `Showing ${firstShown}–${lastShown}${hasNextPage ? '+' : ''} waivers`}
          </Text>
          <Flex align="center" gap="2">
            <Button
              size="1"
              variant="soft"
              color="gray"
              disabled={page <= 0 || paginationBusy}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              aria-label="Previous page"
            >
              <ActionIcons.ChevronLeft size={14} />
              Prev
            </Button>
            <Text size="2" color="gray">
              Page {page + 1}
            </Text>
            <Button
              size="1"
              variant="soft"
              color="gray"
              disabled={!hasNextPage || paginationBusy}
              onClick={() => setPage((p) => p + 1)}
              aria-label="Next page"
            >
              Next
              <ActionIcons.ChevronRight size={14} />
            </Button>
          </Flex>
        </Flex>
      )}
    </Box>
  );
}
