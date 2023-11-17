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
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const RepositoryManager = memo(({ repositoryManagerId, ...otherProps }) => {
  const uiRouterState = useRouterState();
  const repositoryManager = useSelector((state) => selectOwnerById(state, repositoryManagerId));
  const repositoryManagerUrl = uiRouterState.href('management.view.repository_manager', { repositoryManagerId });

  return (
    <NxOverflowTooltip>
      <a href={repositoryManagerUrl} {...otherProps}>
        <div className="iq-owner-name">{repositoryManager?.name}</div>
      </a>
    </NxOverflowTooltip>
  );
});

RepositoryManager.propTypes = {
  repositoryManagerId: PropTypes.string,
};

export default RepositoryManager;
