/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../enzymeUtils';

import Overview from 'MainRoot/componentDetails/overview/Overview';
import * as OverviewComponentInformationTile from 'MainRoot/componentDetails/overview/componentInformationTile/OverviewComponentInformationTile';
import * as SimilarMatchesPopoverContainer from 'MainRoot/componentDetails/overview/SimilarMatchesPopoover/SimilarMatchesPopoverContainer';
import * as RiskRemediationContainer from 'MainRoot/componentDetails/overview/riskRemediation/RiskRemediationContainer';

describe('ComponentDetailsOverview', () => {
  let minimalProps, overviewTileProps, getShallow, loadInnerSourceProducerDataSpy;

  beforeEach(function () {
    loadInnerSourceProducerDataSpy = jasmine.createSpy('loadInnerSourceProducerData');

    spyOn(RiskRemediationContainer, 'RiskRemediationContainer').and.returnValue(<div>RiskRemediationContainer</div>);
    spyOn(SimilarMatchesPopoverContainer, 'default').and.returnValue(<div>Similar Matches Popover</div>);
    spyOn(OverviewComponentInformationTile, 'default').and.returnValue(<div>OverviewComponentInformationTile</div>);

    overviewTileProps = {
      loading: false,
      loadReport: () => {},
      componentInformation: {
        displayName: {
          parts: [{ field: 'Name', value: 'componentname' }],
        },
        matchState: 'unknown',
        pathnames: ['componentPath'],
      },
      toggleShowSimilarMatches: () => {},
      toggleShowOccurrencesPopover: () => {},
      similarMatches: [],
    };
    minimalProps = {
      ...overviewTileProps,
      loadInnerSourceProducerData: loadInnerSourceProducerDataSpy,
    };

    getShallow = enzymeUtils.getShallowComponent(Overview, minimalProps);
  });

  it('renders a OverviewComponentInformationTile with appropriate props', () => {
    const component = getShallow();
    const componentInfoTile = component.find(OverviewComponentInformationTile.default);

    expect(componentInfoTile).toHaveProp(overviewTileProps);
  });

  it('renders the risk remediation container if component is known', () => {
    let component, riskContainer;

    component = getShallow();
    riskContainer = component.find(RiskRemediationContainer.RiskRemediationContainer);
    expect(riskContainer).not.toExist();

    const props = {
      componentInformation: {
        matchState: 'exact',
      },
    };
    component = getShallow(props);
    riskContainer = component.find(RiskRemediationContainer.RiskRemediationContainer);
    expect(riskContainer).toExist();
  });

  it('renders the SimilarMatchesPopoverContainer if component is known', () => {
    let component, similarMatchesContainer;

    component = getShallow();
    similarMatchesContainer = component.find(SimilarMatchesPopoverContainer.default);
    expect(similarMatchesContainer).not.toExist();

    const props = {
      componentInformation: {
        matchState: 'exact',
      },
    };
    component = getShallow(props);
    similarMatchesContainer = component.find(SimilarMatchesPopoverContainer.default);
    expect(similarMatchesContainer).toExist();
  });
});
