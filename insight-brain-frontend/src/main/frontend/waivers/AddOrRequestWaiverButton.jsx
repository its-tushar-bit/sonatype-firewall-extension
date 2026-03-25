/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxStatefulSegmentedButton } from '@sonatype/react-shared-components';

export default function AddOrRequestWaiverButton({
  variant = 'primary',
  hasPermissionForAppWaivers = false,
  isFirewallOrRepository = false,
  isWaiverRequestWorkflowEnabled = true,
  onClickAddWaiver,
  onClickRequestWaiver,
}) {
  return hasPermissionForAppWaivers ? (
    isFirewallOrRepository || !isWaiverRequestWorkflowEnabled ? (
      <NxButton type="button" variant={variant} id="violation-page-add-waiver" onClick={onClickAddWaiver}>
        Add Waiver
      </NxButton>
    ) : (
      <NxStatefulSegmentedButton
        type="button"
        variant={variant}
        onClick={onClickAddWaiver}
        buttonContent="Add Waiver"
        id="violation-page-add-waiver"
      >
        <button
          type="button"
          className="nx-dropdown-button"
          id="violation-page-request-waiver"
          onClick={onClickRequestWaiver}
        >
          Request Waiver
        </button>
      </NxStatefulSegmentedButton>
    )
  ) : !isFirewallOrRepository && isWaiverRequestWorkflowEnabled ? (
    <NxButton type="button" variant={variant} id="violation-page-request-waiver" onClick={onClickRequestWaiver}>
      Request Waiver
    </NxButton>
  ) : null;
}

AddOrRequestWaiverButton.propTypes = {
  variant: PropTypes.string,
  hasPermissionForAppWaivers: PropTypes.bool,
  isFirewallOrRepository: PropTypes.bool,
  isWaiverRequestWorkflowEnabled: PropTypes.bool,
  onClickRequestWaiver: PropTypes.func.isRequired,
  onClickAddWaiver: PropTypes.func.isRequired,
};
