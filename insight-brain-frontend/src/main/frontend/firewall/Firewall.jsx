/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useEffect} from 'react';
import LoadWrapper from '../react/LoadWrapper';
import * as PropTypes from 'prop-types';

export default function Firewall(props) {
  // Actions
  const {
    loadStatus
  } = props;

  // viewState
  const {
    loadedStatus,
    loadStatusError
  } = props;

  // configurationState
  const {
    isEnabled
  } = props;

  const error = loadedStatus && !isEnabled ? 'The Firewall feature is disabled' : loadStatusError;

  useEffect(() => {
    loadStatus();
  }, []);

  return (
    <main id="firewall-page" className="nx-page-main">
      <LoadWrapper loading={!loadedStatus} error={error} retryHandler={loadStatus}>
        <div className="nx-page-title">
          <h1 className="nx-h1" id="firewall-page-title">Firewall</h1>
        </div>
      </LoadWrapper>
    </main>
  );
}

Firewall.propTypes = {
  loadStatus: PropTypes.func.isRequired,
  loadedStatus: PropTypes.bool.isRequired,
  loadStatusError: PropTypes.object,
  isEnabled: PropTypes.bool.isRequired
};
