/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton } from '@sonatype/react-shared-components';

export default function AddOrRequestWaiverButton({
  variant,
  hasPermissionForAppWaivers,
  isFirewallOrRepository,
  onClick,
}) {
  return hasPermissionForAppWaivers ? (
    <NxButton type="button" variant={variant} id="violation-page-add-waiver" onClick={onClick}>
      Add Waiver
    </NxButton>
  ) : !isFirewallOrRepository ? (
    <NxButton type="button" variant={variant} id="violation-page-request-waiver" onClick={onClick}>
      Request Waiver
    </NxButton>
  ) : null;
}

AddOrRequestWaiverButton.defaultProps = {
  variant: 'primary',
  hasPermissionForAppWaivers: false,
  isFirewallOrRepository: false,
};

AddOrRequestWaiverButton.propTypes = {
  variant: PropTypes.string.isRequired,
  hasPermissionForAppWaivers: PropTypes.bool.isRequired,
  isFirewallOrRepository: PropTypes.bool,
  onClick: PropTypes.func.isRequired,
};
