/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTextLink } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function FirewallAutoReleaseQuarantine(props) {
  // viewState
  const { autoReleaseQuarantineCountMTD } = props;

  const $state = useRouterState();

  return (
    <section id="firewall-auto-release-quarantine" className="nx-card iq-firewall-auto-release-quarantine-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Auto Released from Quarantine</h3>
      </header>
      <div className="nx-card__content">
        <div className="nx-card__call-out">{autoReleaseQuarantineCountMTD}</div>

        <div className="nx-card__text">components released month-to-date</div>
      </div>
      <footer className="nx-card__footer">
        <NxTextLink href={$state.href('firewall.firewallAutoUnquarantinePage')}>
          View Auto Release Quarantine
        </NxTextLink>
      </footer>
    </section>
  );
}

FirewallAutoReleaseQuarantine.propTypes = {
  autoReleaseQuarantineCountMTD: PropTypes.string,
};
