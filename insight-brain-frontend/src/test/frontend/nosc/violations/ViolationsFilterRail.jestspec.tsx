/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import ViolationsFilterRail, {
  FACET_COLLAPSE_LIMIT,
  FACET_SERVER_CAP,
} from 'MainRoot/nosc/violations/ViolationsFilterRail';
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
/** Full list→rail label pipeline: page rows + server facet name maps (CLM-42757). */
const LABELS = deriveViolationFacetLabels(MOCK_VIOLATIONS_LIST_RESPONSE.violations, {
  organizations: FACETS?.organizationNames,
  applications: FACETS?.applicationNames,
});

function makeManyOrgs(count = 12): {
  readonly organizations: Record<string, number>;
  readonly labels: Record<string, string>;
} {
  // Zero-pad labels so localeCompare order matches index order (Violations sorts by label).
  const organizations = Object.fromEntries(
    Array.from({ length: count }, (_, index) => [`org-${index}`, index + 1]),
  );
  const labels = Object.fromEntries(
    Array.from({ length: count }, (_, index) => [
      `org-${index}`,
      `Organization ${String(index).padStart(2, '0')}`,
    ]),
  );
  return { organizations, labels };
}

function makeManyApps(count = 12): {
  readonly applications: Record<string, number>;
  readonly labels: Record<string, string>;
} {
  const applications = Object.fromEntries(
    Array.from({ length: count }, (_, index) => [`app-${index}`, index + 1]),
  );
  const labels = Object.fromEntries(
    Array.from({ length: count }, (_, index) => [
      `app-${index}`,
      `Application ${String(index).padStart(2, '0')}`,
    ]),
  );
  return { applications, labels };
}

function baseRailProps(
  overrides: Partial<React.ComponentProps<typeof ViolationsFilterRail>> = {},
): React.ComponentProps<typeof ViolationsFilterRail> {
  return {
    facets: FACETS,
    labels: LABELS,
    selected: createDefaultViolationsFilterState(),
    onToggle: jest.fn(),
    onWaiverTypeChange: jest.fn(),
    onThreatRangeChange: jest.fn(),
    onReset: jest.fn(),
    organizationFacetSearch: '',
    onOrganizationFacetSearchChange: jest.fn(),
    applicationFacetSearch: '',
    onApplicationFacetSearchChange: jest.fn(),
    ...overrides,
  };
}

function renderRail(overrides: Partial<React.ComponentProps<typeof ViolationsFilterRail>> = {}) {
  const props = baseRailProps(overrides);
  render(
    <Theme>
      <ViolationsFilterRail {...props} />
    </Theme>,
  );
  return props;
}


