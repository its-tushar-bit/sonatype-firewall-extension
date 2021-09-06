/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React, { useEffect } from 'react';
import { renderVersionGraph } from '@sonatype/version-graph';

const VersionGraphExplorer = ({
  data,
  selectable,
  showDetails,
  showCurrentVersionLabel,
  versionClick,
  versionDblClick,
}) => {
  useEffect(() => {
    renderVersionGraph({
      data,
      selectable,
      showDetails,
      showCurrentVersionLabel,
      versionClick,
      versionDblClick,
    });
  }, []);
  return (
    <div id="aiVersionChartContainer">
      <div id="aiVersionChartLabels"></div>
      <div id="aiVersionChartViz"></div>
    </div>
  );
};
export const GraphExplorerTypes = {
  data: PropTypes.shape({
    versions: PropTypes.array.isRequired,
    version: PropTypes.string.isRequired,
  }).isRequired,
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
