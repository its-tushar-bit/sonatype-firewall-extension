/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';

export default function FirewallAutoReleaseQuarantine(props) {
  // viewState
  const {
    autoReleaseQuarantineCountMTD
  } = props;

  return (
    <section id="firewall-auto-release-quarantine" className="nx-card iq-firewall-auto-release-quarantine-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Auto Released from Quarantine</h3>
      </header>
      <div className="nx-card__content">
        <div className="nx-card__call-out">
          {autoReleaseQuarantineCountMTD}
        </div>

        <p className="nx-p">components released month-to-date</p>
      </div>
    </section>
  );
}

FirewallAutoReleaseQuarantine.propTypes = {
  autoReleaseQuarantineCountMTD: PropTypes.string
};

