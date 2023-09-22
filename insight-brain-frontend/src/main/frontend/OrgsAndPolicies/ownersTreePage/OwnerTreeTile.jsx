/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import { NxTile, NxButton } from '@sonatype/react-shared-components';
import IqStatefulFilterInput from 'MainRoot/react/IqStatefulFilterInput';

import OwnerTree from './OwnerTree';
import { actions } from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { debounce } from 'debounce';
import { selectSearchTerm } from 'MainRoot/OrgsAndPolicies/ownersTreeSelectors';
import { selectOwnersMap } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';

export default function OwnersTreeTile({
  topParentOrganizationId,
  onToggleTreeNode,
  isNodeOpenSelector,
  shouldDisplayRepositories,
  ...otherProps
}) {
  const INPUT_DEBOUNCE_TIME = 500;
  const dispatch = useDispatch();
  const expandAll = () => dispatch(actions.expandAllTreeNodes());
  const collapseAll = () => dispatch(actions.collapseAllTreeNodes());
  const searchTerm = useSelector(selectSearchTerm);
  const ownersMap = useSelector(selectOwnersMap);

  const isNodeOpen = useCallback(
    (state, ownerId) => {
      if (!isNodeOpenSelector) return true;
      // Top parent organization never should be collapsed.
      if (ownerId === topParentOrganizationId) return true;
      return isNodeOpenSelector(state, ownerId);
    },
    [topParentOrganizationId, isNodeOpenSelector]
  );

  const debouncedSetOwnersTreeSearchTerm = useCallback(
    debounce(
      (value) =>
        dispatch(
          actions.setOwnersTreeSearchTerm({
            searchTerm: value,
            topParentOrganizationId,
            ownersMap,
          })
        ),
      INPUT_DEBOUNCE_TIME
    ),
    []
  );

  const tileTitleId = topParentOrganizationId ? `${topParentOrganizationId}-title` : undefined;
  const label = tileTitleId ? tileTitleId : 'Inheritance Hierarchy';

  return (
    <NxTile id={topParentOrganizationId} aria-label={label} aria-labelledby={tileTitleId} {...otherProps}>
      <NxTile.Header>
        <IqStatefulFilterInput
          id="iq-owner-tree-filter-input"
          placeholder="Org or App Name"
          onChange={debouncedSetOwnersTreeSearchTerm}
          defaultValue={searchTerm}
        />
        <NxTile.HeaderActions>
          <NxButton id="iq-owner-tree__expand-all-button" variant="tertiary" onClick={expandAll}>
            Expand All
          </NxButton>
          <NxButton id="iq-owner-tree__collapse-all-button" variant="tertiary" onClick={collapseAll}>
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
            shouldDisplayRepositories={shouldDisplayRepositories}
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
  shouldDisplayRepositories: PropTypes.bool,
};
