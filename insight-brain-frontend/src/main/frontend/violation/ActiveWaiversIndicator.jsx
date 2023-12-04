/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

export default function ActiveWaiversIndicator({ activeWaiverCount = 0, waived, showUnapplied, isFromAggregatedView }) {
  const hasActiveWaivers = activeWaiverCount > 0;
  const showUnappliedWaiverIndicator = hasActiveWaivers && showUnapplied && waived === false;
  const showActiveWaiverIndicator = hasActiveWaivers && !showUnappliedWaiverIndicator;
  const containerClass = classnames('iq-waiver-indicator', {
    'iq-waiver-indicator--inactive': !hasActiveWaivers,
    'iq-waiver-indicator--active': showActiveWaiverIndicator,
  });

  const iconClass = classnames('iq-waiver-indicator__counter', {
    'iq-waiver-indicator__counter--inactive': !hasActiveWaivers,
  });
  let indicatorText;
  if (isFromAggregatedView) {
    indicatorText = activeWaiverCount === 1 ? 'Waived Violation' : 'Waived Violations';
  } else {
    indicatorText = activeWaiverCount === 1 ? 'Active Waiver' : 'Active Waivers';
  }

  return (
    <div className={containerClass}>
      {showUnappliedWaiverIndicator ? (
        <NxTooltip title="Waiver will be applied after the next report evaluation">
          <div>
            <NxFontAwesomeIcon icon={faInfoCircle} />
            <span>Unapplied Waiver</span>
          </div>
        </NxTooltip>
      ) : (
        <Fragment>
          <span className={iconClass}>{activeWaiverCount}</span>
          <span>{indicatorText}</span>
        </Fragment>
      )}
    </div>
  );
}

ActiveWaiversIndicator.propTypes = {
  activeWaiverCount: PropTypes.number.isRequired,
  waived: PropTypes.bool,
  showUnapplied: PropTypes.bool,
  isFromAggregatedView: PropTypes.bool,
};
