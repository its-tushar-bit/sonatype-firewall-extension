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
} from './PolicyViolationsSelectors';
import {
  selectComponentName,
  selectComponentDetailsLoading,
  selectComponentDetailsLoadErrors,
} from '../componentDetailsSelectors';
import { actions as componentDetailsActions } from '../componentDetailsSlice';
import { setWaiverToDelete } from '../../waivers/waiverActions';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { selectSelectedComponent, selectIsHrcReport } from 'MainRoot/applicationReport/applicationReportSelectors';
import { getComponentNameWithoutVersion } from 'MainRoot/util/componentNameUtils';
import { selectIsAutoWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { FIREWALL_CONTAINER_COMPONENT_DETAILS } from 'MainRoot/constants/states/firewall';
import { API_OWNER_TYPE_APPLICATION, API_OWNER_TYPE_HRC } from 'MainRoot/applicationReport/ownerTypeConstants';

function mapStateToProps(state) {
  const {
    loading,
    loadError,
    showComponentWaiversPopover,
    showViolationsDetailPopover,
    innerSourceTransitiveWaiver,
  } = selectComponentDetailsViolationsSlice(state);
  const isLoadingComponentDetails = selectComponentDetailsLoading(state);
  const componentDetailsLoadError = selectComponentDetailsLoadErrors(state);
  const component = selectSelectedComponent(state);
  const showViewTransitiveViolations = !!(
    innerSourceTransitiveWaiver &&
    component &&
    component.componentIdentifier &&
    component.innerSource
  );
  const componentNameWithoutVersion = component && getComponentNameWithoutVersion(component);

  // Detect if this is a Firewall container component
  const isFirewall = state.router.currentState?.name?.includes(FIREWALL_CONTAINER_COMPONENT_DETAILS);

  return {
    isLoadingComponentDetails,
    componentDetailsLoadError,
    violations: selectComponentViolations(state),
    waivers: selectComponentWaivers(state),
    isAutoWaiverEnabled: selectIsAutoWaiversEnabled(state),
    componentName: selectComponentName(state),
    componentNameWithoutVersion,
    loading,
    error: loadError,
    ...pick(['waiverToDelete'], state.deleteWaiver),
    showComponentWaiversPopover,
    showViolationsDetailPopover,
    showViewTransitiveViolations,
    ownerType: state.router.currentParams.hrcId ? API_OWNER_TYPE_HRC : API_OWNER_TYPE_APPLICATION,
    ownerId: state.router.currentParams.hrcId || state.router.currentParams.publicId,
    isHrcReport: selectIsHrcReport(state),
    isFirewall,
    ...pick(['scanId', 'hash'], state.router.currentParams),
  };
}

const mapDispatchToProps = {
  loadComponentDetails: componentDetailsActions.loadComponentDetails,
  loadPolicyViolationsInformation: actions.load,
  toggleComponentWaiversPopover: actions.toggleComponentWaiversPopover,
  toggleShowViolationsDetailPopover: actions.toggleShowViolationsDetailPopover,
  setViolationsDetailRowClicked: actions.setViolationsDetailRowClicked,
  setSelectedPolicyViolationId: actions.setSelectedPolicyViolationId,
  setViolationType: actions.setViolationType,
  setWaiverToDelete,
  stateGo,
};

export const ViolationsTableTileContainer = connect(mapStateToProps, mapDispatchToProps)(ViolationsTableTile);
