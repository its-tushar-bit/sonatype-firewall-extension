/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  availableScopesPropType,
  componentLicenseDetailsPropType,
  componentPropType,
} from '../../advancedLegalPropTypes';
import { timeAgo } from '../../../utilAngular/CommonServices';
import * as PropTypes from 'prop-types';
import { LegalFileOverviewHeader } from '../common/LegalFileOverviewHeader';

export default function LicenseFilesDetailsOverview(props) {
  const { availableScopes, componentLicenseFileDetails, component, loading, error } = props;

  const licenseLegalData = component && component.licenseLegalData;

  const licenseModification = () => {
    if (licenseLegalData && licenseLegalData.componentLicensesLastUpdatedAt) {
      let age = timeAgo(licenseLegalData.componentLicensesLastUpdatedAt);
      return `${age.age} ${age.qualifier} by ${licenseLegalData.componentLicensesLastUpdatedByUsername}`;
    } else {
      return 'N/A';
    }
  };

  const scopeOwnerId =
    (component && component.licenseLegalData && component.licenseLegalData.componentLicensesScopeOwnerId) ||
    'ROOT_ORGANIZATION_ID';

  return loading || error
    ? null
    : LegalFileOverviewHeader(
        componentLicenseFileDetails.selectedLicense,
        availableScopes,
        licenseModification(),
        'License',
        scopeOwnerId
      );
}

LicenseFilesDetailsOverview.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  availableScopes: availableScopesPropType,
  componentLicenseFileDetails: componentLicenseDetailsPropType,
  component: componentPropType,
};
