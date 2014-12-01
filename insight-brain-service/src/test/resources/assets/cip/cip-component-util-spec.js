/*global window*/
describe('ComponentUtils tests', function() {

  beforeEach(module('ComponentUtils'));

  it('Can enhance reports with unknown componentIdentifier', inject(function(ComponentUtil) {
    var component = {};
    ComponentUtil.enhanceWithComponentIdentifier(component);
    expect(component.componentIdentifier).toBeFalsy();
  }));

  it('Can enhance reports with GAV(EC) only', inject(function(ComponentUtil) {
    //extension and classifier are included as they may be present for claimed components
    var component = {
      groupId: 'g',
      artifactId: 'a',
      version: 'v',
      extension: 'e',
      classifier: 'c'
    };
    var copy = angular.copy(component);
    ComponentUtil.enhanceWithComponentIdentifier(copy);
    expect(copy.componentIdentifier).toBeTruthy();
    expect(copy.componentIdentifier.format).toBe('maven');
    expect(copy.componentIdentifier.coordinates).toEqual(component);
  }));
});
