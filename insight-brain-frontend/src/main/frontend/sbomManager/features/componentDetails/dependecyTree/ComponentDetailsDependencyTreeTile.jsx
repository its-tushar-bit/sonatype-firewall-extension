/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useSelector } from 'react-redux';
import { NxInfoAlert, NxTile } from '@sonatype/react-shared-components';
import {
  selectApplicationName,
  selectComponentDependencyTreeSubset,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';

import DependencyTree from 'MainRoot/DependencyTree/DependencyTree';
import { actions } from 'MainRoot/componentDetails/componentDetailsSlice';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import PropTypes from 'prop-types';

export default function ComponentDetailsDependencyTreeTile(props) {
  const { componentDetails } = props;
  const componentDependencyTreeData = useSelector(selectComponentDependencyTreeSubset);
  const applicationName = useSelector(selectApplicationName);

  if (isNilOrEmpty(componentDetails)) {
    return null;
  }

  const DependencyTreeWarning = (
    <NxInfoAlert className="sbom-component-details-dependency-tree-tile__unavailable-tree-alert">
      Dependency tree not available
    </NxInfoAlert>
  );

  const DependencyTreeSubset = (
    <DependencyTree
      items={componentDependencyTreeData}
      hashToMatch={componentDetails.hash}
      rootName={applicationName}
      treePathToggleAction={actions.toggleIsOpenAtTreePathAction}
    />
  );

  return (
    <NxTile className="sbom-component-details-dependency-tree-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <h2 className="nx-h2">Dependency Tree</h2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        {isNilOrEmpty(componentDependencyTreeData) ? DependencyTreeWarning : DependencyTreeSubset}
      </NxTile.Content>
    </NxTile>
  );
}

ComponentDetailsDependencyTreeTile.propTypes = {
  componentDetails: PropTypes.object,
};
