/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

export default function FirewallQuarantine() {
  return (
    <section id="firewall-quarantine" className="nx-card iq-firewall-quarantine-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Quarantine</h3>
      </header>
      <div className="nx-card__content">
        <div className="nx-card__call-out nx-card__call-out--text-only">
          128
        </div>
        <p className="nx-p">
          components in quarantine
        </p>
      </div>
    </section>

  );
}

