/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import AccessPage from 'MainRoot/OrgsAndPolicies/access/AccessPage';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/access/accessSlice';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import { NX_SEARCH_DROPDOWN_DEBOUNCE_TIME } from '@sonatype/react-shared-components';

// Import SpecUtil for jasmine compatibility layer
import 'TestRoot/SpecUtil';

describe('AccessPage Component', () => {
  let renderComponent,
    createOrUpdateRoleSpy,
    selectRoleSpy,
    removeRoleSpy,
    selectIsGroupSearchEnabledSpy,
    setGroupNameSpy,
    addSelectedUserGroupSpy,
    saveMaskTimerDoneSpy,
    selectTenantModeSpy;

  const addedUsers = [
    {
      displayName: 'Adminasdf BuiltIn',
      email: 'myEmail@gmail.com',
      id: 'Adminasdf',
      internalName: 'BuiltIn',
      realm: 'IQ Server',
      type: 'USER',
    },
    {
      displayName: 'myGroup (Group)',
      email: null,
      id: 'myGroupGROUP',
      internalName: 'myGroup',
      type: 'GROUP',
    },
  ];

  beforeEach(() => {
    jest.spyOn(accessSelectors, 'selectOwnerType').mockReturnValue('organization');
    selectIsGroupSearchEnabledSpy = jest.spyOn(accessSelectors, 'selectIsGroupSearchEnabled').mockReturnValue(true);
    createOrUpdateRoleSpy = jest.spyOn(actions, 'createOrUpdateRole');
    selectRoleSpy = jest.spyOn(actions, 'selectRole');
    removeRoleSpy = jest.spyOn(actions, 'removeRole');
    jest.spyOn(actions, 'setAddedUsers');
    setGroupNameSpy = jest.spyOn(actions, 'setGroupName');
    addSelectedUserGroupSpy = jest.spyOn(actions, 'addSelectedUserGroup');
    selectTenantModeSpy = jest.spyOn(productFeaturesSelectors, 'selectTenantMode').mockReturnValue('single-tenant');
    jest.spyOn(accessSelectors, 'selectFetchUsers').mockReturnValue({
      data: [
        {
          displayName: 'Admin BuiltIn',
          email: 'admin@localhost',
          internalName: 'admin',
          realm: 'IQ Server',
          type: 'USER',
        },
        {
          displayName: 'Authenticated Users (Group)',
          email: null,
          internalName: '(all-authenticated-users)',
          realm: 'IQ Server',
          type: 'GROUP',
        },
      ],
    });

    saveMaskTimerDoneSpy = jest.spyOn(actions, 'saveMaskTimerDone');

    jest.spyOn(actions, 'loadRoles').mockReturnValue({
      type: 'access/loadRoles/fulfilled',
      payload: {
        roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
        data: {
          groupSearchEnabled: true,
          membersByRole: [
            {
              roleId: '2cb71b3468d649789163ea2e212b541e',
              roleName: 'Application Evaluator',
              roleDescription: 'Evaluates applications and views policy violation summary results.',
              membersByOwner: [
                {
                  members: [],
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                },
              ],
            },
            {
              roleId: '90c7c98683b4471cb77a916744540bcc',
              roleName: 'Component Evaluator',
              roleDescription:
                'Evaluates individual components and views policy violation results for a specified application.',
              membersByOwner: [
                {
                  members: [],
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                },
              ],
            },
            {
              roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
              roleName: 'Developer',
              roleDescription: 'Views all information for their assigned organization or application.',
              membersByOwner: [
                {
                  members: [
                    {
                      displayName: 'Admin BuiltIn',
                      email: 'admin@localhost',
                      internalName: 'admin',
                      realm: 'IQ Server',
                      type: 'USER',
                    },
                  ],
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  ownerName: 'Root Organization',
                  ownerType: 'organization',
                },
              ],
            },
          ],
        },
      },
    });

    jest.spyOn(accessSelectors, 'selectAvailableRoles').mockReturnValue([
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
        membersByOwner: [
          {
            members: [],
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
        ],
      },
      {
        roleId: '90c7c98683b4471cb77a916744540bcc',
        roleName: 'Component Evaluator',
        roleDescription:
          'Evaluates individual components and views policy violation results for a specified application.',
        membersByOwner: [
          {
            members: [],
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
        ],
      },
      {
        roleId: '1da70fae1fd54d6cb7999871ebdb9a36',
        roleName: 'Developer',
        roleDescription: 'Views all information for their assigned organization or application.',
        membersByOwner: [
          {
            members: [],
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
        ],
      },
      {
        roleId: '0df46317c031440795007f4ce9c7f002',
        roleName: 'Legal Reviewer',
        roleDescription: 'Reviews legal obligations for component licenses.',
        membersByOwner: [
          {
            members: [],
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
        ],
      },
      {
        roleId: '1cddabf7fdaa47d6833454af10e0a3ef',
        roleName: 'Owner',
        roleDescription: 'Manages assigned organizations, applications, policies, and policy violations.',
        membersByOwner: [
          {
            members: [],
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
          },
        ],
      },
    ]);

    renderComponent = () => render(<AccessPage />);
  });

  it('renders tile with the correct page title "New Role"', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    expect(screen.getByText('New Role')).toBeVisible();
  });

  it('renders tile with the correct page title "Edit Role"', () => {
    renderComponent();
    expect(screen.getByText('Edit Role')).toBeVisible();
  });

  it('renders loading indicator', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      loading: true,
      loadError: null,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error message', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      loading: null,
      loadError: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    expect(screen.getByText('An error occurred loading data.')).toBeVisible();
  });

  it('initial disabled Create button', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const createButton = screen.getByRole('button', { name: 'Create' });
    expect(createButton).toBeVisible();
    fireEvent.click(createButton);
    expect(createOrUpdateRoleSpy).not.toHaveBeenCalled();
    expect(saveMaskTimerDoneSpy).not.toHaveBeenCalled();
  });

  it('initial case for "Edit Role" (dropdown is missing )', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: false,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    expect(screen.queryByText('Select Role')).toBeNull();
  });

  it('initial case for "New Role" (dropdown is present)', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const dropdown = screen.getByRole('combobox');
    expect(dropdown).toBeVisible();
  });

  it('initial disabled Create button', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const createButton = screen.getByRole('button', { name: 'Create' });
    expect(createButton).toBeVisible();
    fireEvent.click(createButton);
    expect(createOrUpdateRoleSpy).not.toHaveBeenCalled();
    expect(saveMaskTimerDoneSpy).not.toHaveBeenCalled();
  });

  it('Search feild is present', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const search = screen.getByRole('searchbox');
    expect(search).toBeVisible();
  });

  it('Add an External Group field is present', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {
        isPristine: true,
        trimmedValue: 'new group',
        validationErrors: null,
        value: 'new group',
      },
      fetchUsers: {},
    });
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    renderComponent();
    const associateGroupField = screen.getByText('Add an External Group');
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    expect(associateGroupField).toBeVisible();
    let addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).not.toHaveClass('disabled');
    expect(addBtn).toHaveTextContent('Add');
    const addInput = within(associateGroupFieldFieldset).getByRole('textbox');
    expect(addInput).toBeVisible();
  });

  it('Add an External Group field is not present in the DOM', () => {
    renderComponent();
    const associateGroupField = screen.queryByText('Add an External Group');
    expect(associateGroupField).toBeNull();
  });

  it('calls setGroupName action when we change Associated Group input field', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    renderComponent();
    const associateGroupField = screen.getByText('Add an External Group');
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    const addInput = within(associateGroupFieldFieldset).getByRole('textbox');
    fireEvent.change(addInput, { target: { value: 'new group' } });
    expect(setGroupNameSpy).toHaveBeenCalledWith('new group');
  });

  it('disables Associated Group input Add button if input field is empty', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    renderComponent();
    const associateGroupField = screen.getByText('Add an External Group');
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    const addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).toHaveClass('disabled');
    fireEvent.click(addBtn);
    expect(addSelectedUserGroupSpy).not.toHaveBeenCalled();
  });

  it('disables Associated Group input Add button if we try to add group which already exist in the Transfer List', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    jest.spyOn(accessSelectors, 'selectUnSortedAddedUsers').mockReturnValue(addedUsers);
    jest.spyOn(accessSelectors, 'selectGroupName').mockReturnValue({
      isPristine: true,
      trimmedValue: '',
      validationErrors: null,
      value: '',
    });
    renderComponent();
    const associateGroupField = screen.getByText('Add an External Group');
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    const addBtn = within(associateGroupFieldFieldset).getByRole('button');
    const addInput = within(associateGroupFieldFieldset).getByRole('textbox');
    fireEvent.change(addInput, { target: { value: 'Group' } });
    expect(addBtn).not.toHaveClass('disabled');
    fireEvent.change(addInput, { target: { value: 'myGroup' } });
    expect(addBtn).toHaveClass('disabled');
    fireEvent.click(addBtn);
    expect(addSelectedUserGroupSpy).not.toHaveBeenCalled();
  });

  it('renders and adds a group + select role', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    jest.spyOn(accessSelectors, 'selectGroupName').mockReturnValue({
      isPristine: true,
      trimmedValue: '',
      validationErrors: null,
      value: '',
    });
    renderComponent();
    const associateGroupField = screen.getByText('Add an External Group');
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    expect(associateGroupField).toBeVisible();
    let addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).toHaveTextContent('Add');
    const addInput = within(associateGroupFieldFieldset).getByRole('textbox');
    expect(addInput).toBeVisible();
    fireEvent.change(addInput, { target: { value: 'new group' } });
    expect(addInput).toHaveValue('new group');
    addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).not.toHaveClass('disabled');
    fireEvent.click(addBtn);
    addBtn = within(associateGroupFieldFieldset).getByRole('button');
    const labels = screen.getAllByRole('checkbox');
    expect(labels.length).toBe(2);
    expect(labels[0].closest('label')).toHaveTextContent('Admin BuiltIn');
    expect(labels[1].closest('label')).toHaveTextContent('new group (Group)');
    const createButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(createButton);
    expect(createOrUpdateRoleSpy).toHaveBeenCalledTimes(1);
  });

  it('select available role', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: '1da70fae1fd54d6cb7999871ebdb9a36' } });
    selectRoleSpy('1da70fae1fd54d6cb7999871ebdb9a36');
    expect(selectRoleSpy).toHaveBeenCalled();
  });

  it('add new user from search + select role (create role success)', (done) => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isDirty: true,
      isNew: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const search = screen.getByRole('searchbox');
    expect(search).toBeVisible();
    fireEvent.focus(search);
    fireEvent.change(search, { target: { value: 'Authenticated Users' } });

    // Use a timeout to test debounced function call
    setTimeout(() => {
      expect(search).toHaveValue('Authenticated Users');
      const searchUser = screen.getByRole('menuitem', { name: 'Authenticated Users (Group)' });
      fireEvent.click(searchUser);
      const select = screen.getByRole('combobox');
      fireEvent.change(select, { target: { value: '2cb71b3468d649789163ea2e212b541e' } });
      const createButton = screen.getByRole('button', { name: 'Create' });
      fireEvent.click(createButton);
      expect(createOrUpdateRoleSpy).toHaveBeenCalledTimes(1);
      done();
    }, NX_SEARCH_DROPDOWN_DEBOUNCE_TIME);
  });

  it('renders initial members list', () => {
    jest.spyOn(accessSelectors, 'selectUnSortedAddedUsers').mockReturnValue(addedUsers);
    renderComponent();
    selectIsGroupSearchEnabledSpy.mockReturnValue(true);
    const groupAlert = screen.queryByText(
      'One or more LDAP servers have group search disabled, which will affect your results'
    );
    const memberCount = screen.getByText('1 User and 1 Group Added');
    const labels = screen.getAllByRole('checkbox');
    expect(labels.length).toBe(2);
    expect(labels[0]).toBeVisible();
    expect(labels[1]).toBeVisible();
    expect(labels[0].closest('label')).toHaveTextContent('Adminasdf BuiltIn');
    expect(labels[1].closest('label')).toHaveTextContent('myGroup');
    expect(groupAlert).toBeNull();
    expect(memberCount).toBeVisible();
  });

  it('initial disabled Update button', () => {
    renderComponent();
    const updateButton = screen.getByRole('button', { name: 'Update' });
    expect(updateButton).toBeVisible();
    fireEvent.click(updateButton);
    expect(createOrUpdateRoleSpy).not.toHaveBeenCalled();
    expect(saveMaskTimerDoneSpy).not.toHaveBeenCalled();
  });

  it('initial active delete button', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isNew: false,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    expect(deleteButton).toBeVisible();
    expect(deleteButton).not.toHaveClass('disabled');
  });

  it('Update button is active by adding user from search', (done) => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isDirty: true,
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const search = screen.getByRole('searchbox');
    fireEvent.focus(search);
    fireEvent.change(search, { target: { value: 'Authenticated Users' } });

    // Use a timeout to test debounced function call
    setTimeout(() => {
      const searchButton = screen.getByRole('menuitem', { name: 'Authenticated Users (Group)' });
      fireEvent.click(searchButton);
      const updateButton = screen.getByRole('button', { name: 'Update' });
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      expect(createOrUpdateRoleSpy).toHaveBeenCalledTimes(1);
      done();
    }, NX_SEARCH_DROPDOWN_DEBOUNCE_TIME);
  });

  it('Update button is active by adding new group by Add button', () => {
    selectIsGroupSearchEnabledSpy.mockReturnValue(false);
    renderComponent();
    const associateGroupField = screen.getByText('Add an External Group');
    const associateGroupFieldFieldset = associateGroupField.closest('.nx-form-row');
    expect(associateGroupField).toBeVisible();
    let addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).toHaveClass('disabled');
    expect(addBtn).toHaveTextContent('Add');
    const addInput = within(associateGroupFieldFieldset).getByRole('textbox');
    expect(addInput).toBeVisible();
    fireEvent.focus(addInput);
    fireEvent.change(addInput, { target: { value: 'new Group' } });
    expect(addInput).toHaveValue('new Group');
    addBtn = within(associateGroupFieldFieldset).getByRole('button');
    expect(addBtn).not.toHaveClass('disabled');
    fireEvent.click(addBtn);
    const labels = screen.getAllByRole('checkbox');
    expect(labels.length).toBe(2);
    expect(labels[0].closest('label')).toHaveTextContent('Admin BuiltIn');
    expect(labels[1].closest('label')).toHaveTextContent('new Group (Group)');
    const updateButton = screen.getByRole('button', { name: 'Update' });
    expect(updateButton).not.toHaveClass('disabled');
    fireEvent.click(updateButton);
    expect(createOrUpdateRoleSpy).toHaveBeenCalledTimes(1);
  });

  it('remove role by Update button (Remove All added user)', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      isDirty: true,
      role: {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
      },
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const removeAllButton = screen.getByRole('button', { name: 'Remove All' });
    fireEvent.click(removeAllButton);
    const updateButton = screen.getByRole('button', { name: 'Update' });
    fireEvent.click(updateButton);
    expect(screen.getByText('Delete Role')).toBeVisible();
    const modalDeleteButton = screen.getByRole('button', { name: 'Continue' });
    expect(modalDeleteButton).toBeVisible();
    fireEvent.click(modalDeleteButton);
    expect(removeRoleSpy).toHaveBeenCalledTimes(1);
  });

  it('remove role (check Delete button)', () => {
    jest.spyOn(accessSelectors, 'selectAccessSlice').mockReturnValue({
      role: {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        roleDescription: 'Evaluates applications and views policy violation summary results.',
      },
      groupName: {},
      fetchUsers: {},
    });
    renderComponent();
    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    expect(deleteButton).toBeVisible();
    fireEvent.click(deleteButton);
    expect(screen.getByText('Delete Role')).toBeVisible();
    const modalDeleteButton = screen.getByRole('button', { name: 'Continue' });
    expect(modalDeleteButton).toBeVisible();
    fireEvent.click(modalDeleteButton);
    expect(removeRoleSpy).toHaveBeenCalledTimes(1);
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
