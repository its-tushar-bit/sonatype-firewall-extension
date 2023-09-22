/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxH1 } from '@sonatype/react-shared-components';

import { actions } from 'MainRoot/OrgsAndPolicies/ownersTreeSlice';
import { selectDisplayedOrganization } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import OwnersTreeTile from 'MainRoot/OrgsAndPolicies/ownersTreePage/OwnerTreeTile';
import { selectIsOwnerNodeExpanded, selectShouldDisplayRepositories } from '../ownersTreeSelectors';

export default function InsufficientPermissionOwnerHierarchyTree() {
  const dispatch = useDispatch();
  const toogleTreeNode = (payload) => dispatch(actions.toogleTreeNode(payload));

  const { name, id } = useSelector(selectDisplayedOrganization);
  const shouldDisplayRepositories = useSelector(selectShouldDisplayRepositories);
  const topParentOrganizationId = shouldDisplayRepositories ? 'ROOT_ORGANIZATION_ID' : id;
  const dottedLineClassName =
    shouldDisplayRepositories && id !== 'ROOT_ORGANIZATION_ID' ? 'repositories-dotted-line' : '';

  return (
    <>
      <header className="nx-page-title">
        <NxH1>{name}</NxH1>
        <div className="nx-page-title__description">
          <p className="nx-p">
            View all organizations and applications on which you have permissions. Click on the link for the org or app
            below to access details.
          </p>
        </div>
      </header>
      <OwnersTreeTile
        topParentOrganizationId={topParentOrganizationId}
        isNodeOpenSelector={selectIsOwnerNodeExpanded}
        onToggleTreeNode={toogleTreeNode}
        shouldDisplayRepositories={shouldDisplayRepositories}
        className={dottedLineClassName}
      />
    </>
  );
}
