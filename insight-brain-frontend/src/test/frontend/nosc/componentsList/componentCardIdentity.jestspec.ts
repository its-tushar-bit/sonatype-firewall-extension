/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { componentCardIdentity } from 'MainRoot/nosc/componentsList/componentCardIdentity';

describe('componentCardIdentity', () => {
  it('builds name@version and keeps the package URL as the coordinate', () => {
    expect(
      componentCardIdentity({
        id: 'pkg:npm/axios@0.24.0',
        name: 'axios',
        subtitle: '0.24.0',
        ecosystem: 'npm',
        source: 'catalog',
      }),
    ).toEqual({
      title: 'axios@0.24.0',
      coordinate: 'pkg:npm/axios@0.24.0',
    });
  });

  it('falls back to ecosystem:name@version for local name-only rows', () => {
    expect(
      componentCardIdentity({
        id: 'log4j-core',
        name: 'log4j-core',
        ecosystem: 'maven',
        source: 'local',
      }),
    ).toEqual({
      title: 'log4j-core',
      coordinate: 'maven:log4j-core',
    });
  });

  it('parses IQ local group : name : version display names', () => {
    expect(
      componentCardIdentity({
        id: 'com.google.guava : guava : 31.1-jre',
        name: 'com.google.guava : guava : 31.1-jre',
        ecosystem: 'maven',
        source: 'local',
      }),
    ).toEqual({
      title: 'guava@31.1-jre',
      coordinate: 'com.google.guava : guava : 31.1-jre',
    });
  });

  it('keeps scoped npm package names and appends subtitle version', () => {
    expect(
      componentCardIdentity({
        id: 'pkg:npm/%40babel/core@7.0.0',
        name: '@babel/core',
        subtitle: '7.0.0',
        ecosystem: 'npm',
        source: 'catalog',
      }),
    ).toEqual({
      title: '@babel/core@7.0.0',
      coordinate: 'pkg:npm/%40babel/core@7.0.0',
    });
  });

  it('preserves scoped name@version when already embedded in the name', () => {
    expect(
      componentCardIdentity({
        id: 'pkg:npm/%40angular/core@15.0.0',
        name: '@angular/core@15.0.0',
        ecosystem: 'npm',
        source: 'catalog',
      }),
    ).toEqual({
      title: '@angular/core@15.0.0',
      coordinate: 'pkg:npm/%40angular/core@15.0.0',
    });
  });
});
