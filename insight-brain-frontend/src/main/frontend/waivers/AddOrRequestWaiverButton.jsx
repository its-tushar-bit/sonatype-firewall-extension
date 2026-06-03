/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxStatefulSegmentedButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';

/**
 * Renders the waiver action button for a policy violation.
 *
 * For Firewall (isFirewallOrRepository=true):
 *  - hasPermissionForAppWaivers (manage) → segmented "Add Waiver / Request Waiver"
 *  - hasFirewallOnlyCreatePermission (create-only) → plain "Request Waiver"
 *  - neither → nothing
 *
 * For Lifecycle / IQ (isFirewallOrRepository=false):
 *  - hasPermissionForAppWaivers + isWaiverRequestWorkflowEnabled → segmented button
 *  - hasPermissionForAppWaivers only → plain "Add Waiver"
 *  - isWaiverRequestWorkflowEnabled only → plain "Request Waiver"
 */
export default function AddOrRequestWaiverButton({
  variant = 'primary',
  hasPermissionForAppWaivers = false,
  hasFirewallOnlyCreatePermission = false,
  isFirewallOrRepository = false,
  isWaiverRequestWorkflowEnabled = true,
  isRequestWaiverGated = false,
  onClickAddWaiver,
  onClickRequestWaiver,
}) {
  const requestWaiverContent = (
    <>
      Request Waiver
      {isRequestWaiverGated && (
        <>
          {' '}
          <NxFontAwesomeIcon icon={faLock} />
        </>
      )}
    </>
  );

  // Firewall-specific rendering
  if (isFirewallOrRepository) {
    if (hasPermissionForAppWaivers) {
      if (!isWaiverRequestWorkflowEnabled) {
        // manage permission but no request workflow: plain "Add Waiver"
        return (
          <NxButton type="button" variant={variant} id="violation-page-add-waiver" onClick={onClickAddWaiver}>
            Add Waiver
          </NxButton>
        );
      }
      // manage permission + request workflow: segmented "Add Waiver" (primary) + "Request Waiver" (dropdown)
      return (
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
      );
    }
    if (hasFirewallOnlyCreatePermission) {
      // create-only permission: plain "Request Waiver" button
      return (
        <NxTooltip title={isRequestWaiverGated ? 'Enterprise Feature' : ''}>
          <NxButton type="button" variant={variant} id="violation-page-request-waiver" onClick={onClickRequestWaiver}>
            {requestWaiverContent}
          </NxButton>
        </NxTooltip>
      );
    }
    return null;
  }

  // Lifecycle / IQ rendering (original logic)
  return hasPermissionForAppWaivers ? (
    !isWaiverRequestWorkflowEnabled ? (
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
  ) : isWaiverRequestWorkflowEnabled ? (
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
  /** Firewall-only: user has create (but not manage) waiver-request privilege */
  hasFirewallOnlyCreatePermission: PropTypes.bool,
  isFirewallOrRepository: PropTypes.bool,
  isWaiverRequestWorkflowEnabled: PropTypes.bool,
  isRequestWaiverGated: PropTypes.bool,
  onClickRequestWaiver: PropTypes.func.isRequired,
  onClickAddWaiver: PropTypes.func.isRequired,
};
