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
import { selectSelectedComponent } from 'MainRoot/applicationReport/applicationReportSelectors';
import { getComponentNameWithoutVersion } from 'MainRoot/util/componentNameUtils';
import { selectIsAutoWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

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
    ownerType: 'application',
    ownerId: state.router.currentParams.publicId,
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
