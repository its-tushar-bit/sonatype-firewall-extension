/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { componentCopyrightDetailsPropType, componentPropType } from '../advancedLegalPropTypes';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

export default function CopyrightList(props) {
  const {
    loading,
    error,
    component,
    copyrightIndex,
    ownerType,
    ownerId,
    hash,
    componentIdentifier,
    stageTypeId,
    componentCopyrightDetails,
    $state,
  } = props;

  const copyrightRef = React.useRef(new Map());

  const selectedCopyright = parseInt(copyrightIndex);

  const plural = (count, name) => (count > 1 ? `${count} ${name}s` : `1 ${name}`);

  const listLinkClass = (index) =>
    classnames('nx-list__link', {
      selected: index === selectedCopyright,
    });

  const getCopyrightFileCount = (itemHash) => {
    const count = componentCopyrightDetails.copyrightFileCounts[itemHash] || -1;
    return count > 0 ? `Found in ${plural(count, 'file')}` : 'Not found in source files';
  };

  const attributionStatus = (item) =>
    item.status === 'enabled' ? 'Included in attribution report' : 'Excluded from attribution report';

  const copyrightSource = (item) =>
    item.originalContentHash ? getCopyrightFileCount(item.originalContentHash) : 'Manually added';

  const copyrightDetailsTargetState = () => {
    if (stageTypeId) {
      return 'legal.stageTypeComponentCopyrightDetails.copyrightDetails';
    }
    return hash
      ? 'legal.componentCopyrightDetails.copyrightDetails'
      : 'legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails';
  };

  const listItems =
    component && component.licenseLegalData
      ? component.licenseLegalData.copyrights.map((item, index) => (
          <li key={index} className="nx-list__item nx-list__item--link">
            <a
              href={$state.href(copyrightDetailsTargetState(), {
                ownerType,
                ownerId,
                hash,
                componentIdentifier,
                stageTypeId,
                copyrightIndex: index,
              })}
              className={listLinkClass(index)}
            >
              <div
                className="nx-list__text nx-truncate-ellipsis"
                ref={(element) => copyrightRef.current.set(index, element)}
              >
                {item.content}
              </div>
              <div className="nx-list__subtext">
                <p className="copyright-detail-p">{attributionStatus(item)}</p>
                <p className="copyright-detail-p">{copyrightSource(item)}</p>
              </div>
            </a>
          </li>
        ))
      : '';

  useEffect(() => {
    if (selectedCopyright && copyrightRef.current && copyrightRef.current.get(selectedCopyright)) {
      copyrightRef.current.get(selectedCopyright).scrollIntoView({
        behavior: 'auto',
        block: 'center',
      });
    }
  });

  // If we're loading data or in error state than the rendering will be handled by CopyrightDetailsHeader
  // component and this component should not be rendered
  return loading || error ? null : (
    <aside className="nx-scrollable nx-viewport-sized__scrollable">
      <ul className="nx-list nx-list--clickable">{listItems}</ul>
    </aside>
  );
}

CopyrightList.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  componentCopyrightDetails: componentCopyrightDetailsPropType,
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  componentIdentifier: PropTypes.string,
  stageTypeId: PropTypes.string,
  $state: PropTypes.object.isRequired,
  copyrightIndex: PropTypes.string.isRequired,
};
