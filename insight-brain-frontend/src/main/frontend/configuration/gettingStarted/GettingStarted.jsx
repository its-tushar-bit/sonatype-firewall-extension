/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { NxLoadWrapper } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { submitData, VISITED_ACTION } from './gettingStartedTelemetryServiceHelper';
import ProductLicenseSummary from './components/ProductLicenseSummary';
import GettingStartedDocLink from './components/GettingStartedDocLink';
import LearningTopics from './components/LearningTopics';
import SystemSetup from './components/SystemSetup';

const firePageVisitedEvent = (prevPage) => submitData(VISITED_ACTION, {}, prevPage);

export default function GettingStarted({
  load,
  loading,
  loadError,
  license,
  isAuthorizedToViewSystemSetup,
  shouldDisplayHdsUnreachable,
  hdsUnreachableErrorMessage,
  hdsUnreachableIncidentId,
  prevState: prevPage,
  isAdmin,
  tenantMode,
}) {
  useEffect(() => {
    load();
    firePageVisitedEvent(prevPage.url);
  }, []);

  return (
    <section id="getting-started" className="nx-page-main">
      <header className="nx-page-title iq-page-title">
        <h1 className="nx-h1">Welcome to IQ Server</h1>
        <div className="nx-page-title__description">
          <p className="nx-p">You can access this page anytime from the help menu.</p>
        </div>
      </header>
      <NxLoadWrapper loading={loading} retryHandler={load} error={loadError}>
        {shouldDisplayHdsUnreachable && (
          <section className="iq-tile iq-alert" id="hds-unreachable-warning">
            <div className="iq-tile-header">
              <div className="iq-tile-header__title">
                <i className="iq-tile-header__icon fa fa-warning"></i>
                <h2>Sonatype Data Services unreachable</h2>
              </div>
            </div>
            <div className="iq-tile-content">{hdsUnreachableErrorMessage}</div>
            <div className="iq-tile-content visual-testing-ignore">
              Search server logs for the following ID for error details: {hdsUnreachableIncidentId}
            </div>
            <div id="connectivity-requirements" className="iq-tile-content">
              Review the{' '}
              <GettingStartedDocLink
                href="http://links.sonatype.com/products/nxiq/doc/requirements"
                text="documentation"
              />
              for connectivity requirements.
            </div>
          </section>
        )}

        {license && isAdmin && <ProductLicenseSummary license={license} tenantMode={tenantMode} />}

        {isAuthorizedToViewSystemSetup && <SystemSetup tenantMode={tenantMode} />}

        <LearningTopics tenantMode={tenantMode} />
      </NxLoadWrapper>
    </section>
  );
}

GettingStarted.propTypes = {
  load: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  isAdmin: PropTypes.bool.isRequired,
  loading: PropTypes.bool.isRequired,
  license: PropTypes.object,
  shouldDisplayHdsUnreachable: PropTypes.bool.isRequired,
  hdsUnreachableIncidentId: PropTypes.string,
  hdsUnreachableErrorMessage: PropTypes.string,
  isAuthorizedToViewSystemSetup: PropTypes.bool.isRequired,
  prevState: PropTypes.object,
  tenantMode: PropTypes.string,
};
