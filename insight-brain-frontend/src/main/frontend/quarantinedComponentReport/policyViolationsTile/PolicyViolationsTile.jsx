/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import PolicyViolationsTable from './PolicyViolationsTable';

export default function PolicyViolationsTile({ violations }) {
  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Policy Violations Causing Quarantine</h2>
        </div>
      </header>
      <PolicyViolationsTable violations={violations} />
    </section>
  );
}
