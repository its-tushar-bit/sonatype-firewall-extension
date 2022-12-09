/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';
import { violationPropTypes } from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTableRow';

export default function FirewallPolicyViolationsWaiverIndicators({ violation }) {
  const { waived, applicableWaivers = [] } = violation;
  const numberOfWaivers = applicableWaivers.length;

  return (
    <Fragment>
      {numberOfWaivers > 0 && (
        <ActiveWaiversIndicator activeWaiverCount={numberOfWaivers} waived={waived} showUnapplied />
      )}
    </Fragment>
  );
}

FirewallPolicyViolationsWaiverIndicators.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
};
