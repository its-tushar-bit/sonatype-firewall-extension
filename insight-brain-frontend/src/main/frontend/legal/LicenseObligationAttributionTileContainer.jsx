/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import * as advancedLegalActions from '../advancedLegal/advancedLegalActions';
import LicenseObligationAttributionTile from './LicenseObligationAttributionTile';
import { pick } from 'ramda';

function mapStateToProps({ advancedLegal }, ownProps) {
  let obligationState = advancedLegal.component.obligations.filter(element => element.name === ownProps.name)[0];
  let attributionState = obligationState.attributions[0];
  return {
    id: attributionState.id,
    originalAttributionText: attributionState.originalContent,
    attributionText: attributionState.content,
    originalObligationFulfilled: obligationState.originalStatus === 'FULFILLED',
    obligationFulfilled: obligationState.status === 'FULFILLED',
    originalScope: attributionState.originalOwnerId,
    scope: attributionState.ownerId,
    showAttributionModal: attributionState.showAttributionModal,
    error: attributionState.error,
    saveAttributionSubmitMask: attributionState.saveAttributionSubmitMask,
    availableScopes: advancedLegal.availableScopes
  };
}

const mapDispatchToProps = pick([
  'setAttributionText',
  'setObligationFulfilled',
  'setAttributionScope',
  'setShowAttributionModal',
  'saveAttribution'
], advancedLegalActions);

export default connect(mapStateToProps, mapDispatchToProps)(LicenseObligationAttributionTile);
