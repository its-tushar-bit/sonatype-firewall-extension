/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, axiosMockAdapter, within, waitFor } from 'TestRoot/SpecUtil';
import { getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import AutoWaiverDetails from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverDetails';
import { lensPath, set } from 'ramda';
import { FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';

describe('Auto Waiver Details', function () {
  let axiosMock,
    renderComponent,
    autoWaiverDetails,
    publicId,
    ownerName,
    initialState,
    expectedAutoWaiverDetailsUrl,
    hrefSpy,
    ownerType,
    autoWaiverId,
    autoWaiverOwnerId,
    organizationId;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    ownerType = 'organization';
    publicId = 'public-id';
    ownerName = 'owner-name';
    autoWaiverId = 'autowaiver-id';
    autoWaiverOwnerId = 'ROOT_ORGANIZATION_ID';
    organizationId = 'ROOT_ORGANIZATION_ID';

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
      autoPolicyWaiverId: autoWaiverId,
      ownerId: autoWaiverOwnerId,
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
      productFeatures: {
        productFeatures: { 'auto-waiver-management': true },
      },
      router: {
        currentParams: {
          autoWaiverId,
          autoWaiverOwnerId,
          organizationId,
          ownerType,
        },
        currentState: {
          name: 'management.edit.organization.auto-waiver-details',
          data: {
            title: 'Organization Auto Waiver Details',
          },
        },
      },
      orgsAndPolicies: {
        root: {
          loading: false,
          loadError: null,
          selectedOwner: {
            name: 'Root Organization',
            nameLowercaseNoWhitespace: 'rootorganization',
            id: 'ROOT_ORGANIZATION_ID',
            parentOrganizationId: null,
            legacyViolationEnabled: null,
            allowLegacyViolationOverride: true,
            repositoryConnectionEnabled: null,
            allowRepositoryConnectionOverride: true,
            artifactoryConnectionEnabled: null,
            allowArtifactoryConnectionOverride: true,
          },
          policiesByOwner: null,
        },
      },
    };

    expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, autoWaiverOwnerId, autoWaiverId);
    renderComponent = (preloadedState = initialState) => render(<AutoWaiverDetails />, { preloadedState });
  });

  describe('has a loading error', () => {
    it('it renders an error with the error message and a retry button', async () => {
      axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(404, 'some error');

      await waitFor(() => {
        renderComponent();
      });

      // hits one for auto waiver details and one for exclusions
      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[1].url).toBe(expectedAutoWaiverDetailsUrl);

      expect(await screen.findByRole('alert')).toBeInTheDocument();
      expect(await screen.findByText(/Some error/i)).toBeInTheDocument();

      const retryButton = await screen.findByRole('button', { name: 'Retry' });
      expect(retryButton).toBeInTheDocument();
      fireEvent.click(retryButton);

      expect(screen.getByText('Loading…')).toBeInTheDocument();
      expect(await screen.findByRole('alert')).toBeInTheDocument();
      expect(axiosMock.history.get.length).toBe(3);
      expect(axiosMock.history.get[2].url).toBe(expectedAutoWaiverDetailsUrl);
    });
  });

  describe('successfully loads waiver details', () => {
    it('it renders the expected auto waiver details', async function () {
      axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, autoWaiverDetails);
      await waitFor(() => {
        renderComponent();
      });

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

        expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, autoWaiverOwnerId, autoWaiverId);

        const mockResponse = { ...autoWaiverDetails, ownerType, publicId };
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, mockResponse);

        initialState = {
          router: {
            currentParams: {
              ownerType,
              autoWaiverOwnerId,
              autoWaiverId,
              type: 'autoWaiver',
              publicId,
              ownerId: autoWaiverOwnerId,
              waiverId: autoWaiverId,
            },
            currentState: {
              name: 'waiver.details',
            },
          },
        };

        await waitFor(() => {
          renderComponent();
        });

        const scope = await screen.findByRole('definition', { name: 'Scope' });
        expect(scope).toBeInTheDocument();

        const ownerManagementLink = within(scope).getByRole('link', { name: 'owner-name' });
        expect(ownerManagementLink).toBeInTheDocument();
        expect(ownerManagementLink).toHaveAttribute('href', '#/management/view/application/app-public-id');
      });

      it('renders a link to the owner management page for organization if ownerType is organization', async () => {
        ownerType = 'organization';
        publicId = 'org-public-id';

        expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, autoWaiverOwnerId, autoWaiverId);

        const mockResponse = { ...autoWaiverDetails, ownerType, publicId };
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, mockResponse);

        initialState = {
          router: {
            currentParams: {
              ownerType,
              autoWaiverOwnerId,
              autoWaiverId,
              type: 'autoWaiver',
              publicId,
              ownerId: autoWaiverOwnerId,
              waiverId: autoWaiverId,
            },
            currentState: {
              name: 'waiver.details',
            },
          },
        };

        await waitFor(() => {
          renderComponent();
        });

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
        await waitFor(() => {
          renderComponent();
        });

        const reason = await screen.findByRole('definition', { name: 'Reason' });
        expect(reason).toBeInTheDocument();
        expect(reason).toHaveTextContent('N/A');
      });

      it('renders "No upgrade path" only when pathForward is true and reachable is false', async () => {
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, { ...autoWaiverDetails, reachability: false });
        await waitFor(() => {
          renderComponent();
        });

        const reason = await screen.findByRole('definition', { name: 'Reason' });
        expect(reason).toBeInTheDocument();
        expect(reason).toHaveTextContent('No upgrade path');
        expect(reason).not.toHaveTextContent('Not reachable');
      });

      it('renders "Not reachable" only when pathForward is false and reachable is true', async () => {
        axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, { ...autoWaiverDetails, pathForward: false });
        await waitFor(() => {
          renderComponent();
        });

        const reason = await screen.findByRole('definition', { name: 'Reason' });
        expect(reason).toBeInTheDocument();
        expect(reason).toHaveTextContent('Not reachable');
        expect(reason).not.toHaveTextContent('No upgrade path');
      });
    });
  });

  describe('action buttons', () => {
    it('renders edit and delete buttons', async () => {
      await waitFor(() => {
        renderComponent();
      });

      expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
    });

    describe('edit button', () => {
      it('opens the auto-waiver modal in edit mode', async () => {
        await waitFor(() => {
          renderComponent();
        });

        const editButton = screen.getByRole('button', { name: 'Edit' });
        fireEvent.click(editButton);

        const autoWaiverModal = screen.getByTestId('iq-auto-waiver-modal');
        expect(autoWaiverModal).toBeInTheDocument();
        expect(within(autoWaiverModal).getByRole('heading', { name: 'Edit Auto-Waiver' })).toBeInTheDocument();
      });

      // Disabled flaky test https://sonatype.atlassian.net/browse/SDEV-1988
      xit('is disabled with a tooltip when waiver is inherited', async () => {
        const selectedOwnerLens = lensPath(['orgsAndPolicies', 'root', 'selectedOwner', 'id']);
        const newState = set(selectedOwnerLens, 'some-other-owner', initialState);

        await waitFor(() => {
          renderComponent(newState);
        });

        const editButton = screen.getByRole('button', { name: 'Edit' });
        expect(editButton).toHaveClass('disabled');

        fireEvent.mouseOver(editButton);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toBeInTheDocument();
        expect(tooltip).toHaveTextContent('Cannot edit an inherited auto-waiver');
      });
    });

    describe('delete button', () => {
      it('opens the delete auto-waiver modal', async () => {
        await waitFor(() => {
          renderComponent();
        });

        const deleteButton = screen.getByRole('button', { name: 'Delete' });
        fireEvent.click(deleteButton);

        const deleteAutoWaiverModal = screen.getByTestId('iq-delete-auto-waiver-modal');
        expect(deleteAutoWaiverModal).toBeInTheDocument();
        expect(within(deleteAutoWaiverModal).getByRole('heading', { name: 'Delete Auto-Waiver' })).toBeInTheDocument();
      });

      // Disabled flaky test https://sonatype.atlassian.net/browse/SDEV-1988
      xit('is disabled with a tooltip when waiver is inherited', async () => {
        const selectedOwnerLens = lensPath(['orgsAndPolicies', 'root', 'selectedOwner', 'id']);
        const newState = set(selectedOwnerLens, 'some-other-owner', initialState);

        await waitFor(() => {
          renderComponent(newState);
        });

        const deleteButton = screen.getByRole('button', { name: 'Delete' });
        expect(deleteButton).toHaveClass('disabled');

        fireEvent.mouseOver(deleteButton);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toBeInTheDocument();
        expect(tooltip).toHaveTextContent('Cannot delete an inherited auto-waiver');
      });
    });
  });

  describe('when viewing auto-waiver details from the waiver detail page', () => {
    beforeEach(() => {
      initialState = {
        router: {
          currentParams: {
            autoWaiverId,
            autoWaiverOwnerId,
            organizationId,
            ownerType,
          },
          currentState: {
            name: 'waiver.details',
          },
        },
      };
    });

    it('does not render the edit and delete buttons', async () => {
      await waitFor(() => {
        renderComponent();
      });

      expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    });

    it('does not render the exclusion log table', async () => {
      await waitFor(() => {
        renderComponent();
      });

      expect(screen.queryByRole('heading', { name: 'Exclusion Log' })).not.toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
  });

  it('renders the exclusion log table', async () => {
    axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, autoWaiverDetails);
    await waitFor(() => {
      renderComponent();
    });

    const exclusionLogHeader = screen.getByRole('heading', { name: 'Exclusion Log' });
    expect(exclusionLogHeader).toBeInTheDocument();

    const exclusionLogTable = await screen.findByRole('table');
    expect(exclusionLogTable).toBeInTheDocument();
  });

  describe('when viewing auto-waiver details from Firewall dashboard (FIREWALL_WAIVER_DETAILS route)', () => {
    beforeEach(() => {
      initialState = {
        router: {
          currentParams: {
            ownerId: autoWaiverOwnerId,
            waiverId: autoWaiverId,
            ownerType,
            type: 'autoWaiver',
          },
          currentState: {
            name: FIREWALL_WAIVER_DETAILS,
          },
        },
      };

      // API should be called with ownerId and waiverId (not autoWaiverOwnerId/autoWaiverId)
      expectedAutoWaiverDetailsUrl = getAutoWaiversConfigurationURLWaiver(ownerType, autoWaiverOwnerId, autoWaiverId);
      axiosMock.onGet(expectedAutoWaiverDetailsUrl).reply(200, autoWaiverDetails);
    });

    it('loads and renders auto-waiver details correctly', async () => {
      await waitFor(() => {
        renderComponent();
      });

      const autoWaiver = await screen.findByTestId('auto-waiver-details-version');
      expect(autoWaiver).toBeInTheDocument();

      // Verify the correct API endpoint was called
      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe(expectedAutoWaiverDetailsUrl);
    });

    it('does not render edit and delete buttons (read-only view)', async () => {
      await waitFor(() => {
        renderComponent();
      });

      expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    });

    it('does not render the exclusion log table (read-only view)', async () => {
      await waitFor(() => {
        renderComponent();
      });

      expect(screen.queryByRole('heading', { name: 'Exclusion Log' })).not.toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
  });

  describe('owner type normalization', () => {
    it('normalizes "root_organization" to "organization" when calling API', async () => {
      const rootOrgOwnerType = 'root_organization';

      initialState = {
        router: {
          currentParams: {
            autoWaiverId,
            autoWaiverOwnerId,
            organizationId,
            ownerType: rootOrgOwnerType,
          },
          currentState: {
            name: 'management.edit.organization.auto-waiver-details',
          },
        },
        orgsAndPolicies: {
          selectedOwner: {
            id: autoWaiverOwnerId,
          },
        },
      };

      // API should be called with 'organization', not 'root_organization'
      const normalizedUrl = getAutoWaiversConfigurationURLWaiver('organization', autoWaiverOwnerId, autoWaiverId);
      axiosMock.onGet(normalizedUrl).reply(200, autoWaiverDetails);

      await waitFor(() => {
        renderComponent();
      });

      const autoWaiver = await screen.findByTestId('auto-waiver-details-version');
      expect(autoWaiver).toBeInTheDocument();

      // Verify API was called with normalized 'organization' type
      const waiverDetailsCall = axiosMock.history.get.find((call) => call.url === normalizedUrl);
      expect(waiverDetailsCall).toBeDefined();
      expect(waiverDetailsCall.url).toContain('/organization/');
      expect(waiverDetailsCall.url).not.toContain('/root_organization/');
    });

    it('normalizes "root_organization" to "organization" for FIREWALL_WAIVER_DETAILS route', async () => {
      const rootOrgOwnerType = 'root_organization';

      initialState = {
        router: {
          currentParams: {
            ownerId: autoWaiverOwnerId,
            waiverId: autoWaiverId,
            ownerType: rootOrgOwnerType,
            type: 'autoWaiver',
          },
          currentState: {
            name: FIREWALL_WAIVER_DETAILS,
          },
        },
      };

      // API should be called with 'organization', not 'root_organization'
      const normalizedUrl = getAutoWaiversConfigurationURLWaiver('organization', autoWaiverOwnerId, autoWaiverId);
      axiosMock.onGet(normalizedUrl).reply(200, autoWaiverDetails);

      await waitFor(() => {
        renderComponent();
      });

      const autoWaiver = await screen.findByTestId('auto-waiver-details-version');
      expect(autoWaiver).toBeInTheDocument();

      // Verify API was called with normalized 'organization' type
      expect(axiosMock.history.get[0].url).toBe(normalizedUrl);
      expect(axiosMock.history.get[0].url).toContain('/organization/');
      expect(axiosMock.history.get[0].url).not.toContain('/root_organization/');
    });
  });
});
