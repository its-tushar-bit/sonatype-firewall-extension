/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.ArrayList;
import java.util.List;

import org.jooq.Record;

/**
 * Accumulates jOOQ Records at the minimum ancestor distance seen so far.
 * When a closer distance arrives, the previous rows are discarded.
 * Used by batch DAO methods that resolve hierarchy via OWNER_ANCESTOR joins.
 */
final class ClosestAncestorAccumulator
{
  int distance;

  final List<Record> rows;

  ClosestAncestorAccumulator(int distance, Record row) {
    this.distance = distance;
    this.rows = new ArrayList<>();
    this.rows.add(row);
  }

  ClosestAncestorAccumulator merge(int candidateDistance, Record candidateRow) {
    if (candidateDistance < distance) {
      distance = candidateDistance;
      rows.clear();
      rows.add(candidateRow);
    }
    else if (candidateDistance == distance) {
      rows.add(candidateRow);
    }
    return this;
  }
}
