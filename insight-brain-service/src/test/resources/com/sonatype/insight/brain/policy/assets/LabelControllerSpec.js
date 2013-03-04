describe('LabelController itemLabel tests', function() {
	var scope,
	    compileElement,
	    setInput;

	beforeEach(module('Labels'));

	beforeEach(inject(function ($rootScope, $compile, $sniffer) {
		var inputElement;
		scope = $rootScope;
		compileInput = function (input) {
			inputElement = angular.element(input);
			var formElement = angular.element("<form name='form'></form>");
			formElement.append(inputElement);
			$compile(formElement)(scope);
		}
		setInput = function (val) {
			inputElement.val(val);

			var evt = document.createEvent('HTMLEvents');
			evt.initEvent(($sniffer.hasEvent('input')) ? 'input' : 'change', false, false);
			inputElement[0].dispatchEvent(evt);
		}
	}));

	it('Test No Spaces', function () {
		compileInput("<input type='text' maxlength='50' name='label' ng-model='label'  item-label />");
		setInput('foo');
		expect(scope.form.$invalid).toEqual(false);
		expect(scope.form.label.$error.invalid).toEqual(false);

		setInput('foo bar');
		expect(scope.form.$invalid).toEqual(true);
		expect(scope.form.label.$error.invalid).toEqual(true);
	});

	it('Test Non Empty', function () {
		compileInput("<input type='text' maxlength='50' name='label' ng-model='label'  item-label />");
		setInput('foo');
		expect(scope.form.$invalid).toEqual(false);
		expect(scope.form.label.$error.empty).toEqual(false);

		setInput('');
		expect(scope.form.$invalid).toEqual(true);
		expect(scope.form.label.$error.empty).toEqual(true);
	});

	it('Test Duplicate', function () {
		scope.selectedLabel = {};
		compileInput("<input type='text' maxlength='50' name='label' ng-model='selectedLabel.label'  item-label />");
		scope.labels = [{ id : 'bar', label : 'bar' }];
		setInput('foo');
		expect(scope.form.$invalid).toEqual(false);
		expect(scope.form.label.$error.duplicate).toEqual(false);

		setInput('bar');
		expect(scope.form.$invalid).toEqual(true);
		expect(scope.form.label.$error.duplicate).toEqual(true);
	});
});