/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

export default function DashboardMask() {
  return (
    <div className="form-mask iq-dashboard-form-mask">
      Please apply or revert filter to see results
    </div>
  );
}

DashboardMask.propTypes = {
  className: PropTypes.string
};
