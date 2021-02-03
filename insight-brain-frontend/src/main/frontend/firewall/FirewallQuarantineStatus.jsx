/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import StatusIndicatorIcon from '../react/statusIndicatorIcon/StatusIndicatorIcon';

export default function FirewallQuarantineStatus() {
  return (
    <section id="firewall-quarantine-status" className="nx-card iq-firewall-quarantine-status-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Quarantine Status</h3>
      </header>
      <div className="nx-card__content">
        <div className="iq-status-indicator">
          <StatusIndicatorIcon status={true}/>
          <span>Active</span>
        </div>
        <p className="nx-p">on 10 of 10 repositories</p>
      </div>
    </section>

  );
}

