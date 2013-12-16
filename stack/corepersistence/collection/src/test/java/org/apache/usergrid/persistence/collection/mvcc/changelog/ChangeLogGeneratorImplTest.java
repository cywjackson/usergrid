/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.mvcc.changelog;

import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import java.util.List;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.guice.CassandraTestCollectionModule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.test.CassandraRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import rx.Observable;

/**
 * Test basic operation of change log
 */
public class ChangeLogGeneratorImplTest {

    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule( CassandraTestCollectionModule.class );

    @Rule
    public final CassandraRule rule = new CassandraRule();

    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

    public ChangeLogGeneratorImplTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getChangeLog method, of class ChangeLogGeneratorImpl.
     */
    @Test
    public void testGetChangeLog() throws ConnectionException {

        System.out.println( "getChangeLog" );

        // create an entity and make a series of changes to it so that versions get created
        CollectionScope context = new CollectionScopeImpl(
                new SimpleId( "organization" ), new SimpleId( "test" ), "test" );

        // Create an entity and change it's fields three times 
        EntityCollectionManager manager = factory.createCollectionManager( context );
        Entity e0 = new Entity( new SimpleId( "test" ) );
        e0.setField( new StringField( "waffleType", "belgian" ) );
        e0.setField( new IntegerField( "waffleIndex", 3 ) );
        e0.setField( new BooleanField( "syrup", false ) );
        Observable<Entity> o0 = manager.write( e0 );
        e0 = o0.toBlockingObservable().lastOrDefault( null );

        Entity e1 = manager.load( e0.getId() ).toBlockingObservable().lastOrDefault( null );
        e1.setField( new StringField( "waffleType", "belgian" ) );
        e1.setField( new IntegerField( "waffleIndex", 4 ) );
        e1.setField( new BooleanField( "syrup", true ) );
        e1.setField( new BooleanField( "butter", true ) );
        Observable<Entity> o1 = manager.write( e1 );
        e1 = o1.toBlockingObservable().lastOrDefault( null );

        Entity e2 = manager.load( e0.getId() ).toBlockingObservable().lastOrDefault( null );
        e2.setField( new StringField( "waffleType", "belgian" ) );
        e2.setField( new IntegerField( "waffleIndex", 6 ) );
        e2.setField( new BooleanField( "syrup", true ) );
        e2.setField( new BooleanField( "butter", true ) );
        e2.setField( new BooleanField( "chocolateChips", true ) );
        e2.setField( new BooleanField( "whippedCream", true ) );
        Observable<Entity> o2 = manager.write( e2 );
        e2 = o2.toBlockingObservable().lastOrDefault( null );

        Entity e3 = manager.load( e0.getId() ).toBlockingObservable().lastOrDefault( null );
        e3.setField( new StringField( "waffleType", "belgian" ) );
        e3.setField( new IntegerField( "waffleIndex", 4 ) );
        e3.setField( new BooleanField( "syrup", false ) );
        e3.setField( new BooleanField( "butter", false ) );
        Observable<Entity> o3 = manager.write( e3 );
        e3 = o3.toBlockingObservable().lastOrDefault( null );

        List<MvccEntity> versions = mvccEntitySerializationStrategy
           .load( context, e0.getId(), e3.getVersion(), 10);
        assertEquals(4, versions.size() );

        ChangeLogGeneratorImpl instance = new ChangeLogGeneratorImpl();
        List<ChangeLogEntry> result = instance.getChangeLog( versions, e2.getVersion() );

        for (ChangeLogEntry cle : result) {
            System.out.println( cle.toString() );
        }
        assertEquals(10, result.size());
    }
}