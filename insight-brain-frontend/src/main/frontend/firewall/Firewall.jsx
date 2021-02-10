/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useEffect} from 'react';
import LoadWrapper from '../react/LoadWrapper';
import FirewallStatus from './FirewallStatus';
import FirewallQuarantineStatus from './FirewallQuarantineStatus';
import FirewallAutoUnquarantineStatus from './FirewallAutoUnquarantineStatus';
import FirewallQuarantine from './FirewallQuarantine';
import FirewallAutoUnquarantine from './FirewallAutoUnquarantine';
import FirewallQuarantineTable from './FirewallQuarantineTable';
import * as PropTypes from 'prop-types';
import FirewallConfigurationModalContainer from './config/FirewallConfigurationModalContainer';

export default function Firewall(props) {
  // Actions
  const {
    loadStatus,
    openConfigurationModal
  } = props;

  // viewState
  const {
    loadedStatus,
    loadStatusError,
    isShowConfigurationModal
  } = props;

  // configurationState
  const {
    isEnabled
  } = props;

  const error = loadedStatus && !isEnabled ? 'The Firewall feature is disabled' : loadStatusError;

  useEffect(() => {
    loadStatus();
  }, []);

  return (
    <main id="firewall-page" className="nx-page-main">
      {isShowConfigurationModal && <FirewallConfigurationModalContainer/>}
      <LoadWrapper loading={!loadedStatus} error={error} retryHandler={loadStatus}>
        <FirewallStatus/>
        <div className="nx-card-container nx-card-container--row iq-firewall__horizontal">
          <FirewallQuarantineStatus/>
          <FirewallAutoUnquarantineStatus onConfigureClicked={openConfigurationModal}/>
          <FirewallQuarantine/>
          <FirewallAutoUnquarantine/>
        </div>
        <FirewallQuarantineTable/>
      </LoadWrapper>
    </main>
  );
}

Firewall.propTypes = {
  loadStatus: PropTypes.func.isRequired,
  loadedStatus: PropTypes.bool.isRequired,
  loadStatusError: PropTypes.object,
  isEnabled: PropTypes.bool.isRequired,
  isShowConfigurationModal: PropTypes.bool.isRequired,
  loadConfiguration: PropTypes.func.isRequired,
  openConfigurationModal: PropTypes.func.isRequired
};
