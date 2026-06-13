/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Heading, Link, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { useWaiversList } from './useWaivers';
import WaiversTable from './WaiversTable';

/**
 * Native Nexus One Waivers list (CLM-39545 / CLM-39709).
 *
 * Mounted at `/waivers`. Reads live from
 * POST /rest/dashboard/policy/policyWaivers?includeAutoWaivers=true with an
 * empty filter (up to 100 most recent waivers across the IQ instance, scoped by
 * the caller's effective permissions).
 *
 * TODO(CLM-39709): sort, filter sidebar, and multi-page pagination are
 * follow-ups; waiver creation stays in Classic (linked from each violation).
 */
export default function WaiversListPage() {
  const offsets = usePreviewShellOffsets();
  const classicWaiverRequestsHref = bundleIndexUrl('classic', '/dashboard/waiverRequests');
  const { loading, error, waivers, hasNextPage, refetch } = useWaiversList({
    includeAutoWaivers: true,
  });

  return (
    // Radix Theme is provided once by NexusOneShellLayout; render content into a
    // fixed, scrollable <main> region below the shell chrome.
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
      <main data-testid="preview-waivers-page">
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" justify="between">
            <Flex align="center" gap="3">
              <DomainIcons.Waivers size={28} color="var(--accent-9)" />
              <Heading size="6">Waivers</Heading>
            </Flex>
            <Flex align="center" gap="4">
              {!loading && !error && (
                <Text size="2" color="gray" data-testid="preview-waivers-count">
                  {waivers.length}
                  {hasNextPage ? '+' : ''} {waivers.length === 1 ? 'waiver' : 'waivers'} in scope
                </Text>
              )}
              <Link size="2" href={classicWaiverRequestsHref} data-testid="preview-waivers-classic-link">
                Open in Classic
              </Link>
            </Flex>
          </Flex>
          <Text size="2" color="gray">
            Active policy-violation waivers across all applications and organizations you can see. Auto-generated
            waivers are included.
          </Text>
        </Flex>

        <WaiversTable
          waivers={waivers}
          loading={loading}
          error={error}
          onRetry={refetch}
          testId="nosc-waivers-list-table"
        />

        {!loading && !error && hasNextPage && (
          <Flex justify="center" mt="4">
            <Text size="2" color="gray" data-testid="preview-waivers-truncated">
              Showing first {waivers.length} waivers. Open in Classic for full pagination + filter sidebar.
            </Text>
          </Flex>
        )}
      </main>
    </Box>
  );
}
