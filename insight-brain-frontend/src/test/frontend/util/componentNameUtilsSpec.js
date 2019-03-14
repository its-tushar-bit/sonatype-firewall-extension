import { getComponentName } from '../../../main/frontend/util/componentNameUtils';

describe('componentNameUtils', function() {
  describe('getComponentName', function() {
    it('sets up a component name from displayName', function() {
      const component = {
        displayName: {
          parts: [
            {field: 'Group', value: 'Foo'},
            {value: ' : '},
            {field: 'Artifact', value: 'Bar'},
            {value: ' : '},
            {field: 'Version', value: '1.0'}
          ]
        },
        filename: 'foo.js',
        filenames: ['foo.jar', 'bar.jar']
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('Foo : Bar : 1.0');
    });

    it('sets up a component name from filename', function() {
      const component = {
        displayName: null,
        filename: 'foo.js',
        filenames: ['bar.jar', 'baz.jar']
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('foo.js');
    });

    it('sets up a component name from filenames', function() {
      const component = {
        displayName: null,
        filename: null,
        filenames: ['foo.jar', 'bar.jar']
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('foo.jar, bar.jar');
    });

    it('assigns unknown to components without any identification', function() {
      const component = {};
      const componentName = getComponentName(component);
      expect(componentName).toEqual('Unknown');
    });
  });
});
