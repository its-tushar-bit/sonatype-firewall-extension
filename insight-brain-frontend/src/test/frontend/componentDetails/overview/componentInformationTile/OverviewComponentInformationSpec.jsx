/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from 'TestRoot/enzymeUtils';

import OverviewComponentInformation from 'MainRoot/componentDetails/overview/componentInformationTile/OverviewComponentInformation';
import * as OccurrencesPopoverContainer from 'MainRoot/componentDetails/overview/occurrencesPopover/OccurrencesPopoverContainer';
import * as InnerSourceProducerAlertContainer from 'MainRoot/componentDetails/overview/InnerSourceProducerAlert/InnerSourceProducerAlertContainer';
import * as InnerSourceProducerReportModalContainer from 'MainRoot/componentDetails/overview/InnerSourceProducerReportModal/InnerSourceProducerReportModalContainer';
import * as InnerSourceProducerPermissionsModalContainer from 'MainRoot/componentDetails/overview/InnerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModalContainer';
import * as ComponentCoordinatesPopover from 'MainRoot/componentDetails/overview/ComponentCoordinatesPopover/ComponentCoordinatesPopover';
import { NxTextLink } from '@sonatype/react-shared-components';

describe('OverviewComponentInformation', () => {
  let minimalProps, getShallow, getMounted, toggleShowComponentCoordinatesPopoverSpy;

  beforeEach(function () {
    toggleShowComponentCoordinatesPopoverSpy = jasmine.createSpy('toggleShowComponentCoordinatesPopover');
    minimalProps = {
      loadInnerSourceProducerData: jasmine.createSpy('loadInnerSourceProducerData'),
      componentInformation: {
        displayName: {
          parts: [{ field: 'Name', value: 'componentname' }],
        },
        matchState: 'unknown',
        pathnames: ['componentPath'],
      },
      versionExplorerData: {},
      toggleShowOccurrencesPopover: jasmine.createSpy('toggleShowOccurrencesPopover'),
      toggleShowComponentCoordinatesPopover: toggleShowComponentCoordinatesPopoverSpy,
      similarMatches: [],
      toggleShowSimilarMatches: jasmine.createSpy('toggleShowSimilarMatches'),
    };

    spyOn(OccurrencesPopoverContainer, 'default').and.returnValue(<div>OccurrencesPopover</div>);
    spyOn(InnerSourceProducerAlertContainer, 'default').and.returnValue(<div>InnerSourceProducerAlertContainer</div>);
    spyOn(InnerSourceProducerReportModalContainer, 'default').and.returnValue(
      <div>InnerSourceProducerReportModalContainer</div>
    );
    spyOn(InnerSourceProducerPermissionsModalContainer, 'default').and.returnValue(
      <div>InnerSourceProducerPermissionsModalContainer</div>
    );
    spyOn(ComponentCoordinatesPopover, 'default').and.returnValue(<div>ComponentCoordinatesPopover</div>);

    getShallow = enzymeUtils.getShallowComponent(OverviewComponentInformation, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(OverviewComponentInformation, minimalProps);
  });

  it('calls `loadInnerSourceProducerData` when mounted', () => {
    const component = getMounted();
    expect(minimalProps.loadInnerSourceProducerData).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  it('renders a tile with content', () => {
    const component = getShallow(),
      content = component.find('.nx-tile-content');

    expect(content).toExist();
  });

  it('renders the inner source containers', () => {
    let component, innerSourceReportContainer, innerSourcePermissionsContainer, innerSourceAlertContainer;

    component = getShallow();
    innerSourceReportContainer = component.find(InnerSourceProducerReportModalContainer.default);
    innerSourcePermissionsContainer = component.find(InnerSourceProducerPermissionsModalContainer.default);
    innerSourceAlertContainer = component.find(InnerSourceProducerAlertContainer.default);

    expect(innerSourceReportContainer).toExist();
    expect(innerSourcePermissionsContainer).toExist();
    expect(innerSourceAlertContainer).toExist();

    const repositorySourceAlert = component.find('.inner-source-repository-source-alert');
    expect(repositorySourceAlert).not.toExist();

    const props = {
      componentInformation: {
        ...minimalProps.componentInformation,
        matchState: 'exact',
        componentIdentifier: {
          format: 'npm',
        },
      },
    };
    component = getShallow(props);
    innerSourceReportContainer = component.find(InnerSourceProducerReportModalContainer.default);
    innerSourcePermissionsContainer = component.find(InnerSourceProducerPermissionsModalContainer.default);
    innerSourceAlertContainer = component.find(InnerSourceProducerAlertContainer.default);

    expect(innerSourceReportContainer).toExist();
    expect(innerSourcePermissionsContainer).toExist();
    expect(innerSourceAlertContainer).toExist();
  });

  it('renders the repository source alert for an inner source component', () => {
    const props = {
      versionExplorerData: {
        sourceResponse: {
          source: 'source',
          sourceMessage: 'message',
        },
      },
    };

    const component = getShallow(props);
    const repositorySourceAlert = component.find('.inner-source-repository-source-alert');
    expect(repositorySourceAlert).toExist();
    expect(repositorySourceAlert).toHaveText(props.versionExplorerData.sourceResponse.sourceMessage);
  });

  describe('when component is unknown', () => {
    it('renders an OccurrencesPopoverContainer', () => {
      const component = getShallow();
      const occurrencesPopover = component.find(OccurrencesPopoverContainer.default);

      expect(occurrencesPopover).not.toBeNull();
    });

    it('only renders the match state as unknown in the Identification Info section', () => {
      const component = getShallow(),
        definitionList = component.find('.iq-identification-info-definition-list'),
        definitionItems = definitionList.find('.nx-read-only__item');

      expect(definitionItems.length).toBe(5);

      const [matchStateLabel, matchStateValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(matchStateLabel).toHaveText('Match State');
      expect(matchStateValue).toHaveText('Unknown');

      const [IdentificationSourceLabel, IdentificationSourceValue] = [
        definitionItems.at(1).find('dt'),
        definitionItems.at(1).find('dd'),
      ];
      expect(IdentificationSourceLabel).toHaveText('Identification Source');
      expect(IdentificationSourceValue).toHaveText('');

      const [OcurrencesLabel, OcurrencesValue] = [definitionItems.at(2).find('dt'), definitionItems.at(2).find('dd')];
      expect(OcurrencesLabel).toHaveText('Occurrences');
      expect(OcurrencesValue).toHaveText('1 File');

      const [websiteLabel, websiteValue] = [definitionItems.at(3).find('dt'), definitionItems.at(3).find('dd')];
      expect(websiteLabel).toHaveText('Website');
      expect(websiteValue).toHaveText('');

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category');
      expect(categoryValue).toHaveText('');
    });
  });

  describe('when component is known', () => {
    let knownComponentProps;
    beforeEach(() => {
      knownComponentProps = {
        componentInformation: {
          componentIdentifier: {
            format: 'custom',
          },
          displayName: {
            parts: [
              { field: 'Artifact Id', value: 'componentArtifactID' },
              { value: ' , ' },
              { field: 'Version', value: 'v1.0.1' },
            ],
          },
          createTime: new Date().getTime() - 100 /* forcing a date less than a day ago */,
          matchState: 'exact',
          identificationSource: 'clair',
          componentCategories: [{ path: 'category1' }, { path: 'category2' }],
          pathnames: ['knownComponentPath', 'knownComponentPath2'],
          similarMatches: [],
          website: 'websitelink.com',
        },
      };
    });

    it('renders an OccurrencesPopoverContainer', () => {
      const component = getShallow(knownComponentProps);
      const occurrencesPopover = component.find(OccurrencesPopoverContainer.default);

      expect(occurrencesPopover).not.toBeNull();
    });

    it('renders all properties shown by the Identification Info section', () => {
      const component = getShallow(knownComponentProps),
        definitionList = component.find('.iq-identification-info-definition-list');

      const definitionItems = definitionList.find('.nx-read-only__item');
      expect(definitionItems.length).toBe(5);

      const [matchStateLabel, matchStateValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(matchStateLabel).toHaveText('Match State');
      expect(matchStateValue).toHaveText('Exact');

      const [IdentificationSourceLabel, IdentificationSourceValue] = [
        definitionItems.at(1).find('dt'),
        definitionItems.at(1).find('dd'),
      ];
      expect(IdentificationSourceLabel).toHaveText('Identification Source');
      expect(IdentificationSourceValue).toHaveText('clair');

      const [OcurrencesLabel, OcurrencesValue] = [definitionItems.at(2).find('dt'), definitionItems.at(2).find('dd')];
      expect(OcurrencesLabel).toHaveText('Occurrences');
      expect(OcurrencesValue).toHaveText('2 Files');

      const [websiteLabel, websiteValue] = [definitionItems.at(3).find('dt'), definitionItems.at(3).find(NxTextLink)];
      expect(websiteLabel).toHaveText('Website');
      expect(websiteValue).toHaveText('Visit Project Website');
      expect(websiteValue).toHaveProp('href', 'websitelink.com');

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category');
      expect(categoryValue).toHaveText('category1,category2');
    });

    it('renders empty website', () => {
      knownComponentProps.componentInformation.website = undefined;
      const component = getShallow(knownComponentProps),
        definitionList = component.find('.iq-identification-info-definition-list');

      const definitionItems = definitionList.find('.nx-read-only__item');

      const [websiteLabel, websiteValue] = [definitionItems.at(3).find('dt'), definitionItems.at(3).find('dd')];
      expect(websiteLabel).toHaveText('Website');
      expect(websiteValue).toHaveText('');
    });
  });

  describe('when component is a similar match', () => {
    it('renders a link that will trigger the opening of the similar matches popover', () => {
      const similarComponentProps = {
        ...minimalProps,
        componentInformation: {
          componentIdentifier: {
            format: 'custom',
          },
          displayName: {
            parts: [
              { field: 'Artifact Id', value: 'componentArtifactID' },
              { value: ' , ' },
              { field: 'Version', value: 'v1.0.1' },
            ],
          },
          matchState: 'similar',
          pathnames: ['componentPath'],
        },
        similarMatches: ['bestMatch', 'otherMatch'],
      };

      const component = getShallow(similarComponentProps),
        definitionList = component.find('.iq-identification-info-definition-list'),
        definitionItems = definitionList.find('.nx-read-only__item');

      const [matchStateLabel, matchStateValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(matchStateLabel).toHaveText('Match State');
      expect(matchStateValue).toHaveText('Similar (View Similar Matches)');

      const viewSimilarMatchesLink = matchStateValue.find('a');
      expect(viewSimilarMatchesLink).toExist();
      viewSimilarMatchesLink.simulate('click');
      expect(minimalProps.toggleShowSimilarMatches).toHaveBeenCalled();
    });
  });

  describe('when an known component does not have categories', () => {
    const knownComponentProps = {
      componentInformation: {
        componentIdentifier: {
          format: 'custom',
        },
        displayName: {
          parts: [
            { field: 'Artifact Id', value: 'componentArtifactID' },
            { value: ' , ' },
            { field: 'Version', value: 'v1.0.1' },
          ],
        },
        createTime: new Date().getTime() - 100 /* forcing a date less than a day ago */,
        matchState: 'exact',
        identificationSource: 'clair',
        componentCategories: [],
        pathnames: ['knownComponentPath', 'knownComponentPath2'],
        similarMatches: [],
      },
    };

    it('renders "Other" in category field', () => {
      const component = getShallow(knownComponentProps),
        definitionList = component.find('.iq-identification-info-definition-list'),
        definitionItems = definitionList.find('.nx-read-only__item');

      expect(definitionItems.length).toBe(5);

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category');
      expect(categoryValue).toHaveText('Other');
    });
  });

  it('calls toggleShowComponentCoordinatesPopover', () => {
    const knownComponentProps = {
      componentInformation: {
        componentIdentifier: {
          format: 'custom',
        },
        displayName: {
          parts: [
            { field: 'Artifact Id', value: 'componentArtifactID' },
            { value: ' , ' },
            { field: 'Version', value: 'v1.0.1' },
          ],
        },
        createTime: new Date().getTime() - 100 /* forcing a date less than a day ago */,
        matchState: 'exact',
        identificationSource: 'clair',
        componentCategories: [{ path: 'category1' }, { path: 'category2' }],
        pathnames: ['knownComponentPath', 'knownComponentPath2'],
        similarMatches: [],
        website: 'websitelink.com',
      },
    };
    const component = getShallow(knownComponentProps),
      button = component.find('.component-coordinates-button');

    button.simulate('click');

    expect(toggleShowComponentCoordinatesPopoverSpy).toHaveBeenCalledTimes(1);
  });

  it('Component Coordinates Popover is not visible if component is unknown', () => {
    const component = getShallow(),
      button = component.find('.component-coordinates-button');

    expect(button).not.toExist();
  });
});
