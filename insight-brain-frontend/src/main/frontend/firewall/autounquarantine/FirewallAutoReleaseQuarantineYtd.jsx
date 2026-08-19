/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';

export default function FirewallAutoReleaseQuarantineYtd(props) {
  // viewState
  const { autoReleaseQuarantineCountYTD } = props;

  return (
    <section id="firewall-auto-release-quarantine-ytd" className="nx-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Auto Released (Year to Date)</h3>
      </header>
      <div className="nx-card__content">
        <div className="nx-card__call-out">{autoReleaseQuarantineCountYTD}</div>

        <div className="nx-card__text">components released year-to-date</div>
      </div>
    </section>
  );
}

FirewallAutoReleaseQuarantineYtd.propTypes = {
  autoReleaseQuarantineCountYTD: PropTypes.string,
};
