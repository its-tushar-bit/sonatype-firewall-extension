/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Box, Flex } from '@radix-ui/themes';

import DashboardComponentsContainer from 'MainRoot/dashboard/results/components/DashboardComponentsContainer';
import DashboardFilter from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilter';
import { toggleFilter } from 'MainRoot/dashboard/filter/dashboardFilterActions';
import {
  applySeverityFilterFromQuery,
  parseComponentsTabQuery,
} from './previewDashboardTabQuery';

import './previewDashboardTabLayout.css';

/**
 * S2-PR-D-4 (CLM-39992): Components tab — Preview port.
 *
 * Wraps the Classic `DashboardComponentsContainer` (already redux-
 * connected) so this tab consumes the EXACT same `dashboard.components`
 * Redux slice with the EXACT same column set, sort defaults, chips,
 * CSV export, pagination, and component drill-down behavior. We do
 * NOT fork the slice and we do NOT modify the Classic table — the
 * whole point of the wrap pattern is parity.
 *
 * What we add on top of Classic:
 *   - The shared filter rail (`DashboardFilter`) is mounted to the
 *     left of the table, same as the Classic `/dashboard/components`
 *     page renders it. The rail uses the same Redux slice — the
 *     filter-load itself is dispatched at the page level (D-4)
 *     so all four tabs share one in-flight load.
 *   - URL-query pre-filter: on first mount, read `?severity=…` and
 *     `?policy=…` from the hash query portion, dispatch the
 *     corresponding filter action, then strip the query so a re-mount
 *     doesn't re-apply. The chip becomes visible + removable in the
 *     filter rail (the user can clear it the same way as any other
 *     applied filter).
 *
 * URL query contract (for D-5 tile→tab wiring):
 *   - `?severity={critical|severe|moderate|low}` → policyThreatLevels
 *     band [min, max] inclusive (8–10 / 4–7 / 2–3 / 0–1). Matches
 *     `PreviewViolationsTab`'s contract.
 *   - `?policy={id}` → policyTypes selectedIds Set containing the
 *     verbatim id. Same forward-compat note as Violations: D-5 will
 *     refine to the exact slice key when the destination filter
 *     facet is finalized; until then the chip is visible + dismissible.
 *
 * What we deliberately do NOT add:
 *   - A Radix `<Theme>` wrapper. The parent `PreviewDashboardPage`
 *     owns the outer Theme + shell offsets so the tab strip stays
 *     visible.
 *   - `position: fixed`. Tab content renders inline inside the
 *     parent `Tabs.Content` panel.
 *   - Any tab-isolation error boundary — the parent already wraps
 *     each tab's content in `TabErrorBoundary`.
 *   - A page-level `loadFilter()` dispatch — that lives on the
 *     parent (`PreviewDashboardPage`) so opening the filter drawer
 *     from any of the four tabs sees data, not a spinner.
 */

export { parseComponentsTabQuery } from './previewDashboardTabQuery';

export default function PreviewComponentsTab(): JSX.Element {
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
    const parsed = parseComponentsTabQuery(hash);

    applySeverityFilterFromQuery(dispatch, parsed.severity);

    if (parsed.policy) {
      dispatch(toggleFilter('policyTypes', new Set([parsed.policy])));
    }

  }, [dispatch]);

  return (
    <Box mt="4" data-testid="nosc-dashboard-components-tab">
      <Flex gap="4" align="start" data-testid="nosc-dashboard-components-layout">
        <Box data-testid="nosc-dashboard-components-filter-slot">
          <DashboardFilter />
        </Box>
        <Box className="preview-dashboard-tab__table-slot" data-testid="nosc-dashboard-components-table-slot">
          <DashboardComponentsContainer />
        </Box>
      </Flex>
    </Box>
  );
}
