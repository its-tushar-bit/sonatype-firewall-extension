/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import FirewallComponentDetailPage from './FirewallComponentDetailsPage';
import { selectFirewallCDPRouteParams, selectFirewallCDP } from '../firewallSelectors';
import { loadComponentDetails, onCDPTabChange } from '../firewallActions';

function mapStateToProps(state) {
  return {
    CDPResponseState: selectFirewallCDP(state),
    routeParams: selectFirewallCDPRouteParams(state),
  };
}

const mapDispatchToProps = {
  loadComponentDetails,
  onCDPTabChange,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallComponentDetailPage);
