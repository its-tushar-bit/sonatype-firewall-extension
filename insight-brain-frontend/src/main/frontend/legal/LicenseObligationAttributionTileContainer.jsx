/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import {
  cancelAttributionModal,
  saveAttribution,
  setAttributionScope,
  setAttributionText,
  setShowAttributionModal
} from './advancedLegalObligationActions';
import LicenseObligationAttributionTile from './LicenseObligationAttributionTile';
import { find, propEq } from 'ramda';

function mapStateToProps({ advancedLegal }, ownProps) {
  const attributionState = find(propEq('obligationName', ownProps.name), advancedLegal.component.attributions);
  return {
    id: attributionState.id,
    originalAttributionText: attributionState.originalContent,
    attributionText: attributionState.content,
    originalScope: attributionState.originalOwnerId,
    scope: attributionState.ownerId,
    showAttributionModal: attributionState.showAttributionModal,
    error: attributionState.error,
    saveAttributionSubmitMask: attributionState.saveAttributionSubmitMask,
    availableScopes: advancedLegal.availableScopes
  };
}

const mapDispatchToProps = {
  setAttributionText,
  setAttributionScope,
  setShowAttributionModal,
  saveAttribution,
  cancelAttributionModal
};

export default connect(mapStateToProps, mapDispatchToProps)(LicenseObligationAttributionTile);
