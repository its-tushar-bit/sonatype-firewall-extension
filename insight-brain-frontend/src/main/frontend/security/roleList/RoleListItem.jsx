/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faAngleRight } from '@fortawesome/free-solid-svg-icons';
import { useRouterState } from '../../react/RouterStateContext';

export default function RoleListItem({ role }) {
  const { id, name, description } = role;

  const history = useRouterState();

  return (
    <li className="nx-list__item nx-list__item--link" tabIndex={0}>
      <a className="nx-list__link" href={history.href('editRole', { roleId: id })}>
        <span className="nx-list__text">{name}</span>
        <span className="nx-list__subtext">{description}</span>
        <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
      </a>
    </li>
  );
}

RoleListItem.propTypes = {
  role: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    builtIn: PropTypes.bool.isRequired,
    permissionCategories: PropTypes.array,
  }).isRequired,
};
