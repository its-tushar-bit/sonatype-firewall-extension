/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import { NxLoadWrapper } from '@sonatype/react-shared-components';
import { CompareVersions } from './CompareVersions';
import { DependencyInformation } from './DependencyInformation';
import { VersionExplorer } from './VersionExplorer';
import { RecommendedVersions } from './RecommendedVersions';
import { AncestorPropTypes, RemediationPropTypes } from '../overviewTypes';

export const RiskRemediation = ({
  directDependency,
  ancestors,
  routeName,
  actualVersion,
  stageId,
  remediation,
  versionExplorerData,
  requestVersionGraphData,
}) => {
  useEffect(() => {
    requestVersionGraphData();
  }, []);

  const overviewComponentRiskRemediationTile_header = (
    <header className="nx-tile-header">
      <div className="nx-tile-header__title">
        <h2 className="nx-h2">Risk Remediation</h2>
      </div>
    </header>
  );

  const overviewComponentRiskRemediationTile_contentDirectDependency = (
    <div className="nx-tile-content">
      <NxLoadWrapper
        loading={versionExplorerData && versionExplorerData.loading}
        retryHandler={requestVersionGraphData}
        error={versionExplorerData.loadError}
      >
        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--50">
            <RecommendedVersions actualVersion={actualVersion} stageId={stageId} remediation={remediation} />
          </div>
          <div className="nx-grid-col nx-grid-col--50">
            <div className="nx-grid-row">
              <div className="nx-grid-col iq-grid-col--100">
                <VersionExplorer versionExplorerData={versionExplorerData} />
              </div>
            </div>
            <div className="nx-grid-row">
              <div className="nx-grid-col iq-grid-col--100">
                <CompareVersions />
              </div>
            </div>
          </div>
        </div>
      </NxLoadWrapper>
    </div>
  );

  const overviewComponentRiskRemediationTile_contentTransitiveDependency = (
    <div className="nx-tile-content">
      <NxLoadWrapper
        loading={versionExplorerData && versionExplorerData.loading}
        retryHandler={requestVersionGraphData}
        error={versionExplorerData.loadError}
      >
        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--50">
            <DependencyInformation routeName={routeName} ancestors={ancestors} />
          </div>
          <div className="nx-grid-col nx-grid-col--50">
            <VersionExplorer versionExplorerData={versionExplorerData} />
          </div>
        </div>
        <div className="nx-grid-row">
          <div className="nx-grid-col nx-grid-col--50">
            <RecommendedVersions actualVersion={actualVersion} stageId={stageId} remediation={remediation} />
          </div>
          <div className="nx-grid-col nx-grid-col--50">
            <CompareVersions />
          </div>
        </div>
      </NxLoadWrapper>
    </div>
  );

  if (directDependency) {
    return (
      <section id="overview-component-risk-remediation-tile" className="nx-tile iq-component-risk-remediation-tile">
        {overviewComponentRiskRemediationTile_header}
        {overviewComponentRiskRemediationTile_contentDirectDependency}
      </section>
    );
  } else {
    return (
      <section id="overview-component-risk-remediation-tile" className="nx-tile iq-component-risk-remediation-tile">
        {overviewComponentRiskRemediationTile_header}
        {overviewComponentRiskRemediationTile_contentTransitiveDependency}
      </section>
    );
  }
};

RiskRemediation.propTypes = {
  ancestors: PropTypes.arrayOf(AncestorPropTypes),
  directDependency: PropTypes.bool.isRequired,
  actualVersion: PropTypes.string.isRequired,
  stageId: PropTypes.string.isRequired,
  remediation: RemediationPropTypes,
  requestVersionGraphData: PropTypes.func,
  routeName: PropTypes.string.isRequired,
  versionExplorerData: PropTypes.shape({
    data: PropTypes.shape({
      version: PropTypes.string,
      versions: PropTypes.array,
    }),
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
};
