/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback } from 'react';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import { NxTextLink, NxTree, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faSitemap, faTerminal } from '@fortawesome/free-solid-svg-icons';

import { selectOwnerById } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { selectFilteredOwners, selectSearchTerm } from 'MainRoot/OrgsAndPolicies/ownersTreeSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

import { renderDisplayName } from 'MainRoot/DependencyTree/dependencyTreeUtil';

const shouldRenderNodeById = (searchTerm, filteredOwners) => (nodeId) =>
  !searchTerm || (searchTerm && filteredOwners.includes(nodeId));

const OwnerTreeNode = ({ ownerId, onToggleTreeNode = () => {}, isNodeOpenSelector = () => true }) => {
  const uiRouterState = useRouterState();
  const { name, organizationIds, applicationIds, publicId: applicationPublicId, id: organizationId, type, synthetic } =
    useSelector((state) => selectOwnerById(state, ownerId)) || {};
  const isOpen = useSelector((state) => isNodeOpenSelector(state, ownerId));
  const searchTerm = useSelector(selectSearchTerm);
  const filteredOwners = useSelector(selectFilteredOwners);
  const shouldRenderNode = shouldRenderNodeById(searchTerm, filteredOwners);

  const hasChildEntities = !!organizationIds?.length || !!applicationIds?.length;
  const items = [...(organizationIds || []), ...(applicationIds || [])];
  const isApplication = type === 'application';
  const href = uiRouterState.href(`management.view.${isApplication ? 'application' : 'organization'}`, {
    ...(isApplication ? { applicationPublicId } : { organizationId }),
  });

  const nodeId = applicationPublicId || organizationId;
  const clickDOMElement = (id) => document.getElementById(id)?.click();
  const toggleTreeNode = useCallback(() => onToggleTreeNode({ ownerId }), [ownerId, onToggleTreeNode]);
  const displayName = renderDisplayName(name, searchTerm, 'iq-owner-tree-page__search-match');

  return (
    <NxTree.Item
      onActivate={() => clickDOMElement(nodeId)}
      collapsible={hasChildEntities}
      isOpen={isOpen}
      onToggleCollapse={toggleTreeNode}
    >
      <NxTree.ItemLabel data-testid="owners-tree-item-label">
        <NxFontAwesomeIcon fixedWidth icon={isApplication ? faTerminal : faSitemap} />
        {synthetic ? (
          <span>{displayName}</span>
        ) : (
          <NxTextLink
            href={href}
            id={nodeId}
            onMouseDown={(e) => {
              e.preventDefault();
            }}
            tabIndex={-1}
          >
            {displayName}
          </NxTextLink>
        )}
      </NxTree.ItemLabel>
      {hasChildEntities && (
        <NxTree>
          {items.map(
            (id) =>
              shouldRenderNode(id) && (
                <MemoizedOwnerTreeNode
                  key={id}
                  ownerId={id}
                  isNodeOpenSelector={isNodeOpenSelector}
                  onToggleTreeNode={onToggleTreeNode}
                  searchTerm={searchTerm}
                />
              )
          )}
        </NxTree>
      )}
    </NxTree.Item>
  );
};

OwnerTreeNode.propTypes = {
  ownerId: PropTypes.string,
  onToggleTreeNode: PropTypes.func,
  isNodeOpenSelector: PropTypes.func,
};

const MemoizedOwnerTreeNode = React.memo(OwnerTreeNode);

export default function OwnerTree({ ownerId, onToggleTreeNode, isNodeOpenSelector }) {
  const searchTerm = useSelector(selectSearchTerm);
  const filteredOwners = useSelector(selectFilteredOwners);
  const shouldRenderNode = shouldRenderNodeById(searchTerm, filteredOwners);

  return ownerId ? (
    shouldRenderNode(ownerId) ? (
      <NxTree className="nx-tree--no-gutter iq-owner-tree">
        <MemoizedOwnerTreeNode
          ownerId={ownerId}
          onToggleTreeNode={onToggleTreeNode}
          isNodeOpenSelector={isNodeOpenSelector}
        />
      </NxTree>
    ) : (
      <p className="iq-dependency-tree__empty" id="iq-owner-tree-empty">
        No matching results
      </p>
    )
  ) : null;
}

OwnerTree.propTypes = {
  ownerId: PropTypes.string,
  onToggleTreeNode: PropTypes.func,
  isNodeOpenSelector: PropTypes.func,
};
