/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('artifactoryRepositoryDeleteConfigurationsSelectors', function () {
  let selectArtifactoryConnectionId;

  beforeEach(() => {
    const module = require('inject-loader!../../../../src/main/frontend/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSelectors')();
    ({ selectArtifactoryConnectionId } = module);
  });

  describe('selectArtifactoryConnectionId', () => {
    it('selects `selectArtifactoryConnectionId`', () => {
      const state = {
        artifactoryRepositoryDeleteConfigurationModal: {
          artifactoryConnectionId: 'someArtifactoryConnection',
        },
      };
      expect(selectArtifactoryConnectionId(state)).toBe('someArtifactoryConnection');
    });
  });
});
