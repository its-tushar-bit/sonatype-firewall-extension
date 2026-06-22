/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Box, Flex } from '@radix-ui/themes';

import PreviewDashboardViolationsTable from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardViolationsTable';
import DashboardFilter from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilter';
import {
  applySeverityFilterFromQuery,
  parseViolationsTabQuery,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardTabQuery';

/**
 * Preview Dashboard — Violations tab.
 *
 * Renders `PreviewDashboardViolationsTable`, which reads the same Classic
 * `dashboard.violations` Redux slice (so the filter rail and drill-down behave
 * like Classic) but is a Phase-1 preview presentation: a fixed Threat / Policy /
 * Application / Component / Age column set.
 *
 * Intentionally NOT carried over from the Classic `DashboardViolationsContainer`
 * in this preview (deferred — see PR description / CLM-40018 follow-up):
 *   - CSV export
 *   - pagination / load-more beyond the first results page
 *   - sortable / configurable columns and chips
 *
 * On first mount, reads `?severity=…`, `?ltg=…`, `?policy=…` from the hash
 * query portion and dispatches matching filter actions. Query params stay in
 * the URL so bookmarks and back/forward preserve the deep-link state.
 */

export default function PreviewViolationsTab(): JSX.Element {
  const dispatch = useDispatch();
  const handledQueryRef = useRef(false);

  useEffect(() => {
    if (handledQueryRef.current) {
      return;
    }
    handledQueryRef.current = true;
    if (typeof window === 'undefined') {
      return;
    }
    const parsed = parseViolationsTabQuery(window.location.hash || '');

    applySeverityFilterFromQuery(dispatch, parsed.severity);

    // Legacy `?ltg=` / `?policy=` query params remain in the URL until the
    // violations slice grows matching facets (CLM-40018 follow-up).
    if (parsed.ltg || parsed.policy) {
      // intentionally no-op until CLM-40018 facet work lands
    }
  }, [dispatch]);

  return (
    <Box mt="4" data-testid="nosc-dashboard-violations-tab">
      <Flex gap="4" align="start" data-testid="nosc-dashboard-violations-layout">
        <Box data-testid="nosc-dashboard-violations-filter-slot">
          <DashboardFilter />
        </Box>
        <Box style={{ flex: 1, minWidth: 0 }} data-testid="nosc-dashboard-violations-table-slot">
          <PreviewDashboardViolationsTable />
        </Box>
      </Flex>
    </Box>
  );
}
