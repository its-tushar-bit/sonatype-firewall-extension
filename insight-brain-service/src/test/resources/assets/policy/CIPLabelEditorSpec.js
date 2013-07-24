/*global window*/
var CLM = { path : '../brain/' };

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
	}));

	it('Test Add Label', function () {
		$http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00').respond([{ "label" : "foo", "color" : "black"},{ "label" : "bar", "color" : "yellow"},{ "label" : "asdf", "color" : null}]);
		$http.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).respond([{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"}]);

		scope.$apply(function () {
			scope.labelInput = 'bar asdf';
		});
		scope.addLabels();
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
		scope.$apply(function () {
			scope.labelInput = 'foo';
		});

		$http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00').respond([{ "label" : "foo", "color" : "black"}]);
		scope.addLabels();
		expect(scope.itemLabels.length).toEqual(1);
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

	it('Test Excess Spaces', function () {
		scope.$apply(function () {
		    scope.labelInput = ' bar  label ';
		});
		$http.expectPUT('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00', { labels: ['foo', 'bar', 'label'], color : null}).respond(function () {
			return [200, ['bar', 'foo']];
		});
		$http.expectGET(new RegExp('\\.\\./brain/rest/label/application/bom1-12345678\\?inherit=true&timestamp=[0-9]+')).respond([{ "label" : "foo", "color" : "black"}, { "label" : "asdf"}, { "label" : "bar"}]);
		scope.addLabels();
		$http.flush();
	});
});