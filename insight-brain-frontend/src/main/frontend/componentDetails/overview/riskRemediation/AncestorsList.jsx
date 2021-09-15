/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { useRouterState } from '../../../react/RouterStateContext';
import { NxTextLink } from '@sonatype/react-shared-components';
import { AncestorPropTypes } from '../overviewTypes';

export const AncestorsList = ({ routeName, ancestors }) => {
  const uiRouterState = useRouterState();

  return (
    <ul className="nx-list">
      {ancestors.map(({ hash, derivedComponentName }) => (
        <li className="nx-list__item" key={hash}>
          <NxTextLink href={uiRouterState.href(routeName, { hash })}>{derivedComponentName}</NxTextLink>
        </li>
      ))}
    </ul>
  );
};

AncestorsList.propTypes = {
  routeName: PropTypes.string.isRequired,
  ancestors: PropTypes.arrayOf(AncestorPropTypes).isRequired,
};
