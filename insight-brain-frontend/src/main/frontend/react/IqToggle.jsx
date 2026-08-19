/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxToggle, NxTooltip } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

/**
 * A wrapper component for react NxToggle adding tooltip capabilities.
 */
export default function IqToggle(props) {
  const { toggleLabel, toggleTooltip, ...otherProps } = props;

  return (
    <NxToggle {...otherProps}>
      <NxTooltip title={toggleTooltip}>
        <span>{toggleLabel}</span>
      </NxTooltip>
    </NxToggle>
  );
}

IqToggle.propTypes = {
  ...NxToggle.propTypes,
  toggleLabel: PropTypes.string,
  toggleTooltip: PropTypes.string,
  inputId: PropTypes.string,
  isChecked: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired,
};
