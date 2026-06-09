/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Grid } from '@radix-ui/themes';
import { AppsScannedTile } from './tiles/AppsScannedTile';
import { LegalObligationsTile } from './tiles/LegalObligationsTile';
import { SeverityStripTile } from './tiles/SeverityStripTile';
import { TopPolicyViolationsTile } from './tiles/TopPolicyViolationsTile';
import { RiskOverTimeTile } from './tiles/RiskOverTimeTile';

import './DashboardOverviewContent.css';

/**
 * Dashboard Overview content (S2-PR-D-3 / CLM-39992 + extended in
 * S2-PR-D-5 / CLM-39641 with the F6 §9.3 deferred tiles).
 *
 * Pure-content component that renders the Dashboard tile grid
 * WITHOUT a Radix `<Theme>` wrapper, WITHOUT `position: fixed`, and
 * WITHOUT `usePreviewShellOffsets`. Both consumers — the standalone
 * `Dashboard.tsx` route AND the Overview tab inside
 * `PreviewDashboardPage.tsx` — wrap this in their own outer Theme +
 * shell offsets.
 *
 * Layout (per F6 §9.3 spec):
 *   row 1: [ Apps Scanned ]   [ Severity Strip (spans 2 cols) ]
 *   row 2: [ Legal Obligations ]   [ Top Policy Violations ]
 *   row 3: [ Risk Over Time (full width) ]
 *
 * The grid uses a 3-column track at `lg` so the Severity Strip can
 * span 2 columns; smaller breakpoints collapse to a 1-column stack
 * (default Radix behavior with `columns={{ initial: '1', sm: '2',
 * lg: '3' }}`). Tiles inside the strip flex-wrap so the strip degrades
 * to 2-up at narrow widths instead of overflowing.
 */
export default function DashboardOverviewContent(): JSX.Element {
  return (
    <Box data-testid="preview-dashboard-overview-content">
      <Grid
        columns={{ initial: '1', sm: '2', lg: '3' }}
        gap="4"
        align="stretch"
        data-testid="preview-dashboard-grid"
      >
        <Box style={{ gridColumn: 'span 1' }}>
          <AppsScannedTile />
        </Box>
        <Box className="grid-span-two-from-sm">
          <SeverityStripTile />
        </Box>

        <Box style={{ gridColumn: 'span 1' }}>
          <LegalObligationsTile />
        </Box>
        <Box className="grid-span-two-from-sm">
          <TopPolicyViolationsTile />
        </Box>

        <Box style={{ gridColumn: '1 / -1' }}>
          <RiskOverTimeTile />
        </Box>
      </Grid>
    </Box>
  );
}