function ControlledOrgSearchRail(
  overrides: Partial<React.ComponentProps<typeof ViolationsFilterRail>> = {},
): JSX.Element {
  const [organizationFacetSearch, setOrganizationFacetSearch] = useState(
    overrides.organizationFacetSearch ?? '',
  );
  return (
    <ViolationsFilterRail
      {...baseRailProps(overrides)}
      organizationFacetSearch={organizationFacetSearch}
      onOrganizationFacetSearchChange={setOrganizationFacetSearch}
    />
  );
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

  it('labels an off-page organization from server facet name maps', () => {
    // End-to-end wire story after CLM-42757: facet key not on the current page still gets a friendly name.
    const facets = {
      ...FACETS,
      organizations: { ...(FACETS?.organizations ?? {}), 'org-off-page': 4 },
      organizationNames: { ...(FACETS?.organizationNames ?? {}), 'org-off-page': 'Off-Page Tribe' },
    };
    renderRail({
      facets,
      labels: deriveViolationFacetLabels(MOCK_VIOLATIONS_LIST_RESPONSE.violations, {
        organizations: facets.organizationNames,
        applications: facets.applicationNames,
      }),
    });
    expect(screen.getByTestId('violations-filter-organizations-option-org-off-page')).toBeInTheDocument();
    expect(
      within(screen.getByTestId('violations-filter-organizations').closest('fieldset') as HTMLElement)
        .getByText('Off-Page Tribe'),
    ).toBeInTheDocument();
  });

  it('shows the server organization name when it differs from the page-row name', () => {
    renderRail({
      labels: deriveViolationFacetLabels(MOCK_VIOLATIONS_LIST_RESPONSE.violations, {
        organizations: { ...(FACETS?.organizationNames ?? {}), 'org-java': 'Server Java-team' },
        applications: FACETS?.applicationNames,
      }),
    });
    expect(
      within(screen.getByTestId('violations-filter-organizations').closest('fieldset') as HTMLElement)
        .getByText('Server Java-team'),
    ).toBeInTheDocument();
  });

  it('renders LEGACY_VIOLATION as a selectable state facet', () => {
    renderRail({
      facets: { totalViolations: 3, states: { OPEN: 2, WAIVED: 1, LEGACY_VIOLATION: 5 } },
    });
    expect(screen.getByTestId('violations-filter-state-option-OPEN')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-state-option-WAIVED')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-state-option-LEGACY_VIOLATION')).toBeInTheDocument();
  });

  it('surfaces the Legacy-vs-classic divergence note only when a Legacy facet is present', () => {
    renderRail({
      facets: { totalViolations: 3, states: { OPEN: 2, WAIVED: 1, LEGACY_VIOLATION: 5 } },
    });
    expect(screen.getByTestId('violations-filter-state-footnote')).toHaveTextContent(
      /waived legacy violation counts under Waived/i,
    );
  });

  it('omits the Legacy divergence note when there is no Legacy facet', () => {
    renderRail({
      facets: { totalViolations: 3, states: { OPEN: 2, WAIVED: 1 } },
    });
    expect(screen.queryByTestId('violations-filter-state-footnote')).not.toBeInTheDocument();
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

  it('collapses long organization lists behind See more', async () => {
    const { organizations, labels } = makeManyOrgs();
    renderRail({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
    });
    expect(screen.getByTestId('violations-filter-organizations-option-org-0')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-filter-organizations-option-org-11')).not.toBeInTheDocument();

    const seeMore = screen.getByTestId('violations-filter-organizations-see-more');
    expect(seeMore).toHaveAttribute('aria-expanded', 'false');
    await user.click(seeMore);
    expect(screen.getByTestId('violations-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(seeMore).toHaveTextContent('See less');
    expect(seeMore).toHaveAttribute('aria-expanded', 'true');
    // Expanded Orgs/Apps stay height-capped so estate-scale facets cannot blow past the rail.
    expect(
      screen.getByTestId('violations-filter-organizations').querySelector('.nosc-violations-filter-scroll'),
    ).toBeInTheDocument();
  });

  it('collapses long application lists behind See more', async () => {
    const { applications, labels } = makeManyApps();
    renderRail({
      facets: { ...FACETS, applications },
      labels: { ...LABELS, applications: labels },
    });
    expect(screen.getByTestId('violations-filter-applications-option-app-0')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-filter-applications-option-app-11')).not.toBeInTheDocument();

    await user.click(screen.getByTestId('violations-filter-applications-see-more'));
    expect(screen.getByTestId('violations-filter-applications-option-app-11')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-applications-see-more')).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  it('collapses See more back to the limited list when the group selection is reset', async () => {
    const { organizations, labels } = makeManyOrgs();
    const baseProps = baseRailProps({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
    });
    const { rerender } = render(
      <Theme>
        <ViolationsFilterRail
          {...baseProps}
          selected={{ ...createDefaultViolationsFilterState(), organizationIds: new Set(['org-0']) }}
        />
      </Theme>,
    );
    await user.click(screen.getByTestId('violations-filter-organizations-see-more'));
    expect(screen.getByTestId('violations-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See less');

    // Simulate "Reset filters": parent clears selection; expanded must collapse with the search reset.
    rerender(
      <Theme>
        <ViolationsFilterRail {...baseProps} selected={createDefaultViolationsFilterState()} />
      </Theme>,
    );
    expect(screen.queryByTestId('violations-filter-organizations-option-org-11')).not.toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See more');
  });

  it('keeps See more expanded when clearing only this group while other filters stay active', async () => {
    // Intentional: expanded is tied to rail-wide filtersActive / explicit See less, not group selectionEmpty.
    const { organizations, labels } = makeManyOrgs();
    const baseProps = baseRailProps({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
    });
    const { rerender } = render(
      <Theme>
        <ViolationsFilterRail
          {...baseProps}
          selected={{ ...withStates('OPEN'), organizationIds: new Set(['org-0']) }}
        />
      </Theme>,
    );
    await user.click(screen.getByTestId('violations-filter-organizations-see-more'));
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See less');

    // Clear only Orgs; State remains → filtersActive stays true → expanded persists.
    rerender(
      <Theme>
        <ViolationsFilterRail {...baseProps} selected={withStates('OPEN')} />
      </Theme>,
    );
    expect(screen.getByTestId('violations-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See less');
  });

  it('collapses See more on Reset even when Orgs had no selection of their own', async () => {
    // Edge case: expand Orgs while another filter is active (org selectionEmpty stays true across Reset).
    const { organizations, labels } = makeManyOrgs();
    const baseProps = baseRailProps({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
    });
    const { rerender } = render(
      <Theme>
        <ViolationsFilterRail {...baseProps} selected={withStates('OPEN')} />
      </Theme>,
    );
    await user.click(screen.getByTestId('violations-filter-organizations-see-more'));
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See less');

    rerender(
      <Theme>
        <ViolationsFilterRail {...baseProps} selected={createDefaultViolationsFilterState()} />
      </Theme>,
    );
    expect(screen.queryByTestId('violations-filter-organizations-option-org-11')).not.toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See more');
  });

  it('shows only selected organizations when the collapsed selection meets the See more limit', () => {
    // When selected count >= FACET_COLLAPSE_LIMIT, collapsed view is selection-only so checked
    // rows are never pushed out by unselected facet noise.
    const { organizations, labels } = makeManyOrgs();
    const selectedIds = Array.from({ length: FACET_COLLAPSE_LIMIT }, (_, index) => `org-${index}`);
    renderRail({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
      selected: {
        ...createDefaultViolationsFilterState(),
        organizationIds: new Set(selectedIds),
      },
    });
    for (const id of selectedIds) {
      expect(screen.getByTestId(`violations-filter-organizations-option-${id}`)).toBeChecked();
    }
    expect(
      screen.queryByTestId(`violations-filter-organizations-option-org-${FACET_COLLAPSE_LIMIT}`),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveTextContent('See more');
  });

  it('surfaces a top-N note when organization facets hit the server cap', () => {
    const { organizations, labels } = makeManyOrgs(FACET_SERVER_CAP);
    renderRail({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
    });
    expect(screen.getByTestId('violations-filter-organizations-server-capped')).toHaveTextContent(
      `Showing top ${FACET_SERVER_CAP} organizations by violation count`,
    );
    expect(screen.getByTestId('violations-filter-organizations-server-capped')).toHaveTextContent(
      'Type to search by name beyond this list',
    );
  });

  it('hides the top-N note while an organization facet search is active', () => {
    const { organizations, labels } = makeManyOrgs(FACET_SERVER_CAP);
    renderRail({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
      organizationFacetSearch: 'zeta',
    });
    expect(screen.queryByTestId('violations-filter-organizations-server-capped')).not.toBeInTheDocument();
  });

  it('renders exactly the server-capped search match set (15) with friendly names', () => {
    // Server display-caps name search at FACET_SERVER_CAP; FE must render that payload without crashing.
    const { organizations, labels } = makeManyOrgs(FACET_SERVER_CAP);
    renderRail({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
      organizationFacetSearch: 'Organization',
    });
    const orgGroup = screen.getByTestId('violations-filter-organizations');
    expect(within(orgGroup).getAllByRole('checkbox')).toHaveLength(FACET_SERVER_CAP);
    expect(within(orgGroup).getByText('Organization 00')).toBeInTheDocument();
    expect(within(orgGroup).queryByTestId('violations-filter-organizations-option-org-15')).not.toBeInTheDocument();
  });

  it('shows a keep-typing hint when search results hit the server display cap', () => {
    const { organizations, labels } = makeManyOrgs(FACET_SERVER_CAP);
    renderRail({
      facets: { ...FACETS, organizations },
      labels: { ...LABELS, organizations: labels },
      organizationFacetSearch: 'Organization',
    });
    expect(screen.getByTestId('violations-filter-organizations-search-capped')).toHaveTextContent(
      `Showing first ${FACET_SERVER_CAP} matches. Keep typing to narrow.`,
    );
    expect(screen.queryByTestId('violations-filter-organizations-server-capped')).not.toBeInTheDocument();
  });

  it('keeps whitespace-stripped client matches aligned with server NameHelper.normalize', () => {
    // Server would return "Zeta Finance" for "zetafinance"; client filter must not hide it.
    renderRail({
      facets: {
        ...FACETS,
        organizations: { 'org-zeta': 9 },
        organizationNames: { 'org-zeta': 'Zeta Finance' },
      },
      labels: { ...LABELS, organizations: { 'org-zeta': 'Zeta Finance' } },
      organizationFacetSearch: 'zetafinance',
    });
    expect(screen.getByTestId('violations-filter-organizations-option-org-zeta')).toBeInTheDocument();
    expect(screen.getByText('Zeta Finance')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-filter-organizations-empty')).not.toBeInTheDocument();
  });

  it('keeps the org rail stable when soft-skip omits an oversized hierarchy from search facets', () => {
    // Soft-skip: oversized org trees are omitted server-side; remaining matches still render.
    renderRail({
      facets: {
        ...FACETS,
        organizations: { 'org-ok': 4 },
        organizationNames: { 'org-ok': 'Ok Org' },
      },
      labels: { ...LABELS, organizations: { 'org-ok': 'Ok Org' } },
      organizationFacetSearch: 'org',
    });
    expect(screen.getByTestId('violations-filter-organizations-search')).toHaveValue('org');
    expect(screen.getByTestId('violations-filter-organizations-option-org-ok')).toBeInTheDocument();
    expect(screen.getByText('Ok Org')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-filter-organizations-empty')).not.toBeInTheDocument();
  });

  it('keeps See more expanded after clearing a search query', async () => {
    // Intentional: expanded survives search-clear so the user is not forced to re-open See more.
    const { organizations, labels } = makeManyOrgs();
    render(
      <Theme>
        <ControlledOrgSearchRail
          facets={{ ...FACETS, organizations }}
          labels={{ ...LABELS, organizations: labels }}
        />
      </Theme>,
    );
    await user.click(screen.getByTestId('violations-filter-organizations-see-more'));
    const search = screen.getByTestId('violations-filter-organizations-search');
    await user.type(search, 'Organization 11');
    expect(screen.queryByTestId('violations-filter-organizations-see-more')).not.toBeInTheDocument();
    await user.clear(search);
    expect(screen.getByTestId('violations-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-see-more')).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  it('filters the searchable organization list by the sidebar search box', async () => {
    render(
      <Theme>
        <ControlledOrgSearchRail />
      </Theme>,
    );
    const orgGroup = screen.getByTestId('violations-filter-organizations');
    // Mock orgs: org-java (Java-team), org-platform (Platform). Search "plat" keeps only Platform.
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'plat');
    expect(within(orgGroup).getByTestId('violations-filter-organizations-option-org-platform')).toBeInTheDocument();
    expect(
      within(orgGroup).queryByTestId('violations-filter-organizations-option-org-java'),
    ).not.toBeInTheDocument();
  });

  it('shows all search matches without collapse when searching a long organization list', async () => {
    const { organizations, labels } = makeManyOrgs();
    render(
      <Theme>
        <ControlledOrgSearchRail
          facets={{ ...FACETS, organizations }}
          labels={{ ...LABELS, organizations: labels }}
        />
      </Theme>,
    );
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'Organization 11');
    expect(screen.getByTestId('violations-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(screen.queryByTestId('violations-filter-organizations-see-more')).not.toBeInTheDocument();
  });

  it('keeps a selected organization visible when the search query does not match its label', async () => {
    render(
      <Theme>
        <ControlledOrgSearchRail
          selected={{
            ...createDefaultViolationsFilterState(),
            organizationIds: new Set(['org-java']),
          }}
        />
      </Theme>,
    );
    const orgGroup = screen.getByTestId('violations-filter-organizations');
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'plat');
    expect(within(orgGroup).getByTestId('violations-filter-organizations-option-org-java')).toBeChecked();
    expect(within(orgGroup).getByTestId('violations-filter-organizations-option-org-platform')).toBeInTheDocument();
  });

  it('shows a "No matches" message when a sidebar search excludes every unselected option', async () => {
    render(
      <Theme>
        <ControlledOrgSearchRail />
      </Theme>,
    );
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'zzz-none');
    expect(screen.getByTestId('violations-filter-organizations-empty')).toBeInTheDocument();
  });

  it('keeps org search mounted with No matches when server returns zero name matches', () => {
    renderRail({
      facets: { ...FACETS, organizations: undefined },
      organizationFacetSearch: 'zzz-none',
    });
    expect(screen.getByTestId('violations-filter-organizations-search')).toBeInTheDocument();
    expect(screen.getByTestId('violations-filter-organizations-empty')).toBeInTheDocument();
  });

  it('lifts organization facet search text to onOrganizationFacetSearchChange', async () => {
    const onOrganizationFacetSearchChange = jest.fn();
    function Harness(): JSX.Element {
      const [organizationFacetSearch, setOrganizationFacetSearch] = useState('');
      return (
        <ViolationsFilterRail
          {...baseRailProps()}
          organizationFacetSearch={organizationFacetSearch}
          onOrganizationFacetSearchChange={(next) => {
            onOrganizationFacetSearchChange(next);
            setOrganizationFacetSearch(next);
          }}
        />
      );
    }
    render(
      <Theme>
        <Harness />
      </Theme>,
    );
    await user.type(screen.getByTestId('violations-filter-organizations-search'), 'zeta');
    expect(onOrganizationFacetSearchChange.mock.calls.at(-1)?.[0]).toBe('zeta');
  });

  it('reflects a parent-cleared organizationFacetSearch after Reset', () => {
    // Parent owns clear-on-reset (rail mounts twice — desktop + drawer — so it cannot own that effect).
    const baseProps = baseRailProps({ organizationFacetSearch: 'plat' });
    const { rerender } = render(
      <Theme>
        <ViolationsFilterRail {...baseProps} />
      </Theme>,
    );
    expect(screen.getByTestId('violations-filter-organizations-search')).toHaveValue('plat');
    rerender(
      <Theme>
        <ViolationsFilterRail {...baseProps} organizationFacetSearch="" />
      </Theme>,
    );
    expect(screen.getByTestId('violations-filter-organizations-search')).toHaveValue('');
    expect(screen.getByTestId('violations-filter-organizations-option-org-java')).toBeInTheDocument();
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

  describe('Legal list props (CLM-43207)', () => {
    it('hides state and waiver sections when hideStateFilter / hideWaiverTypeFilter are set', () => {
      renderRail({ hideStateFilter: true, hideWaiverTypeFilter: true });
      expect(screen.queryByTestId('violations-filter-state')).not.toBeInTheDocument();
      expect(screen.queryByTestId('violations-filter-waiver-type')).not.toBeInTheDocument();
      expect(screen.getByTestId('violations-filter-policy-type')).toBeInTheDocument();
    });

    it('uses LTG section title and identity labels when threatCategoryUseIdentityLabels is set', () => {
      renderRail({
        facets: {
          totalViolations: 1,
          threatCategories: { Copyleft: 3, Banned: 1 },
        },
        threatCategorySectionTitle: 'License Threat Group',
        threatCategoryUseIdentityLabels: true,
      });
      const policyType = screen.getByTestId('violations-filter-policy-type');
      expect(within(policyType).getByText('License Threat Group')).toBeInTheDocument();
      // Identity mode keeps raw LTG names (not Policy Type title-case maps).
      expect(within(policyType).getByText('Copyleft')).toBeInTheDocument();
      expect(within(policyType).getByText('Banned')).toBeInTheDocument();
    });
  });
});
