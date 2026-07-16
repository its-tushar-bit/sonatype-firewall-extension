/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import ViolationsFilterRail from 'MainRoot/nosc/violations/ViolationsFilterRail';
import { MOCK_VIOLATIONS_LIST_RESPONSE } from 'MainRoot/nosc/violations/mockViolationsListData';
import {
  createDefaultViolationsFilterState,
  deriveViolationFacetLabels,
} from 'MainRoot/nosc/violations/violationsListApi';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => {
  // The rail renders Radix Slider + ScrollArea, which need ResizeObserver / Pointer Capture shims.
  installRadixJsdomShims();
});

const FACETS = MOCK_VIOLATIONS_LIST_RESPONSE.facets;
const LABELS = deriveViolationFacetLabels(MOCK_VIOLATIONS_LIST_RESPONSE.violations);

function renderRail(overrides: Partial<React.ComponentProps<typeof ViolationsFilterRail>> = {}) {
  const props = {
    facets: FACETS,
    labels: LABELS,
    selected: createDefaultViolationsFilterState(),
    onToggle: jest.fn(),
    onWaiverTypeChange: jest.fn(),
    onThreatRangeChange: jest.fn(),
    onReset: jest.fn(),
    ...overrides,
  };
  render(
    <Theme>
      <ViolationsFilterRail {...props} />
    </Theme>,
  );
  return props;
}

function withStates(...states: string[]): ViolationsFilterState {
  return { ...createDefaultViolationsFilterState(), states: new Set(states) };
}

