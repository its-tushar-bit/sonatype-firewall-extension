/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getProductLogo } from '../../../main/frontend/util/productLogoUtils';

describe('ProductLogo Utils', function () {
  describe('getProductLogo', function () {
    it('returns a sonatype logo if no product name is specified', function () {
      expect(getProductLogo()).toEqual('images/sonatype.svg');
    });
    it('returns a sonatype logo if an unknown product name is specified', function () {
      expect(getProductLogo('unknown')).toEqual('images/sonatype.svg');
      expect(getProductLogo('weird')).toEqual('images/sonatype.svg');
      expect(getProductLogo('whatever')).toEqual('images/sonatype.svg');
      expect(getProductLogo('')).toEqual('images/sonatype.svg');
    });
    it('returns a firewall logo if Firewall product name is specified — regardless of case', function () {
      expect(getProductLogo('repository firewall')).toEqual('images/nexus_firewall.svg');
      expect(getProductLogo('REPOSITORY FIREWALL')).toEqual('images/nexus_firewall.svg');
      expect(getProductLogo('rePositorY fIrEwAlL')).toEqual('images/nexus_firewall.svg');
    });
    it('returns a lifecycle logo if lifecycle product name is specified — regardless of case', function () {
      expect(getProductLogo('lifecycle')).toEqual('images/nexus_lifecycle.svg');
      expect(getProductLogo('LIFECYCLE')).toEqual('images/nexus_lifecycle.svg');
      expect(getProductLogo('lIfEcYcLe')).toEqual('images/nexus_lifecycle.svg');
    });
    it('returns a lifecycle logo if lifecycle foundation product name is specified — regardless of case', function () {
      expect(getProductLogo('lifecycle foundation')).toEqual('images/nexus_lifecycle.svg');
      expect(getProductLogo('LIFECYCLE FOUNDATION')).toEqual('images/nexus_lifecycle.svg');
      expect(getProductLogo('lIfEcYcLe FoUnDaTiOn')).toEqual('images/nexus_lifecycle.svg');
    });
    it('returns an auditor logo if auditor product name is specified — regardless of case', function () {
      expect(getProductLogo('auditor')).toEqual('images/nexus_auditor.svg');
      expect(getProductLogo('AUDITOR')).toEqual('images/nexus_auditor.svg');
      expect(getProductLogo('aUdItOr')).toEqual('images/nexus_auditor.svg');
    });
  });
});
