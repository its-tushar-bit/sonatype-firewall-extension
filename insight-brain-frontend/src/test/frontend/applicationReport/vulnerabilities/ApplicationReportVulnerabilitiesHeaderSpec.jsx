/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import moment from 'moment-timezone';

import ApplicationReportVulnerabilitiesHeader from '../../../../main/frontend/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesHeader';

describe('ApplicationReportVulnerabilitiesHeader', () => {
  let getShallowComponent;

  beforeAll(function () {
    moment.tz.setDefault('America/New_York');
  });

  afterAll(function () {
    moment.tz.setDefault();
  });

  beforeEach(() => {
    const minimalProps = {
      metadata: {
        reportTitle: 'foo report',
        reportTime: moment('2018-11-11 15:13:11').toDate().getTime(),
        application: { name: 'foo app' },
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      ApplicationReportVulnerabilitiesHeader,
      minimalProps
    );
  });

  it('includes title text with the application name and report title', function () {
    expect(getShallowComponent().find('h1')).toIncludeText(
      'Vulnerabilities for foo app foo report'
    );
  });

  it('includes the formatted date in a nx-tile-header__subtitle that has the visual-testing-ignore class', function () {
    expect(
      getShallowComponent().find(
        '.nx-tile-header__subtitle.visual-testing-ignore'
      )
    ).toHaveText('2018-11-11 15:13:11 UTC-05:00');
  });
});
