/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { useDispatch } from 'react-redux';
import PropTypes from 'prop-types';
import { faTerminal } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxTree, NxThreatIndicator, NxTextLink } from '@sonatype/react-shared-components';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { toggleTreePathAction } from 'MainRoot/applicationReport/applicationReportActions';
import DependencyIndicator from './DependencyIndicator';

const MemoizedTreeNode = React.memo(TreeNode);

function TreeNode({ items }) {
  const dispatch = useDispatch();
  const toggleTreePath = (payload) => dispatch(toggleTreePathAction(payload));

  const goToCDP = (hash) => {
    return dispatch(stateGo('applicationReport.componentDetails', { hash }));
  };

  const renderNode = (nodes) =>
    nodes?.map((item) => {
      return (
        <NxTree.Item
          collapsible={!!item.children}
          isOpen={item.isOpen}
          key={item.hash}
          onToggleCollapse={() => toggleTreePath(item.treePath)}
        >
          <NxTree.ItemLabel>
            <NxThreatIndicator policyThreatLevel={item.policyThreatLevel} />
            {item.isInnerSource && <DependencyIndicator type="inner-source" />}
            <NxTextLink onClick={() => goToCDP(item.hash)}>{item.displayName}</NxTextLink>
          </NxTree.ItemLabel>
          {!!item.children && (
            <NxTree data-testid="tree">
              <MemoizedTreeNode items={item.children} />
            </NxTree>
          )}
        </NxTree.Item>
      );
    });

  return <Fragment>{renderNode(items)}</Fragment>;
}

export default function DependencyTree({ dependencyTree, rootName }) {
  return (
    <NxTree className="nx-tree--no-gutter iq-dependency-tree">
      <NxTree.Item>
        <NxTree.ItemLabel>
          <NxFontAwesomeIcon fixedWidth icon={faTerminal} />
          <span>{rootName}</span>
        </NxTree.ItemLabel>
        <NxTree>
          <MemoizedTreeNode items={dependencyTree} />
        </NxTree>
      </NxTree.Item>
    </NxTree>
  );
}

const treeItemProps = PropTypes.shape({
  children: PropTypes.arrayOf(PropTypes.object),
  isOpen: PropTypes.bool,
  displayName: PropTypes.string,
  treePath: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.number])),
  hash: PropTypes.string,
  policyThreatLevel: PropTypes.number,
  isInnerSource: PropTypes.bool,
});

DependencyTree.propTypes = {
  rootName: PropTypes.string,
  dependencyTree: PropTypes.arrayOf(treeItemProps),
};
