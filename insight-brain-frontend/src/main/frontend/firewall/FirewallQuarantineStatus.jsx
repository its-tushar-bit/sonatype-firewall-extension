/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import StatusIndicatorIcon from '../react/statusIndicatorIcon/StatusIndicatorIcon';
import * as PropTypes from 'prop-types';

export default function FirewallQuarantineStatus(props) {
  //viewState
  const {
    quarantineEnabled,
    quarantineEnabledRepositoryCount,
    repositoryCount
  } = props;

  return (
    <section id="firewall-quarantine-status" className="nx-card iq-firewall-quarantine-status-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Quarantine Status</h3>
      </header>
      <div className="nx-card__content">
        <div className="iq-status-indicator">
          <StatusIndicatorIcon status={quarantineEnabled}/>
          <span>{quarantineEnabled ? 'Active' : 'Inactive'}</span>
        </div>
        <div className="nx-card__text">on {quarantineEnabledRepositoryCount} of {repositoryCount} repositories</div>
      </div>
    </section>

  );
}

FirewallQuarantineStatus.propTypes = {
  quarantineEnabled: PropTypes.bool.isRequired,
  quarantineEnabledRepositoryCount: PropTypes.number.isRequired,
  repositoryCount: PropTypes.number.isRequired
};
