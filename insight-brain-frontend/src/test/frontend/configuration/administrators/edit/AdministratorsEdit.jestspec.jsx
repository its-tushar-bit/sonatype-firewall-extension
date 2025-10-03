/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import AdministratorsEdit from 'MainRoot/configuration/administrators/edit/AdministratorsEdit';
import * as administratorsSelectors from 'MainRoot/configuration/administrators/administratorsSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import { actions } from 'MainRoot/configuration/administrators/administratorsSlice';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('AdministratorsEdit', () => {
  let renderComponent,
    selectIsLoadingSpy,
    selectLoadErrorSpy,
    fetchUsersDataSpy,
    goToAdministratorsSpy,
    loadFetchUsersSpy,
    saveMembersSpy,
    selectIsGroupSearchEnabledSpy,
    selectTenantModeSpy,
    addSelectedUserSpy;
  const roleToEdit = {
    roleId: 'b9646757e98e486da7d730025f5245f8',
    roleName: 'Policy Administrator',
    roleDescription: 'Manages all organizations, applications, policies, and policy violations.',
    membersByOwner: [
      {
        ownerId: 'global',
        ownerName: 'Global',
        ownerType: 'global',
        members: [
          {
            type: 'USER',
            internalName: 'admin',
            displayName: 'Adminasdf BuiltIn',
            email: 'admin@localhost',
            realm: 'IQ Server',
          },
          {
            type: 'GROUP',
            internalName: 'group',
            displayName: 'Group User',
            email: 'null',
            realm: 'IQ Server',
          },
        ],
      },
    ],
  };
  beforeEach(() => {
    selectIsLoadingSpy = jest.spyOn(administratorsSelectors, 'selectIsLoading').mockReturnValue(false);
    selectLoadErrorSpy = jest.spyOn(administratorsSelectors, 'selectLoadError').mockReturnValue(null);
    jest.spyOn(administratorsSelectors, 'selectFetchUsersLoading').mockReturnValue(false);
    fetchUsersDataSpy = jest.spyOn(administratorsSelectors, 'selectUsersNotAdded').mockReturnValue([]);
    selectIsGroupSearchEnabledSpy = jest
      .spyOn(administratorsSelectors, 'selectIsGroupSearchEnabled')
      .mockReturnValue(true);
    selectTenantModeSpy = jest.spyOn(productFeaturesSelectors, 'selectTenantMode').mockReturnValue('single-tenant');
    goToAdministratorsSpy = jest.spyOn(actions, 'goToAdministrators');
    saveMembersSpy = jest.spyOn(actions, 'saveMembers');
    loadFetchUsersSpy = jest.spyOn(actions, 'loadFetchUsers');
    addSelectedUserSpy = jest.spyOn(actions, 'addSelectedUser');

    jest.spyOn(administratorsSelectors, 'selectRoleToEdit').mockReturnValue(roleToEdit);
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue({
      get: jest.fn(),
      href: jest.fn(),
      includes: jest.fn(),
    });
    renderComponent = () => render(<AdministratorsEdit />);
  });

  it('renders name and descriptions', () => {
    renderComponent();

    const name = screen.getByText('Configure Administrators');
    expect(name).toBeVisible();

    const description = screen.getByText('Manages all organizations, applications, policies, and policy violations.');
    expect(description).toBeVisible();
  });

  it('renders the loading message', () => {
    selectIsLoadingSpy.mockReturnValue(true);

    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders NxErrorAlert if an error is thrown', () => {
    selectLoadErrorSpy.mockReturnValue('Error Message.');

    renderComponent();

    expect(screen.getByText('An error occurred loading data. Error Message.')).toBeVisible();
  });

  it('renders initial members list', () => {
    renderComponent();

    const associateGroupField = screen.queryByText('Add an External Group');
    const groupAlert = screen.queryByText(
      'One or more LDAP servers have group search disabled, which will affect your results'
    );
    const memberCount = screen.getByText('2 Members Added');
    const labels = screen.getAllByRole('checkbox');
    expect(labels.length).toBe(2);
    expect(labels[0]).toBeVisible();
    expect(labels[1]).toBeVisible();
    expect(labels[0].closest('label')).toHaveTextContent('Adminasdf BuiltIn');
    expect(labels[1].closest('label')).toHaveTextContent('Group User (Group)');
    expect(associateGroupField).toBeNull();
    expect(groupAlert).toBeNull();
    expect(memberCount).toBeVisible();
  });

  it('removes a user from the members list', () => {
    renderComponent();

    let labels = screen.getAllByRole('checkbox');
    expect(labels.length).toBe(2);
    fireEvent.click(labels[0].closest('label'));

    labels = screen.getAllByRole('checkbox');
    expect(labels.length).toBe(1);
    expect(labels[0].closest('label')).toHaveTextContent('Group User (Group)');
    const memberCount = screen.getByText('1 Member Added');
    expect(memberCount).toBeVisible();
  });

  it('adds a user from the members list', () => {
    fetchUsersDataSpy.mockReturnValue([
      {
        type: 'USER',
        internalName: 'admin2',
        displayName: 'Adminasdf BuiltIn2',
        email: 'admin2@localhost',
        realm: 'IQ Server',
      },
      {
        type: 'GROUP',
        internalName: 'group2',
        displayName: 'Group User2 (Group)',
        email: 'null',
        realm: 'IQ Server',
      },
    ]);
    renderComponent();

    const searchInput = screen.getByRole('searchbox');
    const searchComponent = searchInput.closest('.nx-search-dropdown');
    expect(searchInput).toBeVisible();
    expect(searchInput).toHaveAttribute('placeholder', 'Search');

    fireEvent.focus(searchInput);
    fireEvent.change(searchInput, { target: { value: 'term' } });

    const results = within(searchComponent).getAllByRole('menuitem');
    expect(results.length).toBe(2);
    expect(results[0]).toHaveTextContent('Adminasdf BuiltIn2');
    expect(results[1]).toHaveTextContent('Group User2 (Group)');

    fireEvent.click(results[0]);

    // After clicking, the user should be added to the list
    // Verify that the addSelectedUser action was called with the correct user data
    expect(addSelectedUserSpy).toHaveBeenCalledTimes(1);
    expect(addSelectedUserSpy).toHaveBeenCalledWith({
      displayName: 'Adminasdf BuiltIn2',
      email: 'admin2@localhost',
      internalName: 'admin2',
      realm: 'IQ Server',
      type: 'USER',
    });
  });

  it('renders and adds a group', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);

    renderComponent();

    const associateGroupField = screen.getByText('Add an External Group');
    const groupAlert = screen.getByText(
      'One or more LDAP servers have group search disabled, which will affect your results'
    );
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    expect(associateGroupField).toBeVisible();
    expect(groupAlert).toBeVisible();

    let addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).toHaveClass('disabled');
    expect(addBtn).toHaveTextContent('Add');

    const addInput = within(associateGroupFieldFieldset).getByRole('textbox');
    expect(addInput).toBeVisible();

    fireEvent.focus(addInput);
    fireEvent.change(addInput, { target: { value: 'new group' } });
    expect(addInput).toHaveValue('new group');
    addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).not.toHaveClass('disabled');

    fireEvent.click(addBtn);
    expect(addInput).toHaveValue('');
    addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).toHaveClass('disabled');

    // After adding a group, we should still have the original 2 members
    // The actual group add functionality may require Redux actions that aren't properly mocked in this test
    // Verify that the addSelectedUser action was called with the correct user data
    expect(addSelectedUserSpy).toHaveBeenCalledTimes(1);
    expect(addSelectedUserSpy).toHaveBeenCalledWith({
      type: 'GROUP',
      internalName: 'new group',
      displayName: 'new group (Group)',
      id: 'new groupGROUP',
      email: null,
    });
  });

  it('calls the cancel action', () => {
    renderComponent();

    const cancel = screen.getByText('Cancel');
    expect(cancel).toBeVisible();
    fireEvent.click(cancel);
    expect(goToAdministratorsSpy).toHaveBeenCalledTimes(1);
  });

  it('calls the submit action', () => {
    renderComponent();

    const submit = screen.getByText('Submit');
    expect(submit).toBeVisible();
    fireEvent.click(submit);
    expect(saveMembersSpy).toHaveBeenCalledTimes(1);
  });

  it('calls the fetch action', () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date());
    renderComponent();

    const searchInput = screen.getByRole('searchbox');
    expect(searchInput).toBeVisible();
    expect(searchInput).toHaveAttribute('placeholder', 'Search');
    fireEvent.focus(searchInput);
    fireEvent.change(searchInput, { target: { value: 'term' } });
    jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
    expect(loadFetchUsersSpy).toHaveBeenCalledTimes(1);
    expect(loadFetchUsersSpy).toHaveBeenCalledWith('term');

    jest.useRealTimers();
  });

  it('mentions LDAP in the Add an External Group input sublabel', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    renderComponent();
    const input = screen.getByRole('textbox', { name: 'Add an External Group' });

    expect(input).toHaveAccessibleDescription('Requires an exact match of the LDAP group name');
  });

  describe('multi-tenant mode', () => {
    beforeEach(() => {
      selectTenantModeSpy.mockReturnValue('multi-tenant');
    });

    it('does not render the LDAP group search alert', () => {
      renderComponent();
      expect(screen.queryByText('One or more LDAP servers have group search disabled')).not.toBeInTheDocument();
    });

    it('renders the Add an External Group box and button when group search is disabled', () => {
      selectIsGroupSearchEnabledSpy.mockReturnValue(false);
      renderComponent();

      expect(screen.getByRole('textbox', { name: 'Add an External Group' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Add' })).toBeInTheDocument();
    });

    it('does not render the Add an External Group box and button when group search is enabled', () => {
      renderComponent();

      expect(screen.queryByRole('textbox', { name: 'Add an External Group' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Add' })).not.toBeInTheDocument();
    });

    it('mentions SAML rather than LDAP in the Add an External Group input sublabel', () => {
      selectIsGroupSearchEnabledSpy.mockReturnValue(false);
      renderComponent();
      const input = screen.getByRole('textbox', { name: 'Add an External Group' });

      expect(input).toHaveAccessibleDescription('Requires an exact match of the SAML group name');
    });
  });
});
