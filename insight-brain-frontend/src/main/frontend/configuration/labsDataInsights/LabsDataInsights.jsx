/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';

export default function LabsDataInsights(props) {
  const { loadLabsDataInsights, errorMessage, loadingLabsDataInsights, licenseError } = props,
    loadError = licenseError || errorMessage;

  function load() {
    return loadLabsDataInsights();
  }

  useEffect(() => {
    if (!loadError) {
      load();
    }
  }, []);

  return (
    <main id="labs-data-insights-container" className="nx-page-main">
      <LoadWrapper loading={loadingLabsDataInsights} error={loadError} retryHandler={() => load()} />
      {/* Putting the labs-container div outside the load wrapper as we load a script in data insights actions
       and need this visible for the script to append an iframe before loading is finished */}
      {!loadError && <div id="labs-container"></div>}
    </main>
  );
}

LabsDataInsights.propTypes = {
  loadingLabsDataInsights: PropTypes.bool.isRequired,
  loadLabsDataInsights: PropTypes.func.isRequired,
  errorMessage: PropTypes.string,
  licenseError: PropTypes.string,
};
