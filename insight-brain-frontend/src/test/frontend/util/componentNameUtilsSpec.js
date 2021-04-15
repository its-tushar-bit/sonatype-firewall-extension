/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getComponentName,
  getArtifactName,
} from '../../../main/frontend/util/componentNameUtils';

describe('componentNameUtils', function () {
  describe('getComponentName', function () {
    it('sets up a component name from displayName', function () {
      const component = {
        displayName: {
          parts: [
            { field: 'Group', value: 'Foo' },
            { value: ' : ' },
            { field: 'Artifact', value: 'Bar' },
            { value: ' : ' },
            { field: 'Version', value: '1.0' },
          ],
        },
        filename: 'foo.js',
        filenames: ['foo.jar', 'bar.jar'],
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('Foo : Bar : 1.0');
    });

    it('sets up a component name from filename', function () {
      const component = {
        displayName: null,
        filename: 'foo.js',
        filenames: ['bar.jar', 'baz.jar'],
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('foo.js');
    });

    it('sets up a component name from filenames', function () {
      const component = {
        displayName: null,
        filename: null,
        filenames: ['foo.jar', 'bar.jar'],
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('foo.jar, bar.jar');
    });

    it('assigns unknown to components without any identification', function () {
      const component = {};
      const componentName = getComponentName(component);
      expect(componentName).toEqual('Unknown');
    });
  });

  describe('getArtifactName', function () {
    it('sets up an artifact name from displayName using the name prop', function () {
      const component = {
        displayName: {
          parts: [
            { field: 'Group', value: 'Foo' },
            { value: ' : ' },
            { field: 'Artifact', value: 'Bar' },
            { value: ' : ' },
            { field: 'Version', value: '1.0' },
          ],
          name: 'Baz',
        },
        filename: 'foo.js',
      };
      const artifactName = getArtifactName(component);
      expect(artifactName).toEqual('Baz');
    });

    it('sets up an artifact name from filename', function () {
      const component = {
        displayName: null,
        filename: 'foo.js',
      };
      const artifactName = getArtifactName(component);
      expect(artifactName).toEqual('foo.js');
    });

    it('assigns unknown to components without any identification', function () {
      const component = {};
      const componentName = getComponentName(component);
      expect(componentName).toEqual('Unknown');
    });
  });
});
