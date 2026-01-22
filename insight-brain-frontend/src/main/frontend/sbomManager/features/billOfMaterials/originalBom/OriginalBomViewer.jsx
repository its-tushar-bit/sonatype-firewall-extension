/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadWrapper, NxInfoAlert, NxH2, NxTextLink, NxTree, NxTile } from '@sonatype/react-shared-components';

import { actions } from './originalBomViewerSlice';
import {
  selectLoading,
  selectError,
  selectTreeData,
  selectOpenNodes,
  selectNodeChildren,
  selectVisibleCounts,
} from './originalBomViewerSelectors';
import TreeNodeItems from './components/TreeNodeItems';
import { HELP_URL, BATCH_SIZE } from './utils/constants';

import './OriginalBomViewer.scss';

export default function OriginalBomViewer({ internalAppId, sbomVersion }) {
  const dispatch = useDispatch();

  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const treeData = useSelector(selectTreeData);
  const openNodes = useSelector(selectOpenNodes);
  const nodeChildren = useSelector(selectNodeChildren);
  const visibleCounts = useSelector(selectVisibleCounts);

  const loadOriginalBom = useCallback(() => {
    if (internalAppId && sbomVersion) {
      dispatch(actions.fetchOriginalBom({ internalAppId, sbomVersion }));
    }
  }, [dispatch, internalAppId, sbomVersion]);

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

        <NxInfoAlert className="iq-original-bom-viewer__info">
          This view displays complete original bill of material data for reference only. To learn how to update this
          information, see{' '}
          <NxTextLink href={HELP_URL} external>
            help and documentation
          </NxTextLink>
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
};
