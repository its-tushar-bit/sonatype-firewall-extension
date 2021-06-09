/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import moment from 'moment-timezone';

import ApplicationReportRawDataHeader from '../../../../main/frontend/applicationReport/rawData/ApplicationReportRawDataHeader';

describe('ApplicationReportRawDataHeader', () => {
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
        reportTitle: 'Build Report',
        reportTime: moment('2021-06-06 11:11:11').toDate().getTime(),
        application: { name: 'we are doomed' },
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ApplicationReportRawDataHeader, minimalProps);
  });

  it('includes title text with the application name and report title', () => {
    const header = getShallowComponent().find('.nx-h1');
    expect(header.text()).toBe('Raw Data for we are doomed Build Report');
  });

  it('includes the formatted date in a nx-page-title__description that has the visual-testing-ignore class', () => {
    const time = getShallowComponent().find('.nx-page-title__description.visual-testing-ignore');
    expect(time.text()).toBe('2021-06-06 11:11:11 UTC-04:00');
  });
});
