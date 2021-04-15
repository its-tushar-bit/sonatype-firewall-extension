/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxFontAwesomeIcon,
  NxTable,
  NxTableRow,
  NxTableBody,
  NxTableCell,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { faCheck, faHistory } from '@fortawesome/free-solid-svg-icons';

import * as enzymeUtils from '../../enzymeUtils';

import ComponentDisplay from '../../../../main/frontend/ComponentDisplay/ReactComponentDisplay';

describe('ApplicationReportVulnerabilitiesTable', function () {
  const minimalProps = {
    vulnerabilities: [],
    $state: {
      href: jasmine.createSpy().and.callFake(function (stateName, params) {
        return 'http://localhost/vulnerabilities/' + params.id;
      }),
    },
  };

  const ApplicationReportVulnerabilitiesTable = require('inject-loader!' +
    '../../../../main/frontend/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesTable')({
    '../../util/urlUtil': {
      getBaseUrl: () => 'http://localhost',
    },
  }).default;

  const getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportVulnerabilitiesTable, minimalProps);

  it('renders an nx-tile-content containing a scrollable NxTable', function () {
    expect(getShallowComponent()).toMatchSelector('.nx-tile-content');
    expect(getShallowComponent().children()).toHaveClassName('nx-scrollable');
    expect(getShallowComponent().children()).toHaveClassName('nx-table-container');
    expect(getShallowComponent().children().children()).toMatchSelector(NxTable);
  });

  it('sets the emptyMessage prop on the table body', function () {
    expect(getShallowComponent().find('NxTableBody')).toHaveProp('emptyMessage');
  });

  it('renders a row for each vulnerability', function () {
    const vulnerabilities = [
        {
          key: '1',
          policyThreatLevel: 5,
          waived: true,
          grandfathered: false,
          securityCode: 'CVE-1234',
          cvssScore: 5.9,
        },
        {
          key: '2',
          policyThreatLevel: 9,
          waived: false,
          grandfathered: true,
          securityCode: 'CVE-0000',
          cvssScore: 10.0,
        },
        {
          key: '3',
          policyThreatLevel: 1,
          waived: false,
          grandfathered: false,
          securityCode: 'CVE-1235',
          cvssScore: 8.5,
        },
      ],
      render = getShallowComponent({ vulnerabilities }),
      rows = render.find(NxTableBody).find(NxTableRow),
      firstRowTds = rows.at(0).find(NxTableCell),
      secondRowTds = rows.at(1).find(NxTableCell),
      thirdRowTds = rows.at(2).find(NxTableCell);

    expect(firstRowTds.at(0).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 5);
    expect(firstRowTds.at(0).find('.nx-threat-number')).toHaveText('5');
    expect(secondRowTds.at(0).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 9);
    expect(secondRowTds.at(0).find('.nx-threat-number')).toHaveText('9');
    expect(thirdRowTds.at(0).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 1);
    expect(thirdRowTds.at(0).find('.nx-threat-number')).toHaveText('1');

    expect(firstRowTds.at(1).find('a').first()).toHaveText('CVE-1234');
    expect(secondRowTds.at(1).find('a').first()).toHaveText('CVE-0000');
    expect(thirdRowTds.at(1).find('a').first()).toHaveText('CVE-1235');

    expect(firstRowTds.at(2).children()).toHaveText('5.9');
    expect(secondRowTds.at(2).children()).toHaveText('10.0');
    expect(thirdRowTds.at(2).children()).toHaveText('8.5');

    expect(firstRowTds.at(3).find(ComponentDisplay)).toHaveProp('component', vulnerabilities[0]);
    expect(firstRowTds.at(3).find('.iq-text-indicator--waived')).toExist();
    expect(firstRowTds.at(3).find('.iq-text-indicator--waived').find(NxFontAwesomeIcon)).toHaveProp('icon', faCheck);
    expect(firstRowTds.at(3).find('.iq-text-indicator--grandfathered')).not.toExist();
    expect(secondRowTds.at(3).find(ComponentDisplay)).toHaveProp('component', vulnerabilities[1]);
    expect(secondRowTds.at(3).find('.iq-text-indicator--waived')).not.toExist();
    expect(secondRowTds.at(3).find('.iq-text-indicator--grandfathered')).toExist();
    expect(secondRowTds.at(3).find('.iq-text-indicator--grandfathered').find(NxFontAwesomeIcon)).toHaveProp(
      'icon',
      faHistory
    );
    expect(thirdRowTds.at(3).find(ComponentDisplay)).toHaveProp('component', vulnerabilities[2]);
    expect(thirdRowTds.at(3).find('.iq-text-indicator--waived')).not.toExist();
    expect(thirdRowTds.at(3).find('.iq-text-indicator--grandfathered')).not.toExist();
  });

  it('sets truncate on the ComponentDisplay', function () {
    const vulnerabilities = [
      {
        policyThreatLevel: 1,
        displayName: {
          parts: [
            {
              value: 'foo',
            },
          ],
        },
        securityCode: 'CVE-1234',
        cvssScore: 5.9,
        key: '1234',
      },
    ];

    expect(getShallowComponent({ vulnerabilities }).find(ComponentDisplay)).toHaveProp('truncate', true);
  });

  it('gives each row the key from the data', function () {
    const vulnerabilities = [
        {
          policyThreatLevel: 1,
          displayName: {
            parts: [
              {
                value: 'foo',
              },
            ],
          },
          securityCode: 'CVE-1234',
          cvssScore: 5.9,
          key: '1234',
        },
        {
          policyThreatLevel: 1,
          displayName: {
            parts: [
              {
                value: 'foo',
              },
            ],
          },
          securityCode: 'CVE-1235',
          cvssScore: 5.9,
          key: 'asdf',
        },
      ],
      render = getShallowComponent({ vulnerabilities }),
      rows = render.find(NxTableBody).find(NxTableRow);

    expect(rows.at(0).key()).toBe('1234');
    expect(rows.at(1).key()).toBe('asdf');
  });

  it('renders each security issue id as a link with additional printable link', function () {
    const vulnerabilities = [
        {
          securityCode: 'CVE-1234',
          cvssScore: 1,
        },
        {
          securityCode: 'CVE-1235',
          cvssScore: 1,
        },
      ],
      render = getShallowComponent({ vulnerabilities }),
      rows = render.find(NxTableBody).find(NxTableRow),
      firstRowLinks = rows.at(0).find(NxTableCell).at(1).find('a'),
      secondRowLinks = rows.at(1).find(NxTableCell).at(1).find('a');

    expect(firstRowLinks.first()).not.toHaveClassName('iq-vulnerability-printable-link');
    expect(firstRowLinks.first()).toHaveClassName('iq-vulnerability-refid-link');
    expect(firstRowLinks.first()).toHaveText('CVE-1234');
    expect(firstRowLinks.first()).toHaveProp('href', 'http://localhost/vulnerabilities/CVE-1234');

    expect(firstRowLinks.last()).toHaveClassName('iq-vulnerability-printable-link');
    expect(firstRowLinks.last()).toHaveText('http://localhost/ui/links/vln/CVE-1234');
    expect(firstRowLinks.last()).toHaveProp('href', 'http://localhost/ui/links/vln/CVE-1234');

    expect(secondRowLinks.first()).not.toHaveClassName('iq-vulnerability-printable-link');
    expect(secondRowLinks.first()).toHaveClassName('iq-vulnerability-refid-link');
    expect(secondRowLinks.first()).toHaveText('CVE-1235');
    expect(secondRowLinks.first()).toHaveProp('href', 'http://localhost/vulnerabilities/CVE-1235');

    expect(secondRowLinks.last()).toHaveClassName('iq-vulnerability-printable-link');
    expect(secondRowLinks.last()).toHaveText('http://localhost/ui/links/vln/CVE-1235');
    expect(secondRowLinks.last()).toHaveProp('href', 'http://localhost/ui/links/vln/CVE-1235');
  });
});
