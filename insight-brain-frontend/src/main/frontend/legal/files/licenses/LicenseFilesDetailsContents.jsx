/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  availableScopesPropType,
  componentLicenseDetailsPropType,
  componentPropType,
} from '../../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import LicenseFilesDetailsOverview from './LicenseFilesDetailsOverview';

export default function LicenseFilesDetailsContents(props) {
  const { loading, error, availableScopes, componentLicenseFileDetails, component } = props;

  // If we're loading data or in error state than the rendering will be handled by LegalFileDetailsHeader
  // component and this component should not be rendered
  return loading || error ? null : (
    <LicenseFilesDetailsOverview
      availableScopes={availableScopes}
      component={component}
      componentLicenseFileDetails={componentLicenseFileDetails}
    />
  );
}

LicenseFilesDetailsContents.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  component: componentPropType,
  availableScopes: availableScopesPropType,
  componentLicenseFileDetails: componentLicenseDetailsPropType,
};
