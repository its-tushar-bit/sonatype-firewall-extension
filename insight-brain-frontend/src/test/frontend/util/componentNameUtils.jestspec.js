/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  getComponentName,
  getArtifactName,
  getComponentNameWithoutVersion,
  getFilenameFromPath,
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

    it('sets up a component name from componentName', function () {
      const component = { componentName: 'some component name' };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('some component name');
    });

    it('assigns unknown to components without any identification', function () {
      const component = {};
      const componentName = getComponentName(component);
      expect(componentName).toEqual('Unknown');
    });

    it('sets up a component name from Pathname', function () {
      const component = {
        displayName: {
          parts: [{ field: 'Pathname', value: 'asd/123/asd/123/verylongpath/almost-there/asd.jpg' }],
        },
        filename: null,
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('asd.jpg');
    });

    it('sets up a component name from componentIdentifier', function () {
      const component = {
        componentIdentifier: {
          coordinates: {
            packageId: 'asd',
            version: '123',
          },
        },
      };
      const componentName = getComponentName(component);
      expect(componentName).toEqual('asd : 123');
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

  describe('getFilenameFromPath', function () {
    it('returns the filename from the path', function () {
      const filenamePath = 'bac/acs/asdas/file.jpg';
      const filename = getFilenameFromPath(filenamePath);
      expect(filename).toEqual('file.jpg');
    });

    it('returns the string passed if is not a path', function () {
      const filenamePath = 'my file';
      const filename = getFilenameFromPath(filenamePath);
      expect(filename).toEqual(filenamePath);
    });
  });

  describe('getComponentNameWithoutVersion', function () {
    it('removes the "Version" value and trailing colon from display name parts', function () {
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

      const allVersionsComponentName = getComponentNameWithoutVersion(component);
      expect(allVersionsComponentName).toEqual('Foo : Bar');
    });

    it('sets up a component name from filename', function () {
      const component = {
        displayName: null,
        filename: 'foo.js',
        filenames: ['bar.jar', 'baz.jar'],
      };
      const allVersionsComponentName = getComponentNameWithoutVersion(component);
      expect(allVersionsComponentName).toEqual('foo.js');
    });

    it('sets up a component name from filenames', function () {
      const component = {
        displayName: null,
        filename: null,
        filenames: ['foo.jar', 'bar.jar'],
      };
      const allVersionsComponentName = getComponentNameWithoutVersion(component);
      expect(allVersionsComponentName).toEqual('foo.jar, bar.jar');
    });

    it('sets up a component name from componentIdentifier', function () {
      const component = {
        componentIdentifier: {
          coordinates: {
            packageId: 'asd',
            version: '123',
          },
        },
      };
      const componentName = getComponentNameWithoutVersion(component);
      expect(componentName).toEqual('asd');
    });

    it('sets up a component name from componentName', function () {
      const component = { componentName: 'some component name' };
      const allVersionsComponentName = getComponentNameWithoutVersion(component);
      expect(allVersionsComponentName).toEqual('some component name');
    });

    it('assigns unknown to components without any identification', function () {
      const component = {};
      const allVersionsComponentName = getComponentNameWithoutVersion(component);
      expect(allVersionsComponentName).toEqual('Unknown');
    });
  });
});
