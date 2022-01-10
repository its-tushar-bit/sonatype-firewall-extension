/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

export default function QuarantineComponentOverviewDescriptionTile() {
  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Overview</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <p className="nx-p">
          The purpose of this report is to alert you of a component that has been quarantined due to a policy violation.
          No actions can be taken directly from this report, though you can remediate the component using the following
          information.
        </p>
      </div>
    </section>
  );
}
