/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Flex } from '@radix-ui/themes';

import DashboardWaiversTable from 'MainRoot/dashboard/results/waivers/DashboardWaiversTable';
import DashboardFilter from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilter';

import './previewDashboardTabLayout.css';

/**
 * S2-PR-D-3 (CLM-39992): Waivers tab — Preview port.
 *
 * Wraps the Classic `DashboardWaiversTable` (already self-connected to
 * the `dashboard.waivers` Redux slice via internal useSelector/useDispatch)
 * so the columns, sort defaults, chips, CSV export, and pagination match
 * Classic exactly.
 *
 * URL-query parity note (D-3 spec asked about `?status={…}`): Classic's
 * waivers table does NOT expose an active/expired/expiringSoon filter —
 * the closest first-class facet is the Expiration Date radio
 * (`expirationDate` slice key, options ALL/AUTO/IN_24_HOURS/IN_7_DAYS/
 * IN_30_DAYS/IN_90_DAYS/IN_OVER_90_DAYS — see `staticFilterEntries.js`).
 * Per D-3 spec ("If no such filter exists, just port without query
 * support and document"), the Waivers tab does NOT consume a `?status=`
 * query in this PR. D-5 (tile→tab IA wiring) can add a Waivers-tab URL
 * contract if/when a destination tile needs one.
 *
 * Same shell rules as `PreviewViolationsTab`: NO Theme wrapper, NO
 * `position: fixed`, NO tab-isolation boundary (parent owns those).
 */
export default function PreviewWaiversTab(): JSX.Element {
  return (
    <Box mt="4" data-testid="nosc-dashboard-waivers-tab">
      <Flex gap="4" align="start" data-testid="nosc-dashboard-waivers-layout">
        <Box data-testid="nosc-dashboard-waivers-filter-slot">
          <DashboardFilter />
        </Box>
        <Box className="preview-dashboard-tab__table-slot" data-testid="nosc-dashboard-waivers-table-slot">
          <DashboardWaiversTable />
        </Box>
      </Flex>
    </Box>
  );
}
