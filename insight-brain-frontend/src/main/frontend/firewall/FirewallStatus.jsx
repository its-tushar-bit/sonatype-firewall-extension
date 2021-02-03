/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {faShieldCheck} from '@fortawesome/pro-solid-svg-icons';
import {NxFontAwesomeIcon} from '@sonatype/react-shared-components';

export default function FirewallStatus() {
  return (
    <section id="firewall-status">
      <header className="nx-page-title">
        <h1 className="nx-h1">
          <NxFontAwesomeIcon icon={faShieldCheck} size="sm" className="iq-firewall-protected-icon"/>
          <span>You are protected</span>
        </h1>

        <div className="iq-firewall-status-description">
          <div className="iq-firewall-status-description-line">
          </div>
          <div>
            Firewall is currently monitoring 2201 components in 20 repositories
          </div>
        </div>

      </header>
    </section>
  );
}

