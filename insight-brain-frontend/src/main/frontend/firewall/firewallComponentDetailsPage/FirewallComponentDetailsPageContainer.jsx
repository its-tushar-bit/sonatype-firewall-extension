/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { actions as componentDetailsActions } from 'MainRoot/componentDetails/componentDetailsSlice';
import FirewallComponentDetailsPage from './FirewallComponentDetailsPage';
import {
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallComponentDetailsPage,
} from '../firewallSelectors';
import {
  loadComponentDetails,
  onComponentDetailsPageTabChange,
  loadComponentPolicyViolations,
  loadExistingWaiversData,
  reevaluateComponent,
} from '../firewallActions';
import { selectLabels } from 'MainRoot/componentDetails/componentDetailsSelectors';

function mapStateToProps(state) {
  return {
    componentDetailsPageResponseState: selectFirewallComponentDetailsPage(state),
    routeParams: selectFirewallComponentDetailsPageRouteParams(state),
    labels: selectLabels(state),
  };
}

const mapDispatchToProps = {
  loadComponentDetails,
  onComponentDetailsPageTabChange,
  loadComponentPolicyViolations,
  loadExistingWaiversData,
  reevaluateComponent,
  firewallLoadApplicableLabels: componentDetailsActions.firewallLoadApplicableLabels,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallComponentDetailsPage);
