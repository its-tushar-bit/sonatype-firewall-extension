/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';

import { NxLoadWrapper } from '@sonatype/react-shared-components';
import { CompareVersions } from './CompareVersions';
import { DependencyInformation } from './DependencyInformation';
import { VersionExplorer } from './VersionExplorer';
import { RecommendedVersions } from './RecommendedVersions';
import { AncestorPropTypes, RemediationPropTypes } from '../overviewTypes';

export const RiskRemediation = ({
  ancestors,
  routeName,
  stageId,
  versionExplorerData,
  currentVersion,
  loadVersionExplorerData,
}) => {
  useEffect(() => {
    loadVersionExplorerData();
  }, []);

  const overviewComponentRiskRemediationTile_header = (
    <header className="nx-tile-header">
      <div className="nx-tile-header__title">
        <h2 className="nx-h2">Risk Remediation</h2>
      </div>
    </header>
  );

  const overviewComponentRiskRemediationTile_contentDirectDependency = () => (
    <div className="nx-grid-row">
      <div className="nx-grid-col nx-grid-col--50">
        <RecommendedVersions
          actualVersion={currentVersion}
          stageId={stageId}
          remediation={versionExplorerData.remediation}
        />
      </div>
      <div className="nx-grid-col nx-grid-col--50">
        <div className="nx-grid-row">
          <div className="nx-grid-col iq-grid-col--100">
            <VersionExplorer versions={versionExplorerData.versions} currentVersion={currentVersion} />
          </div>
        </div>
        <div className="nx-grid-row">
          <div className="nx-grid-col iq-grid-col--100">
            <CompareVersions />
          </div>
        </div>
      </div>
    </div>
  );

  const overviewComponentRiskRemediationTile_contentTransitiveDependency = () => (
    <Fragment>
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">
          <DependencyInformation routeName={routeName} ancestors={ancestors} />
        </div>
        <div className="nx-grid-col nx-grid-col--50">
          <VersionExplorer versions={versionExplorerData.versions} currentVersion={currentVersion} />
        </div>
      </div>
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">
          <RecommendedVersions
            actualVersion={currentVersion}
            stageId={stageId}
            remediation={versionExplorerData.remediation}
          />
        </div>
        <div className="nx-grid-col nx-grid-col--50">
          <CompareVersions />
        </div>
      </div>
    </Fragment>
  );

  const content =
    ancestors && ancestors.length
      ? overviewComponentRiskRemediationTile_contentTransitiveDependency()
      : overviewComponentRiskRemediationTile_contentDirectDependency();

  return (
    <section id="overview-component-risk-remediation-tile" className="nx-tile iq-component-risk-remediation-tile">
      {overviewComponentRiskRemediationTile_header}
      <div className="nx-tile-content">
        <NxLoadWrapper
          loading={versionExplorerData && versionExplorerData.loading}
          retryHandler={loadVersionExplorerData}
          error={versionExplorerData.loadError}
        >
          {content}
        </NxLoadWrapper>
      </div>
    </section>
  );
};

RiskRemediation.propTypes = {
  ancestors: PropTypes.arrayOf(AncestorPropTypes),
  currentVersion: PropTypes.string.isRequired,
  stageId: PropTypes.string.isRequired,
  loadVersionExplorerData: PropTypes.func,
  routeName: PropTypes.string.isRequired,
  versionExplorerData: PropTypes.shape({
    versions: PropTypes.array,
    remediation: RemediationPropTypes,
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
};
