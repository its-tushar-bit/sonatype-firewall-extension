/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import PoliciesHeaderTile from 'MainRoot/OrgsAndPolicies/ownerSummary/PoliciesHeaderTile';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';

describe('PoliciesHeaderTile', () => {
  let renderComponent, goToCreatePolicySpy;

  beforeEach(() => {
    spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue('Owner Name');
    goToCreatePolicySpy = spyOn(actions, 'goToCreatePolicy').and.callThrough();

    renderComponent = () => render(<PoliciesHeaderTile />);
  });

  it('renders header with the correct title', () => {
    renderComponent();
    expect(screen.getByText('Policies')).toBeVisible();
  });

  it('renders header with the correct subtitle', () => {
    renderComponent();
    expect(screen.getByText('applying to Owner Name')).toBeVisible();
  });

  it('navigates to policy create page', () => {
    renderComponent();

    const addButton = screen.getByRole('button', { name: 'Add a Policy' });
    fireEvent.click(addButton);

    expect(goToCreatePolicySpy).toHaveBeenCalled();
  });
});
