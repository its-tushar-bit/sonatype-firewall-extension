/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import fs from 'fs';
import path from 'path';

const FRONTEND = path.resolve(__dirname, '../../../main/frontend');

// Targets are normalised to a frontend-relative path so the two files can be compared
// regardless of how each spells the same partial.
function useTargets(scssPath: string): Set<string> {
  const absolute = path.join(FRONTEND, scssPath);
  const content = fs.readFileSync(absolute, 'utf8');
  const dir = path.dirname(absolute);
  return new Set(
    [...content.matchAll(/@use\s+'([^']+)'/g)]
      .map((m) => m[1])
      .map((target) => path.relative(FRONTEND, path.resolve(dir, target)))
  );
}

const scssUses = useTargets('scss/scss.scss');
const aggregateUses = useTargets('scss/waiversEmbed.scss');

describe('waiversEmbed.scss', () => {
  // Nexus One wraps the standalone Classic waiver pages in ClassicComponentMount but does not
  // load scss.scss. The wrapper only positions them, so these partials are what keeps the
  // forms from rendering unstyled. Why each is needed is documented in waiversEmbed.scss.
  //
  // Hand-maintained allowlist of partials the wrapped waiver pages need. Page-level:
  //   addWaiver                            -> waivers/addWaiver
  //   requestWaiver, requestWaiverUpdate   -> waivers/requestWaiver (same component)
  //   requestWaiverReview                  -> waivers/requestWaiverReview
  //   dashboardFirewallWaiverRequestReview -> firewall/waiverRequests/firewallWaiverRequests
  // Shared styling those pages also render:
  //   threat-level label                   -> scss/iq-threat-levels
  //   <ViolationExclamation>               -> react/iqViolationExclamation
  it('includes the waiver-page partials scss.scss loads', () => {
    [
      'waivers/addWaiver',
      'waivers/requestWaiver',
      'waivers/requestWaiverReview',
      'firewall/waiverRequests/firewallWaiverRequests',
      'scss/iq-threat-levels',
      'react/iqViolationExclamation',
    ].forEach((partial) => {
      expect(scssUses).toContain(partial);
      expect(aggregateUses).toContain(partial);
    });
  });
});
