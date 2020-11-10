/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton } from '@sonatype/react-shared-components';

export default function LicenseObligationsTile() {

  return (
    <section id="license-obligations-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">License Obligations</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton variant="tertiary">
            Resolve All
          </NxButton>
        </div>
      </header>
      <div className="nx-tile-content">
        <div>
          accordion 1
        </div>
        <div>
          accordion 2
        </div>
        <div>
          accordion 3
        </div>
      </div>
    </section>
  );
}
