/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { sort } from 'ramda';
import { NxModal, NxButton } from '@sonatype/react-shared-components';
import useEscapeKeyStack from '../../../react/useEscapeKeyStack';

export const UserDetailsModal = ({ user, onClose }) => {
  useEscapeKeyStack(true, onClose);
  return (
    <NxModal id="user-details-modal" onClose={onClose}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          <span>Current User Details</span>
        </h2>
      </header>
      <dl className="user-details">
        <dt>Username</dt>
        <dd id="user-details-modal-username">{user.username}</dd>
        <dt>Display Name</dt>
        <dd id="user-details-modal-display-name">{user.displayName}</dd>
        <dt>Groups</dt>
        <dd id="user-details-modal-groups">{formatGroups(user.groups)}</dd>
      </dl>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton id="user-details-modal-close" onClick={onClose} variant="primary">
            Close
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
};

UserDetailsModal.propTypes = {
  user: PropTypes.shape({
    username: PropTypes.string,
    displayName: PropTypes.string,
    groups: PropTypes.array,
  }).isRequired,
  onClose: PropTypes.func.isRequired,
};

function formatGroups(groups = []) {
  return sort((a, b) => a.localeCompare(b), groups).join(', ');
}

export default UserDetailsModal;
