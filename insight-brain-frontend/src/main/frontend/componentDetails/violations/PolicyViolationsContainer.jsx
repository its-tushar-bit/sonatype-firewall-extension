/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';

import PolicyViolations from './PolicyViolations';
import { actions } from './PolicyViolationsRedux';
import {
  selectComponentDetailsViolationsSlice,
  selectComponentViolations,
  selectComponentWaivers,
} from './PolicyViolationsSelectors';
import { selectComponentName } from '../componentDetailsSelectors';
import { setWaiverToDelete } from '../../waivers/waiverActions';

function mapStateToProps(state) {
  const {
    loading,
    loadError,
    selectedViolationId,
    showComponentWaiversPopover,
  } = selectComponentDetailsViolationsSlice(state);
  return {
    violations: selectComponentViolations(state),
    waivers: selectComponentWaivers(state),
    componentName: selectComponentName(state),
    showComponentWaiversPopover,
    loading,
    loadError,
    selectedViolationId,
    ...pick(['waiverToDelete'], state.deleteWaiver),
  };
}

const mapDispatchToProps = {
  loadPolicyViolationsInformation: actions.load,
  toggleComponentWaiversPopover: actions.toggleComponentWaiversPopover,
  setWaiverToDelete,
  setSelectedViolationId: actions.setSelectedViolationId,
  goToWaivers: actions.goToWaivers,
};

export const PolicyViolationsContainer = connect(mapStateToProps, mapDispatchToProps)(PolicyViolations);
