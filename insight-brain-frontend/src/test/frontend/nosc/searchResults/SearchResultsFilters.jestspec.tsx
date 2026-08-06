/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { SearchResultsFilters } from 'MainRoot/nosc/searchResults/SearchResultsFilters';
import { FacetBucket } from 'MainRoot/nosc/search/searchTypes';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

expect.extend(toHaveNoViolations);

beforeAll(() => installRadixJsdomShims());

function renderRail(props: Partial<React.ComponentProps<typeof SearchResultsFilters>> = {}) {
  const onQueryChange = jest.fn();
  const onReset = jest.fn();
  render(
    <Theme>
      <SearchResultsFilters
        tab="VIOLATION"
        facets={null}
        query="jackson"
        onQueryChange={onQueryChange}
        onReset={onReset}
        resetEnabled={false}
        {...props}
      />
    </Theme>
  );
  return { onQueryChange, onReset };
}

const VIOLATION_FACETS: Record<string, FacetBucket[]> = {
  states: [
    { value: 'OPEN', displayName: 'Open', count: 8940 },
    { value: 'WAIVED', displayName: 'Waived', count: 583 },
  ],
  policyTypes: [
    { value: 'SECURITY', displayName: 'Security', count: 6290 },
    { value: 'LICENSE', displayName: 'License', count: 2324 },
  ],
  organizations: [{ value: 'Sandbox Organization', count: 12 }],
};

