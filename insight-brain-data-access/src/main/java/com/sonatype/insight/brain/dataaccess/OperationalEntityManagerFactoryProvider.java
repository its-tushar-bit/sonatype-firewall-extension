package com.sonatype.insight.brain.dataaccess;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;

public class OperationalEntityManagerFactoryProvider
{
    private static final EntityManagerFactory entityManagerFactory;

    private static final DataSource dataSource;

    private OperationalEntityManagerFactoryProvider()
    {
    }

    static
    {
        dataSource = OperationalDataStoreProvider.get();
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put( "openjpa.ConnectionFactory", dataSource );
        entityManagerFactory = Persistence.createEntityManagerFactory( "InsightBrainODS", props );
    }

    public static EntityManagerFactory get()
    {
        return entityManagerFactory;
    }
}
