/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';

import { findIndex } from 'ramda';
import { selectVersion } from '@sonatype/version-graph';
import { NxLoadWrapper, NxModal, NxLoadError, NxButton } from '@sonatype/react-shared-components';
import { CompareVersions } from './CompareVersions';
import { RecommendedRemediation } from './RecommendedRemediation';
import { VersionExplorer } from './VersionExplorer';
import { RecommendedVersions } from './RecommendedVersions';
import { componentInformationPropType, RemediationPropTypes } from '../overviewTypes';
import { dependencyTreeNodePropType } from 'MainRoot/DependencyTree/DependencyTree';

export const RiskRemediation = ({
  componentInformation,
  dependencyTreeSubset,
  dependencyTreeIsNotSupported,
  stageId,
  versionExplorerData,
  selectedVersionData,
  currentVersion,
  loadVersionExplorerData,
  loadSelectedVersionData,
  currentVersionComparisonData,
  selectedVersionComparisonData,
  resetSelectedVersionData,
  expanded,
  ancestorOnClick,
  toggleAncestorsList,
}) => {
  useEffect(() => {
    loadVersionExplorerData();
  }, []);

  const { loading, loadError, remediation, versions, sourceResponse } = versionExplorerData;
  const { loading: selectedVersionLoading, loadError: selectedVersionError, selectedVersion } = selectedVersionData;
  const source = sourceResponse ? sourceResponse.source : null;
  const isTransitiveDependency = componentInformation.dependencyInfo?.isDirectDependency === false;
  const overviewComponentRiskRemediationTile_header = (
    <header className="nx-tile-header">
      <div className="nx-tile-header__title">
        <h2 className="nx-h2">Risk Remediation</h2>
      </div>
    </header>
  );

  const handleCompare = (version) => {
    const idx = findIndex((v) => v?.componentIdentifier?.coordinates?.version === version, versions);
    if (idx !== -1) {
      selectVersion(idx);
      loadSelectedVersionData(version);
    }
  };

  const content = (
    <Fragment>
      <div className="nx-grid-row">
        <div className="nx-grid-col nx-grid-col--50">
          {isTransitiveDependency && (
            <RecommendedRemediation
              dependencyTreeSubset={dependencyTreeSubset}
              dependencyTreeIsNotSupported={dependencyTreeIsNotSupported}
              ancestorOnClick={ancestorOnClick}
              toggleAncestorsList={toggleAncestorsList}
              expanded={expanded}
            />
          )}
          <RecommendedVersions
            actualVersion={currentVersion}
            stageId={stageId}
            remediation={remediation}
            handleCompare={handleCompare}
          />
        </div>
        <div className="nx-grid-col nx-grid-col--50">
          <VersionExplorer
            versions={versions}
            source={source}
            currentVersion={currentVersion}
            versionClick={loadSelectedVersionData}
            selectedVersionError={selectedVersionError}
          />
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
  componentInformation: PropTypes.objectOf(componentInformationPropType),
  dependencyTreeSubset: PropTypes.arrayOf(dependencyTreeNodePropType),
  dependencyTreeIsNotSupported: PropTypes.bool,
  currentVersion: PropTypes.string.isRequired,
  stageId: PropTypes.string.isRequired,
  loadVersionExplorerData: PropTypes.func,
  loadSelectedVersionData: PropTypes.func,
  resetSelectedVersionData: PropTypes.func,
  routeName: PropTypes.string.isRequired,
  currentVersionComparisonData: PropTypes.object,
  selectedVersionComparisonData: PropTypes.object,
  ancestorOnClick: PropTypes.func,
  toggleAncestorsList: PropTypes.func,
  expanded: PropTypes.bool,
  versionExplorerData: PropTypes.shape({
    versions: PropTypes.array,
    remediation: RemediationPropTypes,
    sourceResponse: PropTypes.shape({
      source: PropTypes.string,
      sourceMessage: PropTypes.string,
    }),
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
  selectedVersionData: PropTypes.shape({
    loading: PropTypes.bool,
    loadError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
};
