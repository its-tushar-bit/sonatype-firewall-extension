/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import router from 'MainRoot/router/routerInstance';
import { render, screen, within } from 'TestRoot/SpecUtil';
import OwnerTree from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnerTree';
import { getOwnersMap } from '../ownerSideNav/nLevelMockData';

describe('OwnerTree', () => {
  let minimalProps;
  let renderComponent;
  let state;
  let ownerId;
  let ownersMap;
  let mockIncludes;

  const fakeRouterState = (url, params) => {
    const isOrganization = url.includes('organization');
    const isSbomManager = mockIncludes('sbomManager');
    const ownerType = isOrganization ? 'organization' : 'application';
    const id = isOrganization ? params.organizationId : params?.applicationPublicId;
    return `#${isSbomManager ? '/sbomManager' : ''}/management/view/${ownerType}/${id}`;
  };

  beforeEach(() => {
    mockIncludes = jest.fn(() => false);
    jest.spyOn(router.stateService, 'href').mockImplementation(fakeRouterState);
    jest.spyOn(router.stateService, 'includes').mockImplementation(mockIncludes);
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);
    ownersMap = getOwnersMap(3, false);
    state = {
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: ownersMap,
        },
        ownersTree: {
          searchTerm: '',
        },
      },
    };

    ownerId = 'ROOT_ORGANIZATION_ID';
    minimalProps = {
      ownerId,
    };
    renderComponent = (preloadedState = state, additionalProps = {}) =>
      render(<OwnerTree {...minimalProps} {...additionalProps} />, { preloadedState });
  });

  it('should not render OwnerTree', () => {
    minimalProps.ownerId = '';
    renderComponent();

    expect(screen.queryByRole('treeitem')).toBe(null);
  });

  it('renders correct amount of tree nodes', () => {
    renderComponent();

    expect(screen.getAllByRole('treeitem').length).toBe(Object.values(ownersMap).length);
    Object.values(ownersMap).forEach(({ name, type }) => {
      const treeItem = screen.getByRole('treeitem', { name: name });
      expect(treeItem).toBeVisible();
      const label = within(treeItem).getAllByTestId('owners-tree-item-label')[0];

      const images = within(label).getAllByRole('img', { hidden: true });
      const ownerIcon = images[images.length - 1];
      expect(ownerIcon).toBeVisible();
      type === 'application'
        ? expect(ownerIcon).toHaveAttribute('data-icon', 'terminal')
        : expect(ownerIcon).toHaveAttribute('data-icon', 'sitemap');
    });
  });

  it('renders correct amount of filtered tree nodes with repositories', () => {
    renderComponent(state, { shouldDisplayRepositories: true });

    expect(screen.getAllByRole('treeitem').length).toBe(Object.values(ownersMap).length + 1);

    const repositoriesTreeItem = screen.getByRole('treeitem', { name: 'Repositories' });
    expect(repositoriesTreeItem).toBeVisible();

    Object.values(ownersMap).forEach(({ name, type }) => {
      const treeItem = screen.getByRole('treeitem', { name: name });
      expect(treeItem).toBeVisible();
      const label = within(treeItem).getAllByTestId('owners-tree-item-label')[0];

      const images = within(label).getAllByRole('img', { hidden: true });
      const ownerIcon = images[images.length - 1];
      expect(ownerIcon).toBeVisible();
      type === 'application'
        ? expect(ownerIcon).toHaveAttribute('data-icon', 'terminal')
        : expect(ownerIcon).toHaveAttribute('data-icon', 'sitemap');
    });
  });

  it('renders correct amount of filtered tree nodes', () => {
    const preloadedState = { ...state };
    preloadedState.orgsAndPolicies.ownersTree.searchTerm = 'organization name 1';
    preloadedState.orgsAndPolicies.ownersTree.filteredOwners = ['ROOT_ORGANIZATION_ID', 'organization id 1'];

    renderComponent(preloadedState);

    expect(screen.getAllByRole('treeitem').length).toBe(2);
  });

  it('renders empty message when the filter has no results', () => {
    const preloadedState = { ...state };
    preloadedState.orgsAndPolicies.ownersTree.searchTerm = 'asdasd';
    preloadedState.orgsAndPolicies.ownersTree.filteredOwners = [];

    renderComponent(preloadedState);

    expect(screen.queryByRole('treeitem')).toBeNull();
    expect(screen.getByText('No matching results')).toBeVisible();
  });

  it('renders correct amount of clickable tree nodes', () => {
    renderComponent();

    Object.values(ownersMap).forEach((owner) => {
      const aTag = screen.getByText(owner.name).closest('a');
      expect(aTag).toHaveAttribute('href', getExpectedHref(owner));
    });
  });

  it('renders correct amount of clickable tree nodes with sbomManager url', () => {
    mockIncludes.mockImplementation((stateName) => stateName === 'sbomManager');
    renderComponent();

    Object.values(ownersMap).forEach((owner) => {
      const aTag = screen.getByText(owner.name).closest('a');
      expect(aTag).toHaveAttribute('href', getExpectedHref(owner, true));
    });
  });

  it('renders non clickable for synthetic orgs', () => {
    // make all organization into synthetic
    Object.values(ownersMap).forEach((owner) => {
      const isOrganization = owner.type === 'organization';
      if (isOrganization) {
        ownersMap[owner.id].synthetic = true;
      }
    });

    renderComponent();

    Object.values(ownersMap).forEach((owner) => {
      if (owner.type === 'organization') {
        expect(screen.getByText(owner.name).closest('a')).toBe(null);
      } else {
        const aTag = screen.getByText(owner.name).closest('a');
        expect(aTag).toHaveAttribute('href', getExpectedHref(owner));
      }
    });
  });
});

const getExpectedHref = (owner, isSbomManager) => {
  const isOrganization = owner.type === 'organization';
  const id = isOrganization ? owner.id : owner.publicId;
  return `#${isSbomManager ? '/sbomManager' : ''}/management/view/${owner.type}/${id}`;
};
