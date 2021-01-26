/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';
import * as firewallActions from './firewallActions';
import Firewall from './Firewall';

function mapStateToProps({firewall}) {
  return {
    ...firewall.viewState,
    ...firewall.configurationState
  };
}

export default connect(mapStateToProps, firewallActions)(Firewall);
