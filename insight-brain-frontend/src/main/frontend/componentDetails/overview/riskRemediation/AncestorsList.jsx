/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';

import { NxTextLink } from '@sonatype/react-shared-components';
import { AncestorPropTypes } from '../overviewTypes';
import { DependencyTypeTag } from 'MainRoot/react/tag';

export const AncestorsList = ({ ancestors, ancestorOnClick, itemsToShow, expanded, toggleAncestorsList }) => {
  const ancestorsElements = !expanded && ancestors.length > itemsToShow ? ancestors.slice(0, itemsToShow) : ancestors;

  return (
    <Fragment>
      <ul className="nx-list">
        {ancestorsElements.map(({ hash, derivedComponentName, innerSource }) => (
          <li className="nx-list__item" key={hash}>
            <span>
              <NxTextLink
                onClick={() => {
                  ancestorOnClick(hash);
                }}
              >
                {derivedComponentName}
              </NxTextLink>
              {innerSource && <DependencyTypeTag type="innerSource" />}
            </span>
          </li>
        ))}
      </ul>
      {ancestors.length > itemsToShow && (
        <NxTextLink className="iq-toggle-list" onClick={toggleAncestorsList}>
          {expanded ? 'Show less' : 'Show more'}
        </NxTextLink>
      )}
    </Fragment>
  );
};

AncestorsList.propTypes = {
  ancestors: PropTypes.arrayOf(AncestorPropTypes).isRequired,
  ancestorOnClick: PropTypes.func.isRequired,
  toggleAncestorsList: PropTypes.func.isRequired,
  itemsToShow: PropTypes.number.isRequired,
  expanded: PropTypes.bool.isRequired,
};
