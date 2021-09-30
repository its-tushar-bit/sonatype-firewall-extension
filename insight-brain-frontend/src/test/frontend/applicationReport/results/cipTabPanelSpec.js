/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';
import componentDisplayModule from '../../../../main/frontend/ComponentDisplay/module';

describe('cipTabPanel', function () {
  let $componentController, $httpBackend, CLMLocations, ownerContext;
  ownerContext = { ownerType: 'application' };

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(
    angular.mock.module(componentDisplayModule.name, function ($provide) {
      $provide.value('OwnerContext', ownerContext);
    })
  );

  beforeEach(inject(function (_$componentController_, _$httpBackend_, _CLMLocations_) {
    $componentController = _$componentController_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
  }));

  it('sets the initial value of vm.selectedTab to componentInfo', function () {
    const controller = $componentController('cipTabPanel');

    expect(controller.selectedTab).toBe('componentInfo');
  });

  describe('selectedComponent watcher', function () {
    let $scope, controller, innerSourceComponent;

    beforeEach(inject(function ($rootScope) {
      $scope = $rootScope.$new();
      controller = $componentController('cipTabPanel', { $scope }, { selectedComponent: {} });

      innerSourceComponent = {
        hash: '1249e25aebb15358bedd',
        matchState: 'test-match-state',
        identificationSource: 'test-identification-source',
        componentIdentifier: {
          coordinates: 'coordinates',
          format: 'format',
        },
        dependencyInfo: { isDirectDependency: false },
        innerSource: true,
        innerSourceData: [
          {
            ownerApplicationId: 'id',
            ownerApplicationName: 'appName',
          },
        ],
      };
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('sets vm.latestReportUrl if already loaded', function () {
      controller.selectedComponent = {
        ...innerSourceComponent,
        latestReport: {
          stage: 'stage',
          url: 'latestReportUrl',
        },
      };
      $scope.$digest();
      expect(controller.selectedComponent.latestReport.url).toContain('latestReportUrl');
    });

    it('sets vm.latestReportUrl with InnerSource report for latest stage', function () {
      const mockResponse = [
        {
          stage: 'build',
          latestReportHtmlUrl: 'buildUrl',
        },
        {
          stage: 'release',
          latestReportHtmlUrl: 'releaseUrl',
        },
        {
          stage: 'develop',
          latestReportHtmlUrl: 'developUrl',
        },
      ];

      controller.selectedComponent = innerSourceComponent;
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(200, mockResponse);
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLMLocations.getInnerSourceComponentLatestVersionUrl(controller.selectedComponent.componentIdentifier)
          )
        )
        .respond(200, '');
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.selectedComponent.latestReport.url).toContain('releaseUrl');
      expect(controller.selectedComponent.insufficientPermissions).toBeFalsy();
    });

    it('sets the latest version for a selected InnerSource component', function () {
      controller.selectedComponent = innerSourceComponent;
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(200, [{}]);
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLMLocations.getInnerSourceComponentLatestVersionUrl(controller.selectedComponent.componentIdentifier)
          )
        )
        .respond(200, '1.0.0');
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.selectedComponent.innerSourceData[0].latestVersion).toContain('1.0.0');
    });

    it('handle the error action if last report URL request fails', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(500, 'error');
      controller.selectedComponent = innerSourceComponent;
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.error).toContain('error');
      expect(controller.selectedComponent.latestReport).toBeUndefined();
      expect(controller.selectedComponent.insufficientPermissions).toBeFalsy();
    });

    it('handle the error action if last report URL request fails due to lack of permissions', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(403, 'error');
      controller.selectedComponent = innerSourceComponent;
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.selectedComponent.insufficientPermissions).toBeTruthy();
    });

    it('handle the error action if request for InnerSource component latest version fails', function () {
      controller.selectedComponent = innerSourceComponent;
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(200, [{}]);
      $httpBackend
        .expectGET(
          SpecUtil.toRegExp(
            CLMLocations.getInnerSourceComponentLatestVersionUrl(controller.selectedComponent.componentIdentifier)
          )
        )
        .respond(500, 'error');
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.error).toContain('error');
      expect(controller.selectedComponent.innerSourceData[0].latestVersion).toBeUndefined();
    });

    it('opens a new tab with the InnerSource report when no version is set', inject(function ($window) {
      spyOn($window, 'open').and.callFake(() => {});

      controller.selectedComponent = innerSourceComponent;
      controller.selectedComponent.latestReport = { url: 'someUrl' };
      controller.openLatestInnerSourceReport();

      expect($window.open).toHaveBeenCalledWith('someUrl', '_blank');
    }));

    it('opens a new tab with the InnerSource report when having the same version', inject(function ($window) {
      spyOn($window, 'open').and.callFake(() => {});

      controller.selectedComponent = innerSourceComponent;
      controller.selectedComponent.latestReport = { url: 'someUrl' };
      controller.selectedComponent.componentIdentifier.coordinates = { version: '1.0.0' };
      controller.selectedComponent.innerSourceData[0].latestVersion = '1.0.0';
      controller.openLatestInnerSourceReport();

      expect($window.open).toHaveBeenCalledWith('someUrl', '_blank');
    }));

    it('opens a modal to go to the InnerSource report when a different version is set', inject(function ($window) {
      spyOn($window, 'open');
      spyOn(controller, 'openInnerSourceProducerReportModal');

      controller.selectedComponent = innerSourceComponent;
      controller.selectedComponent.latestReport = { url: 'someUrl' };
      controller.selectedComponent.componentIdentifier.coordinates = { version: '1.0.0' };
      controller.selectedComponent.innerSourceData[0].latestVersion = '2.0.0';
      controller.openLatestInnerSourceReport();

      expect(controller.openInnerSourceProducerReportModal).toHaveBeenCalled();
      expect($window.open).not.toHaveBeenCalled();
    }));

    it('opens a modal asking to request permissions when not allowed in the producer app', inject(function ($window) {
      spyOn($window, 'open');
      spyOn(controller, 'openInnerSourceProducerPermissionsModal');
      spyOn(controller, 'openInnerSourceProducerReportModal');

      controller.selectedComponent = innerSourceComponent;
      controller.selectedComponent.insufficientPermissions = true;
      controller.openLatestInnerSourceReport();

      expect(controller.openInnerSourceProducerPermissionsModal).toHaveBeenCalled();
      expect(controller.openInnerSourceProducerReportModal).not.toHaveBeenCalled();
      expect($window.open).not.toHaveBeenCalled();
    }));

    it('sets vm.selectedTab to the name of the first tab if its previous value is not present in vm.tabs', function () {
      controller.selectedComponent = { matchState: 'exact' };
      $scope.$digest();
      controller.selectedTab = 'vulnerabilities';

      controller.selectedComponent = { matchState: 'similar' };
      $scope.$digest();

      expect(controller.selectedTab).toBe('vulnerabilities');

      controller.selectedComponent = { matchState: 'unknown' };
      $scope.$digest();

      expect(controller.selectedTab).toBe('componentInfo');
    });

    describe('for applications', function () {
      beforeEach(function () {
        ownerContext.ownerType = 'application';
      });

      ['exact', 'similar'].forEach(function (matchState) {
        it(`updates vm.tabs to include tabs for non-unknown components if the matchState is ${matchState}`, function () {
          // first set a matchState that should not include the additional tabs
          controller.selectedComponent = { matchState: 'unknown' };
          $scope.$digest();

          expect(controller.tabs.length).toBeLessThan(8);

          // then set a matchState that should
          controller.selectedComponent = { matchState };
          $scope.$digest();

          expect(controller.tabs).toContain({
            name: 'componentInfo',
            displayName: 'Component Info',
          });

          expect(controller.tabs).toContain({
            name: 'policy',
            displayName: 'Policy',
          });

          expect(controller.tabs).toContain({
            name: 'similar',
            displayName: 'Similar',
          });

          expect(controller.tabs).toContain({
            name: 'occurrences',
            displayName: 'Occurrences',
          });

          expect(controller.tabs).toContain({
            name: 'licenses',
            displayName: 'Licenses',
          });

          expect(controller.tabs).toContain({
            name: 'vulnerabilities',
            displayName: 'Vulnerabilities',
          });

          expect(controller.tabs).toContain({
            name: 'labels',
            displayName: 'Labels',
          });

          expect(controller.tabs).toContain({
            name: 'auditLog',
            displayName: 'Audit Log',
          });
        });
      });

      it('updates vm.tabs to not include tabs for non-unknown components if the matchState is unknown', function () {
        // first set a matchState that should include all tabs
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'licenses',
          displayName: 'Licenses',
        });

        // then set a matchState that should clear out the additional tabs
        controller.selectedComponent = { matchState: 'unknown' };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'componentInfo',
          displayName: 'Component Info',
        });

        expect(controller.tabs).toContain({
          name: 'policy',
          displayName: 'Policy',
        });

        expect(controller.tabs).toContain({
          name: 'similar',
          displayName: 'Similar',
        });

        expect(controller.tabs).toContain({
          name: 'occurrences',
          displayName: 'Occurrences',
        });

        expect(controller.tabs).not.toContain({
          name: 'licenses',
          displayName: 'Licenses',
        });

        expect(controller.tabs).not.toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities',
        });

        expect(controller.tabs).not.toContain({
          name: 'labels',
          displayName: 'Labels',
        });

        expect(controller.tabs).not.toContain({
          name: 'auditLog',
          displayName: 'Audit Log',
        });
      });

      ['unknown', 'similar'].forEach(function (matchState) {
        it(`updates vm.tabs to include the Claim tab if the matchState is ${matchState}`, function () {
          // first set a matchState that should not include the additional tabs
          controller.selectedComponent = { matchState: 'exact' };
          $scope.$digest();

          expect(controller.tabs).not.toContain({
            name: 'claimComponent',
            displayName: 'Claim',
          });

          // then set a matchState that should
          controller.selectedComponent = { matchState };
          $scope.$digest();

          expect(controller.tabs).toContain({
            name: 'claimComponent',
            displayName: 'Claim',
          });
        });
      });

      it('updates vm.tabs to include the Claim tab if the matchState is exact and identificationSource is Manual', function () {
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).not.toContain({
          name: 'claimComponent',
          displayName: 'Claim',
        });

        controller.selectedComponent = {
          matchState: 'exact',
          identificationSource: 'Manual',
        };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'claimComponent',
          displayName: 'Claim',
        });
      });

      it('updates vm.tabs to not include the Vulnerabilities tab if the identificationSource is Manual', function () {
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities',
        });

        controller.selectedComponent = {
          matchState: 'exact',
          identificationSource: 'Manual',
        };
        $scope.$digest();

        expect(controller.tabs).not.toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities',
        });
      });
    });

    describe('for repositories', function () {
      beforeEach(function () {
        ownerContext.ownerType = 'repository';
      });

      ['exact', 'similar'].forEach(function (matchState) {
        it(`updates vm.tabs to include tabs for non-unknown components if the matchState is ${matchState}`, function () {
          // first set a matchState that should not include the additional tabs
          controller.selectedComponent = { matchState: 'unknown' };
          $scope.$digest();

          expect(controller.tabs.length).toBeLessThan(8);

          // then set a matchState that should
          controller.selectedComponent = { matchState };
          $scope.$digest();

          expect(controller.tabs).toContain({
            name: 'componentInfo',
            displayName: 'Component Info',
          });

          expect(controller.tabs).toContain({
            name: 'policy',
            displayName: 'Policy',
          });

          expect(controller.tabs).toContain({
            name: 'similar',
            displayName: 'Similar',
          });

          expect(controller.tabs).not.toContain({
            name: 'occurrences',
            displayName: 'Occurrences',
          });

          expect(controller.tabs).toContain({
            name: 'licenses',
            displayName: 'Licenses',
          });

          expect(controller.tabs).toContain({
            name: 'vulnerabilities',
            displayName: 'Vulnerabilities',
          });

          expect(controller.tabs).toContain({
            name: 'labels',
            displayName: 'Labels',
          });

          expect(controller.tabs).not.toContain({
            name: 'auditLog',
            displayName: 'Audit Log',
          });
        });
      });

      it('updates vm.tabs to not include tabs for non-unknown components if the matchState is unknown', function () {
        // first set a matchState that should include all tabs
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'licenses',
          displayName: 'Licenses',
        });

        // then set a matchState that should clear out the additional tabs
        controller.selectedComponent = { matchState: 'unknown' };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'componentInfo',
          displayName: 'Component Info',
        });

        expect(controller.tabs).toContain({
          name: 'policy',
          displayName: 'Policy',
        });

        expect(controller.tabs).toContain({
          name: 'similar',
          displayName: 'Similar',
        });

        expect(controller.tabs).not.toContain({
          name: 'occurrences',
          displayName: 'Occurrences',
        });

        expect(controller.tabs).not.toContain({
          name: 'licenses',
          displayName: 'Licenses',
        });

        expect(controller.tabs).not.toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities',
        });

        expect(controller.tabs).not.toContain({
          name: 'labels',
          displayName: 'Labels',
        });

        expect(controller.tabs).not.toContain({
          name: 'auditLog',
          displayName: 'Audit Log',
        });
      });

      ['unknown', 'similar'].forEach(function (matchState) {
        it(`updates vm.tabs to include the Claim tab if the matchState is ${matchState}`, function () {
          // first set a matchState that should not include the additional tabs
          controller.selectedComponent = { matchState: 'exact' };
          $scope.$digest();

          expect(controller.tabs).not.toContain({
            name: 'claimComponent',
            displayName: 'Claim',
          });

          // then set a matchState that should
          controller.selectedComponent = { matchState };
          $scope.$digest();

          expect(controller.tabs).toContain({
            name: 'claimComponent',
            displayName: 'Claim',
          });

          expect(controller.tabs).not.toContain({
            name: 'occurrences',
            displayName: 'Occurrences',
          });

          expect(controller.tabs).not.toContain({
            name: 'auditLog',
            displayName: 'Audit Log',
          });
        });
      });

      it('updates vm.tabs to include the Claim tab if the matchState is exact and identificationSource is Manual', function () {
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).not.toContain({
          name: 'claimComponent',
          displayName: 'Claim',
        });

        controller.selectedComponent = {
          matchState: 'exact',
          identificationSource: 'Manual',
        };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'claimComponent',
          displayName: 'Claim',
        });

        expect(controller.tabs).not.toContain({
          name: 'occurrences',
          displayName: 'Occurrences',
        });

        expect(controller.tabs).not.toContain({
          name: 'auditLog',
          displayName: 'Audit Log',
        });
      });

      it('updates vm.tabs to not include the Vulnerabilities tab if the identificationSource is Manual', function () {
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities',
        });

        controller.selectedComponent = {
          matchState: 'exact',
          identificationSource: 'Manual',
        };
        $scope.$digest();

        expect(controller.tabs).not.toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities',
        });

        expect(controller.tabs).not.toContain({
          name: 'occurrences',
          displayName: 'Occurrences',
        });

        expect(controller.tabs).not.toContain({
          name: 'auditLog',
          displayName: 'Audit Log',
        });
      });
    });
  });

  describe('$destroy()', function () {
    let $scope, controller;

    beforeEach(inject(function (_$rootScope_) {
      $scope = _$rootScope_.$new();
      controller = $componentController('cipTabPanel', { $scope }, { selectedComponent: {} });
    }));

    it('unsubscribes from redux store', function () {
      spyOn(controller, 'reduxUnsubscribe');
      $scope.$destroy();
      expect(controller.reduxUnsubscribe).toHaveBeenCalledTimes(1);
    });

    it('closes the InnerSource producer report modal', function () {
      spyOn(controller, 'closeInnerSourceProducerReportModal');
      $scope.$destroy();
      expect(controller.closeInnerSourceProducerReportModal).toHaveBeenCalledTimes(1);
    });
  });

  describe('getSelectedTab()', function () {
    let $scope, $ngRedux, controller;

    beforeEach(inject(function (_$rootScope_) {
      $scope = _$rootScope_.$new();
    }));

    it('sets the selectedTab given the tabId query param', function () {
      $ngRedux = {
        connect: () => {
          return () => {
            return () => {};
          };
        },
        getState: () => {
          return {
            router: {
              currentParams: {
                tabId: 'tabId',
              },
            },
          };
        },
      };
      controller = $componentController('cipTabPanel', {
        $scope: $scope,
        $ngRedux: $ngRedux,
      });

      expect(controller.selectedTab).toBe('tabId');
    });

    it('sets the selectedTab to the componentInfo tab if tabId is undefined', function () {
      $ngRedux = {
        connect: () => {
          return () => {
            return () => {};
          };
        },
        getState: () => {
          return {
            router: {
              currentParams: {
                tabId: undefined,
              },
            },
          };
        },
      };
      controller = $componentController('cipTabPanel', {
        $scope: $scope,
        $ngRedux: $ngRedux,
      });

      expect(controller.selectedTab).toBe('componentInfo');
    });

    it('sets the selectedTab to the componentInfo tab if currentParams is undefined', function () {
      $ngRedux = {
        connect: () => {
          return () => {
            return () => {};
          };
        },
        getState: () => {
          return {
            router: {
              currentParams: undefined,
            },
          };
        },
      };
      controller = $componentController('cipTabPanel', {
        $scope: $scope,
        $ngRedux: $ngRedux,
      });

      expect(controller.selectedTab).toBe('componentInfo');
    });

    it('sets the selectedTab to the componentInfo tab if router is undefined', function () {
      $ngRedux = {
        connect: () => {
          return () => {
            return () => {};
          };
        },
        getState: () => {
          return {
            router: undefined,
          };
        },
      };
      controller = $componentController('cipTabPanel', {
        $scope: $scope,
        $ngRedux: $ngRedux,
      });

      expect(controller.selectedTab).toBe('componentInfo');
    });

    it('sets the selectedTab to the componentInfo tab if state is undefined', function () {
      $ngRedux = {
        connect: () => {
          return () => {
            return () => {};
          };
        },
        getState: () => {
          return undefined;
        },
      };
      controller = $componentController('cipTabPanel', {
        $scope: $scope,
        $ngRedux: $ngRedux,
      });

      expect(controller.selectedTab).toBe('componentInfo');
    });
  });
});
