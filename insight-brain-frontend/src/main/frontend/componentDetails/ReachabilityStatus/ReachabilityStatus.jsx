/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faShieldAlt } from '@fortawesome/pro-solid-svg-icons';
import './_reachabilityStatus.scss';
const ReachabilityStatus = ({ reachabilityStatus }) =>
  reachabilityStatus && (
    <div
      className={classnames('iq-policy-violation-row__reachability', {
        'iq-policy-violation-row__reachability--reachable': reachabilityStatus === 'REACHABLE',
      })}
    >
      <NxFontAwesomeIcon
        icon={faShieldAlt}
        className={classnames({
          'iq-policy-violation-row__reachability-icon--reachable': reachabilityStatus === 'REACHABLE',
          'iq-policy-violation-row__reachability-icon--non-reachable': reachabilityStatus !== 'REACHABLE',
        })}
      />{' '}
      <span>{getReachabilityDisplayText(reachabilityStatus)}</span>
    </div>
  );

ReachabilityStatus.propTypes = {
  reachabilityStatus: PropTypes.string,
};

export default ReachabilityStatus;

const getReachabilityDisplayText = (status) => {
  switch (status) {
    case 'NON_REACHABLE':
      return 'Not reachable';
    case 'REACHABLE':
      return 'Reachable';
    default:
      return status;
  }
};
