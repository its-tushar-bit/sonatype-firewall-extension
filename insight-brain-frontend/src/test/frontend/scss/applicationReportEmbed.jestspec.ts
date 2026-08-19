/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import fs from 'fs';
import path from 'path';

const FRONTEND = path.resolve(__dirname, '../../../main/frontend');

// Targets are normalised to a frontend-relative path so the two files can be compared
// regardless of how each spells the same partial (scss.scss reaches react/tag/tag as
// '../../frontend/react/tag/tag', the aggregate as '../react/tag/tag').
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
const aggregateUses = useTargets('scss/applicationReportEmbed.scss');

describe('applicationReportEmbed.scss', () => {
  // Nexus One embeds applicationReport.* / Component Details without loading scss.scss.
  // Guard the shared partials so License Detections (and siblings) do not silently lose CSS.
  // Why each partial is needed is documented in applicationReportEmbed.scss itself, next to the
  // @use that would be deleted; this list is only the sync check against scss.scss.
  it('includes the application-report / component-details partials scss.scss loads', () => {
    [
      'applicationReport/applicationReport',
      'componentDetails/ComponentDetails',
      'componentDetails/ViolationsTableTile/componentWaivers/componentWaiversPopover',
      'componentDetails/TransferList/TransferList',
      'componentDetails/TransferList/TransferListHalf',
      'componentDetails/overview/occurrencesPopover/occurrencesPopover',
      'react/tag/tag',
      'violation/violationPage',
      'scss/iq-threat-levels',
    ].forEach((partial) => {
      expect(scssUses).toContain(partial);
      expect(aggregateUses).toContain(partial);
    });
  });
});
