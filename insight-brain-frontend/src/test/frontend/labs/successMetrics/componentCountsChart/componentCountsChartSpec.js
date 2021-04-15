/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global describe, beforeEach, it, expect, inject */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import legacyConfigurationModule from '../../../../../main/frontend/LegacyConfigurationModule';

describe('componentCountsChart', function () {
  beforeEach(
    angular.mock.module(
      successMetricsModule.name,
      legacyConfigurationModule.name,
      'Stores'
    )
  );

  var getVm,
    mockComponentData = {
      componentsPerApplication: 32,
      componentsInTheMostApplications: [
        { componentDisplayName: 'SimpleJson 0.38.0', count: 1 },
        {
          componentDisplayName: 'ch.qos.logback : logback-access : 0.6',
          count: 1,
        },
        {
          componentDisplayName: 'commons-beanutils : commons-beanutils : 1.8.3',
          count: 1,
        },
        { componentDisplayName: 'commons-dbcp : commons-dbcp : 1.4', count: 1 },
        {
          componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1',
          count: 1,
        },
      ],
      componentsWithTheMostViolations: [
        {
          componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1',
          count: 3,
        },
        {
          componentDisplayName:
            'org.apache.geronimo.framework : geronimo-security : 2.1',
          count: 2,
        },
        {
          componentDisplayName: 'org.mortbay.jetty : jetty : 6.1.15',
          count: 2,
        },
        {
          componentDisplayName: 'tomcat : catalina-host-manager : 5.5.23',
          count: 2,
        },
        { componentDisplayName: 'tomcat : tomcat-util : 5.5.23', count: 2 },
      ],
    };

  beforeEach(inject(function ($componentController) {
    getVm = function (componentData) {
      return $componentController(
        'componentCountsChart',
        {},
        { componentData: componentData }
      );
    };
  }));

  it('properly detects empty rows', function () {
    var vm = getVm(mockComponentData);

    expect(vm.showRow('a:b:c')).toBe(true);
    expect(vm.showRow('~empty~')).toBe(false);
    expect(vm.showRow('~empty~123')).toBe(false);
    expect(vm.showRow('123~empty~')).toBe(false);
  });
});
