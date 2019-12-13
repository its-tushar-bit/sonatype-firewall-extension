/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';

describe('addSuccessMetricsReportModal', function() {
  var getVm,
      applicationStoreDeferred,
      organizationStoreDeferred,
      mockApplicationStore = {
        get: function() {
          return applicationStoreDeferred.promise;
        }
      },
      mockOrganizationStore = {
        get: function() {
          return organizationStoreDeferred.promise;
        }
      },
      $rootScope,
      $q;

  beforeEach(angular.mock.module(successMetricsModule.name, 'Stores'));

  beforeEach(inject(function(_$q_, $componentController, _$rootScope_) {
    $rootScope = _$rootScope_;
    $q = _$q_;

    getVm = function(bindings, inject) {
      return $componentController('addSuccessMetricsReportModal', Object.assign({
        ApplicationStore: mockApplicationStore,
        OrganizationStore: mockOrganizationStore
      }, inject), bindings);
    };

    applicationStoreDeferred = $q.defer();
    organizationStoreDeferred = $q.defer();
  }));

  describe('initial state', function() {
    it('should have no error, false loaded, blank name, empty collections, and true isAllApplications', function() {
      var vm = getVm();

      expect(vm.error).toBeUndefined();
      expect(vm.loaded).toBe(false);
      expect(vm.name).toBe('');
      expect(vm.applications.length).toBe(0);
      expect(vm.organizations.length).toBe(0);
      expect(vm.selectedApplications instanceof Set).toBe(true);
      expect(vm.selectedApplications.size).toBe(0);
      expect(vm.selectedOrganizations instanceof Set).toBe(true);
      expect(vm.selectedOrganizations.size).toBe(0);
    });
  });

  describe('$onInit', function() {
    it('resets error back to undefined', function() {
      var vm = getVm();

      vm.error = 'test';

      vm.$onInit();

      expect(vm.error).toBeUndefined();
    });

    it('sets the applications and organizations from their respective Stores', function() {
      var vm = getVm(),
          applications = [{ id: 'app1' }],
          organizations = [{ id: 'org1' }];

      vm.$onInit();
      applicationStoreDeferred.resolve(applications);
      organizationStoreDeferred.resolve(organizations);
      $rootScope.$digest();

      expect(vm.applications).toBe(applications);
      expect(vm.organizations.length).toBe(1);
      expect(vm.organizations[0]).toBe(organizations[0]);

      expect(vm.loaded).toBe(true);
    });

    it('filters out the root organization', function() {
      var vm = getVm(),
          applications = [{ id: 'app1' }],
          organizations = [{ id: 'org1' }, { id: 'ROOT_ORGANIZATION_ID' }];

      vm.$onInit();
      applicationStoreDeferred.resolve(applications);
      organizationStoreDeferred.resolve(organizations);
      $rootScope.$digest();

      expect(vm.applications).toBe(applications);
      expect(vm.organizations.length).toBe(1);
      expect(vm.organizations[0]).toBe(organizations[0]);

      expect(vm.loaded).toBe(true);
    });

    it('sets the error message if there is an error loading the applications', function() {
      var vm = getVm(),
          applicationError = 'Error!',
          organizations = [{ id: 'org1' }];

      vm.$onInit();
      applicationStoreDeferred.reject(applicationError);
      organizationStoreDeferred.resolve(organizations);
      $rootScope.$digest();

      expect(vm.error).toBe('Error!');

      expect(vm.applications.length).toBe(0);
      expect(vm.organizations.length).toBe(0);

      expect(vm.loaded).toBe(true);
    });

    it('sets the error message if there is an error loading the organizations', function() {
      var vm = getVm(),
          applications = [{ id: 'app1' }],
          organizationError = 'Error!';

      vm.$onInit();
      applicationStoreDeferred.resolve(applications);
      organizationStoreDeferred.reject(organizationError);
      $rootScope.$digest();

      expect(vm.error).toBe('Error!');

      expect(vm.applications.length).toBe(0);
      expect(vm.organizations.length).toBe(0);

      expect(vm.loaded).toBe(true);
    });
  });

  describe('onOrgAppSelectionChange', function() {
    it('sets selectedOrganizations and selectedApplications from the parameters', function() {
      var vm = getVm(),
          selectedApplications = new Set(),
          selectedOrganizations = new Set();

      vm.onOrgAppSelectionChange(selectedOrganizations, selectedApplications);

      expect(vm.selectedApplications).toBe(selectedApplications);
      expect(vm.selectedOrganizations).toBe(selectedOrganizations);
    });
  });

  describe('onSubmit', function() {
    it('does nothing if isCreateEnabled is false', function() {
      var close = jasmine.createSpy('close'),
          mockSuccessMetricsDataService = {
            createSuccessMetricsReportForCurrentUser: jasmine.createSpy('createSuccessMetricsReportForCurrentUser')
          },
          vm = getVm({ close: close }, { successMetricsDataService: mockSuccessMetricsDataService });

      expect(vm.isCreateEnabled()).toBe(false);

      vm.onSubmit();

      expect(close).not.toHaveBeenCalled();
      expect(mockSuccessMetricsDataService.createSuccessMetricsReportForCurrentUser).not.toHaveBeenCalled();
    });

    it('calls successMetricsDataService.createSuccessMetricsReportForCurrentUser and then calls close with the result',
        function() {
          var result = { one: 1 },
              close = jasmine.createSpy('close'),
              createDeferred = $q.defer(),
              mockSuccessMetricsDataService = {
                createSuccessMetricsReportForCurrentUser: function() {
                  return createDeferred.promise;
                }
              },
              mockMaskController = {
                wrap: function(promise) { return promise; }
              },
              vm = getVm({ close: close }, { successMetricsDataService: mockSuccessMetricsDataService });

          vm.maskController = mockMaskController;

          // to make isCreateEnabled return true
          vm.addSuccessMetricsReportForm = {};
          vm.name = 'test';

          expect(vm.isCreateEnabled()).toBe(true);

          vm.onSubmit();

          expect(close).not.toHaveBeenCalled();

          createDeferred.resolve(result);
          $rootScope.$digest();

          expect(close).toHaveBeenCalledWith({ result: result });
        }
    );

    it('passes the name and scope to successMetricsDataService.createSuccessMetricsReportForCurrentUser when not ' +
      'isAllApplications', function() {
      var createDeferred = $q.defer(),
          name = 'test name',
          selectedApplications = ['1234', '5678'],
          selectedApplicationsSet = new Set(selectedApplications),
          selectedOrganizations = ['asdf', 'qwerty'],
          selectedOrganizationsSet = new Set(selectedOrganizations),
          mockSuccessMetricsDataService = {
            createSuccessMetricsReportForCurrentUser: jasmine.createSpy('createSuccessMetricsReportForCurrentUser')
                .and.returnValue(createDeferred.promise)
          },
          mockMaskController = {
            wrap: function(promise) { return promise; }
          },
          vm = getVm(null, { successMetricsDataService: mockSuccessMetricsDataService });

      vm.maskController = mockMaskController;

      // to make isCreateEnabled return true
      vm.addSuccessMetricsReportForm = {};

      vm.name = name;
      vm.selectedApplications = selectedApplicationsSet;
      vm.selectedOrganizations = selectedOrganizationsSet;
      vm.isAllApplications = false;
      vm.includeLatestData = true;

      expect(vm.isCreateEnabled()).toBe(true);

      vm.onSubmit();

      expect(mockSuccessMetricsDataService.createSuccessMetricsReportForCurrentUser).toHaveBeenCalledWith({
        name: name,
        scope: {
          organizationIds: selectedOrganizations,
          applicationIds: selectedApplications
        },
        includeLatestData: true
      });
    });

    it('passes empty scope if isAllApplications', function() {
      var createDeferred = $q.defer(),
          name = 'test name',
          selectedApplications = new Set(['1234', '5678']),
          selectedOrganizations = new Set(['asdf', 'qwerty']),
          mockSuccessMetricsDataService = {
            createSuccessMetricsReportForCurrentUser: jasmine.createSpy('createSuccessMetricsReportForCurrentUser')
                .and.returnValue(createDeferred.promise)
          },
          mockMaskController = {
            wrap: function(promise) { return promise; }
          },
          vm = getVm(null, { successMetricsDataService: mockSuccessMetricsDataService });

      vm.maskController = mockMaskController;

      // to make isCreateEnabled return true
      vm.addSuccessMetricsReportForm = {};

      vm.name = name;
      vm.selectedApplications = selectedApplications;
      vm.selectedOrganizations = selectedOrganizations;
      vm.isAllApplications = true;

      expect(vm.isCreateEnabled()).toBe(true);

      vm.onSubmit();

      expect(mockSuccessMetricsDataService.createSuccessMetricsReportForCurrentUser).toHaveBeenCalledWith({
        name: name,
        scope: {},
        includeLatestData: false
      });
    });

    it('sets vm.error if the create request fails', function() {
      var error = 'Error!',
          close = jasmine.createSpy('close'),
          createDeferred = $q.defer(),
          mockSuccessMetricsDataService = {
            createSuccessMetricsReportForCurrentUser: function() {
              return createDeferred.promise;
            }
          },
          mockMaskController = {
            wrap: function(promise) { return promise; }
          },
          vm = getVm({ close: close }, { successMetricsDataService: mockSuccessMetricsDataService });

      vm.maskController = mockMaskController;

      // to make isCreateEnabled return true
      vm.addSuccessMetricsReportForm = {};
      vm.name = 'test';

      expect(vm.isCreateEnabled()).toBe(true);

      vm.onSubmit();

      createDeferred.reject(error);
      $rootScope.$digest();

      expect(close).not.toHaveBeenCalled();
      expect(vm.error).toBe(error);
    });

    it('wraps the promise from the data service in vm.maskController.wrap and waits until the promise returned from ' +
      'that function before closing the window', function() {
      var serviceDeferred = $q.defer(),
          wrapDeferred = $q.defer(),
          servicePromise = serviceDeferred.promise,
          wrapPromise = wrapDeferred.promise,
          close = jasmine.createSpy('close'),
          mockSuccessMetricsDataService = {
            createSuccessMetricsReportForCurrentUser: function() {
              return servicePromise;
            }
          },
          mockMaskController = {
            wrap: jasmine.createSpy('wrap').and.returnValue(wrapPromise)
          },
          vm = getVm({ close: close }, { successMetricsDataService: mockSuccessMetricsDataService });

      vm.maskController = mockMaskController;

      // to make isCreateEnabled return true
      vm.addSuccessMetricsReportForm = {};
      vm.name = 'test';

      expect(mockMaskController.wrap).not.toHaveBeenCalled();

      vm.onSubmit();

      expect(mockMaskController.wrap).toHaveBeenCalledWith(servicePromise);
      expect(close).not.toHaveBeenCalledWith();

      serviceDeferred.resolve('test');
      $rootScope.$digest();

      expect(close).not.toHaveBeenCalled();

      wrapDeferred.resolve('test');
      $rootScope.$digest();

      expect(close).toHaveBeenCalledWith({ result: 'test' });
    });
  });

  describe('isCreateEnabled', function() {
    it('returns false if the successMetricsForm is not bound', function() {
      var vm = getVm();

      expect(vm.addSuccessMetricsReportForm).toBeUndefined();
      expect(vm.isCreateEnabled()).toBe(false);
    });

    it('returns true if the form is valid and isAllApplications is true', function() {
      var vm = getVm();

      vm.isAllApplications = true;
      vm.addSuccessMetricsReportForm = { $invalid: false };

      expect(vm.isCreateEnabled()).toBe(true);
    });

    it('returns true if the form is valid and isAllApplications is false and apps are selected', function() {
      var vm = getVm();

      vm.isAllApplications = false;
      vm.selectedApplications = new Set(['12354']);
      vm.addSuccessMetricsReportForm = { $invalid: false };

      expect(vm.isCreateEnabled()).toBe(true);
    });

    it('returns true if the form is valid and isAllApplications is false and orgs are selected', function() {
      var vm = getVm();

      vm.isAllApplications = false;
      vm.selectedOrganizations = new Set(['12354']);
      vm.addSuccessMetricsReportForm = { $invalid: false };

      expect(vm.isCreateEnabled()).toBe(true);
    });

    it('returns true if the form is valid and isAllApplications is false and both orgs and apps are selected',
        function() {
          var vm = getVm();

          vm.isAllApplications = false;
          vm.selectedOrganizations = new Set(['12354']);
          vm.selectedApplications = new Set(['asdf']);
          vm.addSuccessMetricsReportForm = { $invalid: false };

          expect(vm.isCreateEnabled()).toBe(true);
        }
    );

    it('returns false if the form is invalid', function() {
      var vm = getVm();

      vm.isAllApplications = false;
      vm.selectedOrganizations = new Set(['12354']);
      vm.selectedApplications = new Set(['asdf']);
      vm.addSuccessMetricsReportForm = { $invalid: true };
      expect(vm.isCreateEnabled()).toBe(false);

      vm.selectedOrganizations = new Set();
      expect(vm.isCreateEnabled()).toBe(false);

      vm.selectedApplications = new Set();
      expect(vm.isCreateEnabled()).toBe(false);

      vm.isAllApplications = true;
      expect(vm.isCreateEnabled()).toBe(false);
    });
  });

  describe('getErrorMessage', function() {
    it('returns undefined if vm.error is undefined', function() {
      var vm = getVm();

      expect(vm.getErrorMessage()).toBeUndefined();
    });

    it('returns the data property of the error object', function() {
      var vm = getVm(),
          errorMessage = 'Error!';

      vm.error = { data: errorMessage };

      expect(vm.getErrorMessage()).toBe(errorMessage);
    });
  });
});
