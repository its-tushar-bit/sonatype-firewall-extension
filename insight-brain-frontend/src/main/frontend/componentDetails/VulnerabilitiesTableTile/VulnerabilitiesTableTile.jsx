/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

import VulnerabilitiesTable, { vulnerabilitiesPropTypes } from './VulnerabilitiesTable';
import { useEffect } from 'react';

export default function VulnerabilitiesTableTile({ vulnerabilities, loadVulnerabilities }) {
  useEffect(() => {
    loadVulnerabilities();
  }, []);
  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2" id="component-details-vulnerabilities-title">
            Vulnerabilities
          </h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <VulnerabilitiesTable vulnerabilities={vulnerabilities} loadVulnerabilities={loadVulnerabilities} />
      </div>
    </section>
  );
}

VulnerabilitiesTableTile.propTypes = {
  loadVulnerabilities: PropTypes.func.isRequired,
  vulnerabilities: PropTypes.shape(vulnerabilitiesPropTypes),
};
