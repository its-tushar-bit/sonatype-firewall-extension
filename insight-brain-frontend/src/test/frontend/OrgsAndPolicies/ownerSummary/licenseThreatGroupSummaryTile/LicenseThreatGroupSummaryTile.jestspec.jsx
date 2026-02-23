/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, axiosMockAdapter, within, screen, fireEvent } from 'TestRoot/SpecUtil';
import {
  organizationWithoutLtgsByOwnerPayload,
  rootOrganizationLtgsByOwnerPayload,
  organizationWithMultipleLtgsByOwnerPayload,
  applicationWithoutLtgsByOwnerPayload,
  applicationWithLtgsByOwnerPayload,
} from './licenseThreatGroupSummaryTileMockData';

import LicenseThreatGroupSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/LicenseThreatGroupSummaryTile';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import { getApplicableLicenseGroupsUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('LicenseThreatGroupSummaryTile', () => {
  let axiosMock, goToNewLTGSpy, preloadedState, ownerName, ownerId, ownerType;

  const renderComponent = (preloadedState) => render(<LicenseThreatGroupSummaryTile />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    goToNewLTGSpy = jest.spyOn(actions, 'goToCreateLTG');
  });

  describe('Loading and retry logic', () => {
    beforeAll(() => {
      ownerName = rootOrganizationLtgsByOwnerPayload.ownerName;
      ownerId = rootOrganizationLtgsByOwnerPayload.ownerId;
      ownerType = rootOrganizationLtgsByOwnerPayload.ownerType;

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
              name: ownerName,
            },
          },
        },
      };
    });

    it('renders a loading legend', () => {
      renderComponent(preloadedState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders an alert with retry if something goes wrongs', async () => {
      axiosMock
        .onGet(getApplicableLicenseGroupsUrl(ownerType, ownerId))
        .reply(() => Promise.reject('An error occurred loading data.'));

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

  describe('Owner is Root Organization', () => {
    beforeAll(() => {
      ownerName = rootOrganizationLtgsByOwnerPayload.ownerName;
      ownerId = rootOrganizationLtgsByOwnerPayload.ownerId;
      ownerType = rootOrganizationLtgsByOwnerPayload.ownerType;

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
              name: ownerName,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicableLicenseGroupsUrl(ownerType, ownerId))
        .reply(200, { ...rootOrganizationLtgsByOwnerPayload.ltgs });

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('License Threat Groups')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Threat Group' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToNewLTGSpy).toHaveBeenCalled();
      });
    });
  });

  describe('Owner is Organization with no ltgs', () => {
    beforeAll(() => {
      ownerName = organizationWithoutLtgsByOwnerPayload.ownerName;
      ownerId = organizationWithoutLtgsByOwnerPayload.ownerId;
      ownerType = organizationWithoutLtgsByOwnerPayload.ownerType;

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
              name: ownerName,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicableLicenseGroupsUrl(ownerType, ownerId))
        .reply(200, { ...organizationWithoutLtgsByOwnerPayload.ltgs });

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('License Threat Groups')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Threat Group' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToNewLTGSpy).toHaveBeenCalled();
      });
    });
  });

  describe('Owner is Organization with multiple inherited LTGs', () => {
    beforeAll(() => {
      ownerName = organizationWithMultipleLtgsByOwnerPayload.ownerName;
      ownerId = organizationWithMultipleLtgsByOwnerPayload.ownerId;
      ownerType = organizationWithMultipleLtgsByOwnerPayload.ownerType;

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
              name: ownerName,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicableLicenseGroupsUrl(ownerType, ownerId))
        .reply(200, { ...organizationWithMultipleLtgsByOwnerPayload.ltgs });

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('License Threat Groups')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Threat Group' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToNewLTGSpy).toHaveBeenCalled();
      });
    });
  });

  describe('Owner is Application without LTGs', () => {
    beforeAll(() => {
      ownerName = applicationWithoutLtgsByOwnerPayload.ownerName;
      ownerId = applicationWithoutLtgsByOwnerPayload.ownerId;
      ownerType = applicationWithoutLtgsByOwnerPayload.ownerType;

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
            applicationPublicId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicableLicenseGroupsUrl(ownerType, ownerId))
        .reply(200, { ...applicationWithoutLtgsByOwnerPayload.ltgs });

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('License Threat Groups')).toBeVisible();
      });

      it('Add Policy button is not render', async () => {
        const addButton = await screen.queryByRole('button', { name: 'Add a Threat Group' });
        expect(addButton).not.toBeInTheDocument();
      });
    });
  });

  describe('Owner is Application with LTGs', () => {
    beforeAll(() => {
      ownerName = applicationWithLtgsByOwnerPayload.ownerName;
      ownerId = applicationWithLtgsByOwnerPayload.ownerId;
      ownerType = applicationWithLtgsByOwnerPayload.ownerType;

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
            applicationPublicId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicableLicenseGroupsUrl(ownerType, ownerId))
        .reply(200, { ...applicationWithLtgsByOwnerPayload.ltgs });

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('License Threat Groups')).toBeVisible();
      });

      it('Add Policy button is not render', async () => {
        const addButton = await screen.queryByRole('button', { name: 'Add a Threat Group' });
        expect(addButton).not.toBeInTheDocument();
      });
    });
  });
});
