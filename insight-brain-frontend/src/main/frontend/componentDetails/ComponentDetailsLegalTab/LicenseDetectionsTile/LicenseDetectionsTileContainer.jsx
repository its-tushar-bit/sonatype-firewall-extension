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
  selectComponentDetails,
  selectApplicationInfo,
} from '../../componentDetailsSelectors';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { fetchAdvanceLegalPackFeatures } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice.js';

function mapStateToProps(state) {
  const {
    licenseOverride,
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    selectableLicenses,
    allLicenses,
    loading,
    loadError,
    reviewObligationsButtonIsVisible,
  } = selectLicenseDetectionsTileDataSlice(state);

  const { applicationId, stageId } = selectApplicationInfo(state) ?? { applicationId: null, stageId: null };
  const { hash } = selectComponentDetails(state) ?? { hash: null };
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
    allLicenses,
    loading,
    loadError,
    identificationSource,
    applicationId,
    stageId,
    componentHash: hash,
    reviewObligationsButtonIsVisible,
  };
}

const mapDispatchToProps = {
  loadComponentDetails: componentDetailsActions.loadComponentDetails,
  loadLicenses: actions.load,
  toggleShowEditLicensesPopover: actions.toggleShowEditLicensesPopover,
  stateGo,
  fetchAdvanceLegalPackFeatures,
};

export const LicenseDetectionsTileContainer = connect(mapStateToProps, mapDispatchToProps)(LicenseDetectionsTile);
