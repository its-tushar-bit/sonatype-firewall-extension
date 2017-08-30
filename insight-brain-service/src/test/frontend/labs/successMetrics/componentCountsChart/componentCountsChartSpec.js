/* global describe, beforeEach, it, expect, inject */
describe('componentCountsChart', function() {
  beforeEach(module('successMetricsModule', 'legacyConfiguration'));

  var getVm,
      $q,
      $rootScope,
      mockComponentData = {
        componentsPerApplication: 32,
        componentsInTheMostApplications: [
          {componentDisplayName: 'SimpleJson 0.38.0', count: 1},
          {componentDisplayName: 'ch.qos.logback : logback-access : 0.6', count: 1},
          {componentDisplayName: 'commons-beanutils : commons-beanutils : 1.8.3', count: 1},
          {componentDisplayName: 'commons-dbcp : commons-dbcp : 1.4', count: 1},
          {componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1', count: 1}
        ],
        componentsWithTheMostViolations: [
          {componentDisplayName: 'commons-httpclient : commons-httpclient : 3.1', count: 3},
          {componentDisplayName: 'org.apache.geronimo.framework : geronimo-security : 2.1', count: 2},
          {componentDisplayName: 'org.mortbay.jetty : jetty : 6.1.15', count: 2},
          {componentDisplayName: 'tomcat : catalina-host-manager : 5.5.23', count: 2},
          {componentDisplayName: 'tomcat : tomcat-util : 5.5.23', count: 2}
        ]
      };

  beforeEach(inject(function($componentController, _$q_, _$rootScope_) {
    getVm = function(mockSuccessMetricsDataService) {
      return $componentController('componentCountsChart', {successMetricsDataService: mockSuccessMetricsDataService});
    };
    $q = _$q_;
    $rootScope = _$rootScope_;
  }));

  it('sets the numeric values from the data returned by the successMetricsDataService', function() {
    var mockSuccessMetricsDataService = {
          getComponentCountsData: function() {
            return $q.resolve(mockComponentData);
          }
        },
        vm = getVm(mockSuccessMetricsDataService);
    
    expect(vm.isLoaded).toBe(false);
    
    vm.$onInit();

    $rootScope.$digest();

    expect(vm.isLoaded).toBe(true);
    expect(vm.componentData).toBe(mockComponentData);
  });

  it('sets the error message if the promise is rejected', function() {
    var mockSuccessMetricsDataService = {
          getComponentCountsData: function() {
            return $q.reject('error message');
          }
        },
        vm = getVm(mockSuccessMetricsDataService);
    vm.$onInit();

    $rootScope.$digest();

    expect(vm.error).toEqual('error message');
  });

  it('clears any error message when $onInit is called', function() {
    var mockSuccessMetricsDataService = {
          getComponentCountsData: function() {
            return $q.reject('error message');
          }
        },
        vm = getVm(mockSuccessMetricsDataService);
    vm.$onInit();

    $rootScope.$digest();

    expect(vm.error).toBeDefined();

    vm.$onInit();

    expect(vm.error).toBeUndefined();
  });

  it('properly detects empty rows', function() {
    var mockSuccessMetricsDataService = {
          getComponentCountsData: function() {
            return $q.resolve(mockComponentData);
          },
          EMPTY_PREFIX: '~empty~'
        },
        vm = getVm(mockSuccessMetricsDataService);
    
    vm.$onInit();

    $rootScope.$digest();

    expect(vm.showRow('a:b:c')).toBe(true);
    expect(vm.showRow('~empty~')).toBe(false);
    expect(vm.showRow('~empty~123')).toBe(false);
    expect(vm.showRow('123~empty~')).toBe(false);
  });
});
