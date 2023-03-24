/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import StatusIndicatorIcon from 'MainRoot/react/statusIndicatorIcon/StatusIndicatorIcon';
import * as PropTypes from 'prop-types';

export default function UpgradeAvailableIndicator({ isAbbreviated }) {
  if (isAbbreviated) {
    return (
      <span className="iq-upgrade-available-indicator iq-upgrade-available-indicator--abbreviated">
        <span className="iq-upgrade-available-indicator__text">Available</span>
      </span>
    );
  }

  return (
    <span className="iq-upgrade-available-indicator">
      <StatusIndicatorIcon status={true}></StatusIndicatorIcon>
      <span className="iq-upgrade-available-indicator__text">Upgrade Available</span>
    </span>
  );
}

UpgradeAvailableIndicator.propTypes = {
  isAbbreviated: PropTypes.bool.isRequired,
};
