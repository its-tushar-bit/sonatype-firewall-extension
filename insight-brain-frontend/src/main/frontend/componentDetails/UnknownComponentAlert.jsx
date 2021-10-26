/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxWarningAlert, NxButton } from '@sonatype/react-shared-components';
import { AddProprietaryComponentMatchersPopoverContainer } from './AddProprietaryComponentMatchersPopover/AddProprietaryComponentMatchersPopoverContainer';

export default function UnknownComponentAlert({ onClaimClick, toggleShowMatchersPopover }) {
  return (
    <Fragment>
      <NxWarningAlert className="iq-component-details-unknown-component-alert">
        <span>The component is unknown.</span>
        <NxButton
          id="iq-component-details-unknown-component-claim"
          onClick={onClaimClick}
          variant="secondary"
          title="Claim Component"
        >
          Claim Component
        </NxButton>
        <NxButton
          onClick={toggleShowMatchersPopover}
          variant="primary"
          title="Add Proprietary Component Matchers"
          id="iq-component-details-add-proprietary-component-matchers-btn"
        >
          Add Proprietary Component Matchers
        </NxButton>
      </NxWarningAlert>
      <AddProprietaryComponentMatchersPopoverContainer />
    </Fragment>
  );
}

UnknownComponentAlert.propTypes = {
  onClaimClick: PropTypes.func.isRequired,
  toggleShowMatchersPopover: PropTypes.func.isRequired,
};
