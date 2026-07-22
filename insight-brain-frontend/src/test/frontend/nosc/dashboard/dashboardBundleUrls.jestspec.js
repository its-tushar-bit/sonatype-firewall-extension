/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  dashboardApiHref,
  dashboardSuccessMetricsHref,
} from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

describe('dashboardBundleUrls embed quick links', () => {
  beforeEach(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    setBaseUrl();
  });

  it('dashboardSuccessMetricsHref uses the clean embed path', () => {
    expect(dashboardSuccessMetricsHref()).toBe(
      'http://localhost/assets/nexus-one/index.html#/success-metrics',
    );
  });

  it('dashboardApiHref uses the clean embed path', () => {
    expect(dashboardApiHref()).toBe('http://localhost/assets/nexus-one/index.html#/api');
  });
});
