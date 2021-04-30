/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import FirewallAutoUnquarantinePage from './FirewallAutoUnquarantinePage';
import { pick } from 'ramda';
import {
  loadAutoUnquarantineData,
  loadReleaseQuarantineList,
  openConfigurationModal,
  setAutoUnquarantineGridSorting,
  setAutoUnquarantineGridPage,
  selectReleaseQuarantineComponent,
} from '../firewallActions';

function mapStateToProps({ firewall }) {
  return {
    ...pick(['loadedStatus', 'isShowConfigurationModal', 'loadError'], firewall.viewState),
    ...pick(['isEnabled'], firewall.statusState),
    ...pick(['autoUnquarantineEnabled'], firewall.configurationState),
    ...pick(
      [
        'loadedReleaseQuarantineSummary',
        'autoReleaseQuarantineCountMTD',
        'autoReleaseQuarantineCountYTD',
        'loadedConfiguration',
        'enabledPolicyConditionTypesCount',
        'totalPolicyConditionTypesCount',
      ],
      firewall.autoUnquarantineState.viewState
    ),
    ...firewall.autoUnquarantineState.autoUnquarantineGridState,
    ...firewall.policiesState,
  };
}

const mapDispatchToProps = {
  loadAutoUnquarantineData,
  loadReleaseQuarantineList,
  setAutoUnquarantineGridPage,
  setAutoUnquarantineGridSorting,
  openConfigurationModal,
  selectReleaseQuarantineComponent,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallAutoUnquarantinePage);
