/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

export default function QuarantinedComponentReport(props) {
  // Url parameter
  const { token } = props;

  // Actions
  const { loadComponent } = props;

  // viewState
  const { repositoryComponentId, loadError, dataLoading } = props;

  useEffect(() => {
    loadComponent(token);
  }, []);

  return (
    <main id="quarantined-component-report" className="nx-page-main">
      <NxLoadWrapper retryHandler={() => loadComponent(token)} error={loadError} loading={dataLoading}>
        <div>Token: {token}</div>
        <div>Repository Component Id: {repositoryComponentId}</div>
      </NxLoadWrapper>
    </main>
  );
}

QuarantinedComponentReport.propTypes = {
  token: PropTypes.string.isRequired,
  loadComponent: PropTypes.func.isRequired,
  repositoryComponentId: PropTypes.string.isRequired,
  loadError: PropTypes.string,
  dataLoading: PropTypes.bool.isRequired,
};
