/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import LicenseDetectionsTile from './LicenseDetectionsTile';
import { selectLicenseDetectionsTileDataSlice } from './licenseDetectionsTileSelectors';
import { actions } from './licenseDetectionsTileSlice';
import { actions as componentDetailsActions } from '../../componentDetailsSlice';
import {
  selectComponentDetailsLoading,
  selectComponentDetailsLoadErrors,
  selectComponentIdentificationSource,
} from '../../componentDetailsSelectors';

function mapStateToProps(state) {
  const {
    licenseOverride,
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    selectableLicenses,
    licenseLegalMetadata,
    allLicenses,
    loading,
    loadError,
  } = selectLicenseDetectionsTileDataSlice(state);

  const isLoadingComponentDetails = selectComponentDetailsLoading(state);
  const componentDetailsLoadError = selectComponentDetailsLoadErrors(state);
  const identificationSource = selectComponentIdentificationSource(state);

  return {
    isLoadingComponentDetails,
    componentDetailsLoadError,
    licenseOverride,
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    selectableLicenses,
    licenseLegalMetadata,
    allLicenses,
    loading,
    loadError,
    identificationSource,
  };
}

const mapDispatchToProps = {
  loadComponentDetails: componentDetailsActions.loadComponentDetails,
  loadLicenses: actions.load,
  toggleShowEditLicensesPopover: actions.toggleShowEditLicensesPopover,
};

export const LicenseDetectionsTileContainer = connect(mapStateToProps, mapDispatchToProps)(LicenseDetectionsTile);
