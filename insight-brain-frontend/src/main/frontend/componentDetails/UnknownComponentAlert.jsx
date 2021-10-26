/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxWarningAlert, NxButton } from '@sonatype/react-shared-components';

export default function UnknownComponentAlert({ onClaimClick }) {
  return (
    <NxWarningAlert className="iq-component-details-unknown-component-alert">
      The component is unknown.
      <NxButton
        id="iq-component-details-unknown-component-claim"
        onClick={onClaimClick}
        variant="secondary"
        title="Claim Component"
      >
        Claim Component
      </NxButton>
      <NxButton onClick={() => {}} variant="primary" title="Add Proprietary Component Matchers">
        Add Proprietary Component Matchers
      </NxButton>
    </NxWarningAlert>
  );
}

UnknownComponentAlert.propTypes = {
  onClaimClick: PropTypes.func.isRequired,
};
