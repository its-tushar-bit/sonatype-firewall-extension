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
import { VIOLATIONS_CLASSIC_EXPORT_ORDER_BY } from 'MainRoot/nosc/violations/violationsListExport';
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
      // Classic newestRisks export sorts on the wire field name (RisksFilterDTO), not the UI alias.
      orderBy: VIOLATIONS_CLASSIC_EXPORT_ORDER_BY,
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
      "The search term won't be applied to the CSV and no other sidebar filters are active, so all violations are exported",
    );
    const hintId = csv.getAttribute('aria-describedby');
    expect(hintId).toBe('violations-toolbar-csv-caveat-hint');
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
      "Exports sidebar filters only — the search term won't be applied to the CSV",
    );
    expect(document.getElementById('violations-toolbar-csv-caveat-hint')).toHaveTextContent(
      "search term won't be applied to the CSV",
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

  it('warns that all violations export when only the waiver-type filter is active (CLM-42261)', () => {
    // The waiver-type filter is index-only (RisksFilterDTO has no field for it), so an auto-waived
    // selection alone can't narrow the CSV — the caveat must flag the export-everything case.
    renderToolbar({ totalCount: 3, filters: filterState({ waiverType: 'AUTO' }) });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toBeEnabled();
    expect(csv).toHaveAttribute(
      'title',
      "The waiver-type filter won't be applied to the CSV and no other sidebar filters are active, so all violations are exported",
    );
    expect(document.getElementById('violations-toolbar-csv-caveat-hint')).toHaveTextContent(
      'all violations are exported',
    );
  });

  it('gives a filter-only caveat when the waiver-type filter rides alongside an exportable filter (CLM-42261)', () => {
    renderToolbar({
      totalCount: 3,
      filters: filterState({ waiverType: 'MANUAL', states: new Set(['WAIVED']) }),
    });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toBeEnabled();
    expect(csv).toHaveAttribute(
      'title',
      "Exports sidebar filters only — the waiver-type filter won't be applied to the CSV",
    );
  });

  it('names both search and waiver-type in the caveat when both are active (CLM-42261)', () => {
    renderToolbar({
      totalCount: 3,
      searchValue: 'log4j',
      filters: filterState({ waiverType: 'AUTO' }),
    });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toHaveAttribute(
      'title',
      "The search term and waiver-type filter won't be applied to the CSV and no other sidebar filters are active, so all violations are exported",
    );
  });

  it('disables CSV when only the waiver-type filter is active and it narrows to zero (CLM-42261)', () => {
    // Waiver-type-only + zero results + no exportable filters must not enable an export-everything action.
    renderToolbar({ totalCount: 0, filters: filterState({ waiverType: 'AUTO' }) });
    const csv = screen.getByTestId('violations-toolbar-csv');
    expect(csv).toBeDisabled();
    expect(csv).toHaveAttribute('title', 'No violations to export');
  });

  it('never leaks the waiver-type filter into the export payload (CLM-42261)', () => {
    renderToolbar({ totalCount: 3, filters: filterState({ waiverType: 'AUTO', states: new Set(['WAIVED']) }) });
    const filterInput = screen
      .getByTestId('violations-toolbar-export-form')
      .querySelector('input[name="filter"]') as HTMLInputElement;
    const payload = JSON.parse(filterInput.value);
    expect(payload).not.toHaveProperty('waivedWithAutoWaiver');
    expect(filterInput.value).not.toContain('waivedWithAutoWaiver');
    // The exportable filter still serializes normally.
    expect(payload.policyViolationStates).toEqual(['WAIVED']);
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
