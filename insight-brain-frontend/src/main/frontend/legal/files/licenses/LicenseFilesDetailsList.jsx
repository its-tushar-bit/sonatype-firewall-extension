/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { componentPropType } from '../../advancedLegalPropTypes';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

export default function LicenseFilesDetailsList(props) {
  const {
    component,
    licenseIndex,
    ownerType,
    ownerId,
    stageTypeId,
    hash,
    componentIdentifier,
    loading,
    error,
    $state,
  } = props;

  const adjustedIndex = parseInt(licenseIndex) || 0;

  const listLinkClass = (index) => classnames('nx-list__link', { selected: index === adjustedIndex });

  const attributionStatus = (item) =>
    item.status === 'enabled' ? 'Included in attribution report' : 'Excluded from the report';

  const licenseDetailsTargetState = () => {
    if (stageTypeId) {
      return 'legal.stageTypeComponentLicenseFilesDetails.licenseFilesDetails';
    }
    return hash
      ? 'legal.componentLicenseFilesDetails.licenseFilesDetails'
      : 'legal.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails';
  };

  const licenseRef = React.useRef(new Map());

  const listItems =
    component && component.licenseLegalData
      ? component.licenseLegalData.licenseFiles.map((item, index) => (
          <li key={index} className="nx-list__item nx-list__item--link">
            <a
              href={$state.href(licenseDetailsTargetState(), {
                ownerType,
                ownerId,
                hash,
                componentIdentifier,
                stageTypeId,
                licenseIndex: index,
              })}
              className={listLinkClass(index)}
            >
              <div
                className="nx-list__text nx-truncate-ellipsis"
                ref={(element) => licenseRef.current.set(index, element)}
              >
                {item.relPath ? item.relPath : 'Custom License'}
              </div>
              <div className="nx-list__subtext">{attributionStatus(item)}</div>
            </a>
          </li>
        ))
      : null;

  useEffect(() => {
    if (adjustedIndex && licenseRef.current && licenseRef.current.get(adjustedIndex)) {
      licenseRef.current.get(adjustedIndex).scrollIntoView({
        behavior: 'auto',
        block: 'center',
      });
    }
  });

  return loading || error ? null : (
    <aside className="nx-scrollable nx-viewport-sized__scrollable">
      <ul className="nx-list nx-list--clickable">{listItems}</ul>
    </aside>
  );
}

LicenseFilesDetailsList.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  stageTypeId: PropTypes.string,
  hash: PropTypes.string,
  componentIdentifier: PropTypes.string,
  $state: PropTypes.object.isRequired,
  licenseIndex: PropTypes.string,
};
