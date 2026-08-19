/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { renderVersionGraph } from '@sonatype/version-graph';
import { NxH3, NxP } from '@sonatype/react-shared-components';

import { pathSet } from 'MainRoot/util/jsUtil';
import {
  selectAllVersions,
  selectComponentProperties,
  selectCurrentComponentIdentifier,
  selectCurrentVersion,
} from '../slices/componentsSlice';
import { selectSelectedApplication } from '../slices/applicationsSlice';
import { setVersion } from '../slices/setVersion';
import getViewDetailsUrl from '../getViewDetailsUrl';

import './VersionGraph.scss';

/**
 * Renders the version graph visualization
 */
export default function VersionGraph() {
  const dispatch = useDispatch();
  const componentDetailsList = useSelector(selectAllVersions);
  const currentVersion = useSelector(selectCurrentVersion);
  const appId = useSelector(selectSelectedApplication)?.publicId;
  const currentComponentIdentifier = useSelector(selectCurrentComponentIdentifier);
  const componentProperties = useSelector(selectComponentProperties);

  // Render the version graph when component details list changes
  useEffect(() => {
    if (componentDetailsList && currentVersion) {
      renderVersionGraph({
        data: {
          nextMajorRevisionIndex: componentDetailsList.nextMajorRevisionIndex,
          versions: componentDetailsList,
          version: currentVersion,
        },
        selectable: true,
        showCurrentVersionLabel: true,
        versionClick: handleVersionClick,
        versionDblClick: handleVersionDblClick,
      });
    }
  }, [componentDetailsList, currentVersion]);

  // Handle click on a version in the graph
  const handleVersionClick = (version) => {
    dispatch(setVersion(version));
  };

  // Handle double-click on a version in the graph
  const handleVersionDblClick = (version) => {
    const currentVersion = currentComponentIdentifier.coordinates.version;
    const isCurrentVersion = version === currentVersion;
    const url = getViewDetailsUrl(
      appId,
      isCurrentVersion ? componentProperties.hash : null,
      pathSet(['coordinates', 'version'], version, currentComponentIdentifier),
      isCurrentVersion
    );

    window.open(url, '_blank');
  };

  return (
    // Note: role should be redundant but RTL fails to implement it by default
    <section className="iq-version-graph-chart-section" aria-labelledby="version-graph-header" role="region">
      <header>
        <NxH3 id="version-graph-header">Version Graph</NxH3>
      </header>
      <NxP>Click on the graph below to see details about different versions</NxP>
      {/* Inner IDs are required by the version-graph library; outer scrollport is ours (CLM-44042). */}
      <div className="iq-version-graph-chart-section__scroll">
        <div className="iq-version-graph-chart-section__container" id="aiVersionChartContainer">
          <div className="iq-version-graph-chart-section__labels" id="aiVersionChartLabels"></div>
          <div className="iq-version-graph-chart-section__chart" id="aiVersionChartViz"></div>
        </div>
      </div>
    </section>
  );
}
