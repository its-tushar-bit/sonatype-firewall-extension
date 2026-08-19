/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';

import './EnterpriseModeSwitch.scss';

/**
 * Default/Custom mode switch component for enterprise tier gating (Pattern A from UX spec).
 * Placed in page header, right side.
 *
 * - Default mode: shows read-only/current view
 * - Custom mode (with lock icon): shows enterprise feature preview
 *
 * Only rendered for Pro users (when entitlement is absent).
 * Enterprise users don't see this component at all.
 *
 */
export default function EnterpriseModeSwitch({ isCustomMode, onToggleMode }) {
  return (
    <div className="iq-enterprise-mode-switch">
      <NxButton
        variant={!isCustomMode ? 'primary' : 'secondary'}
        onClick={() => isCustomMode && onToggleMode()}
        className="iq-enterprise-mode-switch__default-btn"
      >
        Default
      </NxButton>
      <NxTooltip title="Enterprise Feature">
        <NxButton
          variant={isCustomMode ? 'primary' : 'secondary'}
          onClick={() => !isCustomMode && onToggleMode()}
          className="iq-enterprise-mode-switch__custom-btn"
        >
          Custom <NxFontAwesomeIcon icon={faLock} />
        </NxButton>
      </NxTooltip>
    </div>
  );
}

EnterpriseModeSwitch.propTypes = {
  isCustomMode: PropTypes.bool.isRequired,
  onToggleMode: PropTypes.func.isRequired,
};
