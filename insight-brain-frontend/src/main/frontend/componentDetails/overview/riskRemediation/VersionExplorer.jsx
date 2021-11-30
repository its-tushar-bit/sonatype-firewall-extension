/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import VersionGraphExplorer from '../VersionGraphExplorer/VersionGraphExplorer';

export const VersionExplorer = (props) => {
  const { versions, currentVersion, source } = props;
  return (
    <section className="iq-version-explorer nx-grid-col__section">
      <header className="nx-grid-header">
        <h3 className="nx-h3 nx-grid-header__title">Version Explorer</h3>
      </header>
      <div className="iq-grid-content">
        {currentVersion && versions && (
          <div>
            <VersionGraphExplorer {...props} />
            {source && <div id="iq-version-explorer-repository-source">Repository Source: {source}</div>}
          </div>
        )}
      </div>
    </section>
  );
};

VersionExplorer.propTypes = {
  versions: PropTypes.array,
  currentVersion: PropTypes.string,
  source: PropTypes.string,
};
