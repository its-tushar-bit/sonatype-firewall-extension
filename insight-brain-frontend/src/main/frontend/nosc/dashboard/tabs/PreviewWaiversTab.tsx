/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';

import { useWaiversList } from 'MainRoot/nosc/waivers/useWaivers';
import WaiversTable from 'MainRoot/nosc/waivers/WaiversTable';

/**
 * S2-PR-D-3 (CLM-39992): Waivers tab — Preview port.
 *
 * Renders the **native Nexus One** WaiversTable (NOT the Classic
 * DashboardWaiversTable). Reading the same `/rest/dashboard/policy/
 * policyWaivers` endpoint as the standalone /waivers page, so
 * rows are clickable and navigate to /waivers/{ownerType}/
 * {ownerId}/{waiverId} — staying inside the Nexus One shell.
 *
 * **Design note:** the Classic DashboardWaiversTable was used in the
 * initial port (S2-PR-D-3) but its rows dispatch a ui-router state
 * change to Classic's `waiver.details`, which drops the user out of
 * the Preview shell. Per design feedback, this PR replaces that wrap
 * with the native nosc table.
 *
 * **Filter rail trade-off:** the shared filter rail
 * (`DashboardFilter`) is intentionally NOT mounted on this tab. The
 * rail is wired to the Classic `dashboard.waivers` Redux slice, but
 * the nosc table reads from `useWaiversList` — a different code path
 * with no Redux filter integration. Showing a non-functional filter
 * sidebar would be more confusing than helpful; the Phase-2 plan adds
 * native filter chips to the nosc WaiversTable itself.
 *
 * Open-in-Classic escape hatch ships in the standalone
 * /waivers page (see WaiversListPage.tsx), so users who need
 * Classic's full filter chrome have a one-click path.
 */
export default function PreviewWaiversTab(): JSX.Element {
  const { loading, error, waivers, hasNextPage, refetch } = useWaiversList({
    includeAutoWaivers: true,
  });

  return (
    <Box mt="4" data-testid="nosc-dashboard-waivers-tab">
      <Box>
        <WaiversTable
          waivers={waivers}
          loading={loading}
          error={error}
          onRetry={refetch}
          testId="nosc-dashboard-waivers-table"
          // Mark row links as originating in the Dashboard tab so the
          // detail page's back-link routes back here, not to the
          // standalone /waivers page.
          linkFrom="dashboard"
        />
        {!loading && !error && hasNextPage && (
          <Flex justify="center" mt="4">
            <Text size="2" color="gray" data-testid="nosc-dashboard-waivers-truncated">
              Showing first {waivers.length} waivers. Open the standalone Waivers
              page (LeftNav → Waivers) or Classic for full pagination.
            </Text>
          </Flex>
        )}
      </Box>
    </Box>
  );
}
