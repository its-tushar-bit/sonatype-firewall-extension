/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

export default function FirewallAutoUnquarantine() {
  return (
    <section id="firewall-auto-unquarantine" className="nx-card iq-firewall-auto-unquarantine-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Auto Unquarantine</h3>
      </header>
      <div className="nx-card__content">
        <div className="nx-card__call-out">
          100
        </div>
        <p className="nx-p">components unquarantined month-to-date</p>
      </div>
      <footer className="nx-card__footer">
        <a href="#" className="nx-text-link">View Auto Unquarantine</a>
      </footer>
    </section>

  );
}

