/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import fs from 'fs';
import path from 'path';

const FRONTEND = path.resolve(__dirname, '../../../main/frontend');

function useTargets(scssPath: string): Set<string> {
  const content = fs.readFileSync(path.join(FRONTEND, scssPath), 'utf8');
  return new Set([...content.matchAll(/@use\s+'([^']+)'/g)].map((m) => m[1]));
}

const scssUses = useTargets('scss/scss.scss');
const aggregateUses = useTargets('scss/orgsAndPoliciesEmbed.scss');

const isOrgsAndPoliciesPartial = (p: string) => /^\.\.\/(OrgsAndPolicies|owner\.manager)\//.test(p);

describe('orgsAndPoliciesEmbed.scss', () => {
  // The nexus-one bundle can't load scss/scss.scss, so it loads orgsAndPoliciesEmbed.scss to render
  // the embedded management pages with parity. The two carry parallel @use lists that must not drift:
  // a stylesheet added to scss.scss but forgotten here silently loses styling in the embed.
  it('includes every OrgsAndPolicies and owner.manager partial that scss.scss loads', () => {
    const inScss = [...scssUses].filter(isOrgsAndPoliciesPartial).sort();
    const inAggregate = [...aggregateUses].filter(isOrgsAndPoliciesPartial).sort();

    expect(inAggregate).toEqual(inScss);
  });

  // The OrgsAndPolicies partials above are auto-compared, but the embed also depends on a few shared
  // partials that live outside those directories (so the prefix filter can't see them). Guard them
  // explicitly - dropping one of these silently breaks the sidebar layout, nav-pill scroll, or the
  // policy editor. New shared dependencies must be added here as well as to the aggregate.
  it('includes the shared partials the embedded management pages depend on', () => {
    [
      '../navPills/navPills',
      'tiles',
      '../react/iqSidebarNav/iqSidebarNav',
      '../react/accessTile/accessTile',
      'policy-editor',
      '../innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModal',
      '../innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurations',
      '../artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModal',
      '../artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurations',
    ].forEach((partial) => {
      expect(aggregateUses).toContain(partial);
    });
  });
});
