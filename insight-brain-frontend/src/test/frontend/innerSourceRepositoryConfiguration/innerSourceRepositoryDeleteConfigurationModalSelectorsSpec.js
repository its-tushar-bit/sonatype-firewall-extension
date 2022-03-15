/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('innerSourceRepositoryDeleteConfigurationsSelectors', function () {
  let selectRepositoryConnectionId;

  beforeEach(() => {
    const module = require('inject-loader!../../../../src/main/frontend/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSelectors')();
    ({ selectRepositoryConnectionId } = module);
  });

  describe('selectRepositoryConnectionId', () => {
    it('selects `selectRepositoryConnectionId`', () => {
      const state = {
        innerSourceRepositoryDeleteConfigurationModal: {
          repositoryConnectionId: 'someRepositoryConnections',
        },
      };
      expect(selectRepositoryConnectionId(state)).toBe('someRepositoryConnections');
    });
  });
});
