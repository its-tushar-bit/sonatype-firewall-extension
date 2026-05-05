/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxReadOnly, NxH2 } from '@sonatype/react-shared-components';
import { backendToRscColorMap } from '../utility/util';
import './_ApplicationCategoryReadOnlyView.scss';

export default function ApplicationCategoryReadOnlyView({ category }) {
  if (!category) {
    return null;
  }

  const renderDetailsSection = () => {
    // Handle both string and RSC field object formats
    const categoryName =
      typeof category.name === 'string' ? category.name || '--' : category.name?.trimmedValue || '--';
    const categoryDescription =
      typeof category.description === 'string'
        ? category.description || '--'
        : category.description?.trimmedValue || '--';
    const categoryColor = category.color || '';
    const rscColor = backendToRscColorMap[categoryColor];

    // Capitalize first letter to match NxColorPicker display format (e.g., "Yellow" not "yellow")
    const colorDisplayName = rscColor ? rscColor.charAt(0).toUpperCase() + rscColor.slice(1) : '';

    return (
      <div className="iq-application-category-readonly-view__section">
        <NxH2>Category Details</NxH2>
        <NxReadOnly>
          <NxReadOnly.Label>Category Name</NxReadOnly.Label>
          <NxReadOnly.Data data-testid="category-name">{categoryName}</NxReadOnly.Data>
        </NxReadOnly>
        <NxReadOnly>
          <NxReadOnly.Label>Brief Description</NxReadOnly.Label>
          <NxReadOnly.Data
            className="iq-application-category-readonly-view__description"
            data-testid="category-description"
          >
            {categoryDescription}
          </NxReadOnly.Data>
        </NxReadOnly>
        {rscColor && (
          <NxReadOnly>
            <NxReadOnly.Label>Color</NxReadOnly.Label>
            <NxReadOnly.Data data-testid="category-color">
              <div className="iq-application-category-readonly-view__color-display">
                <div
                  className={`iq-application-category-readonly-view__color-swatch nx-selectable-color--${rscColor}`}
                  aria-label={`Color swatch: ${colorDisplayName}`}
                />
                <span className="iq-application-category-readonly-view__color-label">{colorDisplayName}</span>
              </div>
            </NxReadOnly.Data>
          </NxReadOnly>
        )}
      </div>
    );
  };

  return (
    <div className="iq-application-category-readonly-view" data-testid="application-category-readonly-view">
      {renderDetailsSection()}
    </div>
  );
}

ApplicationCategoryReadOnlyView.propTypes = {
  category: PropTypes.shape({
    name: PropTypes.oneOfType([PropTypes.string, PropTypes.shape({ trimmedValue: PropTypes.string })]),
    description: PropTypes.oneOfType([PropTypes.string, PropTypes.shape({ trimmedValue: PropTypes.string })]),
    color: PropTypes.string,
  }),
};
