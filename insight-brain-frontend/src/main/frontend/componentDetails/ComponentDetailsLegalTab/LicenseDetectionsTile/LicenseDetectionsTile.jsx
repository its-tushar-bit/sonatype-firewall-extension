/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import LicenseDetections from './LicenseDetections';

export default function LicenseDetectionsTile({
  isLoadingComponentDetails,
  componentDetailsLoadError,
  loadComponentDetails,
  ...licenseDetectionProps
}) {
  return (
    <section className="nx-tile license-detections-tile" id="component-details-legal-license-detections-tile">
      <NxLoadWrapper
        loading={isLoadingComponentDetails}
        error={componentDetailsLoadError}
        retryHandler={loadComponentDetails}
      >
        {() => <LicenseDetections {...licenseDetectionProps} />}
      </NxLoadWrapper>
    </section>
  );
}

LicenseDetectionsTile.propTypes = {
  isLoadingComponentDetails: PropTypes.bool.isRequired,
  componentDetailsLoadError: PropTypes.string,
  loadComponentDetails: PropTypes.func.isRequired,
  ...LicenseDetections.propTypes,
};
