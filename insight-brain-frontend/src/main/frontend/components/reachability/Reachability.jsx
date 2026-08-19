/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';

const getText = (reachable) => {
  if (reachable == null) {
    return '-';
  }

  if (reachable) {
    return 'Reachable';
  }

  return 'Not Reachable';
};

/**
 * Maps a reachability status string to a boolean value.
 *
 * @param {string} status - The reachability status. Expected values are:
 *   - `'REACHABLE'`: Returns `true`.
 *   - `'NON_REACHABLE'`: Returns `false`.
 *   - Any other value: Returns `null`.
 * @returns {boolean|null} - `true` if the status is `'REACHABLE'`, `false` if the status is `'NON_REACHABLE'`,
 * or `null` for any other value.
 */
const mapReachabilityStatusToBoolean = (status) => {
  if (status === 'REACHABLE') {
    return true;
  } else if (status === 'NON_REACHABLE') {
    return false;
  } else {
    return null;
  }
};

export default function Reachability({ reachable }) {
  const reachableBool = typeof reachable === 'string' ? mapReachabilityStatusToBoolean(reachable) : reachable;

  return (
    <span
      className={classnames('iq-reachability', {
        'iq-reachability__reachable': reachableBool,
      })}
    >
      {getText(reachableBool)}
    </span>
  );
}

Reachability.propTypes = {
  reachable: PropTypes.oneOfType([PropTypes.string, PropTypes.bool]),
};
