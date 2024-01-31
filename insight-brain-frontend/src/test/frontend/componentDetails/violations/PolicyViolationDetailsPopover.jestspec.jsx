/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import PolicyViolationDetailsPopover from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover';
import {
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getViolationDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';

describe('PolicyViolationDetailsPopover', () => {
  let renderComponent, state, mockAxiosCalls;
  const violationId = 'violationId';

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

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
    };

    mockAxiosCalls.onGet(getViolationDetailsUrl(violationId)).reply(200, {
      policyName: 'Some policy name',
      threatLevel: 10,
      policyOwner: {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
      },
      applicationPublicId: 'appPublicId',
      constraintViolations: [{ constraintName: 'name', reasons: [], conditions: [] }],
      policyThreatCategory: 'SECURITY',
    });
    mockAxiosCalls.onGet(getApplicableWaiversUrl('violationId')).reply(200, {
      activeWaivers: ['foo'],
      expiredWaivers: ['bar'],
    });
    mockAxiosCalls.onGet(getApplicationSummaryUrl('appPublicId')).reply(200, { id: 'applicationPrivateId' });
    mockAxiosCalls
      .onPut(getPermissionContextTestUrl('application', 'applicationPrivateId'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    renderComponent = (preloadedState = state) =>
      render(<PolicyViolationDetailsPopover onClose={jest.fn(() => {})} />, { preloadedState });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  //TODO CLM-29258 This test is brocken, is not reliable
  xit('renders loading indicator and proper policy name', () => {
    renderComponent();
    const loading = screen.getByText('Loading…');
    const violationName = screen.getByText('Violation of');
    expect(loading).toBeInTheDocument();
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
