/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Box,
  Button,
  Callout,
  Flex,
  Skeleton,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { loadApplicationResults } from 'MainRoot/dashboard/results/dashboardResultsActions';
import {
  selectPreviewApplications,
  selectPreviewApplicationsError,
  selectPreviewApplicationsLoading,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardApplicationsSelectors';
import { PREVIEW_APPLICATIONS_COLUMNS } from 'MainRoot/nosc/dashboard/tabs/previewDashboardApplicationsColumns';
import PreviewDashboardApplicationsRow from 'MainRoot/nosc/dashboard/tabs/PreviewDashboardApplicationsRow';
import {
  SKELETON_ROW_COUNT,
  usePreviewDashboardFilterGate,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardFilterGate';

/**
 * Radix-native rewrite of the Classic
 * DashboardApplicationsContainer → DashboardApplicationsTable chain
 * for use inside the Preview Dashboard "Applications" tab.
 *
 * Data: reads `state.dashboard.applications` via typed selectors and
 * dispatches the Classic `loadApplicationResults` thunk on mount.
 * No parallel slice, no new HTTP endpoint.
 *
 * Behavior parity with Classic (per spec §3.3):
 *   - column set + order (asserted by parity test)
 *   - per-application + per-stage rows
 *   - loading (Skeleton), error (Callout), empty (Text)
 *   - app-name → Preview app-detail link
 *   - stage-name → Classic per-stage report link (the only
 *     report surface that exists today)
 *
 * Deferred to follow-ups (visible Coming-Soon affordances):
 *   - CSV export
 *   - Pagination
 *
 * NO Nx* primitives in this file — the test asserts the rendered
 * DOM contains zero `class*="nx-"` nodes.
 */

export default function PreviewDashboardApplicationsTable(): JSX.Element {
  const dispatch = useDispatch();
  const apps = useSelector(selectPreviewApplications);
  const loading = useSelector(selectPreviewApplicationsLoading);
  const error = useSelector(selectPreviewApplicationsError);
  const { filterLoading, needsAcknowledgement } = usePreviewDashboardFilterGate();

  useEffect(() => {
    // Wait for the Classic dashboardFilter rail to finish loading. Firing
    // pre-filter sends a malformed payload to /rest/dashboard/policy/
    // applicationRisks and the backend returns 400 → "Failed to load".
    // Filter-driven refetches go through the rail's dispatch chain
    // (RESET_ALL_TABS → loadApplicationResults). Only bootstrap the
    // first fetch here when the slice is still empty.
    if (!filterLoading && !needsAcknowledgement && loading) {
      dispatch(loadApplicationResults());
    }
    // `loading` is read above but intentionally excluded from deps:
    // LOAD_RESULTS_REQUESTED keeps `loading === true` for the whole in-flight
    // request, so re-running on `loading` would re-dispatch and loop. We
    // bootstrap once when the slice is empty; the guard prevents re-entry.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch, filterLoading, needsAcknowledgement]);

  return (
    <Box data-testid="nosc-dashboard-applications-table">
      <Flex justify="end" gap="2" mb="3">
        <Tooltip content="Coming soon in Preview — CSV export">
          <span>
            <Button
              variant="soft"
              color="gray"
              disabled
              data-testid="nosc-dashboard-applications-csv"
            >
              <ActionIcons.Download size={16} />
              CSV export
            </Button>
          </span>
        </Tooltip>
      </Flex>

      <Table.Root variant="surface">
        <Table.Header>
          <Table.Row>
            {PREVIEW_APPLICATIONS_COLUMNS.map((col) => (
              <Table.ColumnHeaderCell
                key={col.id}
                justify={col.align === 'right' ? 'end' : 'start'}
              >
                {col.title}
              </Table.ColumnHeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {loading &&
            Array.from({ length: SKELETON_ROW_COUNT }).map((_, i) => (
              <Table.Row
                key={`skeleton-${i}`}
                data-testid="nosc-dashboard-applications-skeleton-row"
              >
                {PREVIEW_APPLICATIONS_COLUMNS.map((col) => (
                  <Table.Cell key={col.id}>
                    <Skeleton width="60px" height="14px" />
                  </Table.Cell>
                ))}
              </Table.Row>
            ))}
          {!loading &&
            apps.map((app) => (
              <PreviewDashboardApplicationsRow
                key={app.applicationId}
                application={app}
              />
            ))}
        </Table.Body>
      </Table.Root>

      {!loading && error && (
        <Box mt="3">
          <Callout.Root color="red" data-testid="nosc-dashboard-applications-error">
            <Callout.Icon>
              <ActionIcons.AlertCircle size={16} />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        </Box>
      )}

      {!loading && !error && apps.length === 0 && (
        <Box mt="6">
          <Flex justify="center">
            <Text color="gray">No applications match the current filters.</Text>
          </Flex>
        </Box>
      )}

      <Flex justify="center" mt="3" gap="2" align="center">
        <Tooltip content="Coming soon in Preview — pagination">
          <span>
            <Button
              variant="ghost"
              color="gray"
              disabled
              data-testid="nosc-dashboard-applications-pagination"
            >
              Pagination
            </Button>
          </span>
        </Tooltip>
      </Flex>
    </Box>
  );
}
