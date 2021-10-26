/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import ViolationsTableTile from './ViolationsTableTile';
import { actions } from './policyViolationsSlice';
import {
  selectComponentDetailsViolationsSlice,
  selectComponentViolations,
  selectComponentWaivers,
  selectSelectedViolationDetail,
} from './PolicyViolationsSelectors';
import {
  selectComponentName,
  selectComponentDetailsLoading,
  selectComponentDetailsLoadErrors,
} from '../componentDetailsSelectors';
import { actions as componentDetailsActions } from '../componentDetailsSlice';
import { setWaiverToDelete } from '../../waivers/waiverActions';

function mapStateToProps(state) {
  const {
    loading,
    loadError,
    showComponentWaiversPopover,
    showViolationsDetailPopover,
    showAddWaiverPopover,
    showRequestWaiverPopover,
    hasPermissionToAddWaivers,
  } = selectComponentDetailsViolationsSlice(state);
  const isLoadingComponentDetails = selectComponentDetailsLoading(state);
  const componentDetailsLoadError = selectComponentDetailsLoadErrors(state);

  return {
    isLoadingComponentDetails,
    componentDetailsLoadError,
    violations: selectComponentViolations(state),
    waivers: selectComponentWaivers(state),
    componentName: selectComponentName(state),
    selectedViolationDetail: selectSelectedViolationDetail(state),
    loading,
    error: loadError,
    ...pick(['waiverToDelete'], state.deleteWaiver),
    showComponentWaiversPopover,
    showViolationsDetailPopover,
    showAddWaiverPopover,
    showRequestWaiverPopover,
    hasPermissionToAddWaivers,
  };
}

const mapDispatchToProps = {
  loadComponentDetails: componentDetailsActions.loadComponentDetails,
  loadPolicyViolationsInformation: actions.load,
  toggleComponentWaiversPopover: actions.toggleComponentWaiversPopover,
  toggleShowViolationsDetailPopover: actions.toggleShowViolationsDetailPopover,
  toggleAddWaiverPopover: actions.toggleAddWaiverPopover,
  toggleRequestWaiverPopover: actions.toggleRequestWaiverPopover,
  setSelectedPolicyViolationId: actions.setSelectedPolicyViolationId,
  setViolationType: actions.setViolationType,
  setWaiverToDelete,
};

export const ViolationsTableTileContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationsTableTile);
