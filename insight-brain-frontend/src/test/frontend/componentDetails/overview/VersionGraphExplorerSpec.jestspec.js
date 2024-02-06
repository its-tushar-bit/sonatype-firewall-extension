/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
jest.mock('@sonatype/version-graph', () => ({
  renderVersionGraph: jest.fn(),
  selectVersion: jest.fn(),
}));

import 'jest-enzyme';
import * as enzymeUtils from '../../enzymeUtils';
import { renderVersionGraph, selectVersion } from '@sonatype/version-graph';
import VersionGraphExplorer from '../../../../main/frontend/componentDetails/overview/VersionGraphExplorer/VersionGraphExplorer';

describe('VersionGraphExplorer', () => {
  let minimalProps, getShallow, getMounted;
  beforeEach(function () {
    minimalProps = {
      versions: [],
      currentVersion: '2',
      selectable: false,
      showDetails: true,
      showCurrentVersionLabel: true,
      versionClick: jest.fn(),
      versionDblClick: jest.fn(),
      selectedVersionError: null,
    };

    getShallow = enzymeUtils.getShallowComponent(VersionGraphExplorer, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(VersionGraphExplorer, minimalProps);
  });

  it('renders the containers for the explorer', () => {
    const component = getShallow(),
      container = component.find('#aiVersionChartContainer'),
      labels = component.find('#aiVersionChartLabels'),
      viz = component.find('#aiVersionChartViz');

    expect(container).toExist();
    expect(labels).toExist();
    expect(viz).toExist();
  });

  it('calls the renderVersionGraph method when mounted', () => {
    getMounted();
    expect(renderVersionGraph).toHaveBeenCalledTimes(1);
    expect(renderVersionGraph).toHaveBeenCalledWith({
      data: {
        versions: [],
        version: '2',
      },
      selectable: false,
      showDetails: true,
      showCurrentVersionLabel: true,
      versionClick: minimalProps.versionClick,
      versionDblClick: minimalProps.versionDblClick,
    });
  });

  it('calls the selectVersion method when selectedVersionError exists', () => {
    getMounted({ selectedVersionError: 'error' });

    expect(selectVersion).toHaveBeenCalledTimes(1);
    expect(selectVersion).toHaveBeenCalledWith(null);
  });
});
