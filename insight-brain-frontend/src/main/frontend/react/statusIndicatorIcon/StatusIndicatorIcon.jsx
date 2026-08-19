/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { faCircle } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function StatusIndicatorIcon({ status }) {
  const iconClassName = classnames('iq-status-indicator-icon', {
    'iq-status-indicator-icon--active': status,
  });

  return <NxFontAwesomeIcon icon={faCircle} className={iconClassName} />;
}

StatusIndicatorIcon.propTypes = {
  status: PropTypes.bool.isRequired,
};
