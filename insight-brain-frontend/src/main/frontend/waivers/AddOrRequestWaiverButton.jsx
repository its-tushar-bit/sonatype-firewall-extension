/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxStatefulSegmentedButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';

export default function AddOrRequestWaiverButton({
  variant = 'primary',
  hasPermissionForAppWaivers = false,
  isFirewallOrRepository = false,
  isWaiverRequestWorkflowEnabled = true,
  isRequestWaiverGated = false,
  onClickAddWaiver,
  onClickRequestWaiver,
}) {
  const requestWaiverContent = (
    <>
      Request Waiver
      {isRequestWaiverGated && <>{' '}<NxFontAwesomeIcon icon={faLock} /></>}
    </>
  );

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
        <NxTooltip title={isRequestWaiverGated ? 'Enterprise Feature' : ''}>
          <button
            type="button"
            className="nx-dropdown-button"
            id="violation-page-request-waiver"
            onClick={onClickRequestWaiver}
          >
            {requestWaiverContent}
          </button>
        </NxTooltip>
      </NxStatefulSegmentedButton>
    )
  ) : !isFirewallOrRepository && isWaiverRequestWorkflowEnabled ? (
    <NxTooltip title={isRequestWaiverGated ? 'Enterprise Feature' : ''}>
      <NxButton type="button" variant={variant} id="violation-page-request-waiver" onClick={onClickRequestWaiver}>
        {requestWaiverContent}
      </NxButton>
    </NxTooltip>
  ) : null;
}

AddOrRequestWaiverButton.propTypes = {
  variant: PropTypes.string,
  hasPermissionForAppWaivers: PropTypes.bool,
  isFirewallOrRepository: PropTypes.bool,
  isWaiverRequestWorkflowEnabled: PropTypes.bool,
  isRequestWaiverGated: PropTypes.bool,
  onClickRequestWaiver: PropTypes.func.isRequired,
  onClickAddWaiver: PropTypes.func.isRequired,
};
