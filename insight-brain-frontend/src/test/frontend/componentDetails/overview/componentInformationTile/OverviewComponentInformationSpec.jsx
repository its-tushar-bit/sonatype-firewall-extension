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

describe('OverviewComponentInformation', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      loadInnerSourceProducerData: jasmine.createSpy('loadInnerSourceProducerData'),
      componentInformation: {
        displayName: {
          parts: [{ field: 'Name', value: 'componentname' }],
        },
        matchState: 'unknown',
        pathnames: ['componentPath'],
      },
      toggleShowOccurrencesPopover: jasmine.createSpy('toggleShowOccurrencesPopover'),
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

    getShallow = enzymeUtils.getShallowComponent(OverviewComponentInformation, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(OverviewComponentInformation, minimalProps);
  });

  it('calls `loadInnerSourceProducerData` when mounted', () => {
    const component = getMounted();
    expect(minimalProps.loadInnerSourceProducerData).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  it('renders a tile with 2 subsections as content', () => {
    const component = getShallow(),
      content = component.find('.nx-tile-content'),
      sections = content.find('section');

    expect(sections.length).toBe(2);
    expect(sections.at(0).find('header')).toHaveText('General Info');
    expect(sections.at(1).find('header')).toHaveText('Identification Info');
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

  describe('when component is unknown', () => {
    it('renders an OccurrencesPopoverContainer', () => {
      const component = getShallow();
      const occurrencesPopover = component.find(OccurrencesPopoverContainer.default);

      expect(occurrencesPopover).not.toBeNull();
    });
    it('only renders the display name in the General Info section', () => {
      const component = getShallow(),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        generalInfoSection = sections.at(0);

      const definitionItems = generalInfoSection.find('.nx-read-only__item');
      expect(definitionItems.length).toBe(2);

      const [formatLabel, formatValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(formatLabel).toHaveText('Type');
      expect(formatValue).toHaveText('');

      const [displayNameLabel, displayNameValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(displayNameLabel).toHaveText('Name');
      expect(displayNameValue).toHaveText('componentname');
    });

    it('only renders the match state as unknown in the Identification Info section', () => {
      const component = getShallow(),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        identificationInfoSection = sections.at(1);

      const definitionItems = identificationInfoSection.find('.nx-read-only__item');
      expect(definitionItems.length).toBe(5);

      const [catalogedDateLabel, catalogedDateValue] = [
        definitionItems.at(0).find('dt'),
        definitionItems.at(0).find('dd'),
      ];
      expect(catalogedDateLabel).toHaveText('Cataloged');
      expect(catalogedDateValue).toHaveText('');

      const [matchStateLabel, matchStateValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(matchStateLabel).toHaveText('Match State');
      expect(matchStateValue).toHaveText('unknown');

      const [OcurrencesLabel, OcurrencesValue] = [definitionItems.at(2).find('dt'), definitionItems.at(2).find('dd')];
      expect(OcurrencesLabel).toHaveText('Occurrences');
      expect(OcurrencesValue).toHaveText('1 File Matches');

      const [IdentificationSourceLabel, IdentificationSourceValue] = [
        definitionItems.at(3).find('dt'),
        definitionItems.at(3).find('dd'),
      ];
      expect(IdentificationSourceLabel).toHaveText('Identification Source');
      expect(IdentificationSourceValue).toHaveText('');

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category');
      expect(categoryValue).toHaveText('');
    });
  });

  describe('when component is known', () => {
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
      },
    };

    it('renders an OccurrencesPopoverContainer', () => {
      const component = getShallow(knownComponentProps);
      const occurrencesPopover = component.find(OccurrencesPopoverContainer.default);

      expect(occurrencesPopover).not.toBeNull();
    });

    it('renders the format and display name in the General Info section', () => {
      const component = getShallow(knownComponentProps),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        generalInfoSection = sections.at(0);

      const definitionItems = generalInfoSection.find('.nx-read-only__item');
      expect(definitionItems.length).toBe(3);

      const [formatLabel, formatValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(formatLabel).toHaveText('Type');
      expect(formatValue).toHaveText('custom');

      const [displayNamePart0Label, displayNamePart0Value] = [
        definitionItems.at(1).find('dt'),
        definitionItems.at(1).find('dd'),
      ];
      expect(displayNamePart0Label).toHaveText('Artifact Id');
      expect(displayNamePart0Value).toHaveText('componentArtifactID');

      const [displayNamePart2Label, displayNamePart2Value] = [
        definitionItems.at(2).find('dt'),
        definitionItems.at(2).find('dd'),
      ];
      expect(displayNamePart2Label).toHaveText('Version');
      expect(displayNamePart2Value).toHaveText('v1.0.1');
    });

    it('renders all properties shown by the Identification Info section', () => {
      const component = getShallow(knownComponentProps),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        identificationInfoSection = sections.at(1);

      const definitionItems = identificationInfoSection.find('.nx-read-only__item');
      expect(definitionItems.length).toBe(5);

      const [catalogedDateLabel, catalogedDateValue] = [
        definitionItems.at(0).find('dt'),
        definitionItems.at(0).find('dd'),
      ];
      expect(catalogedDateLabel).toHaveText('Cataloged');
      expect(catalogedDateValue).toHaveText('Less than a day ago');

      const [matchStateLabel, matchStateValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(matchStateLabel).toHaveText('Match State');
      expect(matchStateValue).toHaveText('exact');

      const [OcurrencesLabel, OcurrencesValue] = [definitionItems.at(2).find('dt'), definitionItems.at(2).find('dd')];
      expect(OcurrencesLabel).toHaveText('Occurrences');
      expect(OcurrencesValue).toHaveText('2 File Matches');

      const [IdentificationSourceLabel, IdentificationSourceValue] = [
        definitionItems.at(3).find('dt'),
        definitionItems.at(3).find('dd'),
      ];
      expect(IdentificationSourceLabel).toHaveText('Identification Source');
      expect(IdentificationSourceValue).toHaveText('clair');

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category');
      expect(categoryValue).toHaveText('category1,category2');
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
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        identificationInfoSection = sections.at(1),
        definitionItems = identificationInfoSection.find('.nx-read-only__item');

      const [matchStateLabel, matchStateValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(matchStateLabel).toHaveText('Match State');
      expect(matchStateValue).toHaveText('similar (View Similar Matches)');

      const viewSimilarMatchesLink = matchStateValue.find('a');
      expect(viewSimilarMatchesLink).toExist();
      viewSimilarMatchesLink.simulate('click');
      expect(minimalProps.toggleShowSimilarMatches).toHaveBeenCalled();
    });
  });
});
