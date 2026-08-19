/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import React from 'react';
import SourceControlRateLimits from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/SourceControlRateLimits';
import { getSourceControlRateLimitsUrl } from 'MainRoot/util/CLMLocation';
import {
  SOURCE_CONTROL_RATE_LIMITS_APPLICATION_MOCK_DATA,
  SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA,
} from 'TestRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsMockData';
import { within } from '@testing-library/react';

describe('SourceControlRateLimits', function () {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    router: {
      currentParams: {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    renderComponent = (preloadedState) =>
      render(<SourceControlRateLimits />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  const expectTitleVisible = () => {
    expect(screen.getByText('Source Control User Usage')).toBeVisible();
  };

  const expectSubTitleVisible = (ownerType, ownerName) => {
    expect(
      screen.getByText(`for ${ownerName + (ownerType === 'organization' ? ' and descendants' : '')}`)
    ).toBeVisible();
  };

  const expectTableHeadersVisible = () => {
    expect(screen.getByText('SCM Provider')).toBeVisible();
    expect(screen.getByText('SCM User ID')).toBeVisible();
    expect(screen.getByText('Defining Owners')).toBeVisible();
    expect(screen.getByText('Associated Applications')).toBeVisible();
    expect(screen.getByText('Rate Limit Remaining')).toBeVisible();
  };

  const expectDisclaimerText = () => {
    expect(
      screen.getByText(
        'This is an experimental page and subject to change. Only GitHub users are supported at this time.'
      )
    ).toBeVisible();
  };

  const expectOrganizationTableDataVisible = async (userEvent) => {
    expectTableRowVisible(
      userEvent,
      'github',
      'userA',
      '80% Average',
      ['core', '60%', '(resets in 6 hours)', 'graphql', '100%', '(resets in 7 hours)'],
      '1 Total',
      '1 Total'
    );
    expectTableRowVisible(
      userEvent,
      'github',
      'userB',
      '70% Average',
      ['core', '100%', '(resets in 8 hours)', 'graphql', '40%', '(resets in 9 hours)'],
      '2 Total',
      '2 Total'
    );
  };

  const expectApplicationTableDataVisible = async () => {
    expectTableRowVisible(
      userEvent,
      'github',
      'userA',
      '10% Average',
      ['core', '20%', '(resets in 6 hours)', 'graphql', '0%', '(resets in 7 hours)'],
      '1 Total',
      '1 Total'
    );
  };

  const expectTableRowVisible = async (
    userEvent,
    provider,
    user,
    averageRateLimit,
    rateLimitParts,
    definingOwners,
    associatedApplications
  ) => {
    const element = screen.getByText(user);
    const parentElement = element.parentElement;

    const providerElement = within(parentElement.children[0]).getByText(provider);
    expect(providerElement).toBeVisible();

    const userElement = within(parentElement.children[1]).getByText(user);
    expect(userElement).toBeVisible();

    const averageRateLimitElement = within(parentElement.children[2]).getByText(averageRateLimit);
    expect(averageRateLimitElement).toBeVisible();

    for (const rateLimitPart of rateLimitParts) {
      const cell = parentElement.children[2];
      const collapsibleItemsToggle = within(cell).getByRole('button');

      await userEvent.click(collapsibleItemsToggle);

      expect(within(cell).getByText(rateLimitPart)).toBeVisible();
    }

    const definingOwnersElement = within(parentElement.children[3]).getByText(definingOwners);
    expect(definingOwnersElement).toBeVisible();

    const associatedApplicationsElement = within(parentElement.children[4]).getByText(associatedApplications);
    expect(associatedApplicationsElement).toBeVisible();
  };

  describe('when there is a load error', function () {
    it('shows the title, table, and error message', async () => {
      const errorMessage = 'Error Message';
      axiosMock.onGet(getSourceControlRateLimitsUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(500, errorMessage);

      renderComponent();

      await waitFor(() => expect(screen.getByText('An error occurred loading data. ' + errorMessage)).toBeVisible());
      expectDisclaimerText();
      expectTitleVisible();
      expectTableHeadersVisible();
    });
  });

  describe('when there is no load error', function () {
    it('shows the title, subtitle, table, and expected data for an organization', async () => {
      const user = userEvent.setup();
      const date = new Date(1684730455000);
      jest.useFakeTimers();
      jest.setSystemTime(date);
      axiosMock
        .onGet(getSourceControlRateLimitsUrl('organization', 'ROOT_ORGANIZATION_ID'))
        .reply(200, SOURCE_CONTROL_RATE_LIMITS_ORGANIZATION_MOCK_DATA);

      renderComponent();

      await waitFor(() => expect(screen.getByText('userA')).toBeVisible());
      expectDisclaimerText();
      expectTitleVisible();
      expectSubTitleVisible('organization', 'Root Organization');
      expectTableHeadersVisible();
      await expectOrganizationTableDataVisible(user);
    });

    it('shows the title, subtitle, table, and expected data for an application', async () => {
      const user = userEvent.setup();
      const date = new Date(1684730455000);
      jest.useFakeTimers();
      jest.setSystemTime(date);
      axiosMock
        .onGet(getSourceControlRateLimitsUrl('application', '17c4ab720bf64becba6be857bda65ffa'))
        .reply(200, SOURCE_CONTROL_RATE_LIMITS_APPLICATION_MOCK_DATA);

      renderComponent({
        router: {
          currentParams: {
            ownerType: 'application',
            ownerId: '17c4ab720bf64becba6be857bda65ffa',
          },
        },
      });

      await waitFor(() => expect(screen.getByText('userA')).toBeVisible());
      expectDisclaimerText();
      expectTitleVisible();
      expectSubTitleVisible('application', 'relay-devtools');
      expectTableHeadersVisible();
      await expectApplicationTableDataVisible(user);
    });
  });
});
