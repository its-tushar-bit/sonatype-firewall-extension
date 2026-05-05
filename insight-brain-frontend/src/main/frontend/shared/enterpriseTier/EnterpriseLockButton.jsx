/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';

/**
 * Button with lock icon indicating an Enterprise-only feature (Pattern B/C from UX spec).
 * Clicking leads to an enterprise feature preview, not the normal action.
 *
 * Used for: Create Policy, Add Category, Create Label, Create Threat Group,
 * Request Waiver, Bulk Waive buttons.
 *
 */
export default function EnterpriseLockButton({ label, onClick, variant = 'tertiary' }) {
  return (
    <NxTooltip title="Enterprise Feature">
      <NxButton variant={variant} onClick={onClick}>
        {label} <NxFontAwesomeIcon icon={faLock} />
      </NxButton>
    </NxTooltip>
  );
}

EnterpriseLockButton.propTypes = {
  label: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
  variant: PropTypes.string,
};
