/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';

const authErrorMessage = `It appears you do not have permission to access this page.
    If you believe this to be incorrect, please contact your administrator.`;

export default function LabsDataInsights(props) {
  const {
        loadLabsDataInsights,
        errorMessage,
        loadingLabsDataInsights,
        isAuthorized
      } = props,
      loadError = isAuthorized ? errorMessage : authErrorMessage;

  function load() {
    if (isAuthorized) {
      return loadLabsDataInsights();
    }
  }

  useEffect(() => {
    if (!loadError) {
      load();
    }
  }, []);

  return (
    <React.Fragment>
      {loadError &&
      <main id="labs-data-insights-container" className="nx-page-main">
        <LoadWrapper loading={loadingLabsDataInsights} error={loadError}
                     retryHandler={() => load()} />
      </main>}
      {/* Putting the labs-container div outside the load wrapper as we load a script in data insights actions and need
      this visible for the script to append an iframe before loading is finished */}
      {!loadError && <div id="labs-container"></div>}
    </React.Fragment>
  );
}

LabsDataInsights.propTypes = {
  loadingLabsDataInsights: PropTypes.bool.isRequired,
  loadLabsDataInsights: PropTypes.func.isRequired,
  errorMessage: PropTypes.string,
  isAuthorized: PropTypes.bool.isRequired
};
