/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { componentPropType } from './advancedLegalPropTypes';

export default function LicenseDetailsTile(props) {
  const {
    component
  } = props;

  return (
    <section id="license-details-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Licenses</h2>
        </div>
        <div className="nx-tile__actions">
          <a href="">View Details</a>
        </div>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list">
          { component.licenseLegalData.effectiveLicenses.map(createItem) }
        </ul>
      </div>
    </section>
  );
}

const createItem = (license, index) => {
  return (
    <li className="nx-list__item" key={ index }>
      <span className="nx-list__text">
        { license }
      </span>
    </li>
  );
};

LicenseDetailsTile.propTypes = {
  component: componentPropType
};
