/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { PreviewDashboardApplication } from './previewDashboardApplicationsSelectors';

/**
 * Column order must match the Classic `DashboardApplicationsTable`
 * column order. Keep this list in lock-step with
 * `dashboard/results/applications/DashboardApplicationsTable.jsx`.
 * The parity check is performed by the table's Jest spec, which
 * asserts the rendered column headers match the titles in this
 * array in this exact order.
 */
export type PreviewApplicationsColumnId =
  | 'name'
  | 'total'
  | 'critical'
  | 'severe'
  | 'moderate'
  | 'low';

export type PreviewApplicationsColumn = {
  id: PreviewApplicationsColumnId;
  title: string;
  /** Sort key on the per-row data shape. `'applicationName'` for
   *  the name column (alphabetic). All numeric columns sort on a
   *  field of `totalApplicationRisk`. */
  sortKey:
    | 'applicationName'
    | keyof PreviewDashboardApplication['totalApplicationRisk'];
  align: 'left' | 'right';
};

export const PREVIEW_APPLICATIONS_COLUMNS = [
  { id: 'name', title: 'Application', sortKey: 'applicationName', align: 'left' },
  { id: 'total', title: 'Risk', sortKey: 'totalRisk', align: 'right' },
  { id: 'critical', title: 'Critical', sortKey: 'criticalRisk', align: 'right' },
  { id: 'severe', title: 'Severe', sortKey: 'severeRisk', align: 'right' },
  { id: 'moderate', title: 'Moderate', sortKey: 'moderateRisk', align: 'right' },
  { id: 'low', title: 'Low', sortKey: 'lowRisk', align: 'right' },
] as const;
