/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import {
  setObligationStatus,
  setShowAllObligationsModal,
  setShowObligationModal,
} from './advancedLegalObligationActions';
import LicenseObligationsTile from './LicenseObligationsTile';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

function mapStateToProps({ advancedLegal, router }) {
  const isSbomManager = selectIsSbomManager({ router });
  return {
    isSbomManager,
    licenseObligations: advancedLegal.component.component.licenseLegalData.obligations,
    licenseLegalMetadata: advancedLegal.component.licenseLegalMetadata,
    showAllObligationsModal: advancedLegal.component.component.licenseLegalData.showAllObligationsModal,
  };
}

const mapDispatchToProps = {
  setObligationStatus,
  setShowObligationModal,
  setShowAllObligationsModal,
};

export default connect(mapStateToProps, mapDispatchToProps)(LicenseObligationsTile);
