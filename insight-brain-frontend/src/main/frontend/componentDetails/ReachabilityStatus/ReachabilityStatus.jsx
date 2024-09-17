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
const ReachabilityStatus = ({ reachabilityStatus }) => {
  if (!reachabilityStatus) {
    return null;
  }

  const statusText = reachabilityStatus === 'REACHABLE' ? 'Reachable' : 'Not reachable';
  const statusClass = classnames('iq-policy-violation-row__reachability', {
    'iq-policy-violation-row__reachability--reachable': reachabilityStatus === 'REACHABLE',
  });
  const iconClass = classnames({
    'iq-policy-violation-row__reachability-icon--reachable': reachabilityStatus === 'REACHABLE',
    'iq-policy-violation-row__reachability-icon--non-reachable': reachabilityStatus !== 'REACHABLE',
  });

  return (
    <div className={statusClass}>
      <NxFontAwesomeIcon className={iconClass} icon={faShieldAlt} />
      <span>{statusText}</span>
    </div>
  );
};

ReachabilityStatus.propTypes = {
  reachabilityStatus: PropTypes.string,
};

export default ReachabilityStatus;
