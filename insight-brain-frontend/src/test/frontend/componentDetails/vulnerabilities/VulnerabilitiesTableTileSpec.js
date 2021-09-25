/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../enzymeUtils';
import VulnerabilitiesTableTile from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableTile';
import VulnerabilitiesTable from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTable';

describe('VulnerabilitiesTableTile', () => {
  let minimalProps, getShallow, getMounted;
  const loadVulnerabilities = jasmine.createSpy('loadVulnerabilities');
  beforeEach(function () {
    minimalProps = {
      vulnerabilities: {
        data: null,
        loading: false,
        error: null,
      },
      loadVulnerabilities,
    };
    getShallow = enzymeUtils.getShallowComponent(VulnerabilitiesTableTile, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(VulnerabilitiesTableTile, minimalProps);
  });

  describe('renders the title correctly', () => {
    it('renders the title correctly', () => {
      const component = getShallow();
      const title = component.find('#component-details-vulnerabilities-title');
      expect(title).toHaveText('Vulnerabilities');
    });
  });

  describe('loadVulnerabilities action', () => {
    it('calls loadVulnerabilities when the component renders', () => {
      getMounted();
      expect(minimalProps.loadVulnerabilities).toHaveBeenCalledTimes(1);
    });
  });

  describe('renders a VulnerabilitiesTable', () => {
    it('renders the table correctly', () => {
      const vulnerabilities = {
        data: ['item1', 'item2'],
        loading: false,
        error: null,
      };
      const component = getShallow({
        vulnerabilities,
      });
      const table = component.find(VulnerabilitiesTable);

      expect(table).toExist();
      expect(table).toHaveProp('loadVulnerabilities', loadVulnerabilities);
      expect(table).toHaveProp('vulnerabilities', vulnerabilities);
    });
  });
});
