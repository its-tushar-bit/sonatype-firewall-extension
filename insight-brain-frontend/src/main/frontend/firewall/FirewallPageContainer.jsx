/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import FirewallPage from './FirewallPage';
import { pick } from 'ramda';
import {
  loadFirewallData,
  openConfigurationModal,
  loadQuarantineList,
  selectQuarantineComponent,
  setQuarantineGridSorting,
  setQuarantineGridPolicyFilter,
  setQuarantineGridPage,
} from './firewallActions';

function mapStateToProps({ firewall }) {
  return {
    ...pick(['loadedStatus', 'isShowConfigurationModal', 'loadError'], firewall.viewState),
    ...pick(['isEnabled'], firewall.statusState),
    ...pick(['autoUnquarantineEnabled'], firewall.configurationState),
    ...pick(
      [
        'loadedReleaseQuarantineSummary',
        'autoReleaseQuarantineCountMTD',
        'loadedConfiguration',
        'enabledPolicyConditionTypesCount',
        'totalPolicyConditionTypesCount',
      ],
      firewall.autoUnquarantineState.viewState
    ),
    ...pick(
      [
        'loadedQuarantineSummary',
        'quarantineEnabled',
        'quarantineEnabledRepositoryCount',
        'repositoryCount',
        'totalComponentCount',
        'quarantinedComponentCount',
      ],
      firewall.quarantineSummaryState.viewState
    ),
    ...firewall.quarantineGridState,
    ...firewall.policiesState,
  };
}

const mapDispatchToProps = {
  loadFirewallData,
  loadQuarantineList,
  setQuarantineGridPage,
  setQuarantineGridSorting,
  setQuarantineGridPolicyFilter,
  openConfigurationModal,
  selectQuarantineComponent,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallPage);
