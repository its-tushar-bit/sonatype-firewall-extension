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
    loadingStatus,
    loadStatusError: loadErrorProp
  } = props;

  // configurationState
  const {
    isEnabled
  } = props;

  const loadStatusError = isEnabled ? loadErrorProp : 'The Firewall feature is disabled';

  useEffect(() => {
    loadStatus();
  }, []);

  return (
    <main id="firewall-page" className="nx-page-main">
      <LoadWrapper loading={loadingStatus} error={loadStatusError} retryHandler={loadStatus}>
        <div className="nx-page-title">
          <h1 className="nx-h1" id="firewall-page-title">Firewall</h1>
        </div>
      </LoadWrapper>
    </main>
  );
}

Firewall.propTypes = {
  loadStatus: PropTypes.func.isRequired,
  loadingStatus: PropTypes.bool.isRequired,
  loadStatusError: PropTypes.object,
  isEnabled: PropTypes.bool.isRequired
};
