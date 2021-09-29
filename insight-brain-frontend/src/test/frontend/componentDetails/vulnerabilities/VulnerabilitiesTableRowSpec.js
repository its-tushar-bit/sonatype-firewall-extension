/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import VulnerabilitiesTableRow from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableRow';

import * as enzymeUtils from '../../enzymeUtils';

describe('VulnerabilitiesTableRow', () => {
  let minimalProps, getShallow, setVulnerabilityIdAndToggleVisibilityMock;

  beforeEach(function () {
    setVulnerabilityIdAndToggleVisibilityMock = jasmine.createSpy('setVulnerabilityIdAndToggleVisibility');
    minimalProps = {
      vulnerability: {
        refId: '1',
        severity: 8.8,
        status: 'status 1',
      },
      setVulnerabilityIdAndToggleVisibility: setVulnerabilityIdAndToggleVisibilityMock,
    };

    getShallow = enzymeUtils.getShallowComponent(VulnerabilitiesTableRow, minimalProps);
  });

  describe('renders row properly', () => {
    it('renders the severity cell', () => {
      const component = getShallow(),
        rowCells = component.find(NxTableCell),
        severityCell = rowCells.at(0);

      expect(rowCells.length).toEqual(4);
      expect(severityCell.find('span')).toHaveText('8');
    });

    it('renders the code cell', () => {
      const component = getShallow(),
        rowCells = component.find(NxTableCell),
        codeCell = rowCells.at(1);

      expect(codeCell.find('span')).toHaveText('1');
    });

    it('renders the status cell', () => {
      const component = getShallow(),
        rowCells = component.find(NxTableCell),
        statusCell = rowCells.at(2);

      expect(statusCell.find('span')).toHaveText('status 1');
    });

    it('renders the chevron cell', () => {
      const component = getShallow(),
        rowCells = component.find(NxTableCell),
        statusCell = rowCells.at(3);

      expect(statusCell).toHaveProp('chevron');
    });

    it('calls setVulnerabilityIdAndToggleVisibility with proper refId on row click ', () => {
      const component = getShallow();
      const rows = component.find(NxTableRow);
      rows.at(0).simulate('click');

      expect(setVulnerabilityIdAndToggleVisibilityMock).toHaveBeenCalledWith('1');
    });
  });
});
