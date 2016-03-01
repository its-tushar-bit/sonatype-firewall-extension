describe('error.modal.controller.spec.js', function() {
  beforeEach(module('utility'));

  var vm;

  beforeEach(inject(function($controller) {
    vm = $controller('error.modal.controller', {
      headerText: 'my header',
      bodyText: 'my body'
    });
  }));

  it('sets modal header and body', function() {
    expect(vm.headerText).toBe('my header');
    expect(vm.bodyText).toBe('my body');
  });
});
