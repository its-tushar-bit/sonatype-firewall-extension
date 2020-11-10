/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faEdit } from '@fortawesome/free-solid-svg-icons';

export default function LicenseDetailsTile() {

  return (
    <section id="license-details-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Licenses</h2>
        </div>
        <div className="nx-tile__actions">
          <a href="">View Details</a>
          <NxButton variant="tertiary">
            <NxFontAwesomeIcon icon={ faEdit }/>
            <span>Edit</span>
          </NxButton>
        </div>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list">
          <li className="nx-list__item">
            <span className="nx-list__text">Apache 2.0</span>
          </li>
          <li className="nx-list__item">
            <span className="nx-list__text">Apache 53.0</span>
          </li>
        </ul>
      </div>
    </section>
  );
}
