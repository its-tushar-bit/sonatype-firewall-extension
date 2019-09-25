import samlModule from '../../../../main/frontend/configuration/saml/module';

describe('onFileChange', function() {
  let scope,
      element;

  beforeEach(angular.mock.module(samlModule.name));
  beforeEach(inject(
      function($rootScope, $compile) {
        scope = $rootScope.$new();
        scope.onFileChangeFunction = jasmine.createSpy();
        element = $compile('<div on-file-change="onFileChangeFunction(file)"></div>')(scope);
      }));

  it('sets the file variable to the element\'s first file, applies the given function, and sets its value to empty',
      function() {
        element[0].files = ['file'];
        element[0].value = 'value';
        element.trigger('change');

        expect(scope.onFileChangeFunction).toHaveBeenCalledWith(element[0].files[0]);
        expect(element[0].value).toBe('');
      });
});
