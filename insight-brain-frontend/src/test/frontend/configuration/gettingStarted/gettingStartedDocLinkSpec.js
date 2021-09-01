/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxTextLink } from '@sonatype/react-shared-components';
import GettingStartedDocLink from '../../../../main/frontend/configuration/gettingStarted/components/GettingStartedDocLink';
import * as enzymeUtils from '../../enzymeUtils';

describe('gettingStartedDocLink', function () {
  let getShallow, initialProps;

  beforeEach(() => {
    initialProps = {
      href: 'testLinkHref',
      text: 'gettingStartedDocLink',
    };
    getShallow = enzymeUtils.getShallowComponent(GettingStartedDocLink, initialProps);
  });

  describe('load', function () {
    it('gettingStarted doc link exists', function () {
      expect(getShallow().find(NxTextLink)).toExist();
    });

    it('has "href" prop set correctly', function () {
      const docLink = getShallow().find(NxTextLink);
      expect(docLink).toHaveProp('href', 'testLinkHref');
    });
  });
});
