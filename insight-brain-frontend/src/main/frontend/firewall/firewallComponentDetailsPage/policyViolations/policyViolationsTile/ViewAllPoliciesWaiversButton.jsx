/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton } from '@sonatype/react-shared-components';

export default function ViewAllPoliciesWaiversButton(props) {
  const { setShowComponentWaiversPopover } = props;

  return (
    <NxButton
      id="firewall-details-view-waivers"
      variant="tertiary"
      onClick={() => setShowComponentWaiversPopover(true)}
    >
      <span>View Existing Waivers</span>
    </NxButton>
  );
}

ViewAllPoliciesWaiversButton.propTypes = {
  setShowComponentWaiversPopover: PropTypes.func.isRequired,
};
