/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import LicenseDetectionsTile from './LicenseDetectionsTile';
import { selectLicenseDetectionsTileDataSlice } from './licenseDetectionsTileSelectors';
import { actions } from './licenseDetectionsTileSlice';

function mapStateToProps(state) {
  const {
    licenseOverride,
    declaredlicenses,
    effectiveLicenses,
    observedlicenses,
    selectableLicenses,
    allLicenses,
    loading,
    loadError,
  } = selectLicenseDetectionsTileDataSlice(state);
  return {
    licenseOverride,
    declaredlicenses,
    effectiveLicenses,
    observedlicenses,
    selectableLicenses,
    allLicenses,
    loading,
    loadError,
  };
}

const mapDispatchToProps = {
  loadLicenses: actions.load,
  toggleShowEditLicensesPopover: actions.toggleShowEditLicensesPopover,
};

export const LicenseDetectionsTileContainer = connect(mapStateToProps, mapDispatchToProps)(LicenseDetectionsTile);
