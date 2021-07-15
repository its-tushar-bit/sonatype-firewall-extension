/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function ActiveWaiversIndicator({ noOfWaivers = 0 }) {
  const noActiveWaivers = noOfWaivers === 0;
  const containerClass = classnames('iq-waiver-indicator', {
    'iq-waiver-indicator--inactive': noActiveWaivers,
  });

  const iconClass = classnames('iq-waiver-indicator__counter', {
    'iq-waiver-indicator__counter--inactive': noActiveWaivers,
  });
  const indicatorText = noOfWaivers === 1 ? 'Active Waiver' : 'Active Waivers';

  return (
    <div className={containerClass}>
      <span className={iconClass}>{noOfWaivers}</span>
      <span>{indicatorText}</span>
    </div>
  );
}

ActiveWaiversIndicator.propTypes = {
  noOfWaivers: PropTypes.number.isRequired,
};
