/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';

import { NxTextLink } from '@sonatype/react-shared-components';
import { DependencyTypeTag } from 'MainRoot/react/tag';
import { dependencyTreeNodePropType } from 'MainRoot/DependencyTree/DependencyTree';

export const AncestorsList = ({
  dependencyTreeSubset,
  ancestorOnClick,
  itemsToShow,
  expanded,
  toggleAncestorsList,
}) => {
  const ancestorsElements =
    !expanded && dependencyTreeSubset.length > itemsToShow
      ? dependencyTreeSubset.slice(0, itemsToShow)
      : dependencyTreeSubset;

  return (
    <Fragment>
      <ul className="nx-list">
        {ancestorsElements.map(({ hash, displayName, isInnerSource }) => (
          <li className="nx-list__item" key={hash}>
            <span>
              <NxTextLink
                onClick={() => {
                  ancestorOnClick(hash);
                }}
              >
                {displayName}
              </NxTextLink>
              {isInnerSource && <DependencyTypeTag type="innerSource" />}
            </span>
          </li>
        ))}
      </ul>
      {dependencyTreeSubset.length > itemsToShow && (
        <NxTextLink className="iq-toggle-list" onClick={toggleAncestorsList}>
          {expanded ? 'Show less' : 'Show more'}
        </NxTextLink>
      )}
    </Fragment>
  );
};

AncestorsList.propTypes = {
  dependencyTreeSubset: PropTypes.arrayOf(dependencyTreeNodePropType).isRequired,
  ancestorOnClick: PropTypes.func.isRequired,
  toggleAncestorsList: PropTypes.func.isRequired,
  itemsToShow: PropTypes.number.isRequired,
  expanded: PropTypes.bool.isRequired,
};
