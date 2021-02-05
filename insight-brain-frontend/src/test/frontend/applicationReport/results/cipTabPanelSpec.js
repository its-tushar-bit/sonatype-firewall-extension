/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';

describe('cipTabPanel', function() {
  let $componentController,
      $httpBackend,
      CLMLocations;

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(inject(function(_$componentController_, _$httpBackend_, _CLMLocations_) {
    $componentController = _$componentController_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
  }));

  it('sets the initial value of vm.selectedTab to componentInfo', function() {
    const controller = $componentController('cipTabPanel');

    expect(controller.selectedTab).toBe('componentInfo');
  });

  describe('selectedComponent watcher', function() {
    let $scope,
        controller,
        innerSourceComponent;

    beforeEach(inject(function($rootScope) {
      $scope = $rootScope.$new();
      controller = $componentController('cipTabPanel', { $scope }, { selectedComponent: {} });

      innerSourceComponent = {
        hash: '1249e25aebb15358bedd',
        matchState: 'test-match-state',
        identificationSource: 'test-identification-source',
        componentIdentifier: {
          coordinates: 'coordinates',
          format: 'format'
        },
        dependencyInfo: {isDirectDependency: false},
        innerSourceData: {
          innerSource: true,
          ownerApplicationId: 'id',
          ownerApplicationName: 'appName',
          ownerComponentName: 'componentName'
        }
      };
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('sets vm.latestReportUrl if already loaded', function() {
      controller.selectedComponent = {
        ...innerSourceComponent,
        latestReport: {
          stage: 'stage',
          url: 'latestReportUrl'
        }
      };
      $scope.$digest();
      expect(controller.selectedComponent.latestReport.url).toContain('latestReportUrl');
    });

    it('sets vm.latestReportUrl with InnerSource report for latest stage', function() {
      const mockResponse = [
        {
          stage: 'build',
          latestReportHtmlUrl: 'buildUrl'
        },
        {
          stage: 'release',
          latestReportHtmlUrl: 'releaseUrl'
        },
        {
          stage: 'develop',
          latestReportHtmlUrl: 'developUrl'
        }
      ];

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(200, mockResponse);
      controller.selectedComponent = innerSourceComponent;
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.selectedComponent.latestReport.url).toContain('releaseUrl');
    });

    it('handle the error action if request fails', function() {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationReportsUrl('id'))).respond(500, 'error');
      controller.selectedComponent = innerSourceComponent;
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.error).toContain('error');
      expect(controller.selectedComponent.latestReport).toBeUndefined();
    });

    ['exact', 'similar'].forEach(function(matchState) {
      it(`updates vm.tabs to include tabs for non-unknown components if the matchState is ${matchState}`, function() {
        // first set a matchState that should not include the additional tabs
        controller.selectedComponent = { matchState: 'unknown' };
        $scope.$digest();

        expect(controller.tabs.length).toBeLessThan(8);

        // then set a matchState that should
        controller.selectedComponent = { matchState };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'componentInfo',
          displayName: 'Component Info'
        });

        expect(controller.tabs).toContain({
          name: 'policy',
          displayName: 'Policy'
        });

        expect(controller.tabs).toContain({
          name: 'similar',
          displayName: 'Similar'
        });

        expect(controller.tabs).toContain({
          name: 'occurrences',
          displayName: 'Occurrences'
        });

        expect(controller.tabs).toContain({
          name: 'licenses',
          displayName: 'Licenses'
        });

        expect(controller.tabs).toContain({
          name: 'vulnerabilities',
          displayName: 'Vulnerabilities'
        });

        expect(controller.tabs).toContain({
          name: 'labels',
          displayName: 'Labels'
        });

        expect(controller.tabs).toContain({
          name: 'auditLog',
          displayName: 'Audit Log'
        });
      });
    });

    it('updates vm.tabs to not include tabs for non-unknown components if the matchState is unknown', function() {
      // first set a matchState that should include all tabs
      controller.selectedComponent = { matchState: 'exact' };
      $scope.$digest();

      expect(controller.tabs).toContain({
        name: 'licenses',
        displayName: 'Licenses'
      });

      // then set a matchState that should clear out the additional tabs
      controller.selectedComponent = { matchState: 'unknown' };
      $scope.$digest();

      expect(controller.tabs).toContain({
        name: 'componentInfo',
        displayName: 'Component Info'
      });

      expect(controller.tabs).toContain({
        name: 'policy',
        displayName: 'Policy'
      });

      expect(controller.tabs).toContain({
        name: 'similar',
        displayName: 'Similar'
      });

      expect(controller.tabs).toContain({
        name: 'occurrences',
        displayName: 'Occurrences'
      });

      expect(controller.tabs).not.toContain({
        name: 'licenses',
        displayName: 'Licenses'
      });

      expect(controller.tabs).not.toContain({
        name: 'vulnerabilities',
        displayName: 'Vulnerabilities'
      });

      expect(controller.tabs).not.toContain({
        name: 'labels',
        displayName: 'Labels'
      });

      expect(controller.tabs).not.toContain({
        name: 'auditLog',
        displayName: 'Audit Log'
      });
    });

    ['unknown', 'similar'].forEach(function(matchState) {
      it(`updates vm.tabs to include the Claim tab if the matchState is ${matchState}`, function() {
        // first set a matchState that should not include the additional tabs
        controller.selectedComponent = { matchState: 'exact' };
        $scope.$digest();

        expect(controller.tabs).not.toContain({
          name: 'claimComponent',
          displayName: 'Claim'
        });

        // then set a matchState that should
        controller.selectedComponent = { matchState };
        $scope.$digest();

        expect(controller.tabs).toContain({
          name: 'claimComponent',
          displayName: 'Claim'
        });

      });
    });

    it('updates vm.tabs to include the Claim tab if the matchState is exact and identificationSource is Manual',
        function() {
          controller.selectedComponent = { matchState: 'exact' };
          $scope.$digest();

          expect(controller.tabs).not.toContain({
            name: 'claimComponent',
            displayName: 'Claim'
          });

          controller.selectedComponent = { matchState: 'exact', identificationSource: 'Manual' };
          $scope.$digest();

          expect(controller.tabs).toContain({
            name: 'claimComponent',
            displayName: 'Claim'
          });
        }
    );

    it('updates vm.tabs to not include the Vulnerabilities tab if the identificationSource is Manual', function() {
      controller.selectedComponent = { matchState: 'exact' };
      $scope.$digest();

      expect(controller.tabs).toContain({
        name: 'vulnerabilities',
        displayName: 'Vulnerabilities'
      });

      controller.selectedComponent = { matchState: 'exact', identificationSource: 'Manual' };
      $scope.$digest();

      expect(controller.tabs).not.toContain({
        name: 'vulnerabilities',
        displayName: 'Vulnerabilities'
      });
    });

    it('sets vm.selectedTab to the name of the first tab if its previous value is not present in vm.tabs', function() {
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
  });
});
