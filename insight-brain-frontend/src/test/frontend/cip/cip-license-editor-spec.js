/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../SpecUtil';
import LicenseGroupMockData from '../policy/LicenseGroupMockData';
import testComponentProviderModule from './cip.TestComponentProvider';
import cipLicenseEditorModule from 'MainRoot/cip/cip.license.editor/cip.license.editor.module';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';

/*global CLM, InsightDatatable, Insight */
SpecUtil.setupProviders('bom1-12345678', 'org1');
function getAppliedLicenseOverrides(appStatus, appLicense, orgStatus, orgLicense) {
  var overrides = {
    licenseOverridesByOwner: [
      {
        ownerId: 'bom1-12345678',
        ownerName: 'Application',
        ownerType: 'application',
        licenseOverride: {
          id: 'app1override',
          ownerId: 'bom1-12345678',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'artifactid',
              groupId: 'org.groupid',
              version: '1',
            },
          },
          groupId: 'org.groupid',
          artifactId: 'artifactid',
          version: '1',
          status: appStatus,
          licenseIds: [appLicense],
          comment: '',
        },
      },
      {
        ownerId: 'org1',
        ownerName: 'Organization',
        ownerType: 'organization',
        licenseOverride: {
          id: 'org1override',
          ownerId: 'org1',
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'artifactid',
              groupId: 'org.groupid',
              version: '1',
            },
          },
          groupId: 'org.groupid',
          artifactId: 'artifactid',
          version: '1',
          status: orgStatus,
          licenseIds: [orgLicense],
          comment: '',
        },
      },
      {
        ownerId: 'root-organization',
        ownerName: 'Root Organization',
        ownerType: 'organization',
        licenseOverride: null,
      },
    ],
  };
  if (appStatus === null) {
    overrides.licenseOverridesByOwner[0].licenseOverride = null;
  }
  if (orgStatus === undefined) {
    overrides.licenseOverridesByOwner.pop();
  } else if (orgStatus === null) {
    overrides.licenseOverridesByOwner[1].licenseOverride = null;
  }
  return overrides;
}

function getLicenseWithThreats(declared, observed, selected, effective) {
  var x = {
    declaredlicenses: [],
    observedlicenses: [],
    effectiveLicenses: [],
    selectableLicenses: selected ? selected : [{ licenseId: 'AFL-1.2', licenseName: 'AFL-1.2' }],
  };
  x.declaredlicenses.push({
    threat: 4,
    license: declared ? declared : {},
  });
  x.observedlicenses.push({
    threat: 9,
    license: observed ? observed : { licenseId: 'AFL-UNSPECIFIED', licenseName: 'AFL' },
  });
  x.effectiveLicenses.push({
    threat: 9,
    license: effective ? effective : x.selectableLicenses[0],
  });
  return x;
}

