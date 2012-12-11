/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class CountingLock
{
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    private final AtomicInteger count;

    CountingLock( final int initialCount )
    {
        count = new AtomicInteger( initialCount );
    }

    void sharedLock()
    {
        rwl.readLock().lock();
    }

    void sharedUnlock()
    {
        rwl.readLock().unlock();
    }

    void exclusiveLock()
    {
        rwl.writeLock().lock();
    }

    void exclusiveUnlock()
    {
        try
        {
            count.incrementAndGet();
        }
        finally
        {
            rwl.writeLock().unlock();
        }
    }

    int count()
    {
        return count.get();
    }
}
