/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxList } from '@sonatype/react-shared-components';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import LoadWrapper from '../../../react/LoadWrapper';
import UserListItem from './UserListItem';

export default function UserList(props) {
  const {
    stateGo,
    loadListPage,
    deleteUser,
    users,
    loading,
    loadError,
    deleteError,
    deleteMaskState,
    currentUsername,
    tenantMode,
  } = props;
  const isMultiTenant = tenantMode === 'multi-tenant';

  useEffect(() => {
    loadListPage();
  }, []);

  const createUser = () => {
    stateGo('createUser');
  };

  return (
    <main id="user-management" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Users</h1>
      </div>
      <LoadWrapper loading={loading} error={loadError} retryHandler={loadListPage}>
        <section className="nx-tile">
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure Users</h2>
            </div>
            <div className="nx-tile__actions">
              {isMultiTenant ? (
                <NxButton id="invite-user" onClick={createUser}>
                  <NxFontAwesomeIcon icon={faPlus} />
                  <span>Invite User</span>
                </NxButton>
              ) : (
                <NxButton variant="tertiary" id="create-user" onClick={createUser}>
                  <NxFontAwesomeIcon icon={faPlus} />
                  <span>Create User</span>
                </NxButton>
              )}
            </div>
          </header>
          <div className="nx-tile-content">
            <NxList
              id="user-management-list"
              emptyMessage="There are no users configured. Click the Invite button to add more users."
            >
              {users.map((user) => (
                <UserListItem
                  key={user.id}
                  user={user}
                  currentUsername={currentUsername}
                  editable={tenantMode !== 'multi-tenant'}
                  {...{ deleteUser, deleteError, deleteMaskState }}
                />
              ))}
            </NxList>
          </div>
        </section>
      </LoadWrapper>
    </main>
  );
}

UserList.propTypes = {
  stateGo: PropTypes.func.isRequired,
  loadListPage: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  currentUsername: PropTypes.string,
  loading: PropTypes.bool,
  tenantMode: PropTypes.string,
  users: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      username: PropTypes.string.isRequired,
      firstName: PropTypes.string.isRequired,
      lastName: PropTypes.string.isRequired,
    })
  ),
  deleteUser: PropTypes.func.isRequired,
  deleteError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
};
