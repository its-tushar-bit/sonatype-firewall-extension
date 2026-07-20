/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getOwnerScope, setOwnerScope, _resetOwnerScopeForTests } from 'GuideRoot/api/ownerScope';

describe('ownerScope', () => {
  afterEach(() => _resetOwnerScopeForTests());

  it('defaults to null (root organization)', () => {
    expect(getOwnerScope()).toBeNull();
  });

  it('holds the id set via setOwnerScope', () => {
    setOwnerScope('payments');
    expect(getOwnerScope()).toBe('payments');
  });

  it('clears back to null when set to null', () => {
    setOwnerScope('payments');
    setOwnerScope(null);
    expect(getOwnerScope()).toBeNull();
  });

  it('_resetOwnerScopeForTests restores the default', () => {
    setOwnerScope('payments');
    _resetOwnerScopeForTests();
    expect(getOwnerScope()).toBeNull();
  });
});
