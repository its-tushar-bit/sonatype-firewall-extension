/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import FirewallPage from './FirewallPage';
import { pick } from 'ramda';
import {
  initializeWelcomeModal,
  loadFirewallData,
  loadQuarantineList,
  openConfigurationModal,
  closeWelcomeModal,
  setQuarantineGridPage,
  setQuarantineGridPolicyFilter,
  setQuarantineGridComponentNameFilter,
  setQuarantineGridRepositoryPublicIdFilter,
  setQuarantineGridSorting,
  goToRepositoryComponentDetailsPage,
  setQuarantineGridPolicyFilterWithProprietaryNameConflict,
  setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode,
} from './firewallActions';

function mapStateToProps({ firewall }) {
  return {
    ...pick(['showWelcomeModal'], firewall),
    ...pick(['isShowConfigurationModal', 'loadError'], firewall.viewState),
    ...pick(['autoUnquarantineEnabled'], firewall.configurationState),
    ...pick(
      [
        'componentsAutoReleased',
        'componentsQuarantined',
        'namespaceAttacksBlocked',
        'safeVersionsSelected',
        'supplyChainAttacksBlocked',
        'waivedComponents',
      ],
      firewall.tileMetricsState
    ),
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
  initializeWelcomeModal,
  loadFirewallData,
  loadQuarantineList,
  closeWelcomeModal,
  setQuarantineGridPage,
  setQuarantineGridSorting,
  setQuarantineGridPolicyFilter,
  setQuarantineGridComponentNameFilter,
  setQuarantineGridRepositoryPublicIdFilter,
  openConfigurationModal,
  goToRepositoryComponentDetailsPage,
  setQuarantineGridPolicyFilterWithProprietaryNameConflict,
  setQuarantineGridPolicyFilterWithSecurityVulnerabilityCategoryMaliciousCode,
};

export default connect(mapStateToProps, mapDispatchToProps)(FirewallPage);
