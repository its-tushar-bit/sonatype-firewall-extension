/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, axiosMockAdapter } from 'TestRoot/SpecUtil';
import OwnerTreePage from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnersTreePage';
import { getOwnersMap } from '../ownerSideNav/nLevelMockData';
import { getOwnerListUrl, getRepositoriesUrl } from 'MainRoot/util/CLMLocation';

describe('OwnerTreePage', () => {
  let mockAxiosCalls;
  let renderComponent;
  let state;
  let ownersMap = getOwnersMap(3);
  const ownerListUrl = getOwnerListUrl();
  const topParentOrganizationId = 'ROOT_ORGANIZATION_ID';
  const ownerListPayload = { topParentOrganizationId, ownersMap };

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      router: {
        prevState: {
          name: 'management.view.organization',
          url: '/organization/{organizationId}',
        },
        prevParams: {
          organizationId: 'organization id 1',
        },
        currentParams: {},
        currentState: {
          name: 'management.tree',
        },
      },
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: {},
        },
      },
    };

    renderComponent = (preloadedState = state) => render(<OwnerTreePage />, { preloadedState });
  });

  it('renders loading indicator and handles error', async () => {
    mockAxiosCalls.reset();

    // ownerListUrl request error
    mockAxiosCalls.onGet(ownerListUrl).replyOnce(404).onGet(ownerListUrl).reply(200, ownerListPayload);
    mockAxiosCalls.onGet(getRepositoriesUrl()).reply(200, { repositories: [] });

    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
    expect(await screen.findByRole('alert', /An error occurred loading data. Error 404/i)).toBeVisible();

    // no errors
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByLabelText('ROOT_ORGANIZATION_ID-title')).toBeVisible();
  });
});
