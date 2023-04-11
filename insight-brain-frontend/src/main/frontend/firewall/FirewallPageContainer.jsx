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
  loadQuarantineList,
  openConfigurationModal,
  setQuarantineGridPage,
  setQuarantineGridPolicyFilter,
  setQuarantineGridComponentNameFilter,
  setQuarantineGridSorting,
  goToRepositoryComponentDetailsPage,
} from './firewallActions';

function mapStateToProps({ firewall }) {
  return {
    ...pick(['isShowConfigurationModal', 'loadError'], firewall.viewState),
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
  setQuarantineGridComponentNameFilter,
  openConfigurationModal,
  goToRepositoryComponentDetailsPage,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallPage);
