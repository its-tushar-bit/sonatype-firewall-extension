/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxTile } from '@sonatype/react-shared-components';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function DependencyTreeTile() {
  const { dependencyTreeEnabled } = useSelector(selectRouterCurrentParams);

  if (!dependencyTreeEnabled) {
    return null;
  }

  return (
    <NxTile className="component-details-dependency-tree-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <h2 className="nx-h2">Dependency Tree</h2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
    </NxTile>
  );
}
