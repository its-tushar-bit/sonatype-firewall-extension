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
import { selectPrefixRoute } from 'MainRoot/reduxUiRouter/routerSelectors';

const Repository = memo(function Repository({ repositoryId, ...otherProps }) {
  const uiRouterState = useRouterState();
  const repository = useSelector((state) => selectOwnerById(state, repositoryId));
  const prefixRoute = useSelector(selectPrefixRoute);
  const repositoryUrl = uiRouterState.href(prefixRoute('management.view.repository'), { repositoryId });

  return (
    <NxOverflowTooltip>
      <a href={repositoryUrl} {...otherProps}>
        <div className="iq-owner-name">{repository?.name}</div>
      </a>
    </NxOverflowTooltip>
  );
});

Repository.propTypes = {
  repositoryId: PropTypes.string,
};

export default Repository;
