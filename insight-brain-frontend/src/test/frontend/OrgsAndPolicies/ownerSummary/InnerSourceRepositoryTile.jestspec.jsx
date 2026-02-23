/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import InnerSourceRepositoryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/InnerSourceRepositoryTile';
import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';

import 'TestRoot/SpecUtil';

describe('InnerSourceRepositoryTile', () => {
  let renderComponent;
  let mockAxiosCalls;
  let repositoryConnectionRequestPayload;
  let state;
  let goToEditInnerSourceRepositoryPageSpy;
  const ownerType = 'organization';
  const ownerId = 'organizationId';
  const repositoryConnectionUrl = getRepositoryConnectionUrl(ownerType, ownerId, null, true);

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  beforeEach(() => {
    goToEditInnerSourceRepositoryPageSpy = jest.spyOn(actions, 'goToEditPage');
    state = {
      productFeatures: {
        productFeatures: {
          'inner-source-repositories': true,
          'inner-source-repository-integration': true,
        },
      },
      router: {
        currentParams: { organizationId: ownerId },
        currentState: { name: ownerType },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: ownerId,
            name: ownerType,
          },
        },
      },
    };
    repositoryConnectionRequestPayload = {
      repositoryConnections: [
        {
          repositoryConnectionId: 'c7fba1d4d918401d876bfb503a4e0ef2',
          ownerType: 'organization',
          ownerId: 'ROOT_ORGANIZATION_ID',
          format: 'maven',
          isAnonymous: true,
          baseUrl: 'adfdsf@sd.com',
          username: null,
        },
        {
          repositoryConnectionId: '134de43ccf6e4c0aad85cb2d32f9c240',
          ownerType: 'organization',
          ownerId: 'ROOT_ORGANIZATION_ID',
          format: 'npm',
          isAnonymous: true,
          baseUrl: 'sdf@s.com',
          username: null,
        },
      ],
      repositoryConnectionStatus: {
        enabled: null,
        inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
        inheritedFromOrganizationName: 'Root Organization',
        allowOverride: false,
        inheritedFromOrgEnabled: true,
        allowChange: true,
      },
    };
    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, repositoryConnectionRequestPayload);
    renderComponent = (preloadedState = state) => render(<InnerSourceRepositoryTile />, { preloadedState });
  });

  it('renders loading indicator and handles error', async () => {
    mockAxiosCalls.reset();
    mockAxiosCalls.onGet(repositoryConnectionUrl).replyOnce(404).onGet().reply(200, repositoryConnectionRequestPayload);

    renderComponent();

    expect(screen.getByText('Loading…')).toBeInTheDocument();
    expect(await screen.findByRole('alert', /An error occurred loading data. Error 404/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('InnerSource Repositories')).toBeInTheDocument();
  });

  it('renders list of configured repositories', async () => {
    renderComponent();

    for (const repository of repositoryConnectionRequestPayload.repositoryConnections) {
      expect(await screen.findByText(repository.baseUrl)).toBeInTheDocument();
      expect(screen.getByText(repository.format)).toBeInTheDocument();
    }
  });

  it('renders inherited list header', async () => {
    renderComponent();

    expect(
      await screen.findByText(
        `Inherited from ${repositoryConnectionRequestPayload.repositoryConnectionStatus.inheritedFromOrganizationName}`
      )
    ).toBeInTheDocument();
  });

  it('renders local list header', async () => {
    mockAxiosCalls.reset();
    repositoryConnectionRequestPayload.repositoryConnectionStatus.inheritedFromOrganizationName = null;
    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, repositoryConnectionRequestPayload);

    renderComponent();

    expect(await screen.findByText('Local')).toBeInTheDocument();
  });

  it('renders no connections are configured text', async () => {
    mockAxiosCalls.reset();
    repositoryConnectionRequestPayload.repositoryConnections = [];
    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, repositoryConnectionRequestPayload);

    renderComponent();

    expect(await screen.findByText('No InnerSource repository connections are configured')).toBeInTheDocument();
  });

  it('renders innerSource not enabled text', async () => {
    renderComponent();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit' }));

    expect(goToEditInnerSourceRepositoryPageSpy).toHaveBeenCalledTimes(1);
  });

  describe('InnerSource Repositories Feature is Disabled', () => {
    const ownerId = 'e270271429f747ef9bebf4ca88f5e6c0';

    it('does not render with a Firewall only license', async () => {
      const state = {
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

      renderComponent(state);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('InnerSource Repositories');
      expect(title).toBeNull();
    });

    it('renders with a non-Firewall only license test', async () => {
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('InnerSource Repositories');
      expect(title).not.toBeNull();
    });
  });
});
