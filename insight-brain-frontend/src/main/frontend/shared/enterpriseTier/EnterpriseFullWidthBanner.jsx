/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTextLink, NxH3 } from '@sonatype/react-shared-components';
import TierTag from 'MainRoot/react/shared/TierTag';
import './EnterpriseFullWidthBanner.scss';

const ENTERPRISE_DEMO_URL = 'https://www.sonatype.com/products/request-demo';

/**
 * Full-width enterprise feature banner for forms and editors.
 *
 * Design specifications:
 * - Full width that bleeds to tile edges using negative margins
 * - Gray background (#F4F4F4) with light gray border (#CECECA)
 * - H3 title with "Enterprise Feature" tag
 * - 6px border radius on all corners
 * - Proper spacing (6x) between banner and content below
 *
 * Usage:
 * 1. Add `iq-hide-form-footer` class to parent NxTile
 * 2. Ensure tile content has no top padding (handled by CSS)
 * 3. Place this component as first child of NxTile.Content or form
 *
 * @param {Object} props
 * @param {string} props.title - Display title for the feature (e.g., "Custom Labels")
 * @param {string} props.featureName - Legacy prop (kept for backwards compatibility, not used)
 * @param {string} props.description - Description text explaining the feature
 * @param {string} [props.className] - Optional additional CSS class
 */
export default function EnterpriseFullWidthBanner({ title, featureName, description, className = '' }) {
  // Use title if provided, otherwise fall back to featureName for backwards compatibility
  const displayTitle = title || featureName;

  return (
    <div className={`iq-enterprise-full-width-banner ${className}`.trim()}>
      <div className="iq-enterprise-full-width-banner__content">
        {displayTitle && (
          <div className="iq-enterprise-full-width-banner__header">
            <NxH3>{displayTitle}</NxH3>
            <TierTag>Enterprise Feature</TierTag>
          </div>
        )}
        <div className="iq-enterprise-full-width-banner__message">
          <div>
            {description}{' '}
            <NxTextLink href={ENTERPRISE_DEMO_URL} external aria-label="Request Demo (opens in new window)">
              Request Demo
            </NxTextLink>
          </div>
        </div>
      </div>
    </div>
  );
}

EnterpriseFullWidthBanner.propTypes = {
  title: PropTypes.string,
  featureName: PropTypes.string,
  description: PropTypes.string.isRequired,
  className: PropTypes.string,
};
