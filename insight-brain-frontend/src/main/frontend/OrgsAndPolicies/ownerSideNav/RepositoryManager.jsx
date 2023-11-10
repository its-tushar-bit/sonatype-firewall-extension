/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { memo } from 'react';
import { useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';

import { NxOverflowTooltip } from '@sonatype/react-shared-components';

import { selectOwnerById } from './ownerSideNavSelectors';

const RepositoryManager = memo(({ repositoryManagerId, ...otherProps }) => {
  const repositoryManager = useSelector((state) => selectOwnerById(state, repositoryManagerId));

  return (
    <NxOverflowTooltip>
      <a {...otherProps}>
        <div className="iq-owner-name">{repositoryManager?.name}</div>
      </a>
    </NxOverflowTooltip>
  );
});

RepositoryManager.propTypes = {
  repositoryManagerId: PropTypes.string,
};

export default RepositoryManager;
