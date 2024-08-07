/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';

import { NxList, NxTextLink } from '@sonatype/react-shared-components';
import { DependencyTypeTag } from 'MainRoot/react/tag';
import { dependencyTreeNodePropType } from 'MainRoot/DependencyTree/DependencyTree';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { useSelector } from 'react-redux';
import {
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageContainerName,
} from 'MainRoot/reduxUiRouter/routerSelectors';

export const AncestorsList = ({ dependencyTreeSubset, itemsToShow, expanded, toggleAncestorsList }) => {
  const uiRouterState = useRouterState();
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);

  const COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME = isPrioritiesPageContainer
    ? `${prioritiesPageContainerName}.componentDetails.overview`
    : 'applicationReport.componentDetails.overview';

  const ancestorsElements =
    !expanded && dependencyTreeSubset.length > itemsToShow
      ? dependencyTreeSubset.slice(0, itemsToShow)
      : dependencyTreeSubset;

  return (
    <Fragment>
      <NxList>
        {ancestorsElements.map(({ hash, displayName, isInnerSource }) => {
          return (
            <NxList.Item key={hash}>
              <span>
                <NxTextLink href={uiRouterState.href(COMPONENT_DETAILS_OVERVIEW_ROUTE_NAME, { hash })}>
                  {displayName}
                </NxTextLink>
                {isInnerSource && <DependencyTypeTag type="innerSource" />}
              </span>
            </NxList.Item>
          );
        })}
      </NxList>
      {dependencyTreeSubset.length > itemsToShow && (
        <button className="iq-toggle-list btn-link" onClick={toggleAncestorsList}>
          {expanded ? 'Show less' : 'Show more'}
        </button>
      )}
    </Fragment>
  );
};

AncestorsList.propTypes = {
  dependencyTreeSubset: PropTypes.arrayOf(dependencyTreeNodePropType).isRequired,
  toggleAncestorsList: PropTypes.func.isRequired,
  itemsToShow: PropTypes.number.isRequired,
  expanded: PropTypes.bool.isRequired,
};
