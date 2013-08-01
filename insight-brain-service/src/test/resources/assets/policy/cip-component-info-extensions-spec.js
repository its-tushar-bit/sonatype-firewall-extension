/* global window */
var applicationId = 'appId', CLM = {
  path: '../brain/'
};
describe('CIP Component info extensions tests', function() {
  var $http, $scope, controller;

  angular.module('Hudson', []).factory('hudson', ['$http', function($http) {
    return $http;
  }]);

  beforeEach(module('ComponentInfo'));

  beforeEach(inject(function($rootScope, $compile, $controller) {
    var node = $("<table id='infoPanelArtifactTable'><tr></tr></table>");
    node.appendTo('body');
    $scope = $rootScope.$new();
    $compile(node)($scope);
    controller = $controller('ComponentInfoController', {
      $scope: $scope
    });
  }));

  afterEach(function() {
    $('#infoPanelArtifactTable').remove();
    $('#componentExistingWaiverModal').remove();
  });

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
  }));

  xit('Validate button and modal injected', inject(function($httpBackend, $controller) {
    $.event.trigger("artifactInfoPanelLoading", {
      gav: {
        hash: '1234'
      }
    });

    expect($('button[data-target="#componentExistingWaiverModal"]').length).toEqual(1);

    expect($('#componentExistingWaiverModal').length).toEqual(1);

    $httpBackend.expectGET(CLM.path + 'rest/policyWaiver/application/appId/component/1234').respond([]);
    $httpBackend.expectGET(CLM.path + 'rest/policy/application/appId/applicable').respond([]);

    $('button[data-target="#componentExistingWaiverModal"]').trigger('click');

    expect($scope.hash).toEqual('1234');
    expect($scope.applicationId).toEqual('appId');
  }));
});