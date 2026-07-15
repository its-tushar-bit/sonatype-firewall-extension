/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import ViolationsToolbar from 'MainRoot/nosc/violations/ViolationsToolbar';
import { createDefaultViolationsFilterState } from 'MainRoot/nosc/violations/violationsListApi';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';

function filterState(overrides: Partial<ViolationsFilterState> = {}): ViolationsFilterState {
  return { ...createDefaultViolationsFilterState(), ...overrides };
}

function renderToolbar(props: {
  totalCount?: number;
  searchValue?: string;
  filters?: ViolationsFilterState;
  onSearchSubmit?: (term: string) => void;
} = {}) {
  const onSearchSubmit = props.onSearchSubmit ?? jest.fn();
  render(
    <Theme>
      <ViolationsToolbar
        totalCount={props.totalCount ?? 3}
        searchValue={props.searchValue ?? ''}
        onSearchSubmit={onSearchSubmit}
        filters={props.filters ?? filterState()}
      />
    </Theme>,
  );
  return { onSearchSubmit };
}

describe('ViolationsToolbar (CLM-42260)', () => {
  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  it('submits the trimmed search term on Enter', async () => {
    const user = userEvent.setup();
    const { onSearchSubmit } = renderToolbar();
    await user.type(screen.getByTestId('violations-toolbar-search'), '  log4j  {enter}');
    expect(onSearchSubmit).toHaveBeenCalledWith('log4j');
  });

  it('posts the CSV export to the Classic violations export endpoint', () => {
    renderToolbar({ totalCount: 3 });
    const form = screen.getByTestId('violations-toolbar-export-form');
    expect(form).toHaveAttribute('action', expect.stringContaining('/rest/dashboard/export/newestRisks'));
    expect(form).toHaveAttribute('method', 'post');
    expect(form).toHaveAttribute('encType', 'multipart/form-data');
  });

  it('serializes the active sidebar filters into the hidden export payload (search excluded)', () => {
    renderToolbar({
      totalCount: 3,
      searchValue: 'log4j',
      filters: filterState({
        states: new Set(['OPEN']),
        threatCategories: new Set(['security']),
        threatRange: [4, 10],
      }),
    });
    const filterInput = screen
      .getByTestId('violations-toolbar-export-form')
      .querySelector('input[name="filter"]') as HTMLInputElement;
    const payload = JSON.parse(filterInput.value);
    expect(payload).toEqual({
      orderBy: '-policyThreatLevel',
      policyViolationStates: ['OPEN'],
      policyThreatCategories: 'security',
      policyThreatLevelRange: { minPolicyThreatLevel: 4, maxPolicyThreatLevel: 10 },
    });
    // Free-text search is index-only and must never leak into the export payload.
    expect(filterInput.value).not.toContain('log4j');
  });

  it('disables the CSV button when there are zero results', () => {
    renderToolbar({ totalCount: 0 });
    expect(screen.getByTestId('violations-toolbar-csv')).toBeDisabled();
    expect(screen.getByTestId('violations-toolbar-csv')).toHaveAttribute('title', 'No violations to export');
  });

  it('warns that all violations export when search is active with no sidebar filters', () => {
    renderToolbar({ totalCount: 3, searchValue: 'log4j' });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toBeEnabled();
    // No filters are active, so the export includes everything — the caveat must say so.
    expect(csv).toHaveAttribute(
      'title',
      'Search is not included in the CSV and no sidebar filters are active, so all violations are exported',
    );
    const hintId = csv.getAttribute('aria-describedby');
    expect(hintId).toBe('violations-toolbar-csv-search-hint');
    expect(document.getElementById(hintId as string)).toHaveTextContent('all violations are exported');
  });

  it('keeps CSV enabled with a filter-only caveat when an active search narrows results to zero', () => {
    // totalCount is search-narrowed but the export streams the filter-only set, so a zero-result search
    // with active filters must not disable a still-valid filter-only export.
    renderToolbar({ totalCount: 0, searchValue: 'nomatch', filters: filterState({ states: new Set(['OPEN']) }) });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toBeEnabled();
    expect(csv).toHaveAttribute(
      'title',
      'Exports sidebar filters only — search term is not included in the CSV',
    );
    expect(document.getElementById('violations-toolbar-csv-search-hint')).toHaveTextContent(
      'search term is not included in the CSV',
    );
  });

  it('disables CSV when a zero-result search has no active filters (no export-everything)', () => {
    // Guards the bot-flagged case: an active search alone must not enable an export of the entire
    // unfiltered violation set.
    renderToolbar({ totalCount: 0, searchValue: 'nomatch' });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toBeDisabled();
    expect(csv).toHaveAttribute('title', 'No violations to export');
  });

  it('wires the empty-state CSV hint to the disabled button for screen readers', () => {
    renderToolbar({ totalCount: 0 });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toHaveAttribute('aria-describedby', 'violations-toolbar-csv-empty-hint');
    expect(document.getElementById('violations-toolbar-csv-empty-hint')).toHaveTextContent(
      'CSV export is unavailable when there are no violations.',
    );
  });

  it('reflects the total count', () => {
    renderToolbar({ totalCount: 1 });
    expect(screen.getByTestId('violations-toolbar-count')).toHaveTextContent('1 violation');
  });
});
