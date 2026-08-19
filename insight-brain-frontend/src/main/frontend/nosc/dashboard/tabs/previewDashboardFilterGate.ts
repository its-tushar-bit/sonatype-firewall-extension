/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useSelector } from 'react-redux';

/**
 * Minimal shape of the Classic `dashboardFilter` slice (defined in plain JS at
 * `MainRoot/dashboard/filter/dashboardFilterReducer.js`, so there is no canonical TS type to import) that the
 * Preview dashboard tables depend on. Declared once here so the tables share one structural contract instead of
 * each redeclaring it.
 */
export interface DashboardFilterLike {
  loading?: boolean;
  needsAcknowledgement?: boolean;
}

export interface RootStateWithFilter {
  dashboardFilter?: DashboardFilterLike;
}

/** Shared skeleton row count for the Preview dashboard tables. */
export const SKELETON_ROW_COUNT = 5;

/**
 * Reads the `dashboardFilter` gating flags shared by every Preview dashboard table: results must not be
 * (re)loaded while the filter rail is still loading or is awaiting user acknowledgement.
 */
export function usePreviewDashboardFilterGate(): {
  filterLoading: boolean;
  needsAcknowledgement: boolean;
} {
  const filterLoading = useSelector(
    (s: RootStateWithFilter): boolean => s.dashboardFilter?.loading ?? false,
  );
  const needsAcknowledgement = useSelector(
    (s: RootStateWithFilter): boolean => s.dashboardFilter?.needsAcknowledgement ?? false,
  );
  return { filterLoading, needsAcknowledgement };
}
