/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Box, Flex } from '@radix-ui/themes';

import PreviewDashboardApplicationsTable from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsTable';
import DashboardFilter from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilter';
import { toggleFilter } from 'MainRoot/dashboard/filter/dashboardFilterActions';
import {
  parseApplicationsTabQuery,
} from './previewDashboardTabQuery';

import './previewDashboardTabLayout.css';

/**
 * S2-PR-D-4 (CLM-39992): Applications tab — Preview port.
 *
 * Renders the new Radix `PreviewDashboardApplicationsTable` as the
 * right-hand table on this tab. The Radix
 * table consumes the same `dashboard.applications` Redux slice the
 * Classic grid did, so chips, sort, pagination, and the underlying
 * filter facets stay in sync — only the rendering layer is swapped.
 *
 * What we add on top of the table:
 *   - The shared filter rail (`DashboardFilter`) is mounted to the
 *     left of the table, same as the Classic
 *     `/dashboard/applications` page renders it. The filter-load
 *     itself is dispatched at the parent page level (D-4) so all
 *     four tabs share one in-flight load.
 *   - URL-query pre-filter: on first mount, read `?org=…`,
 *     `?stage=…`, and `?policy=…` from the hash query portion,
 *     dispatch the corresponding filter action, then strip the
 *     query so a re-mount doesn't re-apply.
 *
 * URL query contract (for D-5 tile→tab wiring):
 *   - `?org={id}`     → `organizations` selectedIds Set containing
 *     the verbatim id. Cleanest match — the slice has a first-class
 *     `organizations` facet (see `defaultFilter.js`).
 *   - `?stage={slug}` → `stages` selectedIds Set containing the
 *     verbatim slug. The slice has a first-class `stages` facet;
 *     the slug is passed through as-is so D-5 can wire to whatever
 *     id form the stages API returns (build / stage-release /
 *     release / operate are common values).
 *   - `?policy={id}`  → `policyTypes` selectedIds Set containing
 *     the verbatim id. Same forward-compat note as Components/
 *     Violations: D-5 can swap to the exact slice key when the
 *     destination filter facet is finalized.
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
 *     parent (`PreviewDashboardPage`).
 */

export { parseApplicationsTabQuery } from './previewDashboardTabQuery';

export default function PreviewApplicationsTab(): JSX.Element {
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
    const parsed = parseApplicationsTabQuery(hash);

    if (parsed.org) {
      dispatch(toggleFilter('organizations', new Set([parsed.org])));
    }

    if (parsed.stage) {
      dispatch(toggleFilter('stages', new Set([parsed.stage])));
    }

    if (parsed.policy) {
      dispatch(toggleFilter('policyTypes', new Set([parsed.policy])));
    }

  }, [dispatch]);

  return (
    <Box mt="4" data-testid="nosc-dashboard-applications-tab">
      <Flex gap="4" align="start" data-testid="nosc-dashboard-applications-layout">
        <Box data-testid="nosc-dashboard-applications-filter-slot">
          <DashboardFilter />
        </Box>
        <Box
          className="preview-dashboard-tab__table-slot"
          data-testid="nosc-dashboard-applications-table-slot"
        >
          <PreviewDashboardApplicationsTable />
        </Box>
      </Flex>
    </Box>
  );
}
