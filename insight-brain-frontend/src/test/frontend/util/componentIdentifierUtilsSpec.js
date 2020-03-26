/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  serializeComponentIdentifier,
  getDependencyInfoComponentId
} from '../../../main/frontend/util/componentIdentifierUtils';

describe('componentIdentifierUtils', function() {
  const unitSeparator = '\u001f';
  const recordSeparator = '\u001e';
  const componentIdentifier = {
    format: 'maven',
    coordinates: {
      extension: 'jar',
      classifier: '',
      version: '0.9.5',
      artifactId: 'openid4java',
      groupId: 'org.openid4java',
      foo: 'bar baz',
      spaceValue: ' '
    }
  };

  describe('serializeComponentIdentifier', function() {
    it('properly serializes componentIdentifier with sorted coordinates fields', function() {
      const expectedValue =
          'maven:' +
          'artifactId' + unitSeparator + 'openid4java' + recordSeparator +
          'classifier' + unitSeparator + recordSeparator +
          'extension' + unitSeparator + 'jar' + recordSeparator +
          'foo' + unitSeparator + 'bar baz' + recordSeparator +
          'groupId' + unitSeparator + 'org.openid4java' + recordSeparator +
          'spaceValue' + unitSeparator + ' ' + recordSeparator +
          'version' + unitSeparator + '0.9.5';
      expect(serializeComponentIdentifier(componentIdentifier)).toEqual(expectedValue);
    });
  });

  describe('getDependencyInfoComponentId', function() {
    it('properly serializes componentIdentifier omitting extension and classifier', function() {
      const expectedValue =
          'maven:' +
          'artifactId' + unitSeparator + 'openid4java' + recordSeparator +
          'foo' + unitSeparator + 'bar baz' + recordSeparator +
          'groupId' + unitSeparator + 'org.openid4java' + recordSeparator +
          'spaceValue' + unitSeparator + ' ' + recordSeparator +
          'version' + unitSeparator + '0.9.5';
      expect(getDependencyInfoComponentId(componentIdentifier)).toEqual(expectedValue);
    });
  });
});
