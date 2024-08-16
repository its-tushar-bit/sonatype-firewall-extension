/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React, { useEffect } from 'react';
import { renderVersionGraph, selectVersion } from '@sonatype/version-graph';

const VersionGraphExplorer = ({
  versions,
  currentVersion,
  selectable,
  showDetails,
  showCurrentVersionLabel,
  versionClick,
  versionDblClick,
  selectedVersionError,
}) => {
  useEffect(() => {
    renderVersionGraph({
      data: {
        versions,
        version: currentVersion,
      },
      selectable,
      showDetails,
      showCurrentVersionLabel,
      versionClick,
      versionDblClick,
    });
  }, []);

  useEffect(() => {
    if (selectedVersionError) {
      selectVersion(null);
    }
  }, [selectedVersionError]);

  return (
    <div id="aiVersionChartContainer" data-testid="aiVersionChartContainer">
      <div id="aiVersionChartLabels"></div>
      <div id="aiVersionChartViz"></div>
    </div>
  );
};
export const GraphExplorerTypes = {
  versions: PropTypes.array.isRequired,
  currentVersion: PropTypes.string.isRequired,
  selectable: PropTypes.bool,
  showCurrentVersionLabel: PropTypes.bool,
  showDetails: PropTypes.bool,
  versionClick: PropTypes.func,
  versionDblClick: PropTypes.func,
};
VersionGraphExplorer.propTypes = GraphExplorerTypes;

VersionGraphExplorer.defaultProps = {
  selectable: true,
  showCurrentVersionLabel: true,
  showDetails: true,
  versionClick: () => {
    // This is intentional as a fallback function
  },
  versionDblClick: () => {
    // This is intentional as a fallback function
  },
};

export default VersionGraphExplorer;
