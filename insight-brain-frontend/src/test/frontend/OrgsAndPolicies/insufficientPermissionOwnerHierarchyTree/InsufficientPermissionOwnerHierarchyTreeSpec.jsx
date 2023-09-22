/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import InsufficientPermissionOwnerHierarchyTree from 'MainRoot/OrgsAndPolicies/insufficientPermissionOwnerHierarchyTree/InsufficientPermissionOwnerHierarchyTree';

describe('InsufficientPermissionOwnerHierarchyTree', () => {
  let renderComponent;
  let state;

  beforeEach(() => {
    const displayedOrganization = { id: 'org', name: 'Insufficient Permission Org' };
    state = {
      orgsAndPolicies: {
        ownerSideNav: {
          ownersMap: { org: displayedOrganization },
          displayedOrganization,
        },
      },
    };

    renderComponent = (preloadedState) => render(<InsufficientPermissionOwnerHierarchyTree />, { preloadedState });
  });

  it('renders page title and description', () => {
    renderComponent(state);

    const title = screen.getByRole('heading', { name: /Insufficient Permission Org/i });
    const description = screen.getByText(
      /view all organizations and applications on which you have permissions\. click on the link for the org or app below to access details\./i
    );

    expect(title).toBeVisible();
    expect(description).toBeVisible();
  });

  it('renders owners tree', () => {
    renderComponent(state);

    const orgName = 'Insufficient Permission Org';
    const ownersTreeTile = screen.getByLabelText('org-title');

    expect(ownersTreeTile).toBeVisible();
    expect(within(ownersTreeTile).getByText(orgName)).toBeVisible();
  });
});
