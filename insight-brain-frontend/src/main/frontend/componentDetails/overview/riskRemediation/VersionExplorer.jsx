/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import VersionGraphExplorer from '../VersionGraphExplorer/VersionGraphExplorer';

export const VersionExplorer = ({ versionExplorerData }) => {
  const data = versionExplorerData.data || {};
  const { version, versions } = data;

  return (
    <section className="iq-version-explorer nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Version Explorer</h3>
      </header>
      <div className="nx-tile-content">
        {version && versions && <VersionGraphExplorer data={versionExplorerData.data} />}
      </div>
    </section>
  );
};

VersionExplorer.propTypes = {
  versionExplorerData: PropTypes.shape({
    data: PropTypes.shape({
      version: PropTypes.string,
      versions: PropTypes.array,
    }),
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
};
