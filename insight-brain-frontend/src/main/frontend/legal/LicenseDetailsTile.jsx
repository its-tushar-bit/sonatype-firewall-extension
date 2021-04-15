/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

export default function LicenseDetailsTile(props) {
  const { licenseNames } = props;

  const isLicensePresent = () => licenseNames.length > 0;

  return (
    <section id="license-details-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Licenses</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list">{isLicensePresent() ? licenseNames.map(createItem) : 'None found'}</ul>
      </div>
    </section>
  );
}

const createItem = (license, index) => {
  return (
    <li className="nx-list__item" key={index}>
      <span className="nx-list__text">{license}</span>
    </li>
  );
};

LicenseDetailsTile.propTypes = {
  licenseNames: PropTypes.arrayOf(PropTypes.string.isRequired),
};
