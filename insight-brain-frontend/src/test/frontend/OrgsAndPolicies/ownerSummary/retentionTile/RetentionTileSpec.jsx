/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RetentionTile from 'MainRoot/OrgsAndPolicies/ownerSummary/retentionTile/RetentionTile';
import { render, axiosMockAdapter, within, screen, fireEvent } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/OrgsAndPolicies/retentionSlice';
import { getRetentionPoliciesUrl } from 'MainRoot/util/CLMLocation';

describe('RetentionTile', () => {
  let axiosMock, goToEditRetentionSpy, preloadedState;
  const ownerId = 'e270271429f747ef9bebf4ca88f5e6c0';
  const renderComponent = (preloadedState) => render(<RetentionTile />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    goToEditRetentionSpy = spyOn(actions, 'goToEditRetention').and.callThrough();
    spyOn(actions, 'loadRetentionTile').and.callThrough();

    axiosMock.onGet(getRetentionPoliciesUrl(ownerId)).reply(200, {});
  });

  describe('Loading and Retry logic', () => {
    beforeAll(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: 'broadcast',
            },
          },
        },
      };
    });

    it('renders a loading indicator', () => {
      renderComponent(preloadedState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders an alert with retry if something goes wrong', async () => {
      axiosMock.onGet(getRetentionPoliciesUrl(ownerId)).reply(() => Promise.reject('An error occurred loading data.'));

      renderComponent(preloadedState);

      let failureAlert = await screen.findByRole('alert');

      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');

      let retryButton = await within(failureAlert).getByRole('button');

      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      expect(await screen.findByText('Loading…')).toBeVisible();
      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
    });
  });

  describe('owner is Application', () => {
    beforeAll(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: 'broadcast-app',
            },
          },
        },
      };
    });

    it('tile is not rendered', () => {
      renderComponent(preloadedState);
      expect(screen.queryByText('Data Retention')).not.toBeInTheDocument();
    });
  });

  describe('owner is Organization', () => {
    beforeAll(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: 'broadcast-org',
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock.onGet(getRetentionPoliciesUrl(ownerId)).reply(200, {
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: true,
              enablePurging: true,
              maxAge: '3 months',
            },
          },
        },
        successMetrics: {
          inheritPolicy: true,
          enablePurging: true,
          maxAge: '1 year',
        },
      });
      renderComponent(preloadedState);
    });

    it('renders tile with proper header', async () => {
      expect(await screen.findByText('Data Retention')).toBeVisible();
      expect(await screen.findByText('applying to broadcast-org')).toBeVisible();
    });

    it('renders success metrics data', async () => {
      expect(await screen.findByText('Success Metrics')).toBeVisible();

      const successMetricsElement = await screen.findByTestId('success-metrics-value');
      expect(successMetricsElement.textContent).toEqual('Max Age: 1 year');
    });

    it('edit retention button is visible and navigates to edit retention page', async () => {
      const editBtn = await screen.findByRole('button', { name: 'Edit' });
      expect(editBtn).not.toBeNull();
      fireEvent.click(editBtn);

      expect(goToEditRetentionSpy).toHaveBeenCalled();
    });
  });
});
