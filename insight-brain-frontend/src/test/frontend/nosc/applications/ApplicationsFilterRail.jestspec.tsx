/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import ApplicationsFilterRail from 'MainRoot/nosc/applications/ApplicationsFilterRail';
import { EMPTY_APPLICATIONS_LIST_FILTERS } from 'MainRoot/nosc/applications/applicationsListFilters';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';

beforeAll(() => {
  installRadixJsdomShims();
});

const MANY_ORGS = Array.from({ length: 12 }, (_, index) => ({
  id: `org-${index}`,
  label: `Organization ${index}`,
  count: index + 1,
}));

const FACETS = {
  totalApplications: 12,
  stages: [{ id: 'build', label: 'Build', count: 3 }],
  organizations: MANY_ORGS,
  applications: [
    { id: 'app-a', label: 'Alpha App', count: 2 },
    { id: 'app-b', label: 'Beta App', count: 1 },
  ],
  policyTypes: [
    { id: 'security', label: 'Security', count: 5 },
    { id: 'license', label: 'License', count: 0 },
    { id: 'quality', label: 'Quality', count: 2 },
    { id: 'other', label: 'Other', count: 0 },
  ],
  violationStates: [
    { id: 'OPEN', label: 'Open', count: 7 },
    { id: 'WAIVED', label: 'Waived', count: 1 },
  ],
};

function renderRail(
  overrides: Partial<React.ComponentProps<typeof ApplicationsFilterRail>> = {},
) {
  const props = {
    facets: FACETS,
    filters: EMPTY_APPLICATIONS_LIST_FILTERS,
    hasActiveFilters: false,
    onToggleFilter: jest.fn(),
    onThreatRangeChange: jest.fn(),
    onResetFilters: jest.fn(),
    ...overrides,
  };
  render(
    <Theme>
      <ApplicationsFilterRail {...props} />
    </Theme>,
  );
  return props;
}

describe('ApplicationsFilterRail', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  it('renders the threat slider instead of threat checkboxes', () => {
    renderRail();
    expect(screen.getByTestId('applications-filter-threat-level-slider')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-threat-level-value')).toHaveTextContent('0 – 10');
    expect(screen.queryByText(/Critical/i)).not.toBeInTheDocument();
  });

  it('collapses long organization lists behind See more', async () => {
    renderRail();
    expect(screen.getByTestId('applications-filter-organizations-option-org-0')).toBeInTheDocument();
    expect(screen.queryByTestId('applications-filter-organizations-option-org-11')).not.toBeInTheDocument();

    await user.click(screen.getByTestId('applications-filter-organizations-see-more'));
    expect(screen.getByTestId('applications-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-organizations-see-more')).toHaveTextContent('See less');
  });

  it('filters organizations by local search', async () => {
    renderRail();
    await user.type(screen.getByTestId('applications-filter-organizations-search'), 'Organization 11');
    expect(screen.getByTestId('applications-filter-organizations-option-org-11')).toBeInTheDocument();
    expect(screen.queryByTestId('applications-filter-organizations-option-org-0')).not.toBeInTheDocument();
  });

  it('keeps facet search text after deselecting the last checked option', async () => {
    const props = renderRail({
      filters: {
        ...EMPTY_APPLICATIONS_LIST_FILTERS,
        organizationIds: new Set(['org-11']),
      },
      hasActiveFilters: true,
    });
    await user.type(screen.getByTestId('applications-filter-organizations-search'), 'Organization 11');
    await user.click(screen.getByTestId('applications-filter-organizations-option-org-11'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('organizationIds', 'org-11');
    expect(screen.getByTestId('applications-filter-organizations-search')).toHaveValue('Organization 11');
  });

  it('renders every policy type and violation state option, including zero counts', () => {
    renderRail();
    ['security', 'license', 'quality', 'other'].forEach((id) => {
      expect(screen.getByTestId(`applications-filter-policy-types-option-${id}`)).toBeInTheDocument();
    });
    expect(screen.getByTestId('applications-filter-violation-states-option-OPEN')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-violation-states-option-WAIVED')).toBeInTheDocument();
  });

  it('omits deferred Kitchen Sink vision filters from the rail (CLM-43211)', () => {
    renderRail();
    expect(screen.queryByTestId('applications-filter-violation-states-option-LEGACY_VIOLATION')).not.toBeInTheDocument();
    expect(screen.queryByText(/Coming Soon/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/^Age$/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/^Categories$/i)).not.toBeInTheDocument();
  });

  it('toggles policy type and violation state selections', async () => {
    const props = renderRail();
    await user.click(screen.getByTestId('applications-filter-policy-types-option-security'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('policyTypes', 'security');

    await user.click(screen.getByTestId('applications-filter-violation-states-option-OPEN'));
    expect(props.onToggleFilter).toHaveBeenCalledWith('violationStates', 'OPEN');
  });

  it('commits the threat slider range through onThreatRangeChange', async () => {
    const props = renderRail();
    const slider = screen.getByTestId('applications-filter-threat-level-slider');
    const thumbs = slider.querySelectorAll('[role="slider"]');
    expect(thumbs.length).toBeGreaterThanOrEqual(2);

    thumbs[0].focus();
    await user.keyboard('{ArrowRight}');
    expect(props.onThreatRangeChange).toHaveBeenCalled();
    const committed = props.onThreatRangeChange.mock.calls.at(-1)?.[0];
    expect(committed[0]).toBeGreaterThanOrEqual(0);
    expect(committed[1]).toBeLessThanOrEqual(10);
    expect(committed[0]).toBeLessThanOrEqual(committed[1]);
  });
});

