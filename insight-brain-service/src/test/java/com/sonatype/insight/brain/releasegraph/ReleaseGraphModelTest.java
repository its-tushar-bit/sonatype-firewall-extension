package com.sonatype.insight.brain.releasegraph;

import static com.sonatype.insight.brain.releasegraph.ReleaseGraphModel.SLOTS;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.GAVPopularity;

public class ReleaseGraphModelTest
{

    @Test
    public void test3ImportantVersionsInLastBucket()
    {
        long[] catalogDates = new long[] { 100l, 100l, 100l };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 2, model.getSlotIndices()[SLOTS - 1] );
        Assert.assertEquals( 1, model.getSlotIndices()[SLOTS - 2] );
        Assert.assertEquals( 0, model.getSlotIndices()[SLOTS - 3] );
    }

    @Test
    public void test3ImportantVersionsInFirstBucket()
    {
        long[] catalogDates = new long[] { 0, 0, 0 };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 0, model.getSlotIndices()[0] );
        Assert.assertEquals( 1, model.getSlotIndices()[1] );
        Assert.assertEquals( 2, model.getSlotIndices()[2] );
    }

    @Test
    public void test3ImportantVersionsInteriorBucket()
    {
        long[] catalogDates = new long[] { 5, 5, 5 };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 0, model.getSlotIndices()[1] );
        Assert.assertEquals( 1, model.getSlotIndices()[2] );
        Assert.assertEquals( 2, model.getSlotIndices()[3] );
    }

    @Test
    public void test2First1SecondBucket()
    {
        // This tests that the value is pushed backwards
        long[] catalogDates = new long[] { 0, 1, 4 };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 0, model.getSlotIndices()[0] );
        Assert.assertEquals( 1, model.getSlotIndices()[1] );
        Assert.assertEquals( 2, model.getSlotIndices()[2] );
    }

    @Test
    public void testLastPushDown()
    {
        // Push down into unoccupied
        long[] catalogDates = new long[] { 97, 100, 100 };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 2, model.getSlotIndices()[SLOTS - 1] );
        Assert.assertEquals( 1, model.getSlotIndices()[SLOTS - 2] );
        Assert.assertEquals( 0, model.getSlotIndices()[SLOTS - 3] );

        // Push down into occupied interesting
        catalogDates = new long[] { 97, 100, 100 };
        popularity = new int[] { 98, 100, 99 };

        model = ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 2, model.getSlotIndices()[SLOTS - 1] );
        Assert.assertEquals( 1, model.getSlotIndices()[SLOTS - 2] );
        Assert.assertEquals( 0, model.getSlotIndices()[SLOTS - 3] );

        // Push down into occupied uninteresting
        catalogDates = new long[] { 0, 97, 100, 100 };
        popularity = new int[] { 50, 9, 100, 99 };

        model = ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 2 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 3 );
        Assert.assertEquals( 3, model.getSlotIndices()[SLOTS - 1] );
        Assert.assertEquals( 2, model.getSlotIndices()[SLOTS - 2] );
        Assert.assertEquals( -1, model.getSlotIndices()[SLOTS - 3] );
    }

    @Test
    public void testPushDown()
    {
        // next box has interesting, we
        // Push down into unoccupied
        long[] catalogDates = new long[] { 95, 95, 97 };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 2, model.getSlotIndices()[SLOTS - 2] );
        Assert.assertEquals( 1, model.getSlotIndices()[SLOTS - 3] );
        Assert.assertEquals( 0, model.getSlotIndices()[SLOTS - 4] );
    }

    @Test
    public void testPushUpMiddle()
    {
        // Current has 2, uninteresting up, down
        long[] catalogDates = new long[] { 95, 95, 100 };
        int[] popularity = new int[] { 98, 100, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( model.getMostPopularVersionIndex(), 1 );
        Assert.assertEquals( model.getMostRecentVersionIndex(), 2 );
        Assert.assertEquals( 2, model.getSlotIndices()[SLOTS - 1] );
        Assert.assertEquals( 1, model.getSlotIndices()[SLOTS - 2] );
        Assert.assertEquals( 0, model.getSlotIndices()[SLOTS - 3] );
    }

    @Test
    public void testMostPopularChosen()
    {
        long[] catalogDates = new long[] { 0, 3, 50, 50, 100 };
        int[] popularity = new int[] { 98, 100, 50, 25, 99 };

        ReleaseGraphModel model =
            ReleaseGraphModel.build( buildGavPopularity( catalogDates, popularity, 0 ), 0, 100, SLOTS );
        Assert.assertEquals( 1, model.getMostPopularVersionIndex() );
        Assert.assertEquals( 4, model.getMostRecentVersionIndex() );
        Assert.assertEquals( 2, model.getSlotIndices()[21] );
    }

    private static GAVPopularity buildGavPopularity( long[] catalogDates, int[] popularity, int currentVersionIndex )
    {
        GAVPopularity gav = new GAVPopularity();
        gav.setCatalogDates( catalogDates );
        gav.setPopularity( popularity );
        gav.setCurrentVersionIndex( currentVersionIndex );
        return gav;
    }
}
