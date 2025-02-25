/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import { getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
import AutoWaiverDetails from 'MainRoot/waivers/waiverDetails/AutoWaiverDetails';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('AutoWaiverDetailsPage', function () {
  let axiosMock,
    renderComponent,
    autoWaiverDetails,
    ownerType,
    ownerId,
    waiverId,
    publicId,
    ownerName,
    initialState,
    expectedAutoWaiverDetailsUrl,
    hrefSpy;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    ownerType = 'organization';
    ownerId = 'owner-id';
    waiverId = 'autowaiver-id';
    publicId = 'public-id';
    ownerName = 'owner-name';

    hrefSpy = jest.fn('href').mockImplementation((state, params) => {
      if (state === 'management.view.organization') {
        return `#/management/view/organization/${params.organizationId}`;
      } else if (state === 'management.view.application') {
        return `#/management/view/application/${params.applicationPublicId}`;
      }
    });
    const routerContextMock = { href: hrefSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);

    autoWaiverDetails = {
      autoPolicyWaiverId: waiverId,
      ownerId,
      threatLevel: 7,
      reachability: true,
      pathForward: true,
      creatorId: 'admin',
      creatorName: 'Admin BuiltIn',
      createTime: '2024-11-13T14:47:07.275+0000',
      publicId,
      ownerName,
    };

    initialState = {
      router: {
        currentParams: {
          ownerType,
          ownerId,
          waiverId,
          type: 'autoWaiver',
          publicId,
        },
        currentState: {
          name: 'waiver.details',
        },
      },
    };

    expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiverId);
    renderComponent = (preloadedState = initialState) => render(<AutoWaiverDetails />, { preloadedState });
  });

  describe('has a loading error', () => {
    it('it renders an error with the error message and a retry button', async () => {
      axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(404, 'some error');

      renderComponent();

      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe(expectedAutoWaiverDetailsUrl);

      expect(screen.getByText('Loading…')).toBeInTheDocument();

      expect(await screen.findByRole('alert')).toBeInTheDocument();
      expect(await screen.findByText(/Some error/i)).toBeInTheDocument();

      const retryButton = await screen.findByRole('button', { name: 'Retry' });
      expect(retryButton).toBeInTheDocument();
      fireEvent.click(retryButton);

      expect(screen.getByText('Loading…')).toBeInTheDocument();
      expect(await screen.findByRole('alert')).toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[1].url).toBe(expectedAutoWaiverDetailsUrl);
    });
  });

  describe('successfully loads waiver details', () => {
    it('it renders the expected auto waiver details', async function () {
      axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, autoWaiverDetails);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeInTheDocument();
      const autoWaiver = await screen.findByTestId('auto-waiver-details-version');
      expect(autoWaiver).toBeInTheDocument();

      const policyThreatLevel = screen.getByRole('definition', { name: 'Policy Threat Level' });
      expect(policyThreatLevel).toBeInTheDocument();
      expect(policyThreatLevel).toHaveTextContent('≤ 7');

      const scope = screen.getByRole('definition', { name: 'Scope' });
      expect(scope).toBeInTheDocument();
      expect(scope).toHaveTextContent('owner-name');

      const ownerManagementLink = within(scope).getByRole('link', { name: 'owner-name' });
      expect(ownerManagementLink).toBeInTheDocument();
      expect(ownerManagementLink).toHaveAttribute('href', '#/management/view/organization/public-id');

      const expiration = screen.getByRole('definition', { name: 'Expiration' });
      expect(expiration).toBeInTheDocument();
      expect(expiration).toHaveTextContent('Auto');

      const components = screen.getByRole('definition', { name: 'Component(s)' });
      expect(components).toBeInTheDocument();

      const version = screen.getByRole('definition', { name: 'Version' });
      expect(version).toBeInTheDocument();
      expect(version).toHaveTextContent('Current or latest non-violating');

      const reason = screen.getByRole('definition', { name: 'Reason' });
      expect(reason).toBeInTheDocument();
      expect(reason).toHaveTextContent('No upgrade path');
      expect(reason).toHaveTextContent('Not reachable');

      const dateCreated = screen.getByRole('definition', { name: 'Date Created' });
      expect(dateCreated).toBeInTheDocument();
      expect(dateCreated).toHaveTextContent('November 13, 2024');
    });

    describe('scope section', () => {
      it('renders a link to the owner management page for application if ownerType is application', async () => {
        ownerType = 'application';
        publicId = 'app-public-id';

        expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiverId);

        const mockResponse = { ...autoWaiverDetails, ownerType, publicId };
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, mockResponse);

        initialState = {
          router: {
            currentParams: {
              ownerType,
              ownerId,
              waiverId,
              type: 'autoWaiver',
              publicId,
            },
            currentState: {
              name: 'waiver.details',
            },
          },
        };

        renderComponent();

        const scope = await screen.findByRole('definition', { name: 'Scope' });
        expect(scope).toBeInTheDocument();

        const ownerManagementLink = within(scope).getByRole('link', { name: 'owner-name' });
        expect(ownerManagementLink).toBeInTheDocument();
        expect(ownerManagementLink).toHaveAttribute('href', '#/management/view/application/app-public-id');
      });

      it('renders a link to the owner management page for organization if ownerType is organization', async () => {
        ownerType = 'organization';
        publicId = 'org-public-id';

        expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, ownerId, waiverId);

        const mockResponse = { ...autoWaiverDetails, ownerType, publicId };
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, mockResponse);

        initialState = {
          router: {
            currentParams: {
              ownerType,
              ownerId,
              waiverId,
              type: 'autoWaiver',
              publicId,
            },
            currentState: {
              name: 'waiver.details',
            },
          },
        };

        renderComponent();

        const scope = await screen.findByRole('definition', { name: 'Scope' });
        expect(scope).toBeInTheDocument();

        const ownerManagementLink = within(scope).getByRole('link', { name: 'owner-name' });
        expect(ownerManagementLink).toBeInTheDocument();
        expect(ownerManagementLink).toHaveAttribute('href', '#/management/view/organization/org-public-id');
      });
    });

    describe('reason section', () => {
      it('renders N/A when pathForward and reachable are false', async () => {
        axiosMock
          .onGet(expectedAutoWaiverDetailsUrl)
          .reply(200, { ...autoWaiverDetails, pathForward: false, reachability: false });
        renderComponent();

        const reason = await screen.findByRole('definition', { name: 'Reason' });
        expect(reason).toBeInTheDocument();
        expect(reason).toHaveTextContent('N/A');
      });

      it('renders "No upgrade path" only when pathForward is true and reachable is false', async () => {
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, { ...autoWaiverDetails, reachability: false });
        renderComponent();

        const reason = await screen.findByRole('definition', { name: 'Reason' });
        expect(reason).toBeInTheDocument();
        expect(reason).toHaveTextContent('No upgrade path');
        expect(reason).not.toHaveTextContent('Not reachable');
      });

      it('renders "Not reachable" only when pathForward is false and reachable is true', async () => {
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, { ...autoWaiverDetails, pathForward: false });
        renderComponent();

        const reason = await screen.findByRole('definition', { name: 'Reason' });
        expect(reason).toBeInTheDocument();
        expect(reason).toHaveTextContent('Not reachable');
        expect(reason).not.toHaveTextContent('No upgrade path');
      });
    });
  });
});
