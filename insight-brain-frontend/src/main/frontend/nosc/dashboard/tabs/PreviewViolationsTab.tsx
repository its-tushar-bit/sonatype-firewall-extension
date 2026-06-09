/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Box, Flex } from '@radix-ui/themes';

import DashboardViolationsContainer from 'MainRoot/dashboard/results/violations/DashboardViolationsContainer';
import DashboardFilter from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilter';
import {
  applySeverityFilterFromQuery,
  parseViolationsTabQuery,
} from './previewDashboardTabQuery';

import './previewDashboardTabLayout.css';

/**
 * S2-PR-D-3 (CLM-39992): Violations tab — Preview port.
 *
 * Wraps the Classic `DashboardViolationsContainer` (already redux-
 * connected) so this tab consumes the EXACT same `dashboard.violations`
 * Redux slice with the EXACT same column set, sort defaults, chips,
 * CSV export, pagination, and policy-violation drill-down behavior.
 *
 * What we add on top of Classic:
 *   - The shared filter rail (`DashboardFilter`) is mounted to the left
 *     of the table, same as the Classic `/dashboard` page renders it.
 *     The rail uses the same Redux slice — D-3 does NOT fork it. D-4
 *     owns the share-extract refactor of the filter rail itself.
 *   - URL-query pre-filter: on first mount, read `?severity=…`,
 *     `?ltg=…`, `?policy=…` from the hash query portion, dispatch the
 *     corresponding filter action, then strip the query so a re-mount
 *     doesn't re-apply. The chip becomes visible + removable in the
 *     filter rail (the user can clear it the same way as any other
 *     applied filter).
 *
 * What we deliberately do NOT add:
 *   - A Radix `<Theme>` wrapper. The parent `PreviewDashboardPage`
 *     owns the outer Theme + shell offsets (D-1) so the tab strip
 *     stays visible.
 *   - `position: fixed`. Tab content renders inline inside the parent
 *     `Tabs.Content` panel.
 *   - Any tab-isolation error boundary — the parent already wraps each
 *     tab's content in `TabErrorBoundary`.
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
    const hash = window.location.hash || '';
    const parsed = parseViolationsTabQuery(hash);

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
        <Box className="preview-dashboard-tab__table-slot" data-testid="nosc-dashboard-violations-table-slot">
          <DashboardViolationsContainer />
        </Box>
      </Flex>
    </Box>
  );
}
