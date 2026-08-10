/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import WaiversFilterRail, {
  WAIVERS_FILTER_COLLAPSED_COUNT,
} from 'MainRoot/nosc/waivers/WaiversFilterRail';
import type { WaiversFilterFacetCounts } from 'MainRoot/nosc/waivers/waiversListTypes';
import { EMPTY_WAIVERS_LIST_FILTERS } from 'MainRoot/nosc/waivers/waiversListFilters';

function renderRail(facets: WaiversFilterFacetCounts) {
  const onToggleFilter = jest.fn();
  return render(
    <Theme>
      <WaiversFilterRail
        facets={facets}
        filters={EMPTY_WAIVERS_LIST_FILTERS}
        hasActiveFilters={false}
        onToggleFilter={onToggleFilter}
        onResetFilters={jest.fn()}
      />
    </Theme>,
  );
}

describe('WaiversFilterRail', () => {
  it('orders facet groups toward the vision filter chrome', () => {
    renderRail({
      totalWaivers: 1,
      threatLevels: [{ id: 'critical', label: 'Critical', count: 1 }],
      autoStatuses: [{ id: 'auto', label: 'Auto', count: 1 }],
      lifecycleStatuses: [{ id: 'active', label: 'Active', count: 1 }],
      expiryStatuses: [{ id: 'active', label: 'Active', count: 1 }],
      waiverStates: [{ id: 'active', label: 'Active', count: 1 }],
      scopes: [{ id: 'application', label: 'Application', count: 1 }],
      policyTypes: [{ id: 'security', label: 'Security', count: 1 }],
      organizations: [{ id: 'org-1', label: 'Org One', count: 1 }],
      applications: [{ id: 'app-1', label: 'App One', count: 1 }],
      policies: [{ id: 'p-1', label: 'Policy One', count: 1 }],
    });

    const rail = screen.getByTestId('waivers-filter-rail');
    const legends = Array.from(rail.querySelectorAll('legend')).map((el) => el.textContent);
    expect(legends).toEqual([
      'Waiver State',
      'Status',
      'Auto vs Manual',
      'Policy Threat',
      'Organizations',
      'Applications',
      'Scope',
      'Policy Types',
      'Policies',
    ]);
  });

  it('collapses long organization lists behind Show more / Show less', async () => {
    const user = userEvent.setup();
    const organizations = Array.from({ length: WAIVERS_FILTER_COLLAPSED_COUNT + 3 }, (_, index) => ({
      id: `org-${index}`,
      label: `Scale - Platform - Org ${index}`,
      count: index + 1,
    }));

    renderRail({
      totalWaivers: 0,
      threatLevels: [],
      autoStatuses: [],
      lifecycleStatuses: [],
      expiryStatuses: [],
      waiverStates: [],
      scopes: [],
      policyTypes: [],
      organizations,
      applications: [],
      policies: [],
    });

    expect(screen.getByText('Scale - Platform - Org 0')).toBeInTheDocument();
    expect(screen.queryByText(`Scale - Platform - Org ${WAIVERS_FILTER_COLLAPSED_COUNT}`)).not.toBeInTheDocument();

    const toggle = screen.getByTestId('waivers-filter-organizations-show-more');
    expect(toggle).toHaveTextContent('Show more (3)');

    await user.click(toggle);
    expect(screen.getByText(`Scale - Platform - Org ${WAIVERS_FILTER_COLLAPSED_COUNT}`)).toBeInTheDocument();
    expect(toggle).toHaveTextContent('Show less');

    await user.click(toggle);
    expect(screen.queryByText(`Scale - Platform - Org ${WAIVERS_FILTER_COLLAPSED_COUNT}`)).not.toBeInTheDocument();
  });

  it('wires the Status section to lifecycleStatusIds', async () => {
    const user = userEvent.setup();
    const onToggleFilter = jest.fn();

    render(
      <Theme>
        <WaiversFilterRail
          facets={{
            totalWaivers: 1,
            threatLevels: [],
            autoStatuses: [],
            lifecycleStatuses: [{ id: 'expiring', label: 'Expires Soon', count: 1 }],
            expiryStatuses: [{ id: 'Active', label: 'Active', count: 1 }],
            waiverStates: [],
            scopes: [],
            policyTypes: [],
            organizations: [],
            applications: [],
            policies: [],
          }}
          filters={EMPTY_WAIVERS_LIST_FILTERS}
          hasActiveFilters={false}
          onToggleFilter={onToggleFilter}
          onResetFilters={jest.fn()}
        />
      </Theme>,
    );

    await user.click(screen.getByTestId('waivers-filter-status-checkbox-expiring'));

    expect(onToggleFilter).toHaveBeenCalledWith('lifecycleStatusIds', 'expiring');
  });
});
