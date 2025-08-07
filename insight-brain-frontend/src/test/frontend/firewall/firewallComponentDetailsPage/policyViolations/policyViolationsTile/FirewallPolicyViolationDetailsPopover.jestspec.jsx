/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationDetailsPopover from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationDetailsPopover';
import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';

describe('FirewallPolicyViolationDetailsPopover', () => {
  let renderComponent, state;
  const violationId = 'violationId';
  const repositoryId = 'repositoryId';

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    state = {
      router: {
        currentParams: {
          id: violationId,
          repositoryId,
          componentIdentifier: '{"name": "name"}',
        },
        currentState: {
          name: 'firewall',
        },
      },
      stages: {
        dashboard: {
          stageTypes: [],
        },
      },
      componentDetailsPolicyViolations: {
        selectedPolicyViolation: {
          policyName: 'Some policy name',
        },
      },
    };

    renderComponent = (preloadedState = state) =>
      render(<FirewallPolicyViolationDetailsPopover selectPolicyId="selectPolicyId" />, { preloadedState });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders proper policy name', async () => {
    renderComponent();
    const violationName = screen.getByText('Violation of');
    expect(violationName).toBeInTheDocument();
    expect(violationName).toHaveTextContent('Violation of Some policy name');
  });

  it('while loading do not render add or request waiver button', () => {
    renderComponent();
    const addWaiverButton = screen.queryByRole('button', { name: 'Add Waiver' });
    const requestWaiverButton = screen.queryByRole('button', { name: 'Request Waiver' });
    expect(addWaiverButton).toBe(null);
    expect(requestWaiverButton).toBe(null);
  });

  // This code was added to do a clean up for an edge case in the workflow
  // Do not use this approach for testing frontend code, this is a special case
  it('calls the cleanup code on unmount', async () => {
    const unsetShowViolationsDetailPopoverSpy = jest.spyOn(actions, 'unsetShowViolationsDetailPopover');
    const unsetRowClickSpy = jest.spyOn(actions, 'unsetViolationsDetailRowClicked');
    const { unmount } = renderComponent();
    unmount();
    expect(unsetShowViolationsDetailPopoverSpy).toHaveBeenCalled();
    expect(unsetRowClickSpy).toHaveBeenCalled();
  });
});
