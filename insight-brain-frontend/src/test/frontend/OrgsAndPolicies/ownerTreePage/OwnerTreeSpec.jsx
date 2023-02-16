/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import RouterStateContext from 'MainRoot/react/RouterStateContext';
import { render, screen, within } from 'TestRoot/SpecUtil';
import OwnerTree from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnerTree';
import { getOwnersMap } from '../ownerSideNav/nLevelMockData';

describe('OwnerTree', () => {
  let minimalProps;
  let renderComponent;
  let state;
  let ownerId;
  let ownersMap;
  let routerContext;

  beforeEach(() => {
    routerContext = { href: null };
    spyOn(routerContext, 'href').and.callFake((url, params) => {
      const isOrganization = url.includes('organization');
      const ownerType = isOrganization ? 'organization' : 'application';
      const id = isOrganization ? params.organizationId : params.applicationPublicId;
      return `#/management/view/${ownerType}/${id}`;
    });
    ownersMap = getOwnersMap(3);
    state = {
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: ownersMap,
        },
      },
    };

    ownerId = 'ROOT_ORGANIZATION_ID';
    minimalProps = {
      ownerId,
    };
    renderComponent = (preloadedState = state, router = routerContext) =>
      render(
        <RouterStateContext.Provider value={router}>
          <OwnerTree {...minimalProps} />
        </RouterStateContext.Provider>,
        { preloadedState }
      );
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

  it('renders correct amount of clickable tree nodes', () => {
    renderComponent();

    Object.values(ownersMap).forEach((owner) => {
      const aTag = screen.getByText(owner.name).closest('a');
      expect(aTag).toHaveAttribute('href', getExpectedHref(owner));
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

const getExpectedHref = (owner) => {
  const isOrganization = owner.type === 'organization';
  const id = isOrganization ? owner.id : owner.publicId;
  return `#/management/view/${owner.type}/${id}`;
};
