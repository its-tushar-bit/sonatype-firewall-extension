/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import OwnersTreeTile from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnerTreeTile';

describe('OwnersTreeTile', () => {
  let renderComponent;
  let state;

  beforeEach(() => {
    state = {
      orgsAndPolicies: {
        ownerSummary: { ownersTreeNodesStatus: {} },
        ownerSideNav: { ownersMap: { sonatype: { id: 'sonatype', name: 'Sonatype' } } },
      },
    };

    renderComponent = (props = {}, preloadedState = state) =>
      render(<OwnersTreeTile topParentOrganizationId="sonatype" {...props} />, { preloadedState });
  });

  it('renders owners tree tile', () => {
    renderComponent();

    const ownersTreeTile = screen.getByLabelText('sonatype-title');

    expect(ownersTreeTile).toBeVisible();
    expect(within(ownersTreeTile).getByRole('tree')).toBeVisible();
  });
});
