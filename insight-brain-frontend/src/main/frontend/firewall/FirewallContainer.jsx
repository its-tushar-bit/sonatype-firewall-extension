/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';
import * as firewallActions from './firewallActions';
import Firewall from './Firewall';
import {pick} from 'ramda';

function mapStateToProps({firewall}) {
  return {
    ...pick(['loadedStatus', 'loadStatusError', 'isShowConfigurationModal'], firewall.viewState),
    ...pick(['isEnabled'], firewall.statusState),
    ...pick(['autoUnquarantineEnabled'], firewall.configurationState),
    ...pick([
      'loadedConfiguration', 'loadConfigurationError', 'enabledPolicyConditionTypesCount',
      'totalPolicyConditionTypesCount'
    ], firewall.autoUnquarantineState.viewState)
  };
}

export default connect(mapStateToProps, firewallActions)(Firewall);
