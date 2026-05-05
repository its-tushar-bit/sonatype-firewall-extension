/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxReadOnly, NxH2 } from '@sonatype/react-shared-components';
import { backendToRscColorMap } from '../utility/util';
import './_ComponentLabelsReadOnlyView.scss';

export default function ComponentLabelsReadOnlyView({ label }) {
  if (!label) {
    return null;
  }

  const renderDetailsSection = () => {
    // Handle both string and RSC field object formats
    const labelName =
      typeof label.label === 'string' ? label.label || '--' : label.label?.trimmedValue || label.label?.value || '--';
    const labelDescription =
      typeof label.description === 'string'
        ? label.description || '--'
        : label.description?.trimmedValue || label.description?.value || '--';
    const labelColor = label.color || '';
    const rscColor = backendToRscColorMap[labelColor];

    // Capitalize first letter to match NxColorPicker display format (e.g., "Yellow" not "yellow")
    const colorDisplayName = rscColor ? rscColor.charAt(0).toUpperCase() + rscColor.slice(1) : '';

    return (
      <div className="iq-component-labels-readonly-view__section">
        <NxH2>Label Details</NxH2>
        <NxReadOnly>
          <NxReadOnly.Label>Label Name</NxReadOnly.Label>
          <NxReadOnly.Data data-testid="label-name">{labelName}</NxReadOnly.Data>
        </NxReadOnly>
        <NxReadOnly>
          <NxReadOnly.Label>Description</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-component-labels-readonly-view__description" data-testid="label-description">
            {labelDescription}
          </NxReadOnly.Data>
        </NxReadOnly>
        {rscColor && (
          <NxReadOnly>
            <NxReadOnly.Label>Color</NxReadOnly.Label>
            <NxReadOnly.Data data-testid="label-color">
              <div className="iq-component-labels-readonly-view__color-display">
                <div
                  className={`iq-component-labels-readonly-view__color-swatch nx-selectable-color--${rscColor}`}
                  aria-label={`Color swatch: ${colorDisplayName}`}
                />
                <span className="iq-component-labels-readonly-view__color-label">{colorDisplayName}</span>
              </div>
            </NxReadOnly.Data>
          </NxReadOnly>
        )}
      </div>
    );
  };

  return (
    <div className="iq-component-labels-readonly-view" data-testid="component-labels-readonly-view">
      {renderDetailsSection()}
    </div>
  );
}

ComponentLabelsReadOnlyView.propTypes = {
  label: PropTypes.shape({
    label: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.shape({
        value: PropTypes.string,
        trimmedValue: PropTypes.string,
      }),
    ]),
    description: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.shape({
        value: PropTypes.string,
        trimmedValue: PropTypes.string,
      }),
    ]),
    color: PropTypes.string,
  }),
};
