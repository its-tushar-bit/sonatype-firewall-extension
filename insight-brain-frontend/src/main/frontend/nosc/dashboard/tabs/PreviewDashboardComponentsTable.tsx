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
  Skeleton,
  Table,
  Text,
} from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { loadComponentResults } from 'MainRoot/dashboard/results/dashboardResultsActions';
import {
  selectPreviewComponents,
  selectPreviewComponentsError,
  selectPreviewComponentsLoading,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardComponentsSelectors';
import { previewDashboardComponentLabel } from 'MainRoot/nosc/dashboard/tabs/previewDashboardComponentLabel';
import { severityColor, ComponentScoreKind } from 'MainRoot/nosc/dashboard/tabs/previewDashboardSeverity';
import {
  SKELETON_ROW_COUNT,
  usePreviewDashboardFilterGate,
} from 'MainRoot/nosc/dashboard/tabs/previewDashboardFilterGate';

/**
 * Radix-native rewrite of the Classic DashboardComponents table.
 *
 * Column set mirrors Classic exactly:
 *   Component | Apps | Risk | Critical | Severe | Moderate | Low
 *
 * Backend `/rest/dashboard/policy/componentRisks` returns
 * `{ dashboardResults, hasNextPage }`. The Classic dashboard reducer
 * stores the rows under `state.dashboard.components.results`. Per-row
 * fields: derivedComponentName (string), affectedApplications,
 * score, scoreCritical/Severe/Moderate/Low, displayName (DTO),
 * filename, componentIdentifier.
 *
 * Click-through: no Preview-side component-detail page yet, so the
 * row is non-clickable for this PR (Classic uses
 * `stateGo('dashboard.component', {hash})` — we'd drop out of the
 * Preview shell, which the previous fix to PreviewWaiversTab argued
 * against doing silently).
 */

function ScoreBadge({
  value,
  kind,
}: {
  value: number | undefined;
  kind: ComponentScoreKind;
}): JSX.Element {
  return (
    <Badge color={severityColor(value, kind)} variant="soft" radius="full">
      {value ?? 0}
    </Badge>
  );
}

export default function PreviewDashboardComponentsTable(): JSX.Element {
  const dispatch = useDispatch();
  const components = useSelector(selectPreviewComponents);
  const loading = useSelector(selectPreviewComponentsLoading);
  const error = useSelector(selectPreviewComponentsError);
  const { filterLoading, needsAcknowledgement } = usePreviewDashboardFilterGate();

  useEffect(() => {
    if (!filterLoading && !needsAcknowledgement && loading) {
      dispatch(loadComponentResults());
    }
    // `loading` is read above but intentionally excluded from deps:
    // LOAD_RESULTS_REQUESTED keeps `loading === true` for the whole in-flight
    // request, so re-running on `loading` would re-dispatch and loop. We
    // bootstrap once when the slice is empty; the guard prevents re-entry.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch, filterLoading, needsAcknowledgement]);

  if (loading) {
    return (
      <Box data-testid="nosc-dashboard-components-table-loading">
        <Skeleton height="36px" mb="2" />
        {Array.from({ length: SKELETON_ROW_COUNT }).map((_, i) => (
          <Skeleton key={i} height="40px" mb="1" />
        ))}
      </Box>
    );
  }

  if (error) {
    return (
      <Callout.Root color="red" data-testid="nosc-dashboard-components-table-error">
        <Callout.Icon>
          <ActionIcons.AlertCircle />
        </Callout.Icon>
        <Callout.Text>
          {error}{' '}
          <Button
            variant="ghost"
            size="1"
            onClick={() => dispatch(loadComponentResults())}
          >
            Retry
          </Button>
        </Callout.Text>
      </Callout.Root>
    );
  }

  if (components.length === 0) {
    return (
      <Box p="6" data-testid="nosc-dashboard-components-table-empty">
        <Text size="3" color="gray">
          No components match the current filters.
        </Text>
      </Box>
    );
  }

  return (
    <Box data-testid="nosc-dashboard-components-table">
      <Table.Root variant="surface" size="2">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Apps</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Risk</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Critical</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Severe</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Moderate</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell justify="end">Low</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {components.map((c, idx) => (
            <Table.Row
              key={c.hash ?? `${previewDashboardComponentLabel(c)}-${idx}`}
              data-testid="nosc-dashboard-components-row"
            >
              <Table.RowHeaderCell>
                <Text size="2">{previewDashboardComponentLabel(c)}</Text>
              </Table.RowHeaderCell>
              <Table.Cell justify="end">
                <Text size="2" color="gray">{c.affectedApplications ?? 0}</Text>
              </Table.Cell>
              <Table.Cell justify="end">
                <ScoreBadge value={c.score} kind="total" />
              </Table.Cell>
              <Table.Cell justify="end">
                <ScoreBadge value={c.scoreCritical} kind="crit" />
              </Table.Cell>
              <Table.Cell justify="end">
                <ScoreBadge value={c.scoreSevere} kind="sev" />
              </Table.Cell>
              <Table.Cell justify="end">
                <ScoreBadge value={c.scoreModerate} kind="mod" />
              </Table.Cell>
              <Table.Cell justify="end">
                <ScoreBadge value={c.scoreLow} kind="low" />
              </Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}
