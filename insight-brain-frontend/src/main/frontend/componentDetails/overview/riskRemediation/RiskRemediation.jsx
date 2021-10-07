/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';

import { NxLoadWrapper, NxModal, NxLoadError, NxButton } from '@sonatype/react-shared-components';
import { CompareVersions } from './CompareVersions';
import { DependencyInformation } from './DependencyInformation';
import { VersionExplorer } from './VersionExplorer';
import { RecommendedVersions } from './RecommendedVersions';
import { AncestorPropTypes, RemediationPropTypes } from '../overviewTypes';

export const RiskRemediation = ({
  ancestors,
  stageId,
  versionExplorerData,
  selectedVersionData,
  currentVersion,
  loadVersionExplorerData,
  loadSelectedVersionData,
  currentVersionComparisonData,
  selectedVersionComparisonData,
  resetSelectedVersionData,
  ancestorOnClick,
}) => {
  useEffect(() => {
    loadVersionExplorerData();
  }, []);

  const { loading, loadError, remediation, versions } = versionExplorerData;
  const { loading: selectedVersionLoading, loadError: selectedVersionError, selectedVersion } = selectedVersionData;

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
        <RecommendedVersions actualVersion={currentVersion} stageId={stageId} remediation={remediation} />
      </div>
      <div className="nx-grid-col nx-grid-col--50">
        <div className="nx-grid-row">
          <div className="nx-grid-col iq-grid-col--100">
            <VersionExplorer
              versions={versions}
              currentVersion={currentVersion}
              versionClick={loadSelectedVersionData}
              selectedVersionError={selectedVersionError}
            />
          </div>
        </div>
        <div className="nx-grid-row">
          <div className="nx-grid-col iq-grid-col--100">
            {currentVersionComparisonData && (
              <CompareVersions
                currentVersion={currentVersionComparisonData}
                selectedVersion={selectedVersionComparisonData}
                loading={selectedVersionLoading}
                error={selectedVersionError}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );

  const overviewComponentRiskRemediationTile_contentTransitiveDependency = () => (
    <Fragment>
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">
          <DependencyInformation ancestors={ancestors} ancestorOnClick={ancestorOnClick} />
        </div>
        <div className="nx-grid-col nx-grid-col--50">
          <VersionExplorer
            versions={versions}
            currentVersion={currentVersion}
            versionClick={loadSelectedVersionData}
            selectedVersionError={selectedVersionError}
          />
        </div>
      </div>
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">
          <RecommendedVersions actualVersion={currentVersion} stageId={stageId} remediation={remediation} />
        </div>
        <div className="nx-grid-col nx-grid-col--50">
          {currentVersionComparisonData && (
            <CompareVersions
              currentVersion={currentVersionComparisonData}
              selectedVersion={selectedVersionComparisonData}
              loading={selectedVersionLoading}
              error={selectedVersionError}
            />
          )}
        </div>
      </div>
    </Fragment>
  );

  const selectedVersionLoadErrorModal = (
    <NxModal
      id="selected-version-error-modal"
      onCancel={resetSelectedVersionData}
      variant="narrow"
      aria-labelledby="modal-narrow-header"
    >
      <header className="nx-modal-header">
        <h2 className="nx-h2" id="modal-narrow-header">
          <span>Error loading component details for version {selectedVersion}</span>
        </h2>
      </header>
      <div className="nx-modal-content">
        <NxLoadError error={selectedVersionError} />
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton onClick={resetSelectedVersionData}>Close</NxButton>
        </div>
      </footer>
    </NxModal>
  );

  const content =
    ancestors && ancestors.length
      ? overviewComponentRiskRemediationTile_contentTransitiveDependency()
      : overviewComponentRiskRemediationTile_contentDirectDependency();

  return (
    <section id="overview-component-risk-remediation-tile" className="nx-tile iq-component-risk-remediation-tile">
      {overviewComponentRiskRemediationTile_header}
      {selectedVersionError && selectedVersionLoadErrorModal}
      <div className="nx-tile-content">
        <NxLoadWrapper loading={loading} retryHandler={loadVersionExplorerData} error={loadError}>
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
  loadSelectedVersionData: PropTypes.func,
  resetSelectedVersionData: PropTypes.func,
  routeName: PropTypes.string.isRequired,
  currentVersionComparisonData: PropTypes.object,
  selectedVersionComparisonData: PropTypes.object,
  ancestorOnClick: PropTypes.func,
  versionExplorerData: PropTypes.shape({
    versions: PropTypes.array,
    remediation: RemediationPropTypes,
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
  selectedVersionData: PropTypes.shape({
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
};
