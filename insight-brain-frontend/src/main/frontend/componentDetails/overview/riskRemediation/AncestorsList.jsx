/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxTextLink } from '@sonatype/react-shared-components';
import { AncestorPropTypes } from '../overviewTypes';

export const AncestorsList = ({ ancestors, ancestorOnClick }) => {
  return (
    <ul className="nx-list">
      {ancestors.map(({ hash, derivedComponentName }) => (
        <li className="nx-list__item" key={hash}>
          <NxTextLink
            onClick={() => {
              ancestorOnClick(hash);
            }}
          >
            {derivedComponentName}
          </NxTextLink>
        </li>
      ))}
    </ul>
  );
};

AncestorsList.propTypes = {
  ancestors: PropTypes.arrayOf(AncestorPropTypes).isRequired,
  ancestorOnClick: PropTypes.func.isRequired,
};
