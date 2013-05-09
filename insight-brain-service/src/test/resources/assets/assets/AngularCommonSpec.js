describe('AngularCommon', function () {
	var scope, compile, httpBackend, regex;

	beforeEach(module('AngularCommon'));
	beforeEach(inject(function ($httpBackend, $rootScope, $compile, regexFactory) {
		scope = $rootScope.$new();
		compile = $compile;
		httpBackend = $httpBackend;
		regex = regexFactory;
	}));

	it('implements errorModal directive', function () {
		httpBackend.expectGET('../assets/components/errorModal.html').respond("<div id='errorModal'></div>");
		var element = compile("<div error-Modal></div>")(scope);
		expect(element).not.toBeUndefined();
	});

	it('provides regex to match unicode characters', function () {
		var allLettersRegex = new RegExp('[' + regex.allLetters().source + ']');
		expect('a'.match(allLettersRegex)).toBeTruthy();
		expect('ñ'.match(allLettersRegex)).toBeTruthy();
		expect('Ҙ'.match(allLettersRegex)).toBeTruthy();
		expect('長'.match(allLettersRegex)).toBeTruthy();
		expect('!'.match(allLettersRegex)).not.toBeTruthy();
		expect('$'.match(allLettersRegex)).not.toBeTruthy();
	});
});