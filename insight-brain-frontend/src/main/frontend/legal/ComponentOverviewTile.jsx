/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

export default function ComponentOverviewTile() {

  return (
    <section id="component-overview-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Component Overview</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        Review status, last edited by, Obligations, Licenses, etc.
      </div>
    </section>
  );
}
