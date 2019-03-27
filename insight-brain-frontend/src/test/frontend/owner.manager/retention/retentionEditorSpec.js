import retentionModule from '../../../../main/frontend/owner.manager/retention/module';
import {disabledRetentionPolicies, inheritedRetentionPolicies, customRetentionPolicies} from './retentionMockData';

describe('retentionEditor', function() {
  let $scope,
      $q,
      $componentController,
      mockCLMContextLocations,
      getRootOrganizationRetentionPoliciesDeferred,
      getRetentionPoliciesDeferred,
      setRetentionPoliciesDeferred,
      mockRetentionService,
      vm;

  beforeEach(angular.mock.module(retentionModule.name));

  beforeEach(inject(function($rootScope, _$q_, _$componentController_) {
    $scope = $rootScope.$new();
    $q = _$q_;
    $componentController = _$componentController_;
    mockCLMContextLocations = jasmine.createSpyObj('CLMContextLocations', ['isRootOrg']);
    mockCLMContextLocations.isRootOrg.and.returnValue(false);
    getRootOrganizationRetentionPoliciesDeferred = $q.defer();
    getRetentionPoliciesDeferred = $q.defer();
    setRetentionPoliciesDeferred = $q.defer();
    mockRetentionService = {
      getRootOrganizationRetentionPolicies: jasmine.createSpy().and.callFake(function() {
        return getRootOrganizationRetentionPoliciesDeferred.promise;
      }),
      getRetentionPolicies: jasmine.createSpy().and.callFake(function() {
        return getRetentionPoliciesDeferred.promise;
      }),
      setRetentionPolicies: jasmine.createSpy().and.callFake(function() {
        return setRetentionPoliciesDeferred.promise;
      })
    };
    vm = $componentController('retentionEditor', {
      $scope: $scope,
      CLMContextLocations: mockCLMContextLocations,
      retentionService: mockRetentionService
    });
    vm.retentionEditorMask = {
      wrap: jasmine.createSpy('wrap')
    };
  }));

  describe('load', function() {
    it('expects whether or not this is the root organization to have been set', function() {
      expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
      expect(vm.isRootOrganization).toBe(false);

      mockCLMContextLocations.isRootOrg.and.returnValue(true);
      vm = $componentController('retentionEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        retentionService: mockRetentionService
      });
      expect(mockCLMContextLocations.isRootOrg).toHaveBeenCalled();
      expect(vm.isRootOrganization).toBe(true);
    });

    it('loads application reports and parent application reports for an organization on success', function() {
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.applicationReports).toEqual(inheritedRetentionPolicies.applicationReports);
      expect(vm.parentApplicationReports).toEqual(customRetentionPolicies.applicationReports);
      expect(vm.error).toBeUndefined();
    });

    it('loads application reports for the root organization on success', function() {
      mockCLMContextLocations.isRootOrg.and.returnValue(true);
      mockRetentionService.getRootOrganizationRetentionPolicies.calls.reset();
      vm = $componentController('retentionEditor', {
        $scope: $scope,
        CLMContextLocations: mockCLMContextLocations,
        retentionService: mockRetentionService
      });

      getRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(mockRetentionService.getRootOrganizationRetentionPolicies).not.toHaveBeenCalled();
      expect(vm.applicationReports).toEqual(customRetentionPolicies.applicationReports);
      expect(vm.parentApplicationReports).toBeUndefined();
      expect(vm.error).toBeUndefined();
    });

    it('sets the error message on failure', function() {
      getRetentionPoliciesDeferred.reject({status: 404, data: 'not found'});

      $scope.$digest();

      expect(vm.applicationReports).toBeUndefined();
      expect(vm.parentApplicationReports).toBeUndefined();
      expect(vm.error).toEqual('not found');
    });

    it('sets the inherit data retention form value', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              inheritPolicy: true
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage'].formValue).toBe('inherit');
      expect(vm.error).toBeUndefined();
    });

    it('sets the don\'t purge data retention form value', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: false
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage'].formValue).toBe('dontPurge');
      expect(vm.error).toBeUndefined();
    });

    it('sets the custom data retention form value', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage']).toEqual({formValue: 'custom', maxCount: null, maxAgeInDays: null});
      expect(vm.error).toBeUndefined();
    });

    it('prioritizes setting inherit over don\'t purge on success', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            'stage 1': {
              inheritPolicy: true,
              enablePurging: false
            },
            'stage 2': {
              inheritPolicy: true,
              enablePurging: true
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('inherit');
      expect(vm.retention['stage 2'].formValue).toBe('inherit');
      expect(vm.error).toBeUndefined();
    });

    it('sets the maxCount for a custom data retention form value if maxCount exists', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxCount: 1
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage']).toEqual({formValue: 'custom', maxCount: 1, maxAgeInDays: null});
      expect(vm.error).toBeUndefined();
    });

    it('sets the maxAgeInDays for a custom data retention form value if maxAge exists', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            'stage 1': {
              enablePurging: true,
              maxAge: '1 day'
            },
            'stage 2': {
              enablePurging: true,
              maxAge: '2 days'
            },
            'stage 3': {
              enablePurging: true,
              maxAge: '1 week'
            },
            'stage 4': {
              enablePurging: true,
              maxAge: '2 weeks'
            },
            'stage 5': {
              enablePurging: true,
              maxAge: '1 month'
            },
            'stage 6': {
              enablePurging: true,
              maxAge: '2 months'
            },
            'stage 7': {
              enablePurging: true,
              maxAge: '1 year'
            },
            'stage 8': {
              enablePurging: true,
              maxAge: '2 years'
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('custom');
      expect(vm.retention['stage 1'].maxAgeInDays).toBe('1');
      expect(vm.retention['stage 2'].formValue).toBe('custom');
      expect(vm.retention['stage 2'].maxAgeInDays).toBe('2');
      expect(vm.retention['stage 3'].formValue).toBe('custom');
      expect(vm.retention['stage 3'].maxAgeInDays).toBe('7');
      expect(vm.retention['stage 4'].formValue).toBe('custom');
      expect(vm.retention['stage 4'].maxAgeInDays).toBe('14');
      expect(vm.retention['stage 5'].formValue).toBe('custom');
      expect(vm.retention['stage 5'].maxAgeInDays).toBe('30');
      expect(vm.retention['stage 6'].formValue).toBe('custom');
      expect(vm.retention['stage 6'].maxAgeInDays).toBe('60');
      expect(vm.retention['stage 7'].formValue).toBe('custom');
      expect(vm.retention['stage 7'].maxAgeInDays).toBe('365');
      expect(vm.retention['stage 8'].formValue).toBe('custom');
      expect(vm.retention['stage 8'].maxAgeInDays).toBe('730');
      expect(vm.error).toBeUndefined();
    });

    it('sets an error message if the maxAge value cannot be parsed', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxAge: 'X year'
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.error).toContain('Unable to parse');
    });

    it('sets an error message if the maxAge time unit cannot be parsed', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxAge: '1 unknown'
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.error).toContain('Unable to parse');
    });
  });

  describe('getParentMaxReportsAndMaxAge', function() {
    it('returns the correct text if both the parent maxCount and maxAge exist', function() {
      getRetentionPoliciesDeferred.resolve(customRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            'stage 1': {
              enablePurging: true,
              maxCount: 1,
              maxAge: '1 day'
            },
            'stage 2': {
              enablePurging: true,
              maxCount: 2,
              maxAge: '2 days'
            },
            'stage 3': {
              enablePurging: true,
              maxCount: 1,
              maxAge: '2 days'
            },
            'stage 4': {
              enablePurging: true,
              maxCount: 2,
              maxAge: '1 day'
            }
          }
        }
      });

      $scope.$digest();

      expect(vm.getParentMaxReportsAndMaxAge('stage 1')).toEqual('keep at most 1 day, 1 report');
      expect(vm.getParentMaxReportsAndMaxAge('stage 2')).toEqual('keep at most 2 days, 2 reports');
      expect(vm.getParentMaxReportsAndMaxAge('stage 3')).toEqual('keep at most 2 days, 1 report');
      expect(vm.getParentMaxReportsAndMaxAge('stage 4')).toEqual('keep at most 1 day, 2 reports');
    });

    it('returns the correct text if only the parent maxCount exists', function() {
      getRetentionPoliciesDeferred.resolve(customRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            'stage 1': {
              enablePurging: true,
              maxCount: 1
            },
            'stage 2': {
              enablePurging: true,
              maxCount: 2
            }
          }
        }
      });

      $scope.$digest();

      expect(vm.getParentMaxReportsAndMaxAge('stage 1')).toEqual('keep at most 1 report');
      expect(vm.getParentMaxReportsAndMaxAge('stage 2')).toEqual('keep at most 2 reports');
    });

    it('returns the correct text if only the parent maxAge exists', function() {
      getRetentionPoliciesDeferred.resolve(customRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxAge: '1 day'
            }
          }
        }
      });

      $scope.$digest();

      expect(vm.getParentMaxReportsAndMaxAge('stage')).toEqual('keep at most 1 day');
    });

    it('returns the correct text if the parent has purging disabled', function() {
      getRetentionPoliciesDeferred.resolve(customRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);

      $scope.$digest();

      expect(vm.getParentMaxReportsAndMaxAge('stage 1')).toEqual('don\'t purge');
    });
  });

  describe('isDirty', function() {
    it('returns true if the form value has changed', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxCount: 1,
              maxAge: '1 day'
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage'].formValue).toBe('custom');
      expect(vm.isDirty()).toBe(false);

      vm.retention['stage'].formValue = 'dontPurge';

      expect(vm.isDirty()).toBe(true);
    });

    it('returns true if the custom maxCount has changed', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxCount: 1,
              maxAge: '1 day'
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage'].maxCount).toBe(1);
      expect(vm.isDirty()).toBe(false);

      vm.retention['stage'].maxCount = 2;

      expect(vm.isDirty()).toBe(true);
    });

    it('returns true if the custom maxAgeInDays has changed', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: true,
              maxCount: 1,
              maxAge: '1 day'
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage'].maxAgeInDays).toBe('1');
      expect(vm.isDirty()).toBe(false);

      vm.retention['stage'].maxAgeInDays = '2';

      expect(vm.isDirty()).toBe(true);
    });

    it('returns false if the non-custom form value has not changed even if the custom inputs have', function() {
      getRetentionPoliciesDeferred.resolve({
        applicationReports: {
          stages: {
            stage: {
              enablePurging: false
            }
          }
        }
      });
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage'].formValue).toBe('dontPurge');
      expect(vm.isDirty()).toBe(false);

      vm.retention['stage'].maxAgeInDays = '1';
      vm.retention['stage'].maxCount = '2';

      expect(vm.isDirty()).toBe(false);
    });
  });

  describe('save', function() {
    it('sets the correct data retention policies for the inherit form value on success', function() {
      getRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('dontPurge');
      expect(vm.retention['stage 2'].formValue).toBe('dontPurge');
      vm.retention['stage 1'].formValue = 'inherit';
      vm.retention['stage 2'].formValue = 'inherit';

      expect(vm.isDirty()).toBe(true);

      vm.save();

      const inheritedRetentionPoliciesWithNullValues = {
        applicationReports: {
          stages: {
            'stage 1': {
              inheritPolicy: true,
              enablePurging: true,
              maxCount: null,
              maxAge: null
            },
            'stage 2': {
              inheritPolicy: true,
              enablePurging: true,
              maxCount: null,
              maxAge: null
            }
          }
        }
      };

      setRetentionPoliciesDeferred.resolve({status: 204, data: 'no content'});
      getRetentionPoliciesDeferred = $q.defer();
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPoliciesWithNullValues);

      $scope.$digest();

      expect(mockRetentionService.setRetentionPolicies).toHaveBeenCalledWith(inheritedRetentionPoliciesWithNullValues);
      expect(vm.applicationReports).toEqual(inheritedRetentionPoliciesWithNullValues.applicationReports);
      expect(vm.isDirty()).toBe(false);
      expect(vm.submitError).toBeUndefined();
    });

    it('sets the correct data retention policies for the don\'t purge form value on success', function() {
      getRetentionPoliciesDeferred.resolve(customRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('custom');
      expect(vm.retention['stage 2'].formValue).toBe('custom');
      vm.retention['stage 1'].formValue = 'dontPurge';
      vm.retention['stage 2'].formValue = 'dontPurge';

      expect(vm.isDirty()).toBe(true);

      vm.save();

      setRetentionPoliciesDeferred.resolve({status: 204, data: 'no content'});
      getRetentionPoliciesDeferred = $q.defer();
      getRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);

      $scope.$digest();

      expect(mockRetentionService.setRetentionPolicies).toHaveBeenCalledWith(disabledRetentionPolicies);
      expect(vm.applicationReports).toEqual(disabledRetentionPolicies.applicationReports);
      expect(vm.isDirty()).toBe(false);
      expect(vm.submitError).toBeUndefined();
    });

    it('sets the correct data retention policies for the custom form value on success', function() {
      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('inherit');
      expect(vm.retention['stage 2'].formValue).toBe('inherit');
      vm.retention['stage 1'].formValue = 'custom';
      vm.retention['stage 1'].maxCount = 1;
      vm.retention['stage 1'].maxAgeInDays = 1;
      vm.retention['stage 2'].formValue = 'custom';

      expect(vm.isDirty()).toBe(true);

      vm.save();

      setRetentionPoliciesDeferred.resolve({status: 204, data: 'no content'});

      const expectedNewApplicationReports = {
        applicationReports: {
          stages: {
            'stage 1': {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 1,
              maxAge: '1 day'
            },
            'stage 2': {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: null,
              maxAge: null
            }
          }
        }
      };

      getRetentionPoliciesDeferred = $q.defer();
      getRetentionPoliciesDeferred.resolve(expectedNewApplicationReports);

      $scope.$digest();

      expect(mockRetentionService.setRetentionPolicies).toHaveBeenCalledWith(expectedNewApplicationReports);
      expect(vm.applicationReports).toEqual(expectedNewApplicationReports.applicationReports);
      expect(vm.isDirty()).toBe(false);
      expect(vm.submitError).toBeUndefined();
    });

    it('sets the submit error on failure', function() {
      getRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('dontPurge');
      expect(vm.retention['stage 2'].formValue).toBe('dontPurge');
      vm.retention['stage 1'].formValue = 'inherit';
      vm.retention['stage 2'].formValue = 'inherit';

      expect(vm.isDirty()).toBe(true);

      vm.save();

      setRetentionPoliciesDeferred.reject({status: 404, data: 'not found'});

      $scope.$digest();

      expect(vm.submitError).toBe('not found');
      expect(vm.error).toBeUndefined();
    });

    it('waits for the saving and loading requests to resolve before clearing the mask', function() {
      getRetentionPoliciesDeferred.resolve(disabledRetentionPolicies);
      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);

      $scope.$digest();

      expect(vm.retention['stage 1'].formValue).toBe('dontPurge');
      vm.retention['stage 1'].formValue = 'inherit';

      expect(vm.isDirty()).toBe(true);

      vm.save();

      getRetentionPoliciesDeferred = $q.defer();
      getRootOrganizationRetentionPoliciesDeferred = $q.defer();

      $scope.$digest();

      expect(vm.retentionEditorMask.wrap).toHaveBeenCalled();
      const promise = vm.retentionEditorMask.wrap.calls.first().args[0];
      const isPromiseResolved = jasmine.createSpy('isPromiseResolved');
      promise.then(isPromiseResolved);

      expect(isPromiseResolved).not.toHaveBeenCalled();

      setRetentionPoliciesDeferred.resolve({status: 204, data: 'no content'});
      $scope.$digest();

      expect(isPromiseResolved).not.toHaveBeenCalled();

      getRetentionPoliciesDeferred.resolve(inheritedRetentionPolicies);
      $scope.$digest();

      expect(isPromiseResolved).not.toHaveBeenCalled();

      getRootOrganizationRetentionPoliciesDeferred.resolve(customRetentionPolicies);
      $scope.$digest();

      expect(isPromiseResolved).toHaveBeenCalled();
    });
  });
});
