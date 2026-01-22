/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTree, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faFolder, faFolderOpen, faFile } from '@fortawesome/pro-regular-svg-icons';

import LoadMoreSentinel from './LoadMoreSentinel';
import { BATCH_SIZE } from '../utils/constants';
import { getChildCount } from '../utils/jsonTreeUtils';

// Helper to count XML node children
const getXmlChildCount = (xmlNode) => {
  let count = 0;

  // Count attributes
  if (xmlNode.attributes) {
    count += xmlNode.attributes.length;
  }

  // Count element and text children
  for (let i = 0; i < xmlNode.childNodes.length; i++) {
    const child = xmlNode.childNodes[i];
    if (child.nodeType === Node.ELEMENT_NODE) {
      count++;
    } else if (child.nodeType === Node.TEXT_NODE && child.textContent?.trim()) {
      count++;
    }
  }

  return count;
};

export default function TreeNodeItems({
  nodes,
  onToggle,
  openNodes,
  nodeChildren,
  visibleCounts,
  onLoadMore,
  parentId,
}) {
  if (!nodes || nodes.length === 0) return null;

  const visibleCount = visibleCounts?.[parentId] || BATCH_SIZE;
  const visibleNodes = nodes.slice(0, visibleCount);
  const remainingCount = nodes.length - visibleCount;

  return (
    <>
      {visibleNodes.map((node, index) => {
        const isOpen = openNodes[node.id] || false;
        const children = nodeChildren[node.id];
        const isLastVisible = index === visibleNodes.length - 1;

        // Node has children if either: 1) children are already loaded, or 2) node has data that can be expanded
        const hasChildren = (children && children.length > 0) || node.rawData || node.xmlNode;
        const icon = hasChildren ? (isOpen ? faFolderOpen : faFolder) : faFile;

        // Calculate child count: use loaded children if available, otherwise calculate from rawData/xmlNode
        let childCount = 0;
        if (hasChildren) {
          if (children) {
            childCount = children.length;
          } else if (node.rawData) {
            childCount = getChildCount(node.rawData);
          } else if (node.xmlNode) {
            childCount = getXmlChildCount(node.xmlNode);
          }
        }

        return (
          <NxTree.Item
            key={node.id}
            collapsible={hasChildren}
            isOpen={isOpen}
            onToggleCollapse={() => onToggle(node.id, node)}
          >
            <NxTree.ItemLabel>
              <NxFontAwesomeIcon icon={icon} />
              <span className="iq-original-bom-viewer__key">{node.name}</span>
              {hasChildren && <span className="iq-original-bom-viewer__count">{` {${childCount}}`}</span>}
              {node.value !== null && (
                <>
                  <span className="iq-original-bom-viewer__separator">: </span>
                  <span className="iq-original-bom-viewer__value">{node.value}</span>
                </>
              )}
            </NxTree.ItemLabel>
            {hasChildren && isOpen && children && (
              <NxTree>
                <TreeNodeItems
                  nodes={children}
                  onToggle={onToggle}
                  openNodes={openNodes}
                  nodeChildren={nodeChildren}
                  visibleCounts={visibleCounts}
                  onLoadMore={onLoadMore}
                  parentId={node.id}
                />
              </NxTree>
            )}
            {isLastVisible && remainingCount > 0 && parentId && (
              <LoadMoreSentinel onLoadMore={() => onLoadMore(parentId)} remainingCount={remainingCount} />
            )}
          </NxTree.Item>
        );
      })}
    </>
  );
}

const TreeNodeShape = {
  id: PropTypes.string.isRequired,
  name: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  value: PropTypes.string,
  rawData: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  xmlNode: PropTypes.object,
};

TreeNodeItems.propTypes = {
  nodes: PropTypes.arrayOf(PropTypes.shape(TreeNodeShape)),
  onToggle: PropTypes.func.isRequired,
  openNodes: PropTypes.objectOf(PropTypes.bool).isRequired,
  nodeChildren: PropTypes.objectOf(PropTypes.array).isRequired,
  visibleCounts: PropTypes.objectOf(PropTypes.number),
  onLoadMore: PropTypes.func,
  parentId: PropTypes.string,
};
