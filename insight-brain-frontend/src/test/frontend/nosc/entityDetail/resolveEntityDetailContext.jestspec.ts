/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */

import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import type { EntityKind } from 'MainRoot/nosc/entityDetail/entityDetailTypes';

describe('resolveEntityDetailContext', () => {
  const fullInput = {
    applicationPublicId: 'my-app',
    applicationName: 'My App',
    componentHash: 'abc',
    componentDisplayName: 'log4j-core 2.14.1',
    policyViolationId: 'pv-1',
    policyName: 'Critical CVE Policy',
    vulnId: 'CVE-2021-44228',
    stageId: 'build',
    scanId: 'scan-1',
  };

  it('marks violation current and links app, component, and vulnerability routes', () => {
    const chain = resolveEntityDetailContext({
      current: 'violation',
      ...fullInput,
    });
    expect(chain.nodes.map((n) => n.kind)).toEqual([
      'application',
      'component',
      'violation',
      'vulnerability',
    ]);
    expect(chain.nodes.find((n) => n.kind === 'violation')?.isCurrent).toBe(true);
    expect(chain.nodes.find((n) => n.kind === 'violation')?.href).toBeNull();
    expect(chain.nodes.find((n) => n.kind === 'application')?.href).toBe(
      '#/applications/my-app?stageId=build&scanId=scan-1',
    );
    expect(chain.nodes.find((n) => n.kind === 'component')?.href).toBe(
      '#/applications/my-app/components/abc?scanId=scan-1',
    );
    expect(chain.nodes.find((n) => n.kind === 'vulnerability')?.href).toBe(
      '#/vulnerabilities/CVE-2021-44228?applicationPublicId=my-app&componentHash=abc&violationId=pv-1&scanId=scan-1',
    );
    expect(chain.stageId).toBe('build');
    expect(chain.scanId).toBe('scan-1');
  });

  it('keeps unavailable nodes when ids missing', () => {
    const chain = resolveEntityDetailContext({
      current: 'violation',
      policyViolationId: 'pv-1',
      policyName: 'Some Policy',
    });
    expect(chain.nodes.find((n) => n.kind === 'application')?.isAvailable).toBe(false);
    expect(chain.nodes.find((n) => n.kind === 'application')?.href).toBeNull();
    expect(chain.nodes.find((n) => n.kind === 'violation')?.isAvailable).toBe(true);
    expect(chain.nodes.find((n) => n.kind === 'violation')?.href).toBeNull();
  });

  it('preserves scanId when stageId is absent', () => {
    const chain = resolveEntityDetailContext({
      current: 'violation',
      policyViolationId: 'pv-1',
      scanId: 'scan-only',
    });
    expect(chain.scanId).toBe('scan-only');
    expect(chain.stageId).toBeUndefined();
  });

  it.each([
    ['application', '#/violations/pv-1?stageId=build&scanId=scan-1'],
    ['component', '#/applications/my-app?stageId=build&scanId=scan-1'],
    ['vulnerability', '#/applications/my-app?stageId=build&scanId=scan-1'],
  ] as const)('current=%s keeps current href null and links other available nodes', (current, expectedSiblingHref) => {
    const chain = resolveEntityDetailContext({
      current: current as EntityKind,
      ...fullInput,
    });
    expect(chain.nodes.find((n) => n.kind === current)?.isCurrent).toBe(true);
    expect(chain.nodes.find((n) => n.kind === current)?.href).toBeNull();

    if (current === 'application') {
      expect(chain.nodes.find((n) => n.kind === 'violation')?.href).toBe(expectedSiblingHref);
      expect(chain.nodes.find((n) => n.kind === 'component')?.href).toBe(
        '#/applications/my-app/components/abc?scanId=scan-1',
      );
      expect(chain.nodes.find((n) => n.kind === 'vulnerability')?.href).toBe(
        '#/vulnerabilities/CVE-2021-44228?applicationPublicId=my-app&componentHash=abc&violationId=pv-1&scanId=scan-1',
      );
    } else {
      expect(chain.nodes.find((n) => n.kind === 'application')?.href).toBe(expectedSiblingHref);
    }
  });

  it('encodes special characters in native entity hrefs', () => {
    const chain = resolveEntityDetailContext({
      current: 'component',
      applicationPublicId: 'a/b c',
      componentHash: 'h/ash',
      policyViolationId: 'pv/1 2',
      vulnId: 'CVE/1 2',
      scanId: 'scan id',
    });
    expect(chain.nodes.find((n) => n.kind === 'application')?.href).toBe(
      '#/applications/a%2Fb%20c?scanId=scan+id',
    );
    expect(chain.nodes.find((n) => n.kind === 'violation')?.href).toBe(
      '#/violations/pv%2F1%202?scanId=scan+id',
    );
    expect(chain.nodes.find((n) => n.kind === 'vulnerability')?.href).toBe(
      '#/vulnerabilities/CVE%2F1%202?applicationPublicId=a%2Fb+c&componentHash=h%2Fash&violationId=pv%2F1+2&scanId=scan+id',
    );
  });
});
