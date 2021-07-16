/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faAngleRight } from '@fortawesome/free-solid-svg-icons';
import { useRouterState } from '../../../react/RouterStateContext';

export default function UserListItem({ user, currentUsername }) {
  const { id, username, firstName, lastName } = user;
  const history = useRouterState();
  const isCurrentUser = currentUsername === username;

  return (
    <li className="nx-list__item nx-list__item--link" tabIndex={0}>
      <a className="nx-list__link" href={history.href('editUser', { userId: id })}>
        <span className="nx-list__text">
          {username} ({firstName} {lastName}){' '}
          {isCurrentUser && <span className="iq-user-list-item-current">Current User</span>}
        </span>
        <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
      </a>
    </li>
  );
}

UserListItem.propTypes = {
  currentUsername: PropTypes.string,
  user: PropTypes.shape({
    id: PropTypes.string.isRequired,
    username: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
  }).isRequired,
};
