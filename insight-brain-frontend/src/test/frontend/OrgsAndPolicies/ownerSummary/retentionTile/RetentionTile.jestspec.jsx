/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RetentionTile from 'MainRoot/OrgsAndPolicies/ownerSummary/retentionTile/RetentionTile';
import { render, axiosMockAdapter, within, screen, fireEvent, waitFor } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/OrgsAndPolicies/retentionSlice';
import { getRetentionPoliciesUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('RetentionTile', () => {
  let axiosMock, goToEditRetentionSpy, preloadedState;
  const ownerId = 'e270271429f747ef9bebf4ca88f5e6c0';
  const renderComponent = (preloadedState) => render(<RetentionTile />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    goToEditRetentionSpy = jest.spyOn(actions, 'goToEditRetention');
    jest.spyOn(actions, 'loadRetention');

    axiosMock.onGet(getRetentionPoliciesUrl(ownerId)).reply(200, {});
  });

  describe('Loading and Retry logic', () => {
    beforeAll(() => {
      preloadedState = {
        productFeatures: {
          productFeatures: {
            'data-retention': true,
            'single-tenant': true,
          },
        },
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

      // After retry, component re-fetches and returns to error state
      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
    });
  });

  describe('owner is Application', () => {
    beforeAll(() => {
      preloadedState = {
        productFeatures: {
          productFeatures: {
            'data-retention': true,
            'single-tenant': true,
          },
        },
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
        productFeatures: {
          productFeatures: {
            'data-retention': true,
            'single-tenant': true,
          },
        },
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
      axiosMock.onGet(getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).reply(200, {});
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
      const successMetricsSection = await screen.findByRole('region', { name: /success metrics/i });
      expect(await successMetricsSection).toBeVisible();
      expect(await successMetricsSection).toHaveTextContent(/max age: 1 year/i);
    });

    it('edit retention button is visible and navigates to edit retention page', async () => {
      const editBtn = await screen.findByRole('button', { name: 'Edit' });
      expect(editBtn).not.toBeNull();
      fireEvent.click(editBtn);

      expect(goToEditRetentionSpy).toHaveBeenCalled();
    });
  });

  describe('Data Retention Feature is Disabled', () => {
    it('does not render without Data Retention Feature', async () => {
      preloadedState = {
        productFeatures: {
          productFeatures: {},
        },
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

      renderComponent(preloadedState);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Data Retention');
      expect(title).toBeNull();
    });

    it('renders with Data Retention Feature', async () => {
      preloadedState = {
        productFeatures: {
          productFeatures: {
            'data-retention': true,
          },
        },
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

      renderComponent(preloadedState);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Data Retention');
      expect(title).not.toBeNull();
    });
  });

  describe('multi-tenant', function () {
    it('does not render if product features indicate that this is a multi-tenant deployment', async function () {
      preloadedState = {
        productFeatures: {
          productFeatures: {
            'data-retention': true,
            'multi-tenant': true,
          },
        },
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

      renderComponent(preloadedState);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Data Retention');
      expect(title).toBeNull();
      expect(actions.loadRetention).not.toHaveBeenCalled();
    });

    it('does render if product features indicate that this is a single-tenant deployment', async function () {
      preloadedState = {
        productFeatures: {
          productFeatures: {
            'data-retention': true,
            'single-tenant': true,
          },
        },
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

      renderComponent(preloadedState);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Data Retention');
      expect(title).toBeInTheDocument();
      expect(actions.loadRetention).toHaveBeenCalled();
    });
  });
});
