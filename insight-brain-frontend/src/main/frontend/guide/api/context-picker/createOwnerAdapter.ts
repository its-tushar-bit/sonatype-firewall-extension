/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import type { OwnerAdapter } from 'GuideRoot/components/navigation/context-picker/OwnerAdapter';
import { SelfHostedOwnerAdapter } from './SelfHostedOwnerAdapter';
import { MockOwnerAdapter } from './MockOwnerAdapter';

/**
 * Selects the {@link OwnerAdapter} the running Guide SPA uses for the policy-context picker.
 *
 * TODO(GUIDE-3046): temporary bridge. The `/api/v2/policy-context/owners/*` endpoints ship in
 * GUIDE-3046 (open at time of writing), so until they land the dev build runs against
 * {@link MockOwnerAdapter} so the picker is developable end-to-end. Production always uses the
 * real {@link SelfHostedOwnerAdapter}. `process.env.NODE_ENV` is a build-time constant (esbuild
 * `define`), so the mock branch is dead-code-eliminated from the production bundle. When
 * GUIDE-3046 merges, delete this file and instantiate {@link SelfHostedOwnerAdapter} directly.
 */
export function createOwnerAdapter(): OwnerAdapter {
  if (process.env.NODE_ENV !== 'production') {
    return new MockOwnerAdapter();
  }
  return new SelfHostedOwnerAdapter();
}
