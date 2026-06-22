/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Badge,
  Box,
  Button,
  Callout,
  Flex,
  Link,
  Skeleton,
  Table,
  Text,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { violationSidebarHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { loadViolationResults } from 'MainRoot/dashboard/results/dashboardResultsActions';
import { previewDashboardComponentLabel } from 'MainRoot/nosc/dashboard/tabs/previewDashboardComponentLabel';
import {
  PreviewDashboardViolation,
  selectPreviewViolations,
  selectPreviewViolationsError,
  selectPreviewViolationsLoading,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardViolationsSelectors';
import { formatAgeFromMs } from 'MainRoot/nosc/dashboard/tabs/previewDashboardViolationFormat';
import { threatColor } from 'MainRoot/nosc/dashboard/tabs/previewDashboardSeverity';
import {
  SKELETON_ROW_COUNT,
  usePreviewDashboardFilterGate,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardFilterGate';

/**
 * Radix-native rewrite of the Classic DashboardViolations table for
 * use inside the Preview Dashboard "Violations" tab.
 *
 * Data: reads `state.dashboard.violations` via typed selectors and
 * dispatches the Classic `loadViolationResults` thunk on mount. No
 * parallel slice, no new HTTP endpoint. Filter rail changes still
 * propagate through the Classic dispatch chain.
 *
 * Click-through: each row navigates to the Classic violation-detail
 * sidebar via the shared `violationSidebarHref` helper — there is no
 * Preview-side violation detail page yet. This matches the Classic
 * row's behavior of dispatching `stateGo('sidebarView.violation', …)`
 * but via a plain `<Link href>` so the user stays in the Nexus One
 * shell until they explicitly click into Classic detail.
 *
 * Column set (fixed, non-sortable in this Phase-1 preview):
 *   Threat | Policy | Application | Component | Age
 *
 * Scope note: unlike the Classic DashboardViolations table this preview does
 * not (yet) provide CSV export, pagination/load-more, or sortable columns —
 * these are deferred (see PreviewViolationsTab doc + PR description).
 */

function violationDetailHref(v: PreviewDashboardViolation): string {
  // No Preview-side violation detail page yet; deep-link to Classic via the shared, context-path-aware helper.
  return violationSidebarHref(v.policyViolationId);
}

// The Classic dashboardFilter rail must finish loading (and not be awaiting acknowledgement) before the
// results load is dispatched: firing pre-filter sends a malformed payload and the backend returns 400 →
// "Failed to load". Shared with the other Preview tables via usePreviewDashboardFilterGate.

export default function PreviewDashboardViolationsTable(): JSX.Element {
  const dispatch = useDispatch();
  const violations = useSelector(selectPreviewViolations);
  const loading = useSelector(selectPreviewViolationsLoading);
  const error = useSelector(selectPreviewViolationsError);
  const { filterLoading, needsAcknowledgement } = usePreviewDashboardFilterGate();

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement && loading) {
      dispatch(loadViolationResults());
    }
    // `loading` is read above but intentionally excluded from deps:
    // LOAD_RESULTS_REQUESTED keeps `loading === true` for the whole in-flight
    // request, so re-running on `loading` would re-dispatch and loop. We
    // bootstrap once when the slice is empty; the guard prevents re-entry.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch, filterLoading, needsAcknowledgement]);

  if (loading) {
    return (
      <Box data-testid="nosc-dashboard-violations-table-loading">
        <Skeleton height="36px" mb="2" />
        {Array.from({ length: SKELETON_ROW_COUNT }).map((_, i) => (
          <Skeleton key={i} height="40px" mb="1" />
        ))}
      </Box>
    );
  }

  if (error) {
    return (
      <Callout.Root color="red" data-testid="nosc-dashboard-violations-table-error">
        <Callout.Icon>
          <ActionIcons.AlertCircle />
        </Callout.Icon>
        <Callout.Text>
          {error}{' '}
          <Button
            variant="ghost"
            size="1"
            onClick={() => dispatch(loadViolationResults())}
          >
            Retry
          </Button>
        </Callout.Text>
      </Callout.Root>
    );
  }

  if (violations.length === 0) {
    return (
      <Box p="6" data-testid="nosc-dashboard-violations-table-empty">
        <Text size="3" color="gray">
          No violations match the current filters.
        </Text>
      </Box>
    );
  }

  return (
    <Box data-testid="nosc-dashboard-violations-table">
      <Table.Root variant="surface" size="2">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Application</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Age</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {violations.map((v) => (
            <Table.Row
              key={v.policyViolationId}
              data-testid="nosc-dashboard-violations-row"
            >
              <Table.Cell>
                <Badge color={threatColor(v.threatLevel)} variant="soft" radius="full">
                  {v.threatLevel}
                </Badge>
              </Table.Cell>
              <Table.Cell>
                <Link
                  href={violationDetailHref(v)}
                  underline="hover"
                  data-testid="nosc-dashboard-violations-row-detail-link"
                >
                  <Text size="2">{v.policyName}</Text>
                </Link>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{v.applicationName}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{previewDashboardComponentLabel(v)}</Text>
              </Table.Cell>
              <Table.Cell justify="end">
                <Text size="2" color="gray">{formatAgeFromMs(v.firstOccurrenceTime)}</Text>
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}
