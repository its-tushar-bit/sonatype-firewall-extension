/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import { equals } from 'ramda';
import { faTerminal } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxTree, NxThreatIndicator, NxTextLink } from '@sonatype/react-shared-components';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import DependencyIndicator from './DependencyIndicator';
import { renderDisplayName } from './dependencyTreeUtil';
import {
  selectIsPrioritiesPageContainer,
  selectIsSbomManager,
  selectPrioritiesPageContainerName,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';

const MemoizedTreeNode = React.memo(TreeNode);

function TreeNode({ items, treePathToggleAction, hashToMatch, searchTerm, isNonClickable }) {
  const dispatch = useDispatch();

  const isSbomManager = useSelector(selectIsSbomManager);
  const routerParams = useSelector(selectRouterCurrentParams);
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);

  const matchesHash = equals(hashToMatch);
  const dispatchToggleTreeAtPath = (payload) => dispatch(treePathToggleAction(payload));
  const goToCDP = (hash) => {
    if (isSbomManager) {
      const { applicationPublicId, sbomVersion } = routerParams;
      return dispatch(stateGo('sbomManager.component', { applicationPublicId, sbomVersion, componentHash: hash }));
    }
    if (isPrioritiesPageContainer) {
      return dispatch(stateGo(`${prioritiesPageContainerName}.componentDetails`, { hash }));
    }
    return dispatch(stateGo('applicationReport.componentDetails', { hash }));
  };

  // the hash alone is not enough.
  // Multi module projects contain duplicate direct dependencies
  const getKey = (item) => item.hash + item.treePath + item.originalTreePath;

  const renderNode = (nodes) =>
    nodes?.map((item) => {
      return (
        <NxTree.Item
          collapsible={!!item.children}
          isOpen={item.isOpen}
          key={getKey(item)}
          onToggleCollapse={() => dispatchToggleTreeAtPath(item.treePath)}
          onActivate={() => goToCDP(item.hash)}
        >
          <NxTree.ItemLabel>
            <NxThreatIndicator className="nx-tree__colored-icon" policyThreatLevel={item.policyThreatLevel} />
            {item.isInnerSource && <DependencyIndicator type="inner-source" />}
            {isNonClickable || matchesHash(item.hash) ? (
              <span className="iq-matched-hash-tree-label">{item.displayName}</span>
            ) : (
              <NxTextLink onClick={() => goToCDP(item.hash)}>
                {renderDisplayName(item.displayName, searchTerm, 'iq-dependency-tree-page__search-match')}
              </NxTextLink>
            )}
          </NxTree.ItemLabel>
          {!!item.children && (
            <NxTree>
              <MemoizedTreeNode
                items={item.children}
                treePathToggleAction={treePathToggleAction}
                hashToMatch={hashToMatch}
                searchTerm={searchTerm}
              />
            </NxTree>
          )}
        </NxTree.Item>
      );
    });

  return <Fragment>{renderNode(items)}</Fragment>;
}

export const dependencyTreeNodePropType = PropTypes.shape({
  children: PropTypes.arrayOf(PropTypes.object),
  isOpen: PropTypes.bool,
  displayName: PropTypes.string,
  treePath: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.number])),
  hash: PropTypes.string,
  policyThreatLevel: PropTypes.number,
  isInnerSource: PropTypes.bool,
});

TreeNode.propTypes = {
  hashToMatch: PropTypes.string,
  searchTerm: PropTypes.string,
  items: PropTypes.arrayOf(dependencyTreeNodePropType),
  treePathToggleAction: PropTypes.func.isRequired,
  isNonClickable: PropTypes.bool,
};

export default function DependencyTree({ rootName, ...rest }) {
  return (
    <NxTree className="nx-tree--no-gutter iq-dependency-tree">
      <NxTree.Item>
        <NxTree.ItemLabel>
          <NxFontAwesomeIcon fixedWidth icon={faTerminal} />
          <span>{rootName}</span>
        </NxTree.ItemLabel>
        <NxTree>
          <MemoizedTreeNode {...rest} />
        </NxTree>
      </NxTree.Item>
    </NxTree>
  );
}

DependencyTree.propTypes = {
  rootName: PropTypes.string,
  ...TreeNode.propTypes,
};
