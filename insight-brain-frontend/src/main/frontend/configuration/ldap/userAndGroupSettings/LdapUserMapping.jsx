/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton, NxModal, NxP, NxTable } from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

const LdapUserMapping = ({
  loadUserMapping,
  toggleUserMappingModalIsOpen,
  toggleUserMappingSortOrder,
  userList,
  loadError,
  sortAscending,
}) => {
  useEffect(() => {
    loadUserMapping();
  }, []);

  const formattedData = (userList || []).map((user) => {
    return { ...user, username: user.username || '', fieldCount: Object.values(user).filter((val) => val).length };
  });
  const sortedData = formattedData.slice().sort((a, b) => {
    if (a.fieldCount !== b.fieldCount) return sortAscending ? b.fieldCount - a.fieldCount : a.fieldCount - b.fieldCount;

    return sortAscending ? a.username.localeCompare(b.username) : b.username.localeCompare(a.username);
  });

  return (
    <NxModal onCancel={toggleUserMappingModalIsOpen} id="ldap-user-mapping-modal" variant="wide">
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          <span>Check User Mapping</span>
        </h2>
      </header>
      <div className="nx-modal-content">
        <NxP>
          Scroll through the table and verify that the values in each column are in correct format. If they are not,
          click &quot;close&quot; and revise your LDAP field mappings
        </NxP>
        <NxTable>
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell
                isSortable
                sortDir={sortAscending ? 'asc' : 'desc'}
                onClick={() => {
                  toggleUserMappingSortOrder();
                }}
              >
                Username
              </NxTable.Cell>
              <NxTable.Cell>Name</NxTable.Cell>
              <NxTable.Cell>Email</NxTable.Cell>
              <NxTable.Cell>Groups</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body
            isLoading={!userList && !loadError}
            error={loadError}
            retryHandler={() => {
              loadUserMapping();
            }}
          >
            {sortedData.map((user) => (
              <NxTable.Row key={user.username}>
                <NxTable.Cell>{user.username}</NxTable.Cell>
                <NxTable.Cell>{user.realName}</NxTable.Cell>
                <NxTable.Cell>{user.email}</NxTable.Cell>
                <NxTable.Cell>{(user.membership || []).join(', ')}</NxTable.Cell>
              </NxTable.Row>
            ))}
          </NxTable.Body>
        </NxTable>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton onClick={toggleUserMappingModalIsOpen}>Close</NxButton>
        </div>
      </footer>
    </NxModal>
  );
};

export const userMappingTypes = {
  loadUserMapping: PropTypes.func.isRequired,
  toggleUserMappingModalIsOpen: PropTypes.func.isRequired,
  sortAscending: PropTypes.bool.isRequired,
  toggleUserMappingSortOrder: PropTypes.func.isRequired,
  userList: PropTypes.arrayOf(
    PropTypes.shape({
      username: PropTypes.string,
      realName: PropTypes.string,
      email: PropTypes.string,
      membership: PropTypes.string,
    })
  ),
  loadError: PropTypes.string,
};

LdapUserMapping.propTypes = userMappingTypes;

export default LdapUserMapping;
