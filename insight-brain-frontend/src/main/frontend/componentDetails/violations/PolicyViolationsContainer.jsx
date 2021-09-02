/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import PolicyViolations from './PolicyViolations';
import { actions } from './PolicyViolationsSlice';
import {
  selectComponentDetailsViolationsSlice,
  selectComponentViolations,
  selectComponentWaivers,
  selectSelectedViolationDetail,
} from './PolicyViolationsSelectors';
import { selectComponentName } from '../componentDetailsSelectors';
import { setWaiverToDelete } from '../../waivers/waiverActions';

function mapStateToProps(state) {
  const {
    loading,
    loadError,
    setSelectedPolicyViolationId,
    showComponentWaiversPopover,
    showViolationsDetailPopover,
    showAddWaiverPopover,
    showRequestWaiverPopover,
    hasPermissionToAddWaivers,
  } = selectComponentDetailsViolationsSlice(state);

  return {
    violations: selectComponentViolations(state),
    waivers: selectComponentWaivers(state),
    componentName: selectComponentName(state),
    selectedViolationDetail: selectSelectedViolationDetail(state),
    loading,
    loadError,
    setSelectedPolicyViolationId,
    ...pick(['waiverToDelete'], state.deleteWaiver),
    showComponentWaiversPopover,
    showViolationsDetailPopover,
    showAddWaiverPopover,
    showRequestWaiverPopover,
    hasPermissionToAddWaivers,
  };
}

const mapDispatchToProps = {
  loadPolicyViolationsInformation: actions.load,
  toggleComponentWaiversPopover: actions.toggleComponentWaiversPopover,
  setShowViolationsDetail: actions.setShowViolationsDetail,
  toggleShowViolationsDetailPopover: actions.toggleShowViolationsDetailPopover,
  toggleAddWaiverPopover: actions.toggleAddWaiverPopover,
  toggleRequestWaiverPopover: actions.toggleRequestWaiverPopover,
  setSelectedPolicyViolationId: actions.setSelectedPolicyViolationId,
  setWaiverToDelete,
};

export const PolicyViolationsContainer = connect(mapStateToProps, mapDispatchToProps)(PolicyViolations);