describe('CIP License Editor', function () {
  beforeEach(
    angular.mock.module(cipLicenseEditorModule.name, testComponentProviderModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  let scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();

    window.InsightDatatable = {
      getLicenseThreatLevelFromArray: $.noop,
    };

    window.Insight = {
      updateSummary: $.noop,
    };

    window.clmEndpoint = {
      type: 'ci',
    };

    window.CLM = {
      path: '/foo/',
    };
  }));

  afterEach(function () {
    scope.$destroy();
    delete InsightDatatable.getLicenseThreatLevelFromArray;
    delete Insight.updateSummary;
  });

  describe('Org with override and App without override', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides(null, null, 'OVERRIDDEN', 'AFL-1.2'));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });

      $controller('LicenseEditorController as vm', { $scope: scope });
      $httpBackend.flush();
    }));

    afterEach(inject(function ($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('does not delete if there is no license override', inject(function ($httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      scope.$apply(function () {
        scope.override.ownerId = 'bom1-12345678';
        scope.override.status = 'DELETE';
        scope.save();
      });
      $httpBackend.flush();

      expect(scope.override).toEqual({
        ownerId: 'org1',
        status: 'OVERRIDDEN',
        licenseIds: ['AFL-1.2'],
      });
      expect(scope.canInherit()).toBeTruthy();
      expect(scope.getInheritableStatus()).toEqual('Open');
    }));
  });

  describe('App+Org with Overrides', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides('ACKNOWLEDGED', null, 'OVERRIDDEN', 'AFL-1.2'));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });

      $controller('LicenseEditorController as vm', { $scope: scope });
      $httpBackend.flush();
    }));

    afterEach(inject(function ($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('Submit disabled when expected', function () {
      expect(scope.isSubmitEnabled()).toBeFalsy();
      scope.vm.licenseEditorForm = {};
      expect(scope.isSubmitEnabled()).toBeFalsy();
      scope.vm.licenseEditorForm.$invalid = true;
      scope.saving = true;
      scope.vm.licenseEditorForm.$dirty = true;
      expect(scope.isSubmitEnabled()).toBeFalsy();
      scope.saving = false;
      expect(scope.isSubmitEnabled()).toBeFalsy();
      scope.vm.licenseEditorForm.$invalid = false;
      expect(scope.isSubmitEnabled()).toBeTruthy();
    });

    it('Default Selection', function () {
      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'ACKNOWLEDGED',
        licenseIds: [],
      });

      expect(scope.canInherit()).toBeTruthy();
      expect(scope.getInheritableStatus()).toEqual('Overridden');
    });

    it('Delete', inject(function ($httpBackend, SelectedComponent) {
      $httpBackend
        .expectDELETE(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/bom1-12345678/app1override'))
        .respond(204);
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());

      scope.$apply(function () {
        scope.override.status = 'DELETE';
        scope.save();
      });
      $httpBackend.flush();

      expect(scope.override).toEqual({
        ownerId: 'org1',
        status: 'OVERRIDDEN',
        licenseIds: ['AFL-1.2'],
      });
      expect(scope.canInherit()).toBeTruthy();
      expect(scope.getInheritableStatus()).toEqual('Open');
    }));

    describe('getLicenseThreatClass', function () {
      it('Unspecified', function () {
        expect(scope.getLicenseThreatClass(undefined)).toEqual('unspecified');
        expect(scope.getLicenseThreatClass(null)).toEqual('unspecified');
      });
      it('Critical', function () {
        expect(scope.getLicenseThreatClass(10)).toEqual('critical');
        expect(scope.getLicenseThreatClass(9)).toEqual('critical');
        expect(scope.getLicenseThreatClass(8)).toEqual('critical');
      });
      it('Severe', function () {
        expect(scope.getLicenseThreatClass(7)).toEqual('severe');
        expect(scope.getLicenseThreatClass(6)).toEqual('severe');
        expect(scope.getLicenseThreatClass(5)).toEqual('severe');
        expect(scope.getLicenseThreatClass(4)).toEqual('severe');
      });
      it('Moderate', function () {
        expect(scope.getLicenseThreatClass(3)).toEqual('moderate');
        expect(scope.getLicenseThreatClass(2)).toEqual('moderate');
        expect(scope.getLicenseThreatClass(1)).toEqual('moderate');
      });
      it('None', function () {
        expect(scope.getLicenseThreatClass(0)).toEqual('none');
      });
    });
  });

  describe('No Overrides', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides(null, null, null, null));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });

      $controller('LicenseEditorController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Default Selection', function () {
      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'OPEN',
        licenseIds: [],
      });
      expect(scope.canInherit()).toBeTruthy();
    });

    it('Add Org', inject(function ($httpBackend, SelectedComponent) {
      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1'))
        .respond(function (method, url, data, headers) {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'org1',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment: '',
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());

      scope.$apply(function () {
        scope.override.ownerId = 'org1';
      });
      scope.$apply(function () {
        scope.override.status = 'ACKNOWLEDGED';
      });
      scope.save();
      $httpBackend.flush();
    }));

    it('Add App', inject(function ($httpBackend, SelectedComponent) {
      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/bom1-12345678'))
        .respond(function (method, url, data, headers) {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'bom1-12345678',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment: '',
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());

      scope.$apply(function () {
        scope.override.ownerId = 'bom1-12345678';
      });
      scope.$apply(function () {
        scope.override.status = 'ACKNOWLEDGED';
      });
      scope.save();
      $httpBackend.flush();
    }));
  });

  describe('onOverrideStatusChange', function () {
    it('clears License IDs', inject(function ($controller) {
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      scope.override = { licenseIds: ['AFL-1.2'] };
      scope.onOverrideStatusChange();
      expect(scope.override.licenseIds.length).toBe(0);
    }));
  });

  describe('Effective license updated', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides(null, null, null, null));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      $httpBackend.flush();
    }));

    afterEach(inject(function ($httpBackend, $rootScope) {
      scope.$apply(function () {
        scope.override.status = 'OVERRIDDEN';
      });
      scope.$apply(function () {
        scope.override.licenseIds = ['AFL-1.2'];
      });

      var emittedComponent;
      $rootScope.$on('clm.grid.licenses.changed', function (e, component) {
        emittedComponent = component;
      });
      scope.save();
      $httpBackend.flush();
      expect(emittedComponent.effectiveLicenses).toEqual(['AFL-1.2']);
    }));

    it('Add Org', inject(function ($httpBackend, SelectedComponent) {
      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1'))
        .respond(function (method, url, data, headers) {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'org1',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'OVERRIDDEN',
            licenseIds: ['AFL-1.2'],
            comment: '',
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());

      scope.$apply(function () {
        scope.override.ownerId = 'org1';
      });
    }));

    it('Add App', inject(function ($httpBackend, SelectedComponent) {
      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/bom1-12345678'))
        .respond(function (method, url, data, headers) {
          var post = angular.fromJson(data);
          expect(post).toEqual({
            id: null,
            ownerId: 'bom1-12345678',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'OVERRIDDEN',
            licenseIds: ['AFL-1.2'],
            comment: '',
          });
          post.id = 'saveOverrideId';
          return [200, post, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());

      scope.$apply(function () {
        scope.override.ownerId = 'bom1-12345678';
      });
    }));
  });

  describe('Root Overrides', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      var applied = getAppliedLicenseOverrides(null, null, 'OVERRIDDEN', 'AFL'),
        licenseOverridesByOwner = applied.licenseOverridesByOwner;
      licenseOverridesByOwner[2].licenseOverride = licenseOverridesByOwner[1].licenseOverride;
      licenseOverridesByOwner[1].licenseOverride = licenseOverridesByOwner[0].licenseOverride = null;

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(applied);

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Cannot Inherit', function () {
      expect(scope.override.ownerId).toEqual('root-organization');
      expect(scope.canInherit()).toBeFalsy();
    });
  });

  describe('Org Overridden', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides(null, null, 'OVERRIDDEN', 'AFL'));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Default Selection', function () {
      expect(scope.override).toEqual({
        ownerId: 'org1',
        status: 'OVERRIDDEN',
        licenseIds: ['AFL'],
      });
      expect(scope.canInherit()).toBeTruthy();
    });

    it('Add Application', inject(function ($httpBackend, SelectedComponent) {
      scope.$apply(function () {
        scope.override.ownerId = 'bom1-12345678';
      });

      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'OPEN',
        licenseIds: [],
      });
      expect(scope.canInherit()).toBeTruthy();

      scope.$apply(function () {
        scope.override.status = 'ACKNOWLEDGED';
      });

      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/bom1-12345678'))
        .respond(function (method, url, data, headers) {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'bom1-12345678',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment: '',
          });
          posted.id = 'AddApplication';
          return [200, posted, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());

      scope.save();
      $httpBackend.flush();

      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'ACKNOWLEDGED',
        licenseIds: [],
      });
      expect(scope.canInherit()).toBeTruthy();
    }));
  });

  describe('App Overridden', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides('OVERRIDDEN', 'AFL-1.2', null, null));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Default Selection', function () {
      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'OVERRIDDEN',
        licenseIds: ['AFL-1.2'],
      });
      expect(scope.canInherit()).toBeTruthy();
      expect(scope.getInheritableStatus()).toEqual('Open');
    });

    it('Add Organization Override', inject(function ($httpBackend, SelectedComponent) {
      scope.$apply(function () {
        scope.override.ownerId = 'org1';
      });

      expect(scope.override).toEqual({
        ownerId: 'org1',
        status: 'OPEN',
        licenseIds: [],
      });
      expect(scope.canInherit()).toBeTruthy();

      scope.$apply(function () {
        scope.override.status = 'ACKNOWLEDGED';
      });

      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/organization/org1'))
        .respond(function (method, url, data, headers) {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'org1',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment: '',
          });
          posted.id = 'AddApplication';
          return [200, posted, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());
      scope.save();
      $httpBackend.flush();

      // View is reset to show the app + Inherit option
      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'OVERRIDDEN',
        licenseIds: ['AFL-1.2'],
      });
      scope.$apply(function () {
        scope.override.ownerId = 'bom1-12345678';
      });
      expect(scope.canInherit()).toBeTruthy();
    }));
  });

  describe('No Organization - App Overridden', function () {
    beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides('OVERRIDDEN', 'AFL'));

      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Default Selection', function () {
      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'OVERRIDDEN',
        licenseIds: ['AFL'],
      });
      expect(scope.canInherit()).toBeTruthy();
    });

    it('Add Application', inject(function ($httpBackend, SelectedComponent) {
      expect(scope.canInherit()).toBeTruthy();

      scope.override.status = 'ACKNOWLEDGED';
      scope.override.licenseIds = [];

      $httpBackend
        .expectPOST(SpecUtil.toRegExp(CLM.path + 'rest/licenseOverride/application/bom1-12345678'))
        .respond(function (method, url, data, headers) {
          var posted = angular.fromJson(data);
          expect(posted).toEqual({
            id: null,
            ownerId: 'bom1-12345678',
            componentIdentifier: SelectedComponent.get().componentIdentifier,
            status: 'ACKNOWLEDGED',
            licenseIds: [],
            comment: '',
          });
          posted.id = 'AddApplication';
          return [200, posted, headers];
        });

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(200, getLicenseWithThreats());
      scope.save();
      $httpBackend.flush();

      expect(scope.override).toEqual({
        ownerId: 'bom1-12345678',
        status: 'ACKNOWLEDGED',
        licenseIds: [],
      });
      expect(scope.canInherit()).toBeTruthy();
    }));
  });

  describe('Synthetic Unknown Licenses', function () {
    function setup(declared, observed, selected) {
      inject(function ($controller, $httpBackend, SelectedComponent) {
        $httpBackend
          .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
          .respond(LicenseGroupMockData.getLicensesData());

        $httpBackend
          .expectGET(
            SpecUtil.toRegExp(
              CLM.path +
                'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
                encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
            )
          )
          .respond(getLicenseWithThreats(declared, observed, selected));

        $httpBackend
          .expectGET(
            SpecUtil.toRegExp(
              CLM.path +
                'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
                encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
            )
          )
          .respond(getAppliedLicenseOverrides('OVERRIDDEN', 'AFL'));

        spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
        $controller('LicenseEditorController', { $scope: scope });
        $httpBackend.flush();
      });
    }

    describe('Selected', function () {
      it('Synthetic Ignored', function () {
        setup({ licenseId: 'AFL-1.2' }, { licenseId: 'No-Sources' }, [{ licenseId: 'AFL-1.2' }]);
        var license = LicenseGroupMockData.getLicensesData()[1];
        license.name = 'AFL-1.2';
        expect(scope.selectableLicenses).toEqual([license]);
      });
    });
  });

  describe('SelectedComponent watcher', function () {
    var SelectedComponent;

    beforeEach(inject(function ($controller, _SelectedComponent_, $httpBackend) {
      SelectedComponent = _SelectedComponent_;
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });

      $httpBackend
        .expectGET(SpecUtil.toRegExp(CLM.path + 'rest/license?filterSynthetic=true'))
        .respond(LicenseGroupMockData.getLicensesData());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/ci/componentDetails/application/bom1-12345678/licenses?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getLicenseWithThreats());

      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLM.path +
              'rest/licenseOverride/application/bom1-12345678?componentIdentifier=' +
              encodeURIComponent(JSON.stringify(SelectedComponent.get().componentIdentifier))
          )
        )
        .respond(getAppliedLicenseOverrides('ACKNOWLEDGED', null, 'OVERRIDDEN', 'AFL-1.2'));

      $httpBackend.flush();
      spyOn(scope, 'doLoad');
    }));

    it('calls doLoad() when new component selected', function () {
      expect(scope.licenses).toBeTruthy();
      SelectedComponent.set({});
      scope.$digest();
      expect(scope.doLoad).toHaveBeenCalled();
    });

    it('does not call doLoad() when selected component changes to null', function () {
      expect(SelectedComponent.get()).not.toBeNull();
      SelectedComponent.set(null);
      scope.$digest();
      expect(scope.doLoad).not.toHaveBeenCalled();
    });

    it('does not call doLoad() when selected component changes to same component', function () {
      SelectedComponent.set(SelectedComponent.get());
      scope.$digest();
      expect(scope.doLoad).not.toHaveBeenCalled();
    });
  });

  describe('doLoad()', function () {
    it('resets licenses', inject(function ($controller) {
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
      scope.licenses = ['AFL-1.2'];
      scope.doLoad();
      expect(scope.licenses).toBeNull();
    }));
  });

  describe('isClaimedComponent()', function () {
    var SelectedComponent;

    beforeEach(inject(function ($controller, _SelectedComponent_) {
      SelectedComponent = _SelectedComponent_;
      spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
      $controller('LicenseEditorController', { $scope: scope });
    }));

    it('returns false if no component selected', function () {
      SelectedComponent.set(null);
      expect(scope.isClaimedComponent()).toBe(false);
    });

    it('returns false if the component\'s identificationSource is not "Manual"', function () {
      SelectedComponent.set({
        identificationSource: 'foo',
      });
      expect(scope.isClaimedComponent()).toBe(false);
    });

    it('returns true if the component\'s identificationSource is "Manual"', function () {
      SelectedComponent.set({
        identificationSource: 'Manual',
      });
      expect(scope.isClaimedComponent()).toBe(true);
    });
  });
});
