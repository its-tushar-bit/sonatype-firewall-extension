/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import FirewallComponentDetailsPage from './FirewallComponentDetailsPage';
import {
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallComponentDetailsPage,
} from '../firewallSelectors';
import { loadComponentDetails, onComponentDetailsPageTabChange } from '../firewallActions';

function mapStateToProps(state) {
  return {
    componentDetailsPageResponseState: selectFirewallComponentDetailsPage(state),
    routeParams: selectFirewallComponentDetailsPageRouteParams(state),
  };
}

const mapDispatchToProps = {
  loadComponentDetails,
  onComponentDetailsPageTabChange: onComponentDetailsPageTabChange,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallComponentDetailsPage);
