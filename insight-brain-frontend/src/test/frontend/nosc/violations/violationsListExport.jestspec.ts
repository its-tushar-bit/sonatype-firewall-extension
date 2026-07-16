/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { buildViolationsListExportPayload } from 'MainRoot/nosc/violations/violationsListExport';
import { createDefaultViolationsFilterState } from 'MainRoot/nosc/violations/violationsListApi';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';

function filterState(overrides: Partial<ViolationsFilterState> = {}): ViolationsFilterState {
  return { ...createDefaultViolationsFilterState(), ...overrides };
}

describe('buildViolationsListExportPayload (CLM-42260)', () => {
  it('sends only the fixed orderBy when no filters are active', () => {
    expect(buildViolationsListExportPayload(createDefaultViolationsFilterState())).toEqual({
      orderBy: '-policyThreatLevel',
    });
  });

  it('maps each filter group to its RisksFilterDTO wire shape', () => {
    const payload = buildViolationsListExportPayload(
      filterState({
        states: new Set(['WAIVED', 'OPEN']),
        threatCategories: new Set(['license', 'security']),
        stageIds: new Set(['release', 'build']),
        organizationIds: new Set(['org-java']),
        applicationIds: new Set(['app-banana', 'app-apple']),
        threatRange: [4, 9],
      }),
    );
    expect(payload).toEqual({
      orderBy: '-policyThreatLevel',
      // states as a sorted array of enum names (PolicyViolationStateFilter @JsonCreator).
      policyViolationStates: ['OPEN', 'WAIVED'],
      // categories as a comma-delimited string (PolicyThreatCategoryFilter String ctor).
      policyThreatCategories: 'license,security',
      stageIds: ['build', 'release'],
      organizationIds: ['org-java'],
      applicationIds: ['app-apple', 'app-banana'],
      // threat range as the object form (PolicyThreatLevelFilter @JsonCreator).
      policyThreatLevelRange: { minPolicyThreatLevel: 4, maxPolicyThreatLevel: 9 },
    });
  });

  it('omits the threat range when it covers the full [0, 10] domain', () => {
    const payload = buildViolationsListExportPayload(filterState({ threatRange: [0, 10] }));
    expect(payload).not.toHaveProperty('policyThreatLevelRange');
  });

  it('omits empty groups so an unfiltered export payload stays minimal', () => {
    const payload = buildViolationsListExportPayload(filterState({ states: new Set(['OPEN']) }));
    expect(payload).toEqual({ orderBy: '-policyThreatLevel', policyViolationStates: ['OPEN'] });
    expect(payload).not.toHaveProperty('policyThreatCategories');
    expect(payload).not.toHaveProperty('stageIds');
  });

  it('never includes the index-only waiver-type filter (RisksFilterDTO has no field for it) (CLM-42261)', () => {
    const payload = buildViolationsListExportPayload(
      filterState({ waiverType: 'AUTO', states: new Set(['WAIVED']) }),
    );
    // Only the exportable state filter is present; the auto/manual waiver selection is dropped.
    expect(payload).toEqual({ orderBy: '-policyThreatLevel', policyViolationStates: ['WAIVED'] });
    expect(payload).not.toHaveProperty('waivedWithAutoWaiver');
  });
});
