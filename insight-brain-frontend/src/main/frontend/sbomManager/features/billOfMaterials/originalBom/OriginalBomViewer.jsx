/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback, useMemo, useRef } from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import debounce from 'debounce';
import {
  NxLoadWrapper,
  NxInfoAlert,
  NxWarningAlert,
  NxH2,
  NxTextLink,
  NxTree,
  NxTile,
  NxFilterInput,
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
  selectSearchValue,
  selectDebouncedSearchValue,
} from './originalBomViewerSelectors';
import TreeNodeItems from './components/TreeNodeItems';
import { HELP_URL, BATCH_SIZE, SEARCH_DEBOUNCE_TIMEOUT_MS, MAX_SEARCH_LENGTH } from './utils/constants';
import { filterTreeNodes, countMatchingNodes } from './utils/searchUtils';

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
  const searchValue = useSelector(selectSearchValue);
  const debouncedSearchValue = useSelector(selectDebouncedSearchValue);

  const debouncedUpdateSearchRef = useRef(
    debounce((value) => {
      dispatch(actions.setDebouncedSearchValue(value));
    }, SEARCH_DEBOUNCE_TIMEOUT_MS)
  );

  useEffect(() => {
    return () => {
      debouncedUpdateSearchRef.current?.clear?.();
    };
  }, []);

  const handleSearchChange = useCallback(
    (value) => {
      const truncatedValue = value.length > MAX_SEARCH_LENGTH ? value.substring(0, MAX_SEARCH_LENGTH) : value;

      dispatch(actions.setSearchValue(truncatedValue));

      debouncedUpdateSearchRef.current?.clear?.();

      if (truncatedValue === '') {
        dispatch(actions.setDebouncedSearchValue(''));
      } else {
        debouncedUpdateSearchRef.current(truncatedValue);
      }
    },
    [dispatch]
  );

  const filteredTreeData = useMemo(() => filterTreeNodes(treeData, debouncedSearchValue), [
    treeData,
    debouncedSearchValue,
  ]);

  const matchingNodeCount = useMemo(() => countMatchingNodes(filteredTreeData), [filteredTreeData]);

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
        <div className="iq-original-bom-viewer__header" role="search" aria-label="Search SBOM components">
          <NxH2>Original Bill of Material Data</NxH2>
          <NxFilterInput
            searchIcon
            id="original-bom-search"
            value={searchValue}
            onChange={handleSearchChange}
            placeholder="Search"
            aria-label="Search SBOM components and attributes"
            aria-describedby={debouncedSearchValue ? 'search-results-count' : undefined}
            className="iq-original-bom-viewer__search-input"
          />
        </div>

        {debouncedSearchValue && (
          <div id="search-results-count" className="iq-original-bom-viewer__results-count" role="status">
            {matchingNodeCount} result{matchingNodeCount !== 1 ? 's' : ''} found
          </div>
        )}

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
              nodes={filteredTreeData}
              onToggle={toggleNode}
              openNodes={openNodes}
              nodeChildren={nodeChildren}
              visibleCounts={visibleCounts}
              onLoadMore={handleLoadMore}
              parentId="root"
              searchTerm={debouncedSearchValue}
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
