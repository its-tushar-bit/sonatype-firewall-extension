package com.sonatype.insight.brain.releasegraph;

import java.util.Arrays;

import com.sonatype.insight.brain.model.GAVPopularity;

public class ReleaseGraphModel
{
    private final int[] slotIndices;

    private final int[] popularity;

    private final int currentVersionIndex;

    private final int mostPopularVersionIndex;

    public ReleaseGraphModel( int[] slotIndices, int[] popularity, int currentVersionIndex,
                                 int mostPopularVersionIndex )
    {
        this.slotIndices = slotIndices;
        this.popularity = popularity;
        this.currentVersionIndex = currentVersionIndex;
        this.mostPopularVersionIndex = mostPopularVersionIndex;
    }

    public int[] getSlotIndices()
    {
        return slotIndices;
    }

    public int getCurrentVersionIndex()
    {
        return currentVersionIndex;
    }

    public int getMostPopularVersionIndex()
    {
        return mostPopularVersionIndex;
    }

    public int getMostRecentVersionIndex()
    {
        return getPopularity().length - 1;
    }

    public int[] getPopularity()
    {
        return popularity;
    }

    public static ReleaseGraphModel build( GAVPopularity model, long startTime, long endTime, int slots )
    {
        final long period = endTime - startTime;
        final long minDiff = period / slots;
        int[] slotIndces = new int[slots];

        ReleaseGraphModel pop =
            new ReleaseGraphModel( slotIndces, model.getPopularity(), model.getCurrentVersionIndex(),
                                      getMostPopularIndex( model.getPopularity() ) );

        Arrays.fill( slotIndces, -1 );
        int[] popularity = model.getPopularity();
        long[] catalogDates = model.getCatalogDates();
        for ( int i = 0; i < popularity.length; i++ )
        {
            int slotIndex = (int) ( ( catalogDates[i] - startTime ) / minDiff );
            if ( slotIndces[slotIndex] == -1 || doPushDown( slotIndex, slotIndces ) )
            {
                slotIndces[slotIndex] = i;
            }
            else if ( !fillUpwards( i, slotIndex, slotIndces ) )
            {
                doBump( i, getLastFilledIndex( slotIndex, slotIndces ), pop );
            }
        }

        return pop;
    }

    /*
     * Attempt to fill the points upwards
     */
    private static boolean fillUpwards( int popularityIndex, int currentSlotIndex, int[] slotIndces )
    {
        for ( int i = currentSlotIndex; i < slotIndces.length; i++ )
        {
            if ( slotIndces[i] == -1 )
            {
                slotIndces[i] = popularityIndex;
                return true;
            }
        }
        return false;
    }

    /*
     * Find the most popular index
     */
    private static int getMostPopularIndex( int[] popularity )
    {
        int maxPopularity = -1;
        int mostPopularIndex = -1;
        for ( int i = 0; i < popularity.length; i++ )
        {
            if ( maxPopularity < popularity[i] )
            {
                maxPopularity = popularity[i];
                mostPopularIndex = i;
            }
        }
        return mostPopularIndex;
    }

    /*
     * Attempt to push values down
     */
    private static boolean doPushDown( int currentIndex, int[] slotIndices )
    {
        if ( currentIndex > 0 )
        {
            if ( slotIndices[currentIndex - 1] == -1 || doPushDown( currentIndex - 1, slotIndices ) )
            {
                // move
                slotIndices[currentIndex - 1] = slotIndices[currentIndex];
                return true;
            }
        }
        return false;
    }

    /*
     * Bump values down
     */
    private static void doBump( int popularityIndex, int currentSlotIndex, ReleaseGraphModel pop )
    {
        int[] slotIndices = pop.getSlotIndices();
        int prevPopIndex = slotIndices[currentSlotIndex - 1];
        if ( isImportant( popularityIndex, pop ) )
        {
            if ( isImportant( prevPopIndex, pop ) )
            {
                doBump( prevPopIndex, currentSlotIndex - 1, pop );
            }
            slotIndices[currentSlotIndex] = popularityIndex;
        }
        else if ( !isImportant( prevPopIndex, pop ) )
        {
            // TODO merge should probably be more sophisticated because of up filling
            int[] popularitydata = pop.getPopularity();
            popularitydata[prevPopIndex] =
                (int) Math.ceil( ( (double) popularitydata[prevPopIndex] + popularitydata[popularityIndex] ) / 2.0 );
        }
        // Dropping this release as the preceding one was an important release
    }

    /*
     * Determine if the release is important
     */
    private static boolean isImportant( int index, ReleaseGraphModel pop )
    {
        return index == pop.getCurrentVersionIndex() || index == pop.getMostPopularVersionIndex()
            || index == pop.getMostRecentVersionIndex();
    }

    private static int getLastFilledIndex( int startIndex, int[] slotIndices )
    {
        for ( int i = startIndex; i < slotIndices.length; i++ )
        {
            if ( slotIndices[i] == -1 )
            {
                return i - 1;
            }
        }
        return slotIndices.length - 1;
    }
}