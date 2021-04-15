/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window*/
describe('ComponentUtils tests', function () {
  beforeEach(angular.mock.module('ComponentUtils'));

  describe('We are able to generate a display name for an unknown component', function () {
    it('Can set the display name and coordinates for an unknown component', inject(function (ComponentUtil) {
      var component = { filenames: ['foo.jar', 'bar.jar'] };
      ComponentUtil.setDisplayNameAndCoordinates(component);
      expect(component.componentIdentifier).toBeFalsy();
      expect(component.displayName).toBeTruthy();
      expect(component.displayName.parts.length).toBe(3);
      expect(component.displayName.parts[0].field).toBe('Filename');
      expect(component.displayName.parts[0].value).toBe('foo.jar');
      expect(component.displayName.parts[1].field).toBeFalsy();
      expect(component.displayName.parts[1].value).toBe(', ');
      expect(component.displayName.parts[2].field).toBe('Filename');
      expect(component.displayName.parts[2].value).toBe('bar.jar');
    }));

    it('Can set the display name and coordinates for an anonymized component', inject(function (ComponentUtil) {
      var component = { hash: 'hash' };
      ComponentUtil.setDisplayNameAndCoordinates(component);
      expect(component.componentIdentifier).toBeFalsy();
      expect(component.displayName).toBeTruthy();
      expect(component.displayName.parts.length).toBe(2);
      expect(component.displayName.parts[0].value).toBe('(Anonymized Path) SHA1: ');
      expect(component.displayName.parts[1].field).toBe('Hash');
      expect(component.displayName.parts[1].value).toBe(component.hash);
    }));
  });

  describe('We are able to enhance a legacy data structure with componentIdentifier', function () {
    it('Can enhance reports with unknown componentIdentifier', inject(function (ComponentUtil) {
      var component = {};
      ComponentUtil.enhanceWithComponentIdentifier(component);
      expect(component.componentIdentifier).toBeFalsy();
    }));

    it('Can enhance reports with GAV(EC) only', inject(function (ComponentUtil) {
      //extension and classifier are included as they may be present for claimed components
      var component = {
        groupId: 'g',
        artifactId: 'a',
        version: 'v',
        extension: 'e',
        classifier: 'c',
      };
      var copy = angular.copy(component);
      ComponentUtil.enhanceWithComponentIdentifier(copy);
      expect(copy.componentIdentifier).toBeTruthy();
      expect(copy.componentIdentifier.format).toBe('maven');
      expect(copy.componentIdentifier.coordinates).toEqual(component);
    }));
  });
});
