/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

/**
 * A tag showing either "Fail" in a red, rounded background, or "Warn" in an orange rounded background
 */
export default function PolicyActionTag({ action }) {
  const cls = classnames('iq-policy-action-tag', {
    'iq-policy-action-tag--fail': action === 'fail',
    'iq-policy-action-tag--warn': action === 'warn',
  });

  return action ? <span className={cls}>{action}</span> : null;
}

PolicyActionTag.propTypes = {
  action: PropTypes.oneOf(['fail', 'warn', null]),
};
