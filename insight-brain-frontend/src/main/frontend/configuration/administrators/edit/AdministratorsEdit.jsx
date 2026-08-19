/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useCallback, useState, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { debounce } from 'debounce';
import {
  NxButton,
  NxDivider,
  NxFieldset,
  NxStatefulForm,
  NxFormGroup,
  NxH1,
  NxH2,
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxSearchDropdown,
  NxStatefulErrorAlert,
  NxStatefulInfoAlert,
  NxTextInput,
  NxTile,
  NxTransferListHalf,
  NX_SEARCH_DROPDOWN_DEBOUNCE_TIME,
} from '@sonatype/react-shared-components';

import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import {
  selectAddedUsers,
  selectUsersNotAdded,
  selectFetchUsersLoading,
  selectFetchUsersLoadingError,
  selectIsLoading,
  selectLoadError,
  selectRoleToEdit,
  selectSubmitError,
  selectSubmitMaskState,
  selectIsGroupSearchEnabled,
  selectFetchUsersPartialError,
} from '../administratorsSelectors';
import { selectTenantMode } from '../../../productFeatures/productFeaturesSelectors';
import { actions } from '../administratorsSlice';
import { removeFormatting } from 'MainRoot/util/formatGroupUsers';
import { allPass, compose, filter, map, not, propEq } from 'ramda';
import classNames from 'classnames';

const RoleDetails = ({ name, description }) => (
  <Fragment>
    <NxH2>Role Details</NxH2>
    <dl className="nx-read-only">
      <dt className="nx-read-only__label">Role Name</dt>
      <dd className="nx-read-only__data">{name}</dd>
      <dt className="nx-read-only__label">Role Description</dt>
      <dd className="nx-read-only__data">{description}</dd>
    </dl>
  </Fragment>
);

RoleDetails.propTypes = {
  name: PropTypes.string,
  description: PropTypes.string,
};

const AdministratorsEdit = () => {
  const dispatch = useDispatch();

  const loadRoles = () => dispatch(actions.loadRolesIfNeeded());

  const isLoading = useSelector(selectIsLoading);
  const loadingError = useSelector(selectLoadError);
  const roleToEdit = useSelector(selectRoleToEdit);

  const fetchUsersLoading = useSelector(selectFetchUsersLoading);
  const fetchUsersLoadingError = useSelector(selectFetchUsersLoadingError);
  const fetchUsersPartialError = useSelector(selectFetchUsersPartialError);
  const fetchUsersData = useSelector(selectUsersNotAdded);
  const addedUsers = useSelector(selectAddedUsers);
  const groupSearchEnabled = useSelector(selectIsGroupSearchEnabled);

  const submitMaskState = useSelector(selectSubmitMaskState);
  const submitError = useSelector(selectSubmitError);
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';

  const [searchText, setSearchText] = useState('');
  const [groupName, setGroupName] = useState('');
  const [addedItemsFilter, setAddedItemsFilter] = useState('');
  const [isPristine, setIsPristine] = useState(true);

  const debouncedOnSearch = useCallback(
    debounce((query) => query && dispatch(actions.loadFetchUsers(query)), NX_SEARCH_DROPDOWN_DEBOUNCE_TIME),
    []
  );

  const onSearch = (query) => {
    dispatch(actions.setLoadingFetchUsers(!!query));
    debouncedOnSearch(query);
  };

  const onSearchMatchSelect = (data) => {
    setSearchText('');
    dispatch(actions.addSelectedUser(data));
  };

  const onRemoveMembers = (_, id) => {
    const filterData = filter(compose(not, propEq('id', id)));
    const formattedUsers = compose(map(removeFormatting), filterData)(addedUsers);
    dispatch(actions.setAddedUsers(formattedUsers));
  };

  const onRemoveAllMembers = () => {
    dispatch(actions.setAddedUsers([]));
  };

  const addGroup = () => {
    if (isAddGroupDisabled) return;
    setGroupName('');
    const group = {
      displayName: `${groupName} (Group)`,
      email: null,
      id: `${groupName}GROUP`,
      internalName: groupName,
      type: 'GROUP',
    };
    dispatch(actions.addSelectedUser(group));
  };

  const onCancel = () => {
    dispatch(actions.goToAdministrators());
  };

  const onSubmit = () => {
    dispatch(actions.saveMembers());
  };

  const isAddGroupDisabled = useMemo(() => {
    const filteredUsers = filter(allPass([propEq('internalName', groupName), propEq('type', 'GROUP')]))(addedUsers);
    return !groupName || filteredUsers.length > 0;
  }, [groupName, addedUsers]);

  const getMembersCount = () => {
    return `${addedUsers.length} Member${addedUsers.length === 1 ? '' : 's'} Added`;
  };

  const addBtnClassnames = classNames({
    ['disabled']: isAddGroupDisabled,
  });

  useEffect(() => {
    loadRoles();
  }, []);

  useEffect(() => {
    setIsPristine(false);
  }, [groupName]);

  return (
    <NxPageMain className="iq-administrators-edit">
      <MenuBarBackButton stateName="administratorsConfig" />
      <NxPageTitle>
        <NxH1>Configure Administrators</NxH1>
      </NxPageTitle>
      <NxTile>
        <NxLoadWrapper loading={isLoading} retryHandler={loadRoles} error={loadingError}>
          <NxTile.Content>
            <RoleDetails name={roleToEdit?.roleName} description={roleToEdit?.roleDescription} />
            <NxDivider />
            <NxH2>Add Members</NxH2>
            <NxStatefulForm
              {...{ onSubmit, onCancel, submitError, submitMaskState }}
              id="administrators-add-members-form"
            >
              <NxFieldset
                label="Search Users and Groups"
                sublabel="use ‘*’ as wildcard (ex. ‘Isa*’ matches ‘Isaac Asimov’)"
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
                  matches={fetchUsersData}
                />
              </NxFieldset>
              {!groupSearchEnabled && (
                <div className="nx-form-row">
                  {/* Note about sublabel: MTIQ doesn't support LDAP and on-prem doesn't currently show
                   * this field for SAML (only for LDAP), hence the conditional string
                   */}
                  <NxFormGroup
                    id="associate-group-form-group"
                    label="Add an External Group"
                    sublabel={`Requires an exact match of the ${isMultiTenant ? 'SAML' : 'LDAP'} group name`}
                  >
                    <NxTextInput
                      value={groupName}
                      onChange={setGroupName}
                      className="associate-group-input"
                      id="add-associate-group-input"
                      isPristine={isPristine}
                      placeholder="e.g., authenticated-users"
                    />
                  </NxFormGroup>
                  <div className="nx-btn-bar">
                    <NxButton
                      onClick={addGroup}
                      type="button"
                      className={addBtnClassnames}
                      aria-disabled={isAddGroupDisabled}
                      id="add-associate-group-btn"
                      title={
                        isAddGroupDisabled
                          ? `Unable to add: field is empty or contains an already added group name.`
                          : ''
                      }
                    >
                      Add
                    </NxButton>
                  </div>
                </div>
              )}
              <NxTransferListHalf
                label="Associated Members"
                filterValue={addedItemsFilter}
                onFilterChange={setAddedItemsFilter}
                showMoveAll
                items={addedUsers}
                onItemChange={onRemoveMembers}
                onMoveAll={onRemoveAllMembers}
                isSelected
                footerContent={getMembersCount()}
              />
            </NxStatefulForm>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    </NxPageMain>
  );
};

export default AdministratorsEdit;
