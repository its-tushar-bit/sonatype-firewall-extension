/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';
import ApplicationCategoriesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ApplicationCategoriesTile';
import * as applicationsSelectors from 'MainRoot/OrgsAndPolicies/applicationsSelectors';
import * as assignApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as createEditApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

import { actions as createEditApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { actions as assignApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';

describe('ApplicationCategoriesTile', () => {
  let renderComponent,
    isAppSpy,
    isOrgSpy,
    selectLoadApplicationsErrorSpy,
    selectLoadingApplicationsSpy,
    selectAppCategoryOwnersSpy,
    selectAreAnyCategoriesDefined,
    selectAppliedCategoriesSpy,
    selectRouterSlice,
    goToCreateCategorySpy,
    goToAssignCategoriesSpy;

  const appliedCategories = [
    {
      id: '40790e4e5f764dee8183bd7e71a8903e',
      organizationId: 'f22bc4ce7d794d47b439d2839f511fec',
      name: 'Custom cat',
      description: 'dsdsdsd',
      color: 'dark-red',
    },
    {
      id: 'a0531d2e64954a42ae667c8c3ef8002c',
      organizationId: 'ROOT_ORGANIZATION_ID',
      name: 'Hosted',
      description: 'Applications that are hosted such as services or software as a service.',
      color: 'light-purple',
    },
  ];
  const appCategoryOwners = [
    {
      ownerId: 'f22bc4ce7d794d47b439d2839f511fec',
      ownerName: 'wencel',
      ownerType: 'organization',
      applicationCategories: [
        {
          id: '40790e4e5f764dee8183bd7e71a8903e',
          name: 'Custom cat',
          description: 'dsdsdsd',
          organizationId: 'f22bc4ce7d794d47b439d2839f511fec',
          color: 'dark-red',
        },
      ],
      parent: false,
    },
    {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
      applicationCategories: [
        {
          id: '838a1a1930394199bf0a8f93bf183d5e',
          name: 'Distributed',
          description: 'Applications that are provided for consumption outside the company',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'light-green',
        },
        {
          id: 'a0531d2e64954a42ae667c8c3ef8002c',
          name: 'Hosted',
          description: 'Applications that are hosted such as services or software as a service.',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'light-purple',
        },
        {
          id: '8f4679e999f247018b39346c7f72f87a',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'yellow',
        },
      ],
      parent: true,
    },
  ];
  const appCategoryRootOwner = [
    {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
      applicationCategories: [
        {
          id: '838a1a1930394199bf0a8f93bf183d5e',
          name: 'Distributed',
          description: 'Applications that are provided for consumption outside the company',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'light-green',
        },
        {
          id: 'a0531d2e64954a42ae667c8c3ef8002c',
          name: 'Hosted',
          description: 'Applications that are hosted such as services or software as a service.',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'light-purple',
        },
        {
          id: '8f4679e999f247018b39346c7f72f87a',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'yellow',
        },
      ],
      parent: false,
    },
  ];
  const emptyAppCategoryOwners = [
    {
      ownerId: 'f22bc4ce7d794d47b439d2839f511fec',
      ownerName: 'wencel',
      ownerType: 'organization',
      applicationCategories: [],
      parent: false,
    },
    {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
      applicationCategories: [],
      parent: true,
    },
  ];

  beforeEach(() => {
    goToCreateCategorySpy = spyOn(createEditApplicationCategoriesActions, 'goToCreateCategory').and.callThrough();
    goToAssignCategoriesSpy = spyOn(assignApplicationCategoriesActions, 'goToEditCategories').and.callThrough();

    selectLoadingApplicationsSpy = spyOn(applicationsSelectors, 'selectLoadingApplications').and.returnValue(false);
    selectLoadApplicationsErrorSpy = spyOn(applicationsSelectors, 'selectLoadApplicationsError').and.returnValue(null);

    isAppSpy = spyOn(routerSelectors, 'selectIsApplication').and.returnValue(true);
    isOrgSpy = spyOn(routerSelectors, 'selectIsOrganization').and.returnValue(false);

    selectAppCategoryOwnersSpy = spyOn(
      createEditApplicationCategoriesSelectors,
      'selectAppCategoryOwners'
    ).and.returnValue([]);

    selectRouterSlice = spyOn(routerSelectors, 'selectRouterSlice');

    spyOn(createEditApplicationCategoriesSelectors, 'selectLoadError').and.returnValue(null);
    spyOn(createEditApplicationCategoriesSelectors, 'selectIsLoading').and.returnValue(false);

    selectAppliedCategoriesSpy = spyOn(assignApplicationCategoriesSelectors, 'selectAppliedCategories').and.returnValue(
      appliedCategories
    );
    selectAreAnyCategoriesDefined = spyOn(
      assignApplicationCategoriesSelectors,
      'selectAreAnyCategoriesDefined'
    ).and.returnValue(true);
    spyOn(assignApplicationCategoriesSelectors, 'selectLoadApplicableCategoriesError').and.returnValue(null);
    spyOn(assignApplicationCategoriesSelectors, 'selectLoadAppliedCategoriesError').and.returnValue(null);
    spyOn(assignApplicationCategoriesSelectors, 'selectLoadingApplicableCategories').and.returnValue(false);
    spyOn(assignApplicationCategoriesSelectors, 'selectLoadingAppliedCategories').and.returnValue(false);

    spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').and.returnValue('Owner Name');

    renderComponent = () => render(<ApplicationCategoriesTile />);
  });

  it('renders loading indicator', () => {
    selectLoadingApplicationsSpy.and.returnValue(true);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error alert on load error', () => {
    selectLoadApplicationsErrorSpy.and.returnValue('Load Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });

  it('renders tile with the correct page title', () => {
    renderComponent();
    expect(screen.getByText('Application Categories')).toBeVisible();
  });

  describe('Application Owner', () => {
    it('renders correct content for the tile and enabled assign category button', () => {
      spyOn(routerStateContext, 'useRouterState').and.returnValue({
        href: jasmine.createSpy('href').and.returnValue('editCategoryHref'),
      });
      renderComponent();
      const subtitle = screen.getByText('assigned to Owner Name');
      const assignedListHeader = screen.getByText('Assigned');
      const localListHeader = screen.queryByText(`Local to ${appCategoryOwners[0].ownerName}`);
      const inheritListHeader = screen.queryByText(/Inherited from/);
      const listItems = screen.getAllByRole('listitem');

      const assignCategoryButton = screen.getByRole('button');

      expect(subtitle).toBeVisible();
      expect(assignCategoryButton).toHaveTextContent('Assign a Category');
      expect(assignCategoryButton).not.toHaveClassName('disabled');
      expect(assignedListHeader).toBeVisible();
      expect(localListHeader).toBeNull();
      expect(inheritListHeader).toBeNull();

      expect(listItems[0]).toHaveTextContent('Custom cat');
      expect(listItems[0]).toHaveTextContent('dsdsdsd');
      expect(listItems[1]).toHaveTextContent('Hosted');
      expect(listItems[1]).toHaveTextContent('Applications that are hosted such as services or software as a service.');

      expect(listItems[0]).not.toHaveClassName('nx-list__item--clickable');
      expect(listItems[1]).not.toHaveClassName('nx-list__item--clickable');

      fireEvent.click(assignCategoryButton);

      expect(goToAssignCategoriesSpy).toHaveBeenCalledTimes(1);
    });

    it('renders disabled assign category button when there are not any categories to assign', () => {
      selectAreAnyCategoriesDefined.and.returnValue(false);
      renderComponent();
      const addCategoryButton = screen.getByRole('button');

      expect(addCategoryButton).toHaveTextContent('Assign a Category');
      expect(addCategoryButton).toHaveClassName('disabled');
    });

    it('renders empty message when there are no categories assigned', () => {
      selectAppliedCategoriesSpy.and.returnValue([]);

      renderComponent();
      const emptyListItem = screen.getByRole('listitem');
      expect(emptyListItem).toHaveTextContent('No application categories assigned');
    });
  });

  describe('Organization Owner', () => {
    beforeEach(() => {
      isAppSpy.and.returnValue(false);
      isOrgSpy.and.returnValue(true);
      selectAppCategoryOwnersSpy.and.returnValue(appCategoryOwners);
      selectAppliedCategoriesSpy.and.returnValue([]);
      spyOn(routerStateContext, 'useRouterState').and.returnValue({
        href: jasmine.createSpy('href').and.returnValue('editCategoryHref'),
      });
      selectRouterSlice.and.returnValue({
        currentState: {
          name: 'management.view.organization',
        },
        currentParams: {
          organizationId: '05602dd5ba934c318adca4e4f5cfe',
        },
      });
    });

    it('renders correct subtile, content and enabled add category button for org with inherited categories', () => {
      renderComponent();
      const subtitle = screen.getByText('available to apps in Owner Name');
      const assignedListHeader = screen.queryByText('Assigned');
      const localListHeader = screen.getByText(`Local to ${appCategoryOwners[0].ownerName}`);
      const inheritListHeader = screen.getByText(/Inherited from/);
      const categoryLists = screen.getAllByRole('list');

      const addCategoryButton = screen.getAllByRole('button')[0];

      expect(subtitle).toBeVisible();
      expect(addCategoryButton).toHaveTextContent('Add a Category');
      expect(assignedListHeader).toBeNull();
      expect(localListHeader).toBeVisible();
      expect(inheritListHeader).toBeVisible();

      expect(categoryLists.length).toBe(2);

      const localListItems = within(categoryLists[0]).getAllByRole('listitem');
      const inheritListItems = within(categoryLists[1]).getAllByRole('listitem');

      expect(localListItems.length).toBe(1);
      expect(inheritListItems.length).toBe(3);

      expect(localListItems[0]).toHaveTextContent('Custom cat');
      expect(localListItems[0]).toHaveTextContent('dsdsdsd');
      expect(inheritListItems[0]).toHaveTextContent('Distributed');
      expect(inheritListItems[0]).toHaveTextContent(
        'Applications that are provided for consumption outside the company'
      );
      expect(inheritListItems[1]).toHaveTextContent('Hosted');
      expect(inheritListItems[1]).toHaveTextContent(
        'Applications that are hosted such as services or software as a service.'
      );
      expect(inheritListItems[2]).toHaveTextContent('Internal');
      expect(inheritListItems[2]).toHaveTextContent('Applications that are used only by your employees');

      expect(localListItems[0]).toHaveClassName('nx-list__item');
      expect(inheritListItems[0]).not.toHaveClassName('nx-list__item');
      expect(inheritListItems[1]).not.toHaveClassName('nx-list__item');
      expect(inheritListItems[2]).not.toHaveClassName('nx-list__item');

      fireEvent.click(addCategoryButton);

      expect(goToCreateCategorySpy).toHaveBeenCalledTimes(1);
    });

    it('renders empty message when there are no categories assigned', () => {
      selectAppCategoryOwnersSpy.and.returnValue(emptyAppCategoryOwners);
      renderComponent();
      const categoryList = screen.getByRole('list');

      const localEmptyListItem = within(categoryList).getByRole('listitem');

      expect(localEmptyListItem).toHaveTextContent('No application categories defined');
    });

    it('renders correct subtile, content and enabled add category button for root org', () => {
      selectAppCategoryOwnersSpy.and.returnValue(appCategoryRootOwner);
      renderComponent();
      const subtitle = screen.getByText('available to apps in Owner Name');
      const assignedListHeader = screen.queryByText('Assigned');
      const localListHeader = screen.getByText(`Local to ${appCategoryOwners[1].ownerName}`);
      const inheritListHeader = screen.queryByText(/Inherited from/);
      const categoryList = screen.getByRole('list');

      const addCategoryButton = screen.getAllByRole('button')[0];

      expect(subtitle).toBeVisible();
      expect(addCategoryButton).toHaveTextContent('Add a Category');
      expect(assignedListHeader).toBeNull();
      expect(localListHeader).toBeVisible();
      expect(inheritListHeader).toBeNull();

      const localListItems = within(categoryList).getAllByRole('listitem');

      expect(localListItems.length).toBe(3);

      expect(localListItems[0]).toHaveTextContent('Distributed');
      expect(localListItems[0]).toHaveTextContent('Applications that are provided for consumption outside the company');
      expect(localListItems[1]).toHaveTextContent('Hosted');
      expect(localListItems[1]).toHaveTextContent(
        'Applications that are hosted such as services or software as a service.'
      );
      expect(localListItems[2]).toHaveTextContent('Internal');
      expect(localListItems[2]).toHaveTextContent('Applications that are used only by your employees');

      expect(localListItems[0]).toHaveClassName('nx-list__item');
      expect(localListItems[1]).toHaveClassName('nx-list__item');
      expect(localListItems[2]).toHaveClassName('nx-list__item');

      fireEvent.click(addCategoryButton);

      expect(goToCreateCategorySpy).toHaveBeenCalledTimes(1);
    });

    it('renders a collapsible button with the correct structure', () => {
      selectAppCategoryOwnersSpy.and.returnValue(appCategoryOwners);
      renderComponent();
      const button = screen.getByRole('button', { name: /Inherited from Root Organization/i });

      // Verify that the button is initially expanded, so the set of icons is hidden.
      const hiddenIconSet = button.querySelectorAll('.nx-icon.hexagon');
      expect(hiddenIconSet.length).toBe(0);

      // Verify that the button collapsed has an icon.
      fireEvent.click(button);
      const icon = button.querySelector('.svg-inline--fa');
      expect(icon).toBeInTheDocument();

      // Verify that the button has a text.
      const text = button.querySelector('.nx-collapsible-items__text');
      expect(text).toBeInTheDocument();
      expect(text.textContent).toContain('Inherited from Root Organization');

      // Verify that the button collapsed has a set of 3 icons.
      const iconSet = button.querySelectorAll('.nx-icon.hexagon');
      expect(iconSet.length).toBe(3);
    });

    it('renders a collapsible button when has inherited categories, and expand and collapse content', () => {
      selectAppCategoryOwnersSpy.and.returnValue(appCategoryOwners);
      renderComponent();
      const button = screen.getByRole('button', { name: /Inherited from Root Organization/i });
      const content = screen.getByRole('group');
      const collapsibleContent = content.parentElement.querySelector('.nx-collapsible-items');

      // Verify that the content is initially expanded.
      expect(collapsibleContent).toHaveClass('nx-collapsible-items--expanded');

      // Verify that after a click the content is collapsed.
      fireEvent.click(button);
      expect(collapsibleContent).toHaveClass('nx-collapsible-items--collapsed');

      // Verify that the expanded content is visible.
      fireEvent.click(button);
      const expandedContent = content.parentElement.querySelector('.nx-collapsible-items__children');
      expect(expandedContent).toBeInTheDocument();
      expect(expandedContent).toHaveAttribute('role', 'list');
    });
  });
});
