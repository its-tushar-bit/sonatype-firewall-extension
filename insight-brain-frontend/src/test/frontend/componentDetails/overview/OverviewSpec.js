/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import Overview from '../../../../main/frontend/componentDetails/overview/Overview';

describe('ComponentDetailsOverview', () => {
  let minimalProps, getShallow;

  beforeEach(function () {
    minimalProps = {
      componentInformation: {
        displayName: {
          parts: [{ field: 'Name', value: 'componentname' }],
        },
        matchState: 'unknown',
        pathnames: ['componentPath'],
      },
    };

    getShallow = enzymeUtils.getShallowComponent(Overview, minimalProps);
  });

  it('renders a tile with 3 subsections as content', () => {
    const component = getShallow(),
      content = component.find('.nx-tile-content'),
      sections = content.find('section');

    expect(sections.length).toBe(2);
    expect(sections.at(0).find('header')).toHaveText('General Info');
    expect(sections.at(1).find('header')).toHaveText('Identification Info');
  });

  describe('when component is unknown', () => {
    it('only renders the display name in the General Info section', () => {
      const component = getShallow(),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        generalInfoSection = sections.at(0);

      const definitionItems = generalInfoSection.find('.iq-inline-definition-list__item');
      expect(definitionItems.length).toBe(2);

      const [formatLabel, formatValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(formatLabel).toHaveText('Type:');
      expect(formatValue).toHaveText('');

      const [displayNameLabel, displayNameValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(displayNameLabel).toHaveText('Name:');
      expect(displayNameValue).toHaveText('componentname');
    });

    it('only renders the match state as unknown in the Identification Info section', () => {
      const component = getShallow(),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        identificationInfoSection = sections.at(1);

      const definitionItems = identificationInfoSection.find('.iq-inline-definition-list__item');
      expect(definitionItems.length).toBe(5);

      const [catalogedDateLabel, catalogedDateValue] = [
        definitionItems.at(0).find('dt'),
        definitionItems.at(0).find('dd'),
      ];
      expect(catalogedDateLabel).toHaveText('Cataloged:');
      expect(catalogedDateValue).toHaveText('');

      const [matchStateLabel, matchStateValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(matchStateLabel).toHaveText('Match State:');
      expect(matchStateValue).toHaveText('unknown');

      const [OcurrencesLabel, OcurrencesValue] = [definitionItems.at(2).find('dt'), definitionItems.at(2).find('dd')];
      expect(OcurrencesLabel).toHaveText('Occurrences:');
      expect(OcurrencesValue).toHaveText('1 File Matches');

      const [IdentificationSourceLabel, IdentificationSourceValue] = [
        definitionItems.at(3).find('dt'),
        definitionItems.at(3).find('dd'),
      ];
      expect(IdentificationSourceLabel).toHaveText('Identification Source:');
      expect(IdentificationSourceValue).toHaveText('');

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category:');
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
      },
    };

    it('renders the format and display name in the General Info section', () => {
      const component = getShallow(knownComponentProps),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        generalInfoSection = sections.at(0);

      const definitionItems = generalInfoSection.find('.iq-inline-definition-list__item');
      expect(definitionItems.length).toBe(3);

      const [formatLabel, formatValue] = [definitionItems.at(0).find('dt'), definitionItems.at(0).find('dd')];
      expect(formatLabel).toHaveText('Type:');
      expect(formatValue).toHaveText('custom');

      const [displayNamePart0Label, displayNamePart0Value] = [
        definitionItems.at(1).find('dt'),
        definitionItems.at(1).find('dd'),
      ];
      expect(displayNamePart0Label).toHaveText('Artifact Id:');
      expect(displayNamePart0Value).toHaveText('componentArtifactID');

      const [displayNamePart2Label, displayNamePart2Value] = [
        definitionItems.at(2).find('dt'),
        definitionItems.at(2).find('dd'),
      ];
      expect(displayNamePart2Label).toHaveText('Version:');
      expect(displayNamePart2Value).toHaveText('v1.0.1');
    });

    it('renders all properties shown by the Identification Info section', () => {
      const component = getShallow(knownComponentProps),
        content = component.find('.nx-tile-content'),
        sections = content.find('section'),
        identificationInfoSection = sections.at(1);

      const definitionItems = identificationInfoSection.find('.iq-inline-definition-list__item');
      expect(definitionItems.length).toBe(5);

      const [catalogedDateLabel, catalogedDateValue] = [
        definitionItems.at(0).find('dt'),
        definitionItems.at(0).find('dd'),
      ];
      expect(catalogedDateLabel).toHaveText('Cataloged:');
      expect(catalogedDateValue).toHaveText('Less than a day ago');

      const [matchStateLabel, matchStateValue] = [definitionItems.at(1).find('dt'), definitionItems.at(1).find('dd')];
      expect(matchStateLabel).toHaveText('Match State:');
      expect(matchStateValue).toHaveText('exact');

      const [OcurrencesLabel, OcurrencesValue] = [definitionItems.at(2).find('dt'), definitionItems.at(2).find('dd')];
      expect(OcurrencesLabel).toHaveText('Occurrences:');
      expect(OcurrencesValue).toHaveText('2 File Matches');

      const [IdentificationSourceLabel, IdentificationSourceValue] = [
        definitionItems.at(3).find('dt'),
        definitionItems.at(3).find('dd'),
      ];
      expect(IdentificationSourceLabel).toHaveText('Identification Source:');
      expect(IdentificationSourceValue).toHaveText('clair');

      const [categoryLabel, categoryValue] = [definitionItems.at(4).find('dt'), definitionItems.at(4).find('dd')];
      expect(categoryLabel).toHaveText('Category:');
      expect(categoryValue).toHaveText('category1,category2');
    });
  });
});
