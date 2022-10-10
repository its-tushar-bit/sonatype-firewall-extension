/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { displayWaiverScope } from '../../../main/frontend/util/waiverUtils';

describe('waiverUtils', function () {
  describe('dislayWaiverScope', () => {
    it('returns a readable label if the scopeOwnerType is `root_organization`', () => {
      const waiver = {
        scopeOwnerType: 'root_organization',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Root Organization');
    });

    it('returns a readable label with name if the scopeOwnerType is `organization`', () => {
      const waiver = {
        scopeOwnerType: 'organization',
        scopeOwnerName: 'a org',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Organization - a org');
    });

    it('returns a readable label with name if the scopeOwnerType is `application`', () => {
      const waiver = {
        scopeOwnerType: 'application',
        scopeOwnerName: 'App X',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Application - App X');
    });

    it('returns a readable label with name if the scopeOwnerType is `repository`', () => {
      const waiver = {
        scopeOwnerType: 'repository',
        scopeOwnerName: 'Repo X',
      };
      const result = displayWaiverScope(waiver);
      expect(result).toEqual('Repository - repository');
    });

    it('returns null if the scopeOwnerType is not valid', () => {
      let waiver = {
        scopeOwnerType: 'weird',
      };
      let result = displayWaiverScope(waiver);
      expect(result).toBeNull();

      waiver.scopeOwnerType = undefined;
      result = displayWaiverScope(waiver);
      expect(result).toBeNull();

      waiver.scopeOwnerType = null;
      result = displayWaiverScope(waiver);
      expect(result).toBeNull();
    });
  });
});
