/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api;

import org.neo4j.collection.primitive.PrimitiveLongCollections;
import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.cursor.Cursor;
import org.neo4j.function.LongPredicate;
import org.neo4j.kernel.api.EntityType;
import org.neo4j.kernel.api.cursor.NodeItem;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.operations.EntityOperations;
import org.neo4j.kernel.impl.api.operations.EntityReadOperations;

/**
 * When looking up nodes by a property value, we have to do a two-stage check.
 * The first stage is to look up the value in lucene, that will find us nodes that may have
 * the correct property value.
 * Then the second stage is to ensure the values actually match the value we are looking for,
 * which requires us to load the actual property value and filter the result we got in the first stage.
 * <p>This class defines the methods for the second stage check.<p>
 */
public class LookupFilter
{
    /**
     * used by the consistency checker
     */
    public static PrimitiveLongIterator exactIndexMatches( PropertyLookup lookup, PrimitiveLongIterator indexedNodeIds,
            int propertyKeyId, Object value )
    {
        if ( isNumberOrArray( value ) )
        {
            return PrimitiveLongCollections.filter( indexedNodeIds,
                    new LookupBasedExactMatchPredicate( lookup, propertyKeyId,
                            value ) );
        }
        return indexedNodeIds;
    }

    /**
     * used in "normal" operation
     */
    public static PrimitiveLongIterator exactIndexMatches( final EntityOperations operations,
            final KernelStatement state, PrimitiveLongIterator indexedNodeIds, int propertyKeyId, Object value )
    {
        if ( isNumberOrArray( value ) )
        {
            return PrimitiveLongCollections.filter( indexedNodeIds, new OperationsBasedExactMatchPredicate( operations,
                    state, propertyKeyId, value ) );
        }
        return indexedNodeIds;
    }

    private static boolean isNumberOrArray( Object value )
    {
        return value instanceof Number || value.getClass().isArray();
    }

    private static abstract class BaseExactMatchPredicate implements LongPredicate
    {
        private final int propertyKeyId;
        private final Object value;

        BaseExactMatchPredicate( int propertyKeyId, Object value )
        {
            this.propertyKeyId = propertyKeyId;
            this.value = value;
        }

        @Override
        public boolean test( long nodeId )
        {
            try
            {
                return nodeProperty( nodeId, propertyKeyId ).valueEquals( value );
            }
            catch ( EntityNotFoundException ignored )
            {
                return false;
            }
        }

        abstract Property nodeProperty( long nodeId, int propertyKeyId ) throws EntityNotFoundException;
    }

    /**
     * used by "normal" operation
     */
    private static class OperationsBasedExactMatchPredicate extends BaseExactMatchPredicate
    {
        final EntityReadOperations readOperations;
        final KernelStatement state;

        OperationsBasedExactMatchPredicate( EntityReadOperations readOperations, KernelStatement state,
                int propertyKeyId, Object value )
        {
            super( propertyKeyId, value );
            this.readOperations = readOperations;
            this.state = state;
        }

        @Override
        Property nodeProperty( long nodeId, int propertyKeyId ) throws EntityNotFoundException
        {
            try ( Cursor<NodeItem> node = readOperations.nodeCursor( state, nodeId ) )
            {
                if ( node.next() )
                {
                    Object value = readOperations.nodeGetProperty( state, node.get(), propertyKeyId );
                    return value == null ? Property.noNodeProperty( nodeId, propertyKeyId ) : Property.property(
                            propertyKeyId, value );
                }
                else
                {
                    throw new EntityNotFoundException( EntityType.NODE, nodeId );
                }
            }
        }
    }

    /**
     * used by CC
     */
    private static class LookupBasedExactMatchPredicate extends BaseExactMatchPredicate
    {
        final PropertyLookup lookup;

        LookupBasedExactMatchPredicate( PropertyLookup lookup, int propertyKeyId, Object value )
        {
            super( propertyKeyId, value );
            this.lookup = lookup;
        }

        @Override
        Property nodeProperty( long nodeId, int propertyKeyId ) throws EntityNotFoundException
        {
            return lookup.nodeProperty( nodeId, propertyKeyId );
        }
    }
}
