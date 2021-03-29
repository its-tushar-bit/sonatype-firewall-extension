/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {connect} from 'react-redux';

import FirewallConfigurationModal from './FirewallConfigurationModal';
import {pick} from 'ramda';
import {
  closeConfigurationModal,
  loadConfiguration,
  saveConfiguration,
  toggleAutoUnquarantineAll,
  toggleAutoUnquarantineEnabled
} from '../firewallActions';

function mapStateToProps({firewallConfigurationModal, firewall}) {
  return {
    ...firewallConfigurationModal.viewState,
    ...firewallConfigurationModal.formState,
    ...pick(['loadedConfiguration', 'loadConfigurationError'], firewall.autoUnquarantineState.viewState)
  };
}

const mapDispatchToProps = {
  loadConfiguration,
  toggleAutoUnquarantineEnabled,
  closeConfigurationModal,
  saveConfiguration,
  toggleAutoUnquarantineAll
};

const FirewallConfigurationModalContainer = connect(mapStateToProps, mapDispatchToProps)(FirewallConfigurationModal);
export default FirewallConfigurationModalContainer;
