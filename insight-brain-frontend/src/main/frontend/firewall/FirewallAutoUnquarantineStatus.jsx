/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import StatusIndicatorIcon from '../react/statusIndicatorIcon/StatusIndicatorIcon';
import * as PropTypes from 'prop-types';

export default function FirewallAutoUnquarantineStatus({onConfigureClicked}) {
  return (
    <section id="firewall-auto-unquarantine-status" className="nx-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Auto Unquarantine Status</h3>
      </header>
      <div className="nx-card__content">
        <div className="iq-status-indicator">
          <StatusIndicatorIcon status={true}/>
          <span>Active</span>
        </div>
        <p className="nx-p">unquarantining 10 of 25 policy types</p>
      </div>
      <footer className="nx-card__footer">
        <a className="nx-text-link" onClick={onConfigureClicked}>Configure</a>
      </footer>
    </section>
  );
}

FirewallAutoUnquarantineStatus.propTypes = {
  onConfigureClicked: PropTypes.func.isRequired
};

