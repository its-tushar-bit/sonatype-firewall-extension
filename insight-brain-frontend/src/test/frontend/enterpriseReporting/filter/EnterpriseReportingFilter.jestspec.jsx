/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  axiosMockAdapter,
  fireEvent,
  render,
  screen,
  setupPortalContainer,
  userEvent,
  waitFor,
  within,
} from 'TestRoot/SpecUtil';
import EnterpriseReportingFilter from 'MainRoot/enterpriseReporting/filter/EnterpriseReportingFilter';
import EnterpriseReportingDashboardPage from 'MainRoot/enterpriseReporting/dashboard/EnterpriseReportingDashboardPage';
import {
  getEnterpriseReportingFilters,
  getDeleteEnterpriseReportingFilter,
  getDefaultEnterpriseReportingFilter,
  getAssignDefaultEnterpriseReportingFilter,
} from 'MainRoot/util/CLMLocation';
import { initialState, actions } from 'MainRoot/enterpriseReporting/filter/enterpriseReportingFilterSlice';

describe('EnterpriseReportingFilter', () => {
  let axiosMock, user;

  const mockFiltersData = [
    {
      id: 'id123',
      name: 'Filter 1',
      filter: {
        'Date Range': 'after 6 month ago',
        Organization: 'org1',
        Application: 'first app,second app',
        Stage: 'release',
      },
    },
    {
      id: 'id456',
      name: 'Filter 2',
      filter: {
        'Date Range': 'after 12 week ago',
        Stage: 'build',
        Organization: 'org 2',
        Application: '',
        'Component Type': '',
      },
    },
  ];

  // API returns filter as JSON string, not object
  const mockFiltersApiResponse = mockFiltersData.map((filter) => ({
    ...filter,
    filter: JSON.stringify(filter.filter),
  }));
  // State for testing initial/loading behavior (filters NOT initialized)
  const uninitializedPreloadedState = {
    ...initialState,
    isOpen: true,
    filtersInitialized: false,
    loadingIframe: false,
  };

  // State for testing filter functionality (filters initialized and ready)
  const initializedPreloadedState = {
    ...initialState,
    isOpen: true,
    filtersInitialized: true,
    loadingIframe: false,
    savedFilters: mockFiltersData,
    appliedFilterName: mockFiltersData[0].name, // Filter 1
    appliedFilter: mockFiltersData[0].filter,
    previewFilterName: mockFiltersData[0].name, // Filter 1
    previewFilter: mockFiltersData[0].filter,
    defaultFilterId: null,
    filterState: 'clean',
  };

  const renderInitialComponent = (stateOverrides = {}) => {
    render(<EnterpriseReportingFilter />, {
      preloadedState: {
        enterpriseReportingFilter: { ...uninitializedPreloadedState, ...stateOverrides },
      },
    });
    const drawer = screen.getByRole('dialog', { hidden: true });
    fireEvent.animationEnd(drawer);
  };

  const renderComponent = (stateOverrides = {}) => {
    render(<EnterpriseReportingFilter />, {
      preloadedState: {
        enterpriseReportingFilter: { ...initializedPreloadedState, ...stateOverrides },
      },
    });
    const drawer = screen.getByRole('dialog', { hidden: true });
    fireEvent.animationEnd(drawer);
  };

  const setupDefaultMocks = () => {
    axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, mockFiltersApiResponse);
    axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, '');
  };

  beforeAll(() => {
    setupPortalContainer();
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    user = userEvent.setup();
  });

  describe('loading', () => {
    it('shows loading state when loadingIframe is true', () => {
      renderInitialComponent({ loadingIframe: true });

      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('shows loading state when loadingSavedFilters is true', () => {
      renderInitialComponent({ loadingSavedFilters: true });

      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('shows loading state when loadingDefaultFilter is true', () => {
      renderInitialComponent({ loadingDefaultFilter: true });

      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('disables the dropdown and all footer buttons while iframe loading', () => {
      renderInitialComponent({ loadingIframe: true });
      expect(getDropdownButton(/Sonatype Default/)).toHaveAttribute('aria-disabled', 'true');
      expect(screen.getByRole('button', { name: 'Save As' })).toHaveAttribute('aria-disabled', 'true');
      expect(screen.getByRole('button', { name: 'Make My Default' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('calls initializeFilters, which makes API calls to load filters', async () => {
      const initializeFiltersSpy = jest.spyOn(actions, 'initializeFilters');
      renderInitialComponent();

      expect(initializeFiltersSpy).toHaveBeenCalled();
      //a call for both loadSavedFilters and loadDefaultFilter
      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[0].url).toBe(getEnterpriseReportingFilters());
      expect(axiosMock.history.get[1].url).toBe(getDefaultEnterpriseReportingFilter());
    });

    it('displays Sonatype Default in dropdown when no user default filter is set', async () => {
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, mockFiltersApiResponse);
      axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, ''); // No default
      renderInitialComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      expect(getDropdownButton('Sonatype Default')).toBeInTheDocument();
    });

    it('displays user default filter name in dropdown when defaultFilterId is set', async () => {
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, mockFiltersApiResponse);
      axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, 'id123'); // Filter 1
      renderInitialComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });
      expect(getDropdownButton('Filter 1')).toBeInTheDocument();
    });

    it('displays Sonatype Default when defaultFilterId does not match any saved filter', async () => {
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, mockFiltersApiResponse);
      axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, 'non-existent-id');
      renderInitialComponent();

      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });
      expect(getDropdownButton('Sonatype Default')).toBeInTheDocument();
    });

    describe('loading error returned', () => {
      it('renders an alert with retry button that dispatches load actions when clicked', async () => {
        axiosMock.onGet(getEnterpriseReportingFilters()).reply(400, 'Error!');
        axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(400, 'Default Problem!');

        renderInitialComponent();

        //a call for both loadSavedFilters and loadDefaultFilter
        expect(axiosMock.history.get.length).toBe(2);

        const alert = await screen.findByRole('alert');
        expect(alert).toHaveTextContent('An error occurred loading data. Error!');

        const button = within(alert).getByRole('button', { name: 'Retry' });
        await user.click(button);

        expect(axiosMock.history.get.length).toBe(4);
      });

      it('malformed filter JSON in API response renders alert to retry', async () => {
        const malformedResponse = [{ id: 'id123', name: 'Bad Filter', filter: '{invalid json}' }];
        axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, malformedResponse);
        axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, '');

        renderInitialComponent();

        // Should handle error gracefully
        await waitFor(() => {
          expect(screen.getByRole('alert')).toBeInTheDocument();
        });
      });
    });
  });

  describe('Filter Display', () => {
    beforeEach(() => {
      setupDefaultMocks();
    });

    it('displays previewFilter values when appliedFilterName & previewFilterName differ', async () => {
      renderComponent();

      // Wait for API calls to complete (initializeFilters)
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      // User selects Filter 2 from dropdown
      await selectFilter2FromDropdown(user, 'Filter 1');
      const dropdownButton = getDropdownButton('Filter 2');
      expect(dropdownButton).toBeInTheDocument();

      const dateFilter = screen.getByRole('button', { name: /Date Range/i });
      const stageFilter = screen.getByRole('button', { name: /Stage/i });
      const orgFilter = screen.getByRole('button', { name: /Organization/i });
      const appFilter = screen.getByRole('button', { name: /Application/i });
      const previewFilter = mockFiltersData[1].filter;

      await user.click(dateFilter);
      const dateList = getCollapsibleFilterList(dateFilter);
      expect(within(dateList).getByText(previewFilter['Date Range'])).toBeInTheDocument();

      await user.click(stageFilter);
      const stageList = getCollapsibleFilterList(stageFilter);
      expect(within(stageList).getByText(previewFilter['Stage'])).toBeInTheDocument();

      await user.click(orgFilter);
      const orgList = getCollapsibleFilterList(orgFilter);
      expect(within(orgList).getByText(previewFilter['Organization'])).toBeInTheDocument();

      await user.click(appFilter);
      const appList = getCollapsibleFilterList(appFilter);
      const appNames = previewFilter['Application'].split(',');
      appNames.forEach((app) => {
        expect(within(appList).getByText(app)).toBeInTheDocument();
      });
    });

    it('displays appliedFilter when names match', async () => {
      //match preview with applied, except for one updated filter value to confirm appliedFilter renders
      renderComponent({
        previewFilter: { ...mockFiltersData[0].filter, Stage: 'source' },
      });

      // Wait for API calls to complete
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      const dropdownButton = getDropdownButton(mockFiltersData[0].name);
      expect(dropdownButton).toBeInTheDocument();

      const stageButton = screen.getByRole('button', { name: /Stage/i });
      await user.click(stageButton);

      const stageList = getCollapsibleFilterList(stageButton);
      expect(within(stageList).getByText('release')).toBeInTheDocument();
      expect(within(stageList).queryByText('source')).not.toBeInTheDocument();
    });

    it('when filter has emtpy value, shows "is any value" text and no counter', async () => {
      renderComponent({
        appliedFilter: { Organization: '' },
        previewFilter: { Organization: '' },
      });

      // Wait for API calls to complete
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      const orgButton = screen.getByRole('button', { name: /Organization/i });
      await user.click(orgButton);

      const orgList = getCollapsibleFilterList(orgButton);
      expect(within(orgList).getByText('is any value')).toBeInTheDocument();
      expect(orgButton.querySelector('.nx-counter')).not.toBeInTheDocument();
    });

    it('when filter has values, render counter', async () => {
      renderComponent();

      // Wait for API calls to complete
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      const orgButton = screen.getByRole('button', { name: /Organization/i });
      const orgCounter = within(orgButton).getByText('1');
      expect(orgCounter).toHaveClass('nx-counter');

      const appButton = screen.getByRole('button', { name: /Application/i });
      const appCounter = within(appButton).getByText('2');
      expect(appCounter).toHaveClass('nx-counter');
    });
  });

  describe('Filters Dropdown', () => {
    beforeEach(() => {
      setupDefaultMocks();
    });

    describe('Button Content', () => {
      it('displays Sonatype Default in dropdown label when previewFilterName is null', () => {
        renderComponent({ appliedFilterName: null, previewFilterName: null });
        expect(getDropdownButton('Sonatype Default')).toBeInTheDocument();
      });

      it('updates previewFilterName and button text according to dropdown selection', async () => {
        renderComponent({ appliedFilterName: null, previewFilterName: null });

        let dropdownButton = getDropdownButton('Sonatype Default');
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        const filterButton = within(menu).getByRole('button', { name: mockFiltersData[0].name });
        await user.click(filterButton);

        //confirm menu is closed (no menu buttons rendered) to test the dropdown trigger button text
        expect(menu).not.toBeInTheDocument();
        dropdownButton = getDropdownButton(mockFiltersData[0].name);
        expect(dropdownButton).toBeInTheDocument();
      });

      it('displays a dirty asterisk alongside name when filterState is "changed" ', () => {
        renderComponent({ filterState: 'changed' });

        const dropdownButton = screen.getByRole('button', {
          name: '* ' + mockFiltersData[0].name,
        });
        expect(dropdownButton).toBeInTheDocument();
      });
    });

    describe('Menu', () => {
      it('renders the names of each saved filter, plus Sonatype Default', async () => {
        renderComponent();
        const dropdownButton = getDropdownButton(mockFiltersData[0].name);
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        mockFiltersData.forEach((filter) => {
          expect(within(menu).getByText(filter.name)).toBeInTheDocument();
        });
        expect(within(menu).getByText('Sonatype Default')).toBeInTheDocument();
      });

      it('if filter state is "changed", it renders an asterisk next to the appliedFilterName', async () => {
        renderComponent({ filterState: 'changed' });
        const dropdownButton = getDropdownButton(/\*\s*Filter 1/);
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        expect(within(menu).getByText(/\*\s*Filter 1/)).toBeInTheDocument();
      });

      it('renders a "Default" tag next to Sonatype Default if no user default specified', async () => {
        renderComponent();
        const dropdownButton = getDropdownButton(mockFiltersData[0].name);
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        const defaultOption = within(menu).getByRole('button', { name: 'Sonatype Default' }).parentElement;
        const tag = within(defaultOption).getByText('Default');
        expect(tag).toBeInTheDocument();
      });

      it('renders a "Default" tag only next to user default', async () => {
        // Mock the default filter API to return the first filter's ID
        axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, mockFiltersData[0].id);

        renderComponent({ defaultFilterId: mockFiltersData[0].id });

        // Wait for initializeFilters to complete
        await waitFor(() => {
          expect(axiosMock.history.get.length).toBe(2);
        });

        const dropdownButton = getDropdownButton(mockFiltersData[0].name);
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        const defaultOption = within(menu).getByRole('button', { name: mockFiltersData[0].name }).parentElement;
        expect(within(defaultOption).getByText('Default')).toBeInTheDocument();

        const nonDefaultOption = within(menu).getByRole('button', { name: mockFiltersData[1].name }).parentElement;
        expect(within(nonDefaultOption).queryByText('Default')).not.toBeInTheDocument();
      });

      it('changes the "Save As" button to "Apply" when filter option is selected', async () => {
        renderComponent();

        expect(screen.getByRole('button', { name: 'Save As' })).toBeInTheDocument();

        const dropdownButton = getDropdownButton(mockFiltersData[0].name);
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        const filterToSelect = within(menu).getByRole('button', { name: mockFiltersData[1].name });
        await user.click(filterToSelect);

        await waitFor(() => {
          expect(screen.getByRole('button', { name: 'Apply' })).toBeInTheDocument();
        });
      });

      it('renders delete modal with selected filter name when delete button clicked', async () => {
        renderComponent();

        const dropdownButton = getDropdownButton(mockFiltersData[0].name);
        await user.click(dropdownButton);

        const menu = dropdownButton.nextElementSibling;
        const deleteFilterBtn = within(menu).getByRole('button', { name: mockFiltersData[1].name }).nextElementSibling;
        await user.click(deleteFilterBtn);

        //assert the delete modal renders
        await waitFor(() => {
          const modal = screen.getAllByRole('dialog')[1];
          expect(modal).toHaveAttribute('aria-modal', 'true');
          expect(within(modal).getByText(/You are about to delete "Filter 2" filter/)).toBeInTheDocument();
        });
      });
    });
  });

  describe('Footer', () => {
    beforeEach(() => {
      setupDefaultMocks();
    });

    describe('when filterState is "clean"', () => {
      it('disables "Save As" button', () => {
        renderComponent();
        expect(screen.getByRole('button', { name: 'Save As' })).toBeDisabled();
      });

      it('does not render a "Revert" button', () => {
        renderComponent();
        expect(screen.queryByRole('button', { name: 'Revert' })).not.toBeInTheDocument();
      });

      describe('Make My Default button', () => {
        it('is disabled when user default filter applied', () => {
          renderComponent({ defaultFilterId: mockFiltersData[0].id });
          expect(screen.getByRole('button', { name: 'Make My Default' })).toBeDisabled();
        });

        it('marks filter as default when button clicked and saved filter currently applied', async () => {
          const saveDefaultMock = axiosMock
            .onPut(getAssignDefaultEnterpriseReportingFilter(mockFiltersData[0].id))
            .reply(200, mockFiltersData[0].id);

          renderComponent();

          const defaultButton = screen.getByRole('button', { name: 'Make My Default' });
          await user.click(defaultButton);

          await waitFor(() => {
            expect(saveDefaultMock.history.put.length).toBe(1);
            expect(saveDefaultMock.history.put[0].url).toBe(getAssignDefaultEnterpriseReportingFilter('id123'));
          });
        });

        it('deletes filter as default when button clicked and Sonatype Default filter currently applied', async () => {
          const deleteDefaultMock = axiosMock.onDelete(getDefaultEnterpriseReportingFilter()).reply(200, '');
          axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, mockFiltersData[0].id);

          renderComponent({
            appliedFilterName: null,
            previewFilterName: null,
            defaultFilterId: mockFiltersData[0].id,
          });

          const defaultButton = screen.getByRole('button', { name: 'Make My Default' });
          await user.click(defaultButton);

          await waitFor(() => {
            expect(deleteDefaultMock.history.delete.length).toBe(1);
          });
        });

        it('renders an error message when saveDefaultFilter fails', async () => {
          const saveDefaultMock = axiosMock
            .onPut(getAssignDefaultEnterpriseReportingFilter(mockFiltersData[0].id))
            .reply(400, 'Error!');

          renderComponent();
          const defaultButton = screen.getByRole('button', { name: 'Make My Default' });
          await user.click(defaultButton);

          await waitFor(() => {
            expect(saveDefaultMock.history.put.length).toBe(1);
          });
          const alert = screen.getByRole('alert');
          expect(alert).toHaveTextContent('Error! Click Make My Default again to retry');
        });
      });
    });

    describe('when filterState is "changed"', () => {
      const changedFilterState = {
        ...initializedPreloadedState,
        filterState: 'changed',
        appliedFilter: { ...mockFiltersData[0].filter, Stage: 'source' },
      };
      it('disables "Make My Default" button', () => {
        renderComponent(changedFilterState);
        expect(screen.getByRole('button', { name: 'Make My Default' })).toBeDisabled();
      });

      it('renders "Revert" button which dispatches revertFilterChanges when clicked', async () => {
        const revertChangesSpy = jest.spyOn(actions, 'revertFilterChanges');
        renderComponent(changedFilterState);

        const revertBtn = screen.getByRole('button', { name: 'Revert' });
        await user.click(revertBtn);

        expect(revertChangesSpy).toHaveBeenCalled();
      });

      it('renders "Save As" button and shows save modal when clicked', async () => {
        renderComponent(changedFilterState);

        const saveButton = screen.getByRole('button', { name: 'Save As' });
        await user.click(saveButton);

        await waitFor(() => {
          const modal = screen.getAllByRole('dialog')[1];
          expect(modal).toHaveAttribute('aria-modal', 'true');
          expect(within(modal).getByText('Save Filter')).toBeInTheDocument();
        });
      });
    });

    describe('when previewFilterName does not match appliedFilterName', () => {
      it('does not render a "Revert" button', () => {
        renderComponent();
        expect(screen.queryByRole('button', { name: 'Revert' })).not.toBeInTheDocument();
      });

      it('disables "Make My Default" button', () => {
        renderComponent({ previewFilterName: mockFiltersData[1].name });
        expect(screen.getByRole('button', { name: 'Make My Default' })).toBeDisabled();
      });

      it('renders an "Apply Button" in place of "Save As", and dispatches applySavedFilterAndRunDashboard on click', async () => {
        const applySpy = jest.spyOn(actions, 'applySavedFilterAndRunDashboard');
        renderComponent();

        // Wait for initializeFilters to complete
        await waitFor(() => {
          expect(axiosMock.history.get.length).toBe(2);
        });

        await selectFilter2FromDropdown(user, 'Filter 1');
        expect(screen.queryByRole('button', { name: 'Save As' })).not.toBeInTheDocument();

        const applyBtn = screen.getByRole('button', { name: 'Apply' });
        await user.click(applyBtn);

        expect(applySpy).toHaveBeenCalled();
      });

      it('if there are filter changes, opens the UnsavedFilterModal', async () => {
        renderComponent({ filterState: 'changed' });
        const applySpy = jest.spyOn(actions, 'applySavedFilterAndRunDashboard');

        // Wait for initializeFilters to complete
        await waitFor(() => {
          expect(axiosMock.history.get.length).toBe(2);
        });

        await selectFilter2FromDropdown(user, /\*\s*Filter 1/);
        const applyBtn = screen.getByRole('button', { name: 'Apply' });
        await user.click(applyBtn);

        expect(applySpy).not.toHaveBeenCalled();
        await waitFor(() => {
          expect(screen.getByText('Unsaved filters will be lost')).toBeInTheDocument();
        });
      });
    });
  });

  describe('deleting', () => {
    beforeEach(() => {
      setupDefaultMocks();
      axiosMock.onDelete(getDeleteEnterpriseReportingFilter(mockFiltersData[1].id)).reply(200);
    });

    it('removes deleted filter from dropdown after successful deletion', async () => {
      renderComponent();
      const dropdownButton = getDropdownButton(mockFiltersData[0].name);
      await openDeleteModal(user, dropdownButton, mockFiltersData[1].name);

      // Set up the mock for loadSavedFilters() call that happens AFTER deletion
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, [mockFiltersApiResponse[0]]);

      const deleteBtn = screen.getByRole('button', { name: 'Continue' });
      await user.click(deleteBtn);

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(1); // Only drawer remains
      });

      await user.click(dropdownButton);
      const updatedMenu = dropdownButton.nextElementSibling;

      // Filter 2 should NOT be in dropdown anymore
      expect(within(updatedMenu).queryByText(mockFiltersData[1].name)).not.toBeInTheDocument();
      // Filter 1 should still be there
      expect(within(updatedMenu).getByText(mockFiltersData[0].name)).toBeInTheDocument();
    });

    it("if deleted filter is currently applied filter, resets to user's default filter", async () => {
      renderComponent({
        previewFilterName: mockFiltersData[1].name,
        appliedFilterName: mockFiltersData[1].name,
      });

      const dropdownButton = getDropdownButton(mockFiltersData[1].name);
      await openDeleteModal(user, dropdownButton, mockFiltersData[1].name);

      // Set up the mock for loadSavedFilters() call that happens AFTER deletion
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, [mockFiltersApiResponse[0]]);

      const deleteBtn = screen.getByRole('button', { name: 'Continue' });
      await user.click(deleteBtn);

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(1); // Only drawer remains
      });

      expect(screen.getByRole('button', { name: 'Sonatype Default' })).toBeInTheDocument();
    });

    it('if deleted filter is user default, renders a separate warning and calls loadDefaultFilter on button click', async () => {
      axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, mockFiltersData[1].id);
      renderComponent({
        defaultFilterId: mockFiltersData[1].id,
      });

      // Wait for initializeFilters to complete (which calls loadDefaultFilter)
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      // Verify loadDefaultFilter was called during initialization
      expect(axiosMock.history.get[1].url).toBe(getDefaultEnterpriseReportingFilter());

      const dropdownButton = getDropdownButton(mockFiltersData[0].name);
      await openDeleteModal(user, dropdownButton, mockFiltersData[1].name, true);

      expect(screen.getByText(/Once deleted, your default filter set will revert/)).toBeInTheDocument();

      const deleteBtn = screen.getByRole('button', { name: 'Continue' });
      await user.click(deleteBtn);

      // Wait for delete to complete and loadDefaultFilter to be called again
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBeGreaterThan(2);
      });

      // Find the second call to loadDefaultFilter after the delete
      const defaultFilterCalls = axiosMock.history.get.filter(
        (req) => req.url === getDefaultEnterpriseReportingFilter()
      );
      expect(defaultFilterCalls.length).toBe(2);
    });

    it('if deleteFilter call rejects, renders an error and retry button', async () => {
      axiosMock.onDelete(getDeleteEnterpriseReportingFilter(mockFiltersData[1].id)).reply(400, 'Error!');
      renderComponent();

      const dropdownButton = getDropdownButton(mockFiltersData[0].name);
      await openDeleteModal(user, dropdownButton, mockFiltersData[1].name);

      const deleteBtn = screen.getByRole('button', { name: 'Continue' });
      await user.click(deleteBtn);

      // Wait for error to appear
      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent('An error occurred deleting data. Error!');
      });

      expect(axiosMock.history.delete.length).toBe(1);
      const retryBtn = screen.getByRole('button', { name: 'Retry' });
      await user.click(retryBtn);

      expect(axiosMock.history.delete.length).toBe(2);
    });
  });

  describe('saving', () => {
    const changedFilter = { ...mockFiltersData[0].filter, Stage: 'source' };

    beforeEach(() => {
      setupDefaultMocks();
    });

    it('successfully adds new filter to dropdown when filter is saved', async () => {
      const apiResponseAfterSave = [
        ...mockFiltersApiResponse,
        { name: 'Test', id: 'id789', filter: JSON.stringify(changedFilter) },
      ];

      renderComponent({
        appliedFilter: changedFilter,
        filterState: 'changed',
      });

      // After render, reset handlers and set up mocks for the save flow
      axiosMock.resetHandlers();
      axiosMock.onPost(getEnterpriseReportingFilters()).reply(200, { name: 'Test', filter: changedFilter });
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, apiResponseAfterSave);

      await openAndSelectSaveMode(user, 'Save New Filter Set', 'Test');

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(1);
      });
      const dropdownBtn = screen.getByRole('button', { name: 'Test' });
      await user.click(dropdownBtn);
      const menu = dropdownBtn.nextElementSibling;
      expect(within(menu).getByRole('button', { name: 'Test' })).toBeInTheDocument();
    });

    it('successfully updates filter when overridden', async () => {
      const apiResponseAfterSave = [
        { ...mockFiltersData[0], filter: JSON.stringify(changedFilter) },
        { ...mockFiltersData[1], filter: JSON.stringify(mockFiltersData[1].filter) },
      ];

      renderComponent({
        appliedFilter: changedFilter,
        filterState: 'changed',
      });
      expect(getDropdownButton(/\*\s*Filter 1/)).toBeInTheDocument();

      // After render, reset handlers and set up mocks for the save flow
      axiosMock.resetHandlers();
      axiosMock.onPut(getEnterpriseReportingFilters()).reply(200, { ...mockFiltersData[0], filter: changedFilter });
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, apiResponseAfterSave);
      axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, '');

      await openAndSelectSaveMode(user, 'Save (overwrite Filter 1)');

      expect(screen.getByText(/You are about to permanently overwrite Filter 1/)).toBeInTheDocument();

      const finalSubmitBtn = screen.getByRole('button', { name: 'Continue' });
      await user.click(finalSubmitBtn);
      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(1);
      });

      expect(getDropdownButton('Filter 1')).toBeInTheDocument();
      const stageBtn = screen.getByRole('button', { name: /Stage/i });
      await user.click(stageBtn);

      const stageList = getCollapsibleFilterList(stageBtn);
      expect(within(stageList).getByText('source')).toBeInTheDocument();
    });

    it('renders an error message if user does not provide a filter name', async () => {
      const submitSpy = jest.spyOn(actions, 'saveFilter');
      renderComponent({
        appliedFilter: changedFilter,
        filterState: 'changed',
      });
      await openAndSelectSaveMode(user, 'Save New Filter Set');
      const saveBtn = screen.getByRole('button', { name: 'Save As' });
      await user.click(saveBtn);

      expect(submitSpy).not.toHaveBeenCalled();
      expect(screen.getAllByRole('alert')[1]).toHaveTextContent(/Must be non-empty/);
    });

    it('renders an error and retry button if saveFilter request fails', async () => {
      axiosMock.onPost(getEnterpriseReportingFilters()).reply(400, 'Error!');
      renderComponent({
        appliedFilter: changedFilter,
        filterState: 'changed',
      });

      await openAndSelectSaveMode(user, 'Save New Filter Set', 'Test');
      expect(axiosMock.history.post.length).toBe(1);

      const alert = screen.getByRole('alert');
      expect(alert).toHaveTextContent(/An error occurred saving data. Error!/);
      const retryBtn = within(alert).getByRole('button', { name: 'Retry' });

      await user.click(retryBtn);
      expect(axiosMock.history.post.length).toBe(2);
    });

    it('renders a "Name In Use" warning of user tries to save filter with same name, and overrides name when saved', async () => {
      const apiResponseAfterSave = [
        { id: 'id123', name: 'FILTER 1', filter: JSON.stringify(changedFilter) },
        { ...mockFiltersData[1], filter: JSON.stringify(mockFiltersData[1].filter) },
      ];

      renderComponent({
        appliedFilter: changedFilter,
        filterState: 'changed',
      });

      // After render, reset handlers and set up mocks for the save flow
      axiosMock.resetHandlers();
      axiosMock
        .onPut(getEnterpriseReportingFilters())
        .reply(200, { id: mockFiltersData[0].id, name: 'FILTER 1', filter: changedFilter });
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, apiResponseAfterSave);

      await openAndSelectSaveMode(user, 'Save New Filter Set', 'FILTER 1');

      expect(screen.getByText(/"Filter 1" is already in use/)).toBeInTheDocument();
      const continueBtn = screen.getByRole('button', { name: 'Continue' });
      await user.click(continueBtn);

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(1);
      });

      expect(getDropdownButton('FILTER 1')).toBeInTheDocument();
    });

    it('when default checkbox selected, makes saved filter default', async () => {
      const apiResponseAfterSave = [
        ...mockFiltersApiResponse,
        { name: 'Test', id: 'id789', filter: JSON.stringify(changedFilter) },
      ];

      renderComponent({
        appliedFilter: changedFilter,
        filterState: 'changed',
      });

      // After render, reset handlers and set up mocks for the save flow
      axiosMock.resetHandlers();
      axiosMock.onPost(getEnterpriseReportingFilters()).reply(200, { name: 'Test', filter: changedFilter });
      axiosMock.onGet(getEnterpriseReportingFilters()).reply(200, apiResponseAfterSave);
      axiosMock.onGet(getDefaultEnterpriseReportingFilter()).reply(200, 'id789');

      await openAndSelectSaveMode(user, 'Save New Filter Set', 'Test', true);

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(1);
      });
      const dropdownBtn = screen.getByRole('button', { name: 'Test' });
      await user.click(dropdownBtn);

      const menu = dropdownBtn.nextElementSibling;
      const newFilterBtn = within(menu).getByRole('button', { name: 'Test' });
      const defaultOption = newFilterBtn.parentElement;
      expect(within(defaultOption).getByText('Default')).toBeInTheDocument();
    });
  });

  describe('unsavedFilterModal', () => {
    beforeEach(() => {
      setupDefaultMocks();
    });

    it('allows users to save changes', async () => {
      const saveModalSpy = jest.spyOn(actions, 'setShowSaveFilterModal');
      renderComponent({ filterState: 'changed' });

      // Wait for initializeFilters to complete
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      await selectFilter2FromDropdown(user, /\*\s*Filter 1/);
      const applyBtn = screen.getByRole('button', { name: 'Apply' });
      await user.click(applyBtn);

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(2);
      });

      const saveBtn = screen.getByRole('button', { name: 'Save Current Filters' });
      expect(saveBtn).toBeInTheDocument();

      await user.click(saveBtn);
      expect(saveModalSpy).toHaveBeenCalledWith(true);
    });

    it('allows users to apply filter without saving changes', async () => {
      const saveModalSpy = jest.spyOn(actions, 'setShowSaveFilterModal');
      const applyFilterSpy = jest.spyOn(actions, 'applySavedFilterAndRunDashboard');
      renderComponent({ filterState: 'changed' });

      // Wait for initializeFilters to complete
      await waitFor(() => {
        expect(axiosMock.history.get.length).toBe(2);
      });

      await selectFilter2FromDropdown(user, /\*\s*Filter 1/);
      const applyBtn = screen.getByRole('button', { name: 'Apply' });
      await user.click(applyBtn);

      await waitFor(() => {
        expect(screen.getAllByRole('dialog').length).toBe(2);
      });

      const saveBtn = screen.getByRole('button', { name: 'Apply Anyway' });
      expect(saveBtn).toBeInTheDocument();

      await user.click(saveBtn);
      expect(applyFilterSpy).toHaveBeenCalled();
      expect(saveModalSpy).not.toHaveBeenCalled();
    });
  });

  describe('closing behaviour', () => {
    const renderDashboardPageWithDrawer = (filterStateOverrides) => {
      render(<EnterpriseReportingDashboardPage />, {
        preloadedState: {
          enterpriseReportingDashboard: {
            loading: false,
            loadError: null,
            dashboardTabs: [],
            activeDashboardTab: 0,
            selectedDashboardName: 'Test Dashboard',
            selectedDashboard: {
              category: 'enterprise',
              dashboardId: 'test-dashboard',
            },
          },
          enterpriseReportingFilter: {
            ...initializedPreloadedState,
            isOpen: true, // Start with drawer open
            ...filterStateOverrides, // Allow test-specific overrides
          },
          productFeatures: {
            loading: false,
          },
          router: {
            currentParams: { id: 'test-dashboard' },
            currentState: { name: 'enterpriseReportingDashboard' },
            prevState: { name: 'otherState' },
          },
        },
      });
      const drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);
    };

    beforeEach(() => {
      setupDefaultMocks();
      window.clmServerVersion = '1.197.0-SNAPSHOT';
    });

    it('resets previewFilterName when drawer is closed without applying and reopened via button', async () => {
      renderDashboardPageWithDrawer();

      // Verify starting with Filter 1
      expect(getDropdownButton(mockFiltersData[0].name)).toBeInTheDocument();

      // Select Filter 2
      const dropdownButton = getDropdownButton(mockFiltersData[0].name);
      await user.click(dropdownButton);

      const menu = dropdownButton.nextElementSibling;
      const filter2Button = within(menu).getByRole('button', { name: mockFiltersData[1].name });
      await user.click(filter2Button);

      await waitFor(() => {
        expect(getDropdownButton(mockFiltersData[1].name)).toBeInTheDocument();
      });

      // Close drawer
      const closeButton = screen.getByLabelText('Close');
      await user.click(closeButton);

      let drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      // Reopen drawer
      const openButton = screen.getByRole('button', { name: 'Save / Apply Filters' });
      await user.click(openButton);

      drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);

      // Confirm dropdown button renders Filter 1
      await waitFor(() => {
        expect(getDropdownButton(mockFiltersData[0].name)).toBeInTheDocument();
      });
    });

    it('clears default filter alert when drawer is closed', async () => {
      renderDashboardPageWithDrawer({
        defaultFilterId: mockFiltersData[0].id,
        showDefaultFilterSuccessAlert: true,
      });

      // Verify success alert is showing
      await waitFor(() => {
        const successAlert = screen.getByRole('status');
        expect(successAlert).toHaveTextContent(/Filter 1 is now your default filter set/);
      });

      // Close drawer
      const closeButton = screen.getAllByLabelText('Close')[0]; // Get first one (drawer close, not alert close)
      await user.click(closeButton);

      let drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);

      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      // Reopen drawer
      const openButton = screen.getByRole('button', { name: 'Save / Apply Filters' });
      await user.click(openButton);

      drawer = screen.getByRole('dialog', { hidden: true });
      fireEvent.animationEnd(drawer);

      // Ensure success alert is no longer rendered
      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
      });
    });
  });

  const selectFilter2FromDropdown = async (user, currentFilterName) => {
    // Click dropdown button showing current filter
    const dropdownButton = getDropdownButton(currentFilterName);
    await user.click(dropdownButton);

    // Select Filter 2 from the menu
    const menu = dropdownButton.nextElementSibling;
    const filter2Button = within(menu).getByRole('button', { name: 'Filter 2' });
    await user.click(filter2Button);
  };

  const openDeleteModal = async (user, dropdownBtn, filterToDelete, isDefault) => {
    await user.click(dropdownBtn);

    const menu = dropdownBtn.nextElementSibling;
    let deleteFilterBtn;

    //if filter is default, need to traverse across 'Default' tag to get to delete btn
    if (isDefault) {
      deleteFilterBtn = within(menu).getByRole('button', { name: filterToDelete }).nextElementSibling
        .nextElementSibling;
    } else {
      deleteFilterBtn = within(menu).getByRole('button', { name: filterToDelete }).nextElementSibling;
    }
    expect(within(menu).getByText(mockFiltersData[1].name)).toBeInTheDocument();

    await user.click(deleteFilterBtn);

    //assert the delete modal renders
    await waitFor(() => {
      const modal = screen.getAllByRole('dialog')[1];
      expect(modal).toHaveAttribute('aria-modal', 'true');
    });
  };

  const openAndSelectSaveMode = async (user, radioName, newFilterName, isDefault) => {
    const saveBtn = screen.getByRole('button', { name: 'Save As' });
    await user.click(saveBtn);

    await waitFor(() => {
      expect(screen.getAllByRole('dialog').length).toBe(2);
    });
    const radio = screen.getByLabelText(radioName);
    await user.click(radio);

    if (newFilterName) {
      const input = screen.getByRole('textbox');
      await user.type(input, newFilterName);
    }

    if (isDefault) {
      const checkbox = screen.getByRole('checkbox');
      await user.click(checkbox);
    }

    const submitBtn = screen.getByRole('button', { name: 'Save' });
    await user.click(submitBtn);
  };

  const getDropdownButton = (buttonName) => {
    const header = screen.getByRole('dialog').querySelector('.nx-drawer-header');
    return within(header).getByRole('button', { name: buttonName });
  };

  const getCollapsibleFilterList = (el) => {
    const filterContainer = el.closest('.nx-collapsible-items');
    return within(filterContainer).getByRole('list');
  };
});
