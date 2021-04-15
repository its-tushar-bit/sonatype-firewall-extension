/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import Firewall from './Firewall';
import { pick } from 'ramda';
import { loadData, openConfigurationModal } from './firewallActions';

function mapStateToProps({ firewall }) {
  return {
    ...pick(
      ['loadedStatus', 'isShowConfigurationModal', 'loadError'],
      firewall.viewState
    ),
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
  };
}

const mapDispatchToProps = {
  loadData,
  openConfigurationModal,
};

export default connect(mapStateToProps, mapDispatchToProps)(Firewall);
