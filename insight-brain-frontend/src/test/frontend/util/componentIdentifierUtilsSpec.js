import {serializeComponentIdentifier} from '../../../main/frontend/util/componentIdentifierUtils';

describe('componentIdentifierUtils', function() {

  describe('serializeComponentIdentifier', function() {
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

    it('properly serializes componentIdentifier with sorted coordinates fields', function() {
      const unitSeparator = '\u001f';
      const recordSeparator = '\u001e';
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
});
