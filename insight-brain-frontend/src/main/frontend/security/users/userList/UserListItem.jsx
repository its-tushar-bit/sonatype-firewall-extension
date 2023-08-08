/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxList } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';

import { useRouterState } from '../../../react/RouterStateContext';
import DeleteModal from '../modals/DeleteModal';

export default function UserListItem({ user, currentUsername, editable, deleteUser, deleteError, deleteMaskState }) {
  const { id: userId, username, firstName, lastName } = user;
  const isCurrentUser = currentUsername === username;

  const history = useRouterState();
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const ListItem = editable ? NxList.LinkItem : NxList.Item;
  const listItemProps = editable ? { href: history.href('editUser', { userId }) } : null;

  return (
    <ListItem {...listItemProps}>
      <NxList.Text>
        {username} ({firstName} {lastName})
        {isCurrentUser && (
          <>
            {' '}
            <span className="iq-user-list-item-current">Current User</span>
          </>
        )}
      </NxList.Text>
      {!editable && (
        <>
          <NxList.Actions>
            <NxButton
              variant="icon-only"
              className={classnames('iq-user-list-item__delete-btn', isCurrentUser ? 'disabled' : undefined)}
              onClick={isCurrentUser ? undefined : () => setShowDeleteModal(true)}
              title={isCurrentUser ? 'Current user cannot be deleted' : 'Delete user'}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </NxList.Actions>
          {showDeleteModal && (
            <DeleteModal
              {...{ userId, username, deleteUser, deleteError, deleteMaskState }}
              onCancel={() => setShowDeleteModal(false)}
            />
          )}
        </>
      )}
    </ListItem>
  );
}

UserListItem.propTypes = {
  currentUsername: PropTypes.string,
  user: PropTypes.shape({
    id: PropTypes.string,
    username: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
  }).isRequired,
  editable: PropTypes.bool.isRequired,
  deleteUser: PropTypes.func.isRequired,
  deleteError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
};
