describe('helpMenu', function() {
  var vm;

  beforeEach(module('mainHeader'));

  beforeEach(inject(function($componentController) {
    window.clmServerVersion = '1.2.3-4';
    vm = $componentController('helpMenu');
  }));

  it('Major Minor Version', function () {
    expect(vm.majorMinorVersion).toEqual('1.2');
  });
});
