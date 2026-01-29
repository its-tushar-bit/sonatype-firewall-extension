/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxLoadWrapper,
  NxInfoAlert,
  NxWarningAlert,
  NxH2,
  NxTextLink,
  NxTree,
  NxTile,
} from '@sonatype/react-shared-components';

import { actions } from './originalBomViewerSlice';
import {
  selectLoading,
  selectError,
  selectTreeData,
  selectOpenNodes,
  selectNodeChildren,
  selectVisibleCounts,
  selectComponentNotFound,
} from './originalBomViewerSelectors';
import TreeNodeItems from './components/TreeNodeItems';
import { HELP_URL, BATCH_SIZE } from './utils/constants';

import './OriginalBomViewer.scss';

export default function OriginalBomViewer({ internalAppId, sbomVersion, componentPurl }) {
  const dispatch = useDispatch();

  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const treeData = useSelector(selectTreeData);
  const openNodes = useSelector(selectOpenNodes);
  const nodeChildren = useSelector(selectNodeChildren);
  const visibleCounts = useSelector(selectVisibleCounts);
  const componentNotFound = useSelector(selectComponentNotFound);

  const loadOriginalBom = useCallback(() => {
    if (internalAppId && sbomVersion) {
      dispatch(actions.fetchOriginalBom({ internalAppId, sbomVersion, componentPurl }));
    }
  }, [dispatch, internalAppId, sbomVersion, componentPurl]);

  useEffect(() => {
    loadOriginalBom();
  }, [loadOriginalBom]);

  const toggleNode = (nodeId, node) => {
    dispatch(actions.toggleNode({ nodeId, node }));
  };

  const handleLoadMore = (nodeId) => {
    dispatch(actions.loadMoreChildren({ nodeId, batchSize: BATCH_SIZE }));
  };

  return (
    <div className="iq-original-bom-viewer">
      <NxLoadWrapper loading={loading} error={error} retryHandler={loadOriginalBom}>
        <NxH2>Original Bill of Material Data</NxH2>

        {componentNotFound && (
          <NxWarningAlert className="iq-original-bom-viewer__warning">
            The selected component was not found in the SBOM. Displaying the complete SBOM instead.
          </NxWarningAlert>
        )}

        <NxInfoAlert className="iq-original-bom-viewer__info">
          {componentPurl && !componentNotFound ? (
            'Showing original bill of material data for the selected component only.'
          ) : (
            <>
              This view displays complete original bill of material data for reference only. To learn how to update this
              information, see{' '}
              <NxTextLink href={HELP_URL} external>
                help and documentation
              </NxTextLink>
            </>
          )}
        </NxInfoAlert>

        <NxTile>
          <NxTree className="iq-original-bom-viewer__tree">
            <TreeNodeItems
              nodes={treeData}
              onToggle={toggleNode}
              openNodes={openNodes}
              nodeChildren={nodeChildren}
              visibleCounts={visibleCounts}
              onLoadMore={handleLoadMore}
              parentId="root"
            />
          </NxTree>
        </NxTile>
      </NxLoadWrapper>
    </div>
  );
}

OriginalBomViewer.propTypes = {
  internalAppId: PropTypes.string.isRequired,
  sbomVersion: PropTypes.string.isRequired,
  componentPurl: PropTypes.string,
};
