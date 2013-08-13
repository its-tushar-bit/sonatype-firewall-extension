/*global window*/
describe('ComponentLabelEditor tests', function() {
	var scope, $http;

	angular.module('TestGavProvider', []).service('ComponentLabelEditorGAV', function () {
		return {
			hash : '3102cdd0edd5a05afe00',
			applicationId : 'bom1-12345678'
		};
	});

	beforeEach(module('ComponentLabelEditor', 'TestGavProvider'));
	//setup our http backend to return what we want
	beforeEach(inject(function ($rootScope, $controller, $httpBackend) {
		$http = $httpBackend;

		$httpBackend.expectGET(new RegExp('\\.\\./brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00\\?timestamp=[0-9]+')).
			respond([{ "label" : "foo", "color" : "black"}]);
		$httpBackend.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).
			respond([{ "label" : "foo", "color" : "black"}]);

		scope = $rootScope.$new();
		$controller('LabelsController', {$scope: scope, global: {}});
		$httpBackend.flush();
	}));
	
	afterEach(inject(function ($httpBackend) {
		$httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
	}));

	it('Test Add Label', function () {
		$http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00',{ "labels" : ["foo","bar"]}).respond([{ "label" : "foo", "color" : "black"},{ "label" : "bar", "color" : "yellow"}]);
		$http.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).respond([{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"}]);

    scope.addLabel({
      label: 'bar'
    });
		$http.flush();
		expect(scope.itemLabels.length).toEqual(2);

    $http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00',{ "labels" : ["foo","bar","asdf"]}).respond([{ "label" : "foo", "color" : "black"},{ "label" : "bar", "color" : "yellow"},{ "label" : "asdf"}]);
    $http.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).respond([{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"},{ "label" : "asdf"}]);

    scope.addLabel({
      label: 'asdf'
    });
    $http.flush();
    expect(scope.itemLabels.length).toEqual(3);
    
		var barLabel = null;
		angular.forEach(scope.itemLabels, function(item, key){
			if (item.label === 'bar') {
				barLabel = item;
			}
		});
		expect(barLabel).toNotEqual(null);
		expect(barLabel.color).toEqual('yellow');
	});
	
	it('Test Duplicate Ignored', function () {
    $http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00',{ "labels" : ["foo","bar"]}).respond([{ "label" : "foo", "color" : "black"},{ "label" : "bar", "color" : "yellow"}]);
    $http.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).respond([{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"}]);

    scope.addLabel({
      label: 'bar'
    });
    $http.flush();
    expect(scope.itemLabels.length).toEqual(2);

    scope.addLabel({
      label: 'bar'
    });
    
    //note that i am using the afterEach to validate noRequests/expectations to finish this test
    
    expect(scope.itemLabels.length).toEqual(2);
	});

	it('Test Remove', function () {
		$http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00').respond([]);
		$http.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).respond([{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"}]);
		scope.removeLabel( scope.itemLabels[0] );
		$http.flush();
		expect( scope.itemLabels.length ).toEqual(0);
	});

	it('Test Filter', function () {
		scope.itemLabels = [{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"}];
		expect(scope.isApplied({ "label" : "bbb"})).toEqual(true);
		expect(scope.isApplied({ "label" : "foo"})).toEqual(false);
	});
});