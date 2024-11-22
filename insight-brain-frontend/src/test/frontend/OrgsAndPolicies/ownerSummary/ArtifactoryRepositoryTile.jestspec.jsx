/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import ArtifactoryRepositoryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ArtifactoryRepositoryTile';
import { getArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';

describe('ArtifactoryRepositoryTile', () => {
  let renderComponent;
  let mockAxiosCalls;
  let state;
  const ownerType = 'organization';
  const ownerId = 'organizationId';
  const repositoryConnectionUrl = getArtifactoryConnectionUrl(ownerType, ownerId, null, true);
  const disabledRepositoryConnectionRequestPayload = {
    artifactoryConnection: null,
    artifactoryConnectionStatus: {
      enabled: false,
      inheritedFromOrganizationId: null,
      inheritedFromOrganizationName: null,
      allowOverride: true,
      inheritedFromOrgEnabled: null,
      allowChange: true,
    },
    ownerDTO: {
      ownerPublicId: 'ROOT_ORGANIZATION_ID',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'ORGANIZATION',
    },
  };

  const enabledRepositoryConnectionRequestPayload = {
    artifactoryConnection: {
      artifactoryConnectionId: '2a6f33074bf44146a32cf3d13d566e82',
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
      isAnonymous: true,
      baseUrl: 'http://test.com',
      username: null,
    },
    artifactoryConnectionStatus: {
      enabled: true,
      inheritedFromOrganizationId: null,
      inheritedFromOrganizationName: null,
      allowOverride: true,
      inheritedFromOrgEnabled: null,
      allowChange: true,
    },
    ownerDTO: {
      ownerPublicId: 'ROOT_ORGANIZATION_ID',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'ORGANIZATION',
    },
  };

  const enabledEmptyRepositoryConnectionRequestPayload = {
    artifactoryConnection: null,
    artifactoryConnectionStatus: {
      enabled: true,
      inheritedFromOrganizationId: null,
      inheritedFromOrganizationName: null,
      allowOverride: true,
      inheritedFromOrgEnabled: null,
      allowChange: true,
    },
    ownerDTO: {
      ownerPublicId: 'ROOT_ORGANIZATION_ID',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'ORGANIZATION',
    },
  };

  const inheritedRepositoryConnectionRequestPayload = {
    artifactoryConnection: {
      artifactoryConnectionId: '2a6f33074bf44146a32cf3d13d566e82',
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
      isAnonymous: true,
      baseUrl: 'http://test.com',
      username: null,
    },
    artifactoryConnectionStatus: {
      enabled: null,
      inheritedFromOrganizationId: 'ROOT_ORGANIZATION_ID',
      inheritedFromOrganizationName: 'Root Organization',
      allowOverride: true,
      inheritedFromOrgEnabled: true,
      allowChange: true,
    },
    ownerDTO: {
      ownerPublicId: '29a1ad4fc87d4492ae3690dd03e520dc',
      ownerId: '29a1ad4fc87d4492ae3690dd03e520dc',
      ownerName: '1st Level Child',
      ownerType: 'ORGANIZATION',
    },
  };

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      productFeatures: {
        productFeatures: { 'built-from-source': true },
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

    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, enabledRepositoryConnectionRequestPayload);
    renderComponent = (preloadedState = state) => render(<ArtifactoryRepositoryTile />, { preloadedState });
  });

  it('renders loading indicator and handles error', async () => {
    mockAxiosCalls.reset();
    mockAxiosCalls
      .onGet(repositoryConnectionUrl)
      .replyOnce(404)
      .onGet()
      .reply(200, enabledRepositoryConnectionRequestPayload);

    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
    expect(await screen.findByRole('alert', /An error occurred loading data. Error 404/i)).toBeVisible();

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Artifactory Repository')).toBeVisible();
  });

  it('renders configured repository', async () => {
    renderComponent();
    expect(await screen.findByText('http://test.com')).toBeVisible();
  });

  it('renders inherited list header', async () => {
    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, inheritedRepositoryConnectionRequestPayload);
    renderComponent();

    expect(await screen.findByText(`Inherited from Root Organization`)).toBeVisible();
  });

  it('renders local list header', async () => {
    renderComponent();

    expect(await screen.findByText('Local')).toBeVisible();
  });

  it('renders no connections are configured text', async () => {
    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, enabledEmptyRepositoryConnectionRequestPayload);
    renderComponent();
    expect(await screen.findByText('No Artifactory repository connection is configured')).toBeVisible();
  });

  it('renders artifactory not enabled text', async () => {
    mockAxiosCalls.onGet(repositoryConnectionUrl).reply(200, disabledRepositoryConnectionRequestPayload);
    renderComponent();
    expect(await screen.findByText('Artifactory repository connection is disabled')).toBeVisible();
  });
});
