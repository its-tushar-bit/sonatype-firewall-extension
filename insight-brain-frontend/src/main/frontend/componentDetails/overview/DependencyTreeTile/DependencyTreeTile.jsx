/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxInfoAlert, NxTile } from '@sonatype/react-shared-components';
import {
  selectApplicationInfo,
  selectDependencyTreeSubset,
  selectComponentDetails,
  selectIsLabelsLoading,
} from 'MainRoot/componentDetails/componentDetailsSelectors';
import { selectDependencyTreeIsOldReport } from 'MainRoot/applicationReport/applicationReportSelectors';
import DependencyTree from 'MainRoot/DependencyTree/DependencyTree';
import { actions } from 'MainRoot/componentDetails/componentDetailsSlice';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function DependencyTreeTile() {
  const dependencyTree = useSelector(selectDependencyTreeSubset);
  const applicationInfo = useSelector(selectApplicationInfo);
  const componentDetails = useSelector(selectComponentDetails);
  const isLoading = useSelector(selectIsLabelsLoading);
  const isOldReport = useSelector(selectDependencyTreeIsOldReport);

  if (isLoading) {
    return null;
  }

  const DependencyTreeWarning = (
    <NxInfoAlert className="component-details-dependency-tree-tile__unavailable-tree-alert">
      {isOldReport
        ? 'Dependency Tree not available for this report. Please re-scan the application.'
        : 'Dependency tree not available'}
    </NxInfoAlert>
  );

  const DependencyTreeSubset = (
    <DependencyTree
      items={dependencyTree}
      hashToMatch={componentDetails.hash}
      rootName={applicationInfo?.applicationName}
      treePathToggleAction={actions.toggleIsOpenAtTreePathAction}
    />
  );

  return (
    <NxTile className="component-details-dependency-tree-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <h2 className="nx-h2">Dependency Tree</h2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>{isNilOrEmpty(dependencyTree) ? DependencyTreeWarning : DependencyTreeSubset}</NxTile.Content>
    </NxTile>
  );
}
