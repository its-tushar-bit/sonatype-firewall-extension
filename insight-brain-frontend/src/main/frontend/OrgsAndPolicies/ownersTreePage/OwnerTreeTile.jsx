/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback } from 'react';
import { useDispatch } from 'react-redux';
import PropTypes from 'prop-types';
import { NxTile, NxH2, NxButton } from '@sonatype/react-shared-components';

import OwnerTree from './OwnerTree';
import { actions } from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function OwnersTreeTile({
  id,
  topParentOrganizationId,
  onExpandAllClick,
  onCollapseAllClick,
  onToggleTreeNode,
  isNodeOpenSelector,
  ...otherProps
}) {
  const dispatch = useDispatch();
  const expandAll = () => dispatch(actions.expandAllTreeNodes());
  const collapseAll = () => dispatch(actions.collapseAllTreeNodes());
  const isNodeOpen = useCallback(
    (state, ownerId) => {
      if (!isNodeOpenSelector) return true;
      // Top parent organization never should be collapsed.
      if (ownerId === topParentOrganizationId) return true;
      return isNodeOpenSelector(state, ownerId);
    },
    [topParentOrganizationId, isNodeOpenSelector]
  );

  const tileTitleId = id ? `${id}-title` : undefined;
  const label = tileTitleId ? undefined : 'Inheritance Hierarchy';

  return (
    <NxTile id={id} aria-label={label} aria-labelledby={tileTitleId} {...otherProps}>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2 id={tileTitleId}>Inheritance Hierarchy</NxH2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderActions>
          <NxButton id="iq-owner-tree__expand-all-button" variant="tertiary" onClick={onExpandAllClick || expandAll}>
            Expand All
          </NxButton>
          <NxButton
            id="iq-owner-tree__collapse-all-button"
            variant="tertiary"
            onClick={onCollapseAllClick || collapseAll}
          >
            Collapse All
          </NxButton>
        </NxTile.HeaderActions>
      </NxTile.Header>
      <NxTile.Content>
        {!isNilOrEmpty(topParentOrganizationId) && (
          <OwnerTree
            ownerId={topParentOrganizationId}
            onToggleTreeNode={onToggleTreeNode}
            isNodeOpenSelector={isNodeOpen}
          />
        )}
      </NxTile.Content>
    </NxTile>
  );
}

OwnersTreeTile.propTypes = {
  id: PropTypes.string,
  topParentOrganizationId: PropTypes.string,
  isNodeOpenSelector: PropTypes.func,
  onToggleTreeNode: PropTypes.func,
  onExpandAllClick: PropTypes.func,
  onCollapseAllClick: PropTypes.func,
};
