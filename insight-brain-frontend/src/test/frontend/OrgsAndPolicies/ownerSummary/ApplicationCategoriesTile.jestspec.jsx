/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within, fireEvent, waitFor } from 'TestRoot/SpecUtil';
import ApplicationCategoriesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ApplicationCategoriesTile';
import * as assignApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as createEditApplicationCategoriesSelectors from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import router from 'MainRoot/router/routerInstance';

import { actions as createEditApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { actions as assignApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';

describe('ApplicationCategoriesTile', () => {
  let renderComponent,
    isAppSpy,
    isOrgSpy,
    selectLoadingSpy,
    selectLoadErrorSpy,
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
    goToCreateCategorySpy = jest.spyOn(createEditApplicationCategoriesActions, 'goToCreateCategory');
    goToAssignCategoriesSpy = jest.spyOn(assignApplicationCategoriesActions, 'goToEditCategories');

    selectLoadErrorSpy = jest.spyOn(createEditApplicationCategoriesSelectors, 'selectLoadError').mockReturnValue(null);
    selectLoadingSpy = jest.spyOn(createEditApplicationCategoriesSelectors, 'selectIsLoading').mockReturnValue(false);

    isAppSpy = jest.spyOn(routerSelectors, 'selectIsApplication').mockReturnValue(true);
    isOrgSpy = jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(false);

    selectAppCategoryOwnersSpy = jest
      .spyOn(createEditApplicationCategoriesSelectors, 'selectAppCategoryOwners')
      .mockReturnValue([]);

    selectRouterSlice = jest.spyOn(routerSelectors, 'selectRouterSlice');

    selectAppliedCategoriesSpy = jest
      .spyOn(assignApplicationCategoriesSelectors, 'selectAppliedCategories')
      .mockReturnValue(appliedCategories);
    selectAreAnyCategoriesDefined = jest
      .spyOn(assignApplicationCategoriesSelectors, 'selectAreAnyCategoriesDefined')
      .mockReturnValue(true);
    jest.spyOn(assignApplicationCategoriesSelectors, 'selectLoadApplicableCategoriesError').mockReturnValue(null);
    jest.spyOn(assignApplicationCategoriesSelectors, 'selectLoadAppliedCategoriesError').mockReturnValue(null);
    jest.spyOn(assignApplicationCategoriesSelectors, 'selectLoadingApplicableCategories').mockReturnValue(false);
    jest.spyOn(assignApplicationCategoriesSelectors, 'selectLoadingAppliedCategories').mockReturnValue(false);

    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerName').mockReturnValue('Owner Name');

    jest.spyOn(router.stateService, 'href').mockReturnValue('editCategoryHref');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);

    renderComponent = () => render(<ApplicationCategoriesTile />);
  });

  describe('ApplicationsManagement Feature Enabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsOrgsAndAppsEnabled').mockReturnValue(true);
    });
    it('renders loading indicator', () => {
      selectLoadingSpy.mockReturnValue(true);
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders error alert on load error', () => {
      selectLoadErrorSpy.mockReturnValue('Load Error');
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
        renderComponent();
        const subtitle = screen.getByText('assigned to Owner Name');
        const assignedListHeader = screen.getByText('Assigned');
        const localListHeader = screen.queryByText(`Local to ${appCategoryOwners[0].ownerName}`);
        const inheritListHeader = screen.queryByText(/Inherited from/);
        const listItems = screen.getAllByRole('listitem');

        const assignCategoryButton = screen.getByRole('button');

        expect(subtitle).toBeVisible();
        expect(assignCategoryButton).toHaveTextContent('Assign a Category');
        expect(assignCategoryButton).not.toHaveClass('disabled');
        expect(assignedListHeader).toBeVisible();
        expect(localListHeader).toBeNull();
        expect(inheritListHeader).toBeNull();

        expect(listItems[0]).toHaveTextContent('Custom cat');
        expect(listItems[0]).toHaveTextContent('dsdsdsd');
        expect(listItems[1]).toHaveTextContent('Hosted');
        expect(listItems[1]).toHaveTextContent(
          'Applications that are hosted such as services or software as a service.'
        );

        expect(listItems[0]).not.toHaveClass('nx-list__item--clickable');
        expect(listItems[1]).not.toHaveClass('nx-list__item--clickable');

        fireEvent.click(assignCategoryButton);

        expect(goToAssignCategoriesSpy).toHaveBeenCalledTimes(1);
      });

      it('renders disabled assign category button when there are not any categories to assign', () => {
        selectAreAnyCategoriesDefined.mockReturnValue(false);
        renderComponent();
        const addCategoryButton = screen.getByRole('button');

        expect(addCategoryButton).toHaveTextContent('Assign a Category');
        expect(addCategoryButton).toHaveClass('disabled');
      });

      it('renders empty message when there are no categories assigned', () => {
        selectAppliedCategoriesSpy.mockReturnValue([]);

        renderComponent();
        const emptyListItem = screen.getByRole('listitem');
        expect(emptyListItem).toHaveTextContent('No application categories assigned');
      });
    });

    describe('Organization Owner', () => {
      beforeEach(() => {
        isAppSpy.mockReturnValue(false);
        isOrgSpy.mockReturnValue(true);
        selectAppCategoryOwnersSpy.mockReturnValue(appCategoryOwners);
        selectAppliedCategoriesSpy.mockReturnValue([]);
        selectRouterSlice.mockReturnValue({
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

        expect(localListItems[0]).toHaveClass('nx-list__item');
        expect(inheritListItems[0]).not.toHaveClass('nx-list__item');
        expect(inheritListItems[1]).not.toHaveClass('nx-list__item');
        expect(inheritListItems[2]).not.toHaveClass('nx-list__item');

        fireEvent.click(addCategoryButton);

        expect(goToCreateCategorySpy).toHaveBeenCalledTimes(1);
      });

      it('renders empty message when there are no categories assigned', () => {
        selectAppCategoryOwnersSpy.mockReturnValue(emptyAppCategoryOwners);
        renderComponent();
        const categoryList = screen.getByRole('list');

        const localEmptyListItem = within(categoryList).getByRole('listitem');

        expect(localEmptyListItem).toHaveTextContent('No application categories defined');
      });

      it('renders correct subtile, content and enabled add category button for root org', () => {
        selectAppCategoryOwnersSpy.mockReturnValue(appCategoryRootOwner);
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
        expect(localListItems[0]).toHaveTextContent(
          'Applications that are provided for consumption outside the company'
        );
        expect(localListItems[1]).toHaveTextContent('Hosted');
        expect(localListItems[1]).toHaveTextContent(
          'Applications that are hosted such as services or software as a service.'
        );
        expect(localListItems[2]).toHaveTextContent('Internal');
        expect(localListItems[2]).toHaveTextContent('Applications that are used only by your employees');

        expect(localListItems[0]).toHaveClass('nx-list__item');
        expect(localListItems[1]).toHaveClass('nx-list__item');
        expect(localListItems[2]).toHaveClass('nx-list__item');

        fireEvent.click(addCategoryButton);

        expect(goToCreateCategorySpy).toHaveBeenCalledTimes(1);
      });

      it('renders a collapsible button with the correct structure', () => {
        selectAppCategoryOwnersSpy.mockReturnValue(appCategoryOwners);
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
        selectAppCategoryOwnersSpy.mockReturnValue(appCategoryOwners);
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

  describe('ApplicationsManagement Feature Disabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsOrgsAndAppsEnabled').mockReturnValue(false);
    });

    it('does not render with a Firewall only license', async () => {
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Application Categories');
      expect(title).toBeNull();
    });
  });

  describe('Pro Tier Gating', () => {
    beforeEach(() => {
      jest.spyOn(productFeaturesSelectors, 'selectIsOrgsAndAppsEnabled').mockReturnValue(true);
      jest.spyOn(productFeaturesSelectors, 'selectHasCustomAppCategories').mockReturnValue(false);
      isAppSpy.mockReturnValue(false);
      isOrgSpy.mockReturnValue(true);
      selectLoadingSpy.mockReturnValue(false);
    });

    it('shows lock icon and preview text when custom-application-categories feature is absent', () => {
      renderComponent();
      expect(screen.getByText('Preview Add a Category')).toBeVisible();
      expect(screen.queryByText('Add a Category')).not.toBeInTheDocument();
    });

    it('shows Enterprise Feature tooltip on the edit button', () => {
      renderComponent();
      expect(screen.getByText('Preview Add a Category')).toBeVisible();
    });
  });
});