describe('SearchResultsFilters — per-tab facet rail (CLM-42453)', () => {
  it('renders a facet section per key with checkbox rows and count badges', () => {
    renderRail({ facets: VIOLATION_FACETS });

    // Section label from the prototype mapping, exposed as the group's accessible name.
    expect(screen.getByRole('group', { name: 'Violation State' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Policy Types' })).toBeInTheDocument();
    // A checkbox row + its count badge.
    expect(screen.getByRole('checkbox', { name: 'Open' })).toBeInTheDocument();
    expect(screen.getByText('8,940')).toBeInTheDocument();
  });

  it('orders sections to the prototype rail order (states before policyTypes before orgs)', () => {
    renderRail({ facets: VIOLATION_FACETS });
    const sections = screen
      .getAllByRole('group')
      .map((el) => el.getAttribute('aria-labelledby'))
      .map((id) => (id ? document.getElementById(id)?.textContent : null))
      .filter((label) => label !== 'Policy Threat Level');
    expect(sections).toEqual(['Violation State', 'Policy Types', 'Organizations']);
  });

  it('selecting a checkbox adds the field:value predicate and re-queries', async () => {
    const user = userEvent.setup();
    const { onQueryChange } = renderRail({ facets: VIOLATION_FACETS });

    await user.click(screen.getByRole('checkbox', { name: 'Security' }));
    expect(onQueryChange).toHaveBeenCalledWith('jackson policyViolationThreatCategory:SECURITY');
  });

  it('unchecking a selected checkbox removes the predicate', async () => {
    const user = userEvent.setup();
    const { onQueryChange } = renderRail({
      facets: VIOLATION_FACETS,
      query: 'jackson policyViolationThreatCategory:SECURITY',
    });

    await user.click(screen.getByRole('checkbox', { name: 'Security' }));
    expect(onQueryChange).toHaveBeenCalledWith('jackson');
  });

  it('renders the Policy Threat Level slider on entity tabs', () => {
    renderRail({ facets: VIOLATION_FACETS });
    expect(screen.getByRole('group', { name: 'Policy Threat Level' })).toBeInTheDocument();
    // A dual-handle range slider exposes one slider role per handle.
    expect(screen.getAllByRole('slider')).toHaveLength(2);
  });

  it('does not render an Age section (the grammar has no `age` field)', () => {
    // An `age:` predicate compiles to a no-op and surfaces an "unknown filter"
    // warning to the user, so the section is not offered at all.
    renderRail({ tab: 'APPLICATION', facets: { organizations: VIOLATION_FACETS.organizations } });
    expect(screen.queryByRole('group', { name: 'Age' })).not.toBeInTheDocument();
    expect(screen.queryByText('Age')).not.toBeInTheDocument();
  });

  it('writes the active tab\'s threat-level field, not one fixed field', async () => {
    // Each tab indexes the threat level under its own field; the wrong field
    // compiles to a match-nothing clause with no warning and empties the results.
    for (const [tab, field] of [
      ['VIOLATION', 'policyViolationThreatLevel'],
      ['APPLICATION', 'applicationMaxPolicyThreatLevel'],
      ['COMPONENT', 'componentMaxPolicyThreatLevel'],
      ['WAIVER', 'policyWaiverThreatLevel'],
    ] as const) {
      const { onQueryChange } = renderRail({ tab, query: `jackson ${field}:[3 TO 7]` });
      const group = screen.getAllByRole('group', { name: 'Policy Threat Level' }).at(-1);
      // The slider reads its committed range back from the tab's own field.
      expect(group).toHaveTextContent('3 – 7');
      expect(onQueryChange).not.toHaveBeenCalled();
    }
  });

  it('renders the WAIVER status buckets as a read-only counts list, with no checkboxes', () => {
    // No bucket has a working predicate, so the section renders labels + counts rather
    // than checkboxes that can never be ticked. A permanently-disabled control looks
    // identical to a working one and gives no reason it cannot be used.
    renderRail({
      tab: 'WAIVER',
      facets: {
        status: [
          { value: 'active', displayName: 'Active', count: 12 },
          { value: 'expired', displayName: 'Expired', count: 3 },
        ],
      },
    });
    const group = screen.getByRole('group', { name: 'Status' });
    expect(group).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: 'Active' })).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: 'Expired' })).not.toBeInTheDocument();
    // The counts are still shown -- the section is informative, just not filterable,
    // and it says so.
    expect(group).toHaveTextContent('Active');
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(group).toHaveTextContent(/informational and cannot be filtered on/i);
  });

  it('offers waiverType AUTO as a checkbox and MANUAL as a read-only count', () => {
    // MANUAL has no distinct grammar value: a predicate for it would be the same token
    // states.WAIVED emits, so ticking either control would show both as checked.
    renderRail({
      tab: 'WAIVER',
      facets: {
        waiverType: [
          { value: 'AUTO', displayName: 'Automatic', count: 4 },
          { value: 'MANUAL', displayName: 'Manual', count: 9 },
        ],
      },
    });
    expect(screen.getByRole('checkbox', { name: 'Automatic' })).toBeEnabled();
    expect(screen.queryByRole('checkbox', { name: 'Manual' })).not.toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Waiver Type' })).toHaveTextContent('Manual');
    expect(screen.getByText('9')).toBeInTheDocument();
  });

  it('explains the unfilterable bucket in a mixed section and describes the row', () => {
    // A mixed section (Auto toggles, Manual has no token) must still explain why Manual
    // carries no control, otherwise the bare label + count reads as a broken filter to
    // sighted and AT users alike.
    renderRail({
      tab: 'WAIVER',
      facets: {
        waiverType: [
          { value: 'AUTO', displayName: 'Automatic', count: 4 },
          { value: 'MANUAL', displayName: 'Manual', count: 9 },
        ],
      },
    });
    const group = screen.getByRole('group', { name: 'Waiver Type' });
    expect(group).toHaveTextContent(/cannot be filtered on/i);

    // The read-only row points at that note, so the explanation is announced with the row
    // rather than only being visible above the section.
    const manualRow = screen.getByTestId('nosc-search-facet-waiverType-readonly-MANUAL');
    const noteId = manualRow.getAttribute('aria-describedby');
    expect(noteId).toBeTruthy();
    expect(document.getElementById(noteId as string)).toHaveTextContent(/cannot be filtered on/i);

    // The toggleable bucket in the same section keeps its working checkbox.
    expect(screen.getByRole('checkbox', { name: 'Automatic' })).toBeEnabled();
  });

  it('renders no checkbox for a bucket value the grammar cannot express', () => {
    // The parser's quote reader has no escape handling, so a backslash-bearing value has
    // no round-trippable predicate; the bucket must not offer a filter that breaks on click.
    renderRail({
      tab: 'VIOLATION',
      facets: {
        organizations: [
          { value: 'Clean Org', count: 5 },
          { value: 'Odd\\Org', count: 2 },
        ],
      },
    });
    expect(screen.getByRole('checkbox', { name: 'Clean Org' })).toBeEnabled();
    expect(screen.queryByRole('checkbox', { name: 'Odd\\Org' })).not.toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Organizations' })).toHaveTextContent('Odd\\Org');
  });

  it('explains why the reset button is disabled', () => {
    // A disabled control gives AT no reason on its own.
    renderRail({ facets: VIOLATION_FACETS, resetEnabled: false });
    const reset = screen.getByRole('button', { name: /reset filters/i });
    expect(reset).toBeDisabled();
    const describedBy = reset.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy as string)).toHaveTextContent('No filters applied');
  });

  it('drops the reset description once a reset would do something', () => {
    renderRail({ facets: VIOLATION_FACETS, resetEnabled: true });
    const reset = screen.getByRole('button', { name: /reset filters/i });
    expect(reset).toBeEnabled();
    expect(reset).not.toHaveAttribute('aria-describedby');
  });

  it('renders no threat-level slider on a tab without a threat-level field', () => {
    renderRail({ tab: 'VULNERABILITY', facets: { organizations: VIOLATION_FACETS.organizations } });
    expect(screen.queryByRole('group', { name: 'Policy Threat Level' })).not.toBeInTheDocument();
    expect(screen.queryByRole('slider')).not.toBeInTheDocument();
  });

  it('shows only the slider for threatLevel, never a duplicate checkbox section', () => {
    // WAIVER emits a bucketed `threatLevel` facet. Rendered as checkboxes it would
    // sit next to the slider under the same "Policy Threat Level" heading.
    renderRail({
      tab: 'WAIVER',
      facets: {
        threatLevel: [
          { value: '7', count: 4 },
          { value: '9', count: 2 },
        ],
      },
    });
    // Exactly one section owns the heading, and it is the slider (not checkboxes).
    expect(screen.getAllByRole('group', { name: 'Policy Threat Level' })).toHaveLength(1);
    expect(screen.getAllByRole('slider')).toHaveLength(2);
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('renders nothing when every facet key is unrenderable and the tab has no client section', () => {
    // An empty bucket list / unknown key must not reserve a rail that shows only
    // the Reset button.
    renderRail({
      tab: 'VULNERABILITY',
      facets: { organizations: [], somethingNew: [{ value: 'x', count: 1 }] },
    });
    expect(screen.queryByRole('button', { name: /reset filters/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
  });

  it('the Reset filters button calls onReset when enabled', async () => {
    const user = userEvent.setup();
    const { onReset } = renderRail({ facets: VIOLATION_FACETS, resetEnabled: true });
    const reset = screen.getByRole('button', { name: /reset filters/i });
    expect(reset).not.toBeDisabled();
    await user.click(reset);
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it('disables Reset filters when the parent reports nothing to reset', () => {
    // The parent owns the decision: a reset also returns to page 1, so it can be
    // meaningful past page 1 even with no predicates in the query.
    renderRail({ facets: VIOLATION_FACETS, resetEnabled: false });
    expect(screen.getByRole('button', { name: /reset filters/i })).toBeDisabled();
  });

  it('renders nothing when there are no facets and no client-side section (VULNERABILITY, no facets)', () => {
    renderRail({ tab: 'VULNERABILITY', facets: null });
    expect(screen.queryByRole('button', { name: /reset filters/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
  });

  it('reflects an already-selected predicate as a checked box', () => {
    renderRail({ facets: VIOLATION_FACETS, query: 'jackson policyViolationThreatCategory:SECURITY' });
    expect(screen.getByRole('checkbox', { name: 'Security' })).toBeChecked();
  });

  it('names every checkbox and section, with no axe violations', async () => {
    // The checkbox is a <button role="checkbox">, so a wrapping <label> does not name
    // it; each section is a labelled group. Both are asserted here so a regression
    // that strips the naming surfaces as a failure rather than a silent a11y loss.
    const { container } = render(
      <Theme>
        <SearchResultsFilters
          tab="VIOLATION"
          facets={VIOLATION_FACETS}
          query="jackson"
          onQueryChange={jest.fn()}
          onReset={jest.fn()}
          resetEnabled
        />
      </Theme>
    );
    expect(await axe(container)).toHaveNoViolations();
  });
});
