/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import PolicyViolationDetailsPopover from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover';

import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';

describe('PolicyViolationDetailsPopover', () => {
  let renderComponent, state;
  const violationId = 'violationId';

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    state = {
      router: { currentParams: { id: violationId } },
      stages: {
        dashboard: {
          stageTypes: [],
        },
      },
      componentDetailsPolicyViolations: {
        violations: [
          {
            policyName: 'Some policy name',
            threatLevel: 10,
            policyViolationId: violationId,
          },
        ],
        selectedPolicyViolationId: violationId,
      },
      violation: {
        loading: true,
        activeWaivers: [],
        expiredWaivers: [],
        hasPermissionForAppWaivers: false,
        violationDetails: {
          policyOwner: { ownerId: 'owner1' },
        },
      },
      productFeatures: {
        productFeatures: {
          'waiver-request-workflow-enabled': true,
        },
      },
      applicationReport: {
        reportData: {
          report: {
            isProxyStage: false,
          },
        },
        containerImagesEvaluationEnabled: false,
      },
    };

    renderComponent = (preloadedState = state) =>
      render(<PolicyViolationDetailsPopover onClose={jest.fn(() => {})} />, { preloadedState });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('while loading do not render add or request waiver button', () => {
    renderComponent();
    const addWaiverButton = screen.queryByText('Add Waiver');
    const requestWaiverButton = screen.queryByText('Request Waiver');
    expect(addWaiverButton).not.toBeInTheDocument();
    expect(requestWaiverButton).not.toBeInTheDocument();
  });

  it('while not loading render add or request waiver button', () => {
    renderComponent({
      ...state,
      violation: {
        ...state.violation,
        loading: false,
      },
    });
    const requestWaiverButton = screen.getByText('Request Waiver');
    expect(requestWaiverButton).toBeInTheDocument();
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
