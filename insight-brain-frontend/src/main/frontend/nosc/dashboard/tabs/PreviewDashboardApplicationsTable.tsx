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
  VisuallyHidden,
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
 *   - CSV export (CLM-39992)
 *   - Pagination (CLM-39709)
 *
 * NO Nx* primitives in this file — the test asserts the rendered
 * DOM contains zero `class*="nx-"` nodes.
 */

const SKELETON_ROW_COUNT = 5;

export default function PreviewDashboardApplicationsTable(): JSX.Element {
  const dispatch = useDispatch();
  const apps = useSelector(selectPreviewApplications);
  const loading = useSelector(selectPreviewApplicationsLoading);
  const error = useSelector(selectPreviewApplicationsError);
  // Re-fetching on every mount would race the Classic
  // `LOAD_RESULTS_REQUESTED` reducer which clears `results` back to
  // null, momentarily flashing the Skeleton over already-loaded
  // rows (and re-fetching after an error or after an explicit empty
  // result is already a user action, not a mount side effect).
  // Dispatch only when the slice has not been populated yet — i.e.
  // the loading-flag selector is true (results===null && error===null).
  const shouldFetch = loading;

  useEffect(() => {
    if (shouldFetch) {
      dispatch(loadApplicationResults());
    }
    // Captured at mount only, intentionally: filter-driven refetches are
    // handled by the filter rail's own dispatch chain
    // (LOAD_FILTER_REQUESTED → RESET_ALL_TABS → loadApplicationResults).
    // Re-running this effect when `shouldFetch` flips true post-mount
    // would double-fire the thunk (once from filter middleware, once
    // here) and stomp on in-flight results.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch]);

  return (
    <Box data-testid="nosc-dashboard-applications-table">
      <Flex justify="end" gap="2" mb="3">
        <Tooltip content="Coming soon in Preview — CSV export (CLM-39992)">
          {/* Disabled native buttons swallow pointerenter, so the
              <span> wrapper gives Radix a real hover target. The
              VisuallyHidden sibling carries the Jira tracking ID
              once in DOM (Radix Tooltip duplicates its content into
              an aria-describedby clone, so the ID has to live
              outside the Tooltip to keep it uniquely findable). */}
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
        <VisuallyHidden data-testid="nosc-dashboard-applications-csv-jira">
          Tracked in CLM-39992.
        </VisuallyHidden>
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
            <Callout.Text>
              {error instanceof Error ? error.message : String(error)}
            </Callout.Text>
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
        <Tooltip content="Coming soon in Preview — pagination (CLM-39709)">
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
        <VisuallyHidden data-testid="nosc-dashboard-applications-pagination-jira">
          Tracked in CLM-39709.
        </VisuallyHidden>
      </Flex>
    </Box>
  );
}
