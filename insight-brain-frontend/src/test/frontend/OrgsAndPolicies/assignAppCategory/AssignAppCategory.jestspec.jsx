/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import AssignAppCategory from 'MainRoot/OrgsAndPolicies/assignAppCategory/AssignAppCategory';
import * as assignAppCategoriesSelectors from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('Assign Application Categories Component', () => {
  let renderComponent,
    assignApplicableCategoriesLoadingSpy,
    assignApplicableCategoriesLoadErrorSpy,
    assignAppCategoriesUpdateSpy,
    assignAppCategoriesSaveSpy,
    saveMaskTimerDoneSpy,
    loadApplicableCategoriesSpy,
    selectCategoriesSpy;

  const ownerName = 'TestOwner';

  beforeEach(() => {
    assignApplicableCategoriesLoadingSpy = jest
      .spyOn(assignAppCategoriesSelectors, 'selectLoadingApplicableCategories')
      .mockReturnValue(false);
    assignApplicableCategoriesLoadErrorSpy = jest
      .spyOn(assignAppCategoriesSelectors, 'selectLoadApplicableCategoriesError')
      .mockReturnValue(null);

    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').mockReturnValue(ownerName);

    selectCategoriesSpy = jest.spyOn(assignAppCategoriesSelectors, 'selectCategories').mockReturnValue([
      {
        color: 'dark-red',
        description: 'just a test to see',
        id: 'a8c1ce6a94504dfcbf7c0a0f4d5b8192',
        isApplied: true,
        name: 'Custom',
        organizationId: 'ROOT_ORGANIZATION_ID',
      },
    ]);
    assignAppCategoriesUpdateSpy = jest.spyOn(actions, 'updateAppliedCategories');
    assignAppCategoriesSaveSpy = jest.spyOn(actions, 'saveAppliedCategories');
    saveMaskTimerDoneSpy = jest.spyOn(actions, 'saveMaskTimerDone');
    loadApplicableCategoriesSpy = jest.spyOn(actions, 'loadApplicableCategories').mockReturnValue({
      type: 'applicationCategories/assign/loadApplicableCategories/fulfilled',
      payload: {
        data: [
          {
            id: '68d05f2bcbed42cb91b629a4dfa160a6',
            name: 'Custom',
            description: 'Test description',
            color: 'dark-red',
            organizationID: 'ROOT_ORGANIZATION_ID',
          },
        ],
      },
    });

    renderComponent = () => render(<AssignAppCategory />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();
    expect(screen.getByText(`Categories assigned to ${ownerName}`)).toBeVisible();
  });

  it('renders loading indicator', () => {
    assignApplicableCategoriesLoadingSpy.mockReturnValue(true);
    assignApplicableCategoriesLoadErrorSpy.mockReturnValue(null);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error message', () => {
    assignApplicableCategoriesLoadingSpy.mockReturnValue(null);
    assignApplicableCategoriesLoadErrorSpy.mockReturnValue(true);
    renderComponent();
    expect(screen.getByText('An error occurred loading data.')).toBeVisible();
  });

  it('keeps update button inactive', () => {
    renderComponent();
    const updateButton = screen.getByRole('button', 'Update');
    expect(updateButton).toBeVisible();
    fireEvent.click(updateButton);
    expect(assignAppCategoriesSaveSpy).not.toHaveBeenCalled();
    expect(saveMaskTimerDoneSpy).not.toHaveBeenCalled();
  });

  it('updates categories when clicked', () => {
    selectCategoriesSpy.mockReturnValue([
      {
        color: 'dark-red',
        description: 'Test Category',
        id: 'a8c1ce6a94504dfcbf7c0a0f4d5b8192',
        isApplied: false,
        name: 'Custom',
        organizationId: 'ROOT_ORGANIZATION_ID',
      },
    ]);

    renderComponent();
    expect(loadApplicableCategoriesSpy).toHaveBeenCalledTimes(1);
    const fieldSet = screen.getByRole('group', {
      name: `Categories assigned to ${ownerName}`,
    });
    const icons = within(fieldSet).getAllByRole('presentation');
    fireEvent.click(icons[0]);
    const updateButton = screen.getByRole('button', 'Update');
    expect(assignAppCategoriesUpdateSpy).toHaveBeenCalledTimes(1);
    expect(assignAppCategoriesSaveSpy).not.toHaveBeenCalled();
    expect(updateButton).toBeVisible();
    expect(updateButton).not.toHaveClass('disabled');
    fireEvent.click(updateButton);
    expect(assignAppCategoriesSaveSpy).toHaveBeenCalledTimes(1);
  });
});