describe('ViolationsFilterRail', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  it('renders state, policy-type, stage, org, and app groups with facet counts', () => {
    renderRail();
    expect(screen.getByTestId('violations-filter-state')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-policy-type')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-stages')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-applications')).toBeInTheDocument();

    // Count badge for the OPEN state facet (mock: states { OPEN: 2, WAIVED: 1 }).
    const openOption = screen.getByTestId('violations-filter-state-option-OPEN');
    expect(within(openOption.closest('label') as HTMLElement).getByText('2')).toBeInTheDocument();
  });

  it('omits LEGACY_VIOLATION from the state facet', () => {
    renderRail({
      facets: { totalViolations: 3, states: { OPEN: 2, WAIVED: 1, LEGACY_VIOLATION: 5 } },
    });
    expect(screen.getByTestId('violations-filter-state-option-OPEN')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-state-option-WAIVED')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-filter-state-option-LEGACY_VIOLATION')).not.toBeInTheDocument();
  });

  it('lifts a checkbox toggle to onToggle with the group and id', async () => {
    const { onToggle } = renderRail();
    await user.click(screen.getByTestId('violations-filter-state-option-OPEN'));
    expect(onToggle).toHaveBeenCalledWith('states', 'OPEN');
  });

  it('reflects the selected checkbox as checked', () => {
    renderRail({ selected: withStates('OPEN') });
    expect(screen.getByTestId('violations-filter-state-option-OPEN')).toBeChecked();
    expect(screen.getByTestId('violations-filter-state-option-WAIVED')).not.toBeChecked();
  });

  it('disables Reset when no filter is active', () => {
    renderRail();
    expect(screen.getByTestId('violations-filter-reset')).toBeDisabled();
  });

  it('enables Reset when a filter is active and fires onReset', async () => {
    const { onReset } = renderRail({ selected: withStates('OPEN') });
    const reset = screen.getByTestId('violations-filter-reset');
    expect(reset).toBeEnabled();
    await user.click(reset);
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it('filters the searchable organization list by the sidebar search box', async () => {
    renderRail();
    const orgGroup = screen.getByTestId('violations-filter-organizations');
    // Mock orgs: org-java (Java-team), org-platform (Platform). Search "plat" keeps only Platform.
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'plat');
    expect(within(orgGroup).getByTestId('violations-filter-organizations-option-org-platform')).toBeInTheDocument();
    expect(
      within(orgGroup).queryByTestId('violations-filter-organizations-option-org-java'),
    ).not.toBeInTheDocument();
  });

  it('keeps a selected organization visible when the search query does not match its label', async () => {
    renderRail({
      selected: {
        ...createDefaultViolationsFilterState(),
        organizationIds: new Set(['org-java']),
      },
    });
    const orgGroup = screen.getByTestId('violations-filter-organizations');
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'plat');
    expect(within(orgGroup).getByTestId('violations-filter-organizations-option-org-java')).toBeChecked();
    expect(within(orgGroup).getByTestId('violations-filter-organizations-option-org-platform')).toBeInTheDocument();
  });

  it('shows a "No matches" message when a sidebar search excludes every unselected option', async () => {
    renderRail();
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'zzz-none');
    expect(screen.getByTestId('violations-filter-organizations-empty')).toBeInTheDocument();
  });

  it('clears a stale sidebar search when the group selection is reset', async () => {
    const baseProps = {
      facets: FACETS,
      labels: LABELS,
      onToggle: jest.fn(),
      onWaiverTypeChange: jest.fn(),
      onThreatRangeChange: jest.fn(),
      onReset: jest.fn(),
    };
    const { rerender } = render(
      <Theme>
        <ViolationsFilterRail
          {...baseProps}
          selected={{ ...createDefaultViolationsFilterState(), organizationIds: new Set(['org-platform']) }}
        />
      </Theme>,
    );
    const search = screen.getByTestId('violations-filter-organizations-search');
    await user.type(search, 'plat');
    expect(search).toHaveValue('plat');
    expect(
      screen.queryByTestId('violations-filter-organizations-option-org-java'),
    ).not.toBeInTheDocument();

    // Simulate "Reset filters": the parent clears this group's selection.
    rerender(
      <Theme>
        <ViolationsFilterRail {...baseProps} selected={createDefaultViolationsFilterState()} />
      </Theme>,
    );
    expect(screen.getByTestId('violations-filter-organizations-search')).toHaveValue('');
    // Full list is back — the previously excluded org reappears.
    expect(
      screen.getByTestId('violations-filter-organizations-option-org-java'),
    ).toBeInTheDocument();
  });

  it('renders the threat-level range value from the selection', () => {
    renderRail({ selected: { ...createDefaultViolationsFilterState(), threatRange: [4, 9] } });
    expect(screen.getByTestId('violations-filter-threat-value')).toHaveTextContent('4 – 9');
  });

  it('keeps a selected id visible even when the post-filter facet map omits it', () => {
    // Facet map without WAIVED, but WAIVED is selected: it must still render so it can be toggled off.
    renderRail({
      facets: { totalViolations: 2, states: { OPEN: 2 } },
      selected: withStates('WAIVED'),
    });
    expect(screen.getByTestId('violations-filter-state-option-WAIVED')).toBeChecked();
  });

  describe('waiver-type radio (CLM-42261)', () => {
    it('renders the Any/Auto/Manual options with facet counts on the waiver buckets', () => {
      // Mock facets carry waiverTypes { AUTO: 1 }, so the Auto option shows a "1" badge.
      renderRail();
      const section = screen.getByTestId('violations-filter-waiver-type');
      expect(within(section).getByTestId('violations-filter-waiver-type-option-any')).toBeInTheDocument();
      const autoOption = within(section).getByTestId('violations-filter-waiver-type-option-auto');
      const manualOption = within(section).getByTestId('violations-filter-waiver-type-option-manual');
      expect(within(autoOption.closest('label') as HTMLElement).getByText('1')).toBeInTheDocument();
      // Manual has no count in the mock, so it renders no badge.
      expect(within(manualOption.closest('label') as HTMLElement).queryByText('0')).not.toBeInTheDocument();
    });

    it('reads facet counts keyed by the literal AUTO / MANUAL wire strings', () => {
      // Feed a facets map keyed by the raw backend wire strings (not the imported constants) so this
      // independently pins the frontend to the AUTO/MANUAL contract — a backend rename would blank these.
      renderRail({ facets: { totalViolations: 9, waiverTypes: { AUTO: 3, MANUAL: 5 } } });
      const section = screen.getByTestId('violations-filter-waiver-type');
      const auto = within(section).getByTestId('violations-filter-waiver-type-option-auto');
      const manual = within(section).getByTestId('violations-filter-waiver-type-option-manual');
      expect(within(auto.closest('label') as HTMLElement).getByText('3')).toBeInTheDocument();
      expect(within(manual.closest('label') as HTMLElement).getByText('5')).toBeInTheDocument();
    });

    it('lifts a radio selection to onWaiverTypeChange', async () => {
      const { onWaiverTypeChange } = renderRail();
      await user.click(screen.getByTestId('violations-filter-waiver-type-option-auto'));
      expect(onWaiverTypeChange).toHaveBeenCalledWith('AUTO');
    });

    it('reflects the selected waiver type as checked', () => {
      renderRail({ selected: { ...createDefaultViolationsFilterState(), waiverType: 'MANUAL' } });
      expect(screen.getByTestId('violations-filter-waiver-type-option-manual')).toBeChecked();
      expect(screen.getByTestId('violations-filter-waiver-type-option-any')).not.toBeChecked();
    });

    it('hides the section when there are no waiver counts and nothing is selected', () => {
      renderRail({ facets: { totalViolations: 2, states: { OPEN: 2 } } });
      expect(screen.queryByTestId('violations-filter-waiver-type')).not.toBeInTheDocument();
    });

    it('keeps the section visible (to switch back) when a waiver type is selected without facet data', () => {
      renderRail({
        facets: { totalViolations: 2, states: { OPEN: 2 } },
        selected: { ...createDefaultViolationsFilterState(), waiverType: 'AUTO' },
      });
      expect(screen.getByTestId('violations-filter-waiver-type')).toBeInTheDocument();
      expect(screen.getByTestId('violations-filter-waiver-type-option-auto')).toBeChecked();
    });
  });
});
