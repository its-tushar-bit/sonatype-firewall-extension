/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useCallback, useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxButton,
  NxFieldset,
  NxLoadWrapper,
  NxSearchDropdown,
  NxTransferListHalf,
  NxFormGroup,
  NxTextInput,
  NxFormSelect,
  NxStatefulInfoAlert,
  NxFormRow,
  NxButtonBar,
  NX_SEARCH_DROPDOWN_DEBOUNCE_TIME,
  NxStatefulErrorAlert,
  NxTooltip,
} from '@sonatype/react-shared-components';
import DeleteAccessModal from './DeleteAccessModal';
import { sortByDisplayName } from 'MainRoot/util/formatGroupUsers';

import {
  selectIsGroupSearchEnabled,
  selectValidationError,
  selectNoRolesAvailableError,
  selectAccessSlice,
  selectUnSortedAddedUsers,
  selectAvailableRoles,
  selectFetchUsers,
} from './accessSelectors';
import { selectTenantMode } from '../../productFeatures/productFeaturesSelectors';

import { actions } from './accessSlice';
import { allPass, filter, inc, prop, propEq, reduceBy } from 'ramda';
import { debounce } from 'debounce';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = (isDirty, validationError) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function AccessPage() {
  const dispatch = useDispatch();
  const accessProps = useSelector(selectAccessSlice);
  const { isNew, loading, isDirty, loadError, role, submitMaskState, groupName } = accessProps;
  const fetchUsers = accessProps.fetchUsers;
  const {
    loadError: fetchUsersLoadingError,
    partialError: fetchUsersPartialError,
    loading: fetchUsersLoading,
    submitError,
  } = fetchUsers;
  const fetchUsersData = useSelector(selectFetchUsers);
  const groupSearchEnabled = useSelector(selectIsGroupSearchEnabled);
  const validationError = useSelector(selectValidationError);
  const noRolesAvailableError = useSelector(selectNoRolesAvailableError);
  const addedUsers = sortByDisplayName(useSelector(selectUnSortedAddedUsers));
  const availableRoles = useSelector(selectAvailableRoles);
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';
  const createOrUpdateRole = () => dispatch(actions.createOrUpdateRole());
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [addedItemsFilter, setAddedItemsFilter] = useState('');

  const onChangeGroupName = (val) => dispatch(actions.setGroupName(val));

  const debouncedOnSearch = useCallback(
    debounce((query) => query && dispatch(actions.loadFetchUsers(query)), NX_SEARCH_DROPDOWN_DEBOUNCE_TIME),
    []
  );

  const isAddGroupDisabled = (() => {
    const filteredUsers = filter(allPass([propEq('internalName', groupName.trimmedValue), propEq('type', 'GROUP')]))(
      addedUsers
    );
    return !groupName.trimmedValue || filteredUsers.length > 0;
  })();

  useEffect(() => {
    doLoad();
  }, []);

  function doLoad() {
    dispatch(actions.loadRolesIfNeeded());
  }

  const openDeleteModal = () => setIsDeleteModalOpen(true);
  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
    dispatch(actions.clearDeleteError());
  };
  const removeRole = () => dispatch(actions.removeRole());

  const onSearch = (query) => {
    dispatch(actions.setLoadingFetchUsers(!!query));
    debouncedOnSearch(query);
  };

  const onSearchMatchSelect = (data) => {
    setSearchText('');
    dispatch(actions.addSelectedUser(data));
  };

  const onRemoveMembers = (_, idToRemove) => {
    const filteredData = addedUsers.filter(({ id }) => id !== idToRemove);
    dispatch(actions.setAddedUsers(filteredData));
  };

  const onRemoveAllMembers = () => {
    dispatch(actions.setAddedUsers([]));
  };

  const addedMembersCount = () => {
    const { GROUP: groupsNumber = 0, USER: usersNumber = 0 } = reduceBy(inc, 0, prop('type'), addedUsers);
    return `${usersNumber} User${usersNumber === 1 ? '' : 's'} and ${groupsNumber} Group${
      groupsNumber === 1 ? '' : 's'
    } Added`;
  };

  const selectAvailableRole = (event) => {
    const roleId = event.target.value;
    dispatch(actions.setRole(roleId));
  };

  const addGroup = () => {
    if (isAddGroupDisabled) return;
    dispatch(actions.addSelectedUserGroup());
  };

  function getUserTooltipTitle(user) {
    if (user.realm && !user.email) {
      return <>{user.realm}</>;
    }
    if (user.realm && user.email) {
      return (
        <>
          {user.realm} <br /> {user.email}
        </>
      );
    }
    if (!user.realm && user.email) {
      return <>{user.email}</>;
    }
    return user.displayName;
  }

  const getUserItems = (addedUsers) =>
    addedUsers.map((user) => {
      return {
        id: user.id,
        displayName: user.displayName,
        tooltip: {
          title: getUserTooltipTitle(user),
        },
      };
    });

  return (
    <div id="create-edit-access-page">
      <NxPageTitle>
        {isNew ? (
          <NxH1>New Role</NxH1>
        ) : (
          <NxPageTitle.Headings>
            <NxH1>Edit Role</NxH1>
            <NxPageTitle.Subtitle>{role?.roleName}</NxPageTitle.Subtitle>
          </NxPageTitle.Headings>
        )}
      </NxPageTitle>

      <NxLoadWrapper loading={loading} error={loadError || noRolesAvailableError} retryHandler={doLoad}>
        <NxTile>
          <NxStatefulForm
            onSubmit={addedUsers.length ? createOrUpdateRole : openDeleteModal}
            doLoad={doLoad}
            submitBtnText={isNew ? 'Create' : 'Update'}
            submitMaskMessage="Saving…"
            submitError={submitError}
            submitMaskState={submitMaskState}
            validationErrors={getValidationMessage(isDirty, validationError)}
            additionalFooterBtns={
              !isNew && (
                <NxButton id="delete-role-button" variant="tertiary" onClick={openDeleteModal} type="button">
                  Delete
                </NxButton>
              )
            }
            id="access-add-members-form"
          >
            <NxTile.Content>
              {isNew && (
                <NxFormGroup label="Role" isRequired>
                  <NxFormSelect onChange={selectAvailableRole}>
                    <option value={''}>Select Role</option>
                    {availableRoles?.map((role) => {
                      return (
                        <option key={role.roleId} value={role.roleId}>
                          {role.roleName}
                        </option>
                      );
                    })}
                  </NxFormSelect>
                </NxFormGroup>
              )}
              <NxFieldset
                label="Search Users and Groups"
                sublabel="Search by name or use ‘*’ as wildcard (ex. ‘Isa*’ matches ‘Isaac Asimov’)"
                isRequired
              >
                {!groupSearchEnabled && !isMultiTenant && (
                  <NxStatefulInfoAlert id="ldap-servers-alert">
                    One or more LDAP servers have group search disabled, which will affect your results
                  </NxStatefulInfoAlert>
                )}
                {fetchUsersPartialError && <NxStatefulErrorAlert>{fetchUsersPartialError}</NxStatefulErrorAlert>}
                <NxSearchDropdown
                  {...{ searchText, onSearch }}
                  onSelect={onSearchMatchSelect}
                  onSearchTextChange={setSearchText}
                  loading={fetchUsersLoading}
                  error={fetchUsersLoadingError}
                  matches={fetchUsersData.data}
                />
              </NxFieldset>
              {!groupSearchEnabled && (
                <NxFormRow>
                  {/* Note about sublabel: MTIQ doesn't support LDAP and on-prem doesn't currently show
                   * this field for SAML (only for LDAP), hence the conditional string
                   */}
                  <NxFormGroup
                    id="associate-group-form-group"
                    label="Add an External Group"
                    sublabel={`Requires an exact match of the ${isMultiTenant ? 'SAML' : 'LDAP'} group name`}
                  >
                    <NxTextInput
                      onChange={onChangeGroupName}
                      className="associate-group-input"
                      id="add-associate-group-input"
                      placeholder="e.g., authenticated-users"
                      {...groupName}
                    />
                  </NxFormGroup>
                  <NxButtonBar>
                    <NxTooltip
                      title={
                        isAddGroupDisabled
                          ? 'Unable to add: field is empty or contains an already added group name.'
                          : ''
                      }
                    >
                      <NxButton
                        onClick={addGroup}
                        type="button"
                        className={isAddGroupDisabled ? 'disabled' : ''}
                        id="add-associate-group-btn"
                      >
                        Add
                      </NxButton>
                    </NxTooltip>
                  </NxButtonBar>
                </NxFormRow>
              )}
              <NxTransferListHalf
                label="Associated Members"
                filterValue={addedItemsFilter}
                onFilterChange={setAddedItemsFilter}
                showMoveAll
                items={getUserItems(addedUsers)}
                onItemChange={onRemoveMembers}
                onMoveAll={onRemoveAllMembers}
                isSelected
                footerContent={addedMembersCount()}
              />
            </NxTile.Content>
          </NxStatefulForm>
        </NxTile>
      </NxLoadWrapper>

      {isDeleteModalOpen && role && <DeleteAccessModal removeRole={removeRole} closeDeleteModal={closeDeleteModal} />}
    </div>
  );
}
