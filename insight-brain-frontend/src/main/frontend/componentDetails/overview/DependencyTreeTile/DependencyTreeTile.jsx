/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxTile } from '@sonatype/react-shared-components';

import {
  selectApplicationInfo,
  selectDependencyTreeSubset,
  selectComponentDetails,
} from 'MainRoot/componentDetails/componentDetailsSelectors';
import DependencyTree from 'MainRoot/DependencyTree/DependencyTree';
import { actions } from 'MainRoot/componentDetails/componentDetailsSlice';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function DependencyTreeTile() {
  const dependencyTree = useSelector(selectDependencyTreeSubset);
  const applicationInfo = useSelector(selectApplicationInfo);
  const componentDetails = useSelector(selectComponentDetails);
  const { dependencyTreeEnabled } = useSelector(selectRouterCurrentParams);

  if (!dependencyTreeEnabled || !dependencyTree) {
    return null;
  }

  return (
    <NxTile className="component-details-dependency-tree-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <h2 className="nx-h2">Dependency Tree</h2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <DependencyTree
          items={dependencyTree}
          hashToMatch={componentDetails.hash}
          rootName={applicationInfo?.applicationName}
          treePathToggleAction={actions.toggleIsOpenAtTreePathAction}
        />
      </NxTile.Content>
    </NxTile>
  );
}
