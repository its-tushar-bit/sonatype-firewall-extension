/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxInfoAlert } from '@sonatype/react-shared-components';

export default function DashboardMask() {
  return (
    <div className="form-mask iq-dashboard-form-mask">
      <NxInfoAlert>Please apply or revert filter to see results.</NxInfoAlert>
    </div>
  );
}

DashboardMask.propTypes = {
  className: PropTypes.string,
};
