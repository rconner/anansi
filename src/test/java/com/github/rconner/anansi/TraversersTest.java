/*
 * Copyright (c) 2012-2013 Ray A. Conner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.rconner.anansi;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeTraverser;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.github.rconner.util.IterableTest.assertIteratorEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class TraversersTest {

    // We'll be using Multimaps to define adjacency for test cases.
    // Lacking a key (vertex) means that there are no children for that vertex. There is no way, using this
    // representation, to denote whether or not a vertex is present. Any vertex is present.

    private final Multimap<String, String> empty = ImmutableMultimap.of();

    private final Multimap<String, String> singleEdge = ImmutableMultimap.of( "A", "B" );

    // Warning! Do not perform a post-order traversal on this graph.
    private final Multimap<String, String> loop = ImmutableMultimap.of( "A", "A" );

    // Warning! Do not perform a post-order traversal on this graph.
    private final Multimap<String, String> cycle = ImmutableListMultimap.<String, String>builder()
            .put( "A", "B" )
            .put( "B", "C" )
            .put( "C", "A" )
            .build();

    private final Multimap<String, String> tree = ImmutableListMultimap.<String, String>builder()
            .put( "A", "B" )
            .put( "A", "C" )
            .put( "B", "D" )
            .put( "B", "E" )
            .put( "C", "F" )
            .put( "C", "G" )
            .build();

    // Has two paths from A to D.
    private final Multimap<String, String> dag = ImmutableListMultimap.<String, String>builder()
            .put( "A", "B" )
            .put( "A", "C" )
            .put( "B", "D" )
            .put( "B", "E" )
            .put( "C", "D" )
            .put( "D", "G" )
            .build();

    private static TreeTraverser<String> adjacencyFor( final Multimap<String, String> graph ) {
        return new TreeTraverser<String>() {
            @Override
            public Iterable<String> children( final String vertex ) {
                return graph.get( vertex );
            }
        };
    }

    private static TreeTraverser<String> mutableAdjacencyFor( final Multimap<String, String> graph ) {
        return adjacencyFor( ArrayListMultimap.create( graph ) );
    }


    static final String[] EMPTY_EXPECTED_VERTICES = new String[0];

    static void assertNextVerticesAre( final Iterator<String> iterator, final String... expected ) {
        for( final String vertex : expected ) {
            assertThat( iterator.hasNext(), is( true ) );
            assertThat( iterator.next(), is( vertex ) );
        }
    }

    static void assertTraversalContains( final Iterable<String> traversal, final String... expected ) {
        final Iterator<String> iterator = traversal.iterator();
        assertNextVerticesAre( iterator, expected );
        assertThat( iterator.hasNext(), is( false ) );
        assertIteratorEmpty( iterator );
    }

    static void assertTraversalBegins( final Iterable<String> traversal, final String... expected ) {
        final Iterator<String> iterator = traversal.iterator();
        assertNextVerticesAre( iterator, expected );
        assertThat( iterator.hasNext(), is( true ) );
    }

    // empty()

    @Test
    public void empty() {
        final TreeTraverser<String> traverser = Traversers.empty();
        assertTraversalContains( traverser.children( "A" ), EMPTY_EXPECTED_VERTICES );
    }

    // preOrder( Traverser )

    @Test
    public void preOrderEmpty() {
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacencyFor( empty ) );
        assertTraversalContains( traverser, "A" );
    }

    @Test
    public void preOrderSingleEdge() {
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacencyFor( singleEdge ) );
        assertTraversalContains( traverser, "A", "B" );
    }

    @Test
    public void preOrderLoop() {
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacencyFor( loop ) );
        assertTraversalBegins( traverser, "A", "A", "A", "A" );
    }

    @Test
    public void preOrderCycle() {
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacencyFor( cycle ) );
        assertTraversalBegins( traverser, "A", "B", "C", "A", "B" );
    }

    @Test
    public void preOrderTree() {
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacencyFor( tree ) );
        assertTraversalContains( traverser, "A", "B", "D", "E", "C", "F", "G" );
    }

    private static void assertPreOrderFullDag( final Iterable<String> traversal ) {
        assertTraversalContains( traversal, "A", "B", "D", "G", "E", "C", "D", "G" );
    }

    @Test
    public void preOrderDag() {
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacencyFor( dag ) );
        assertPreOrderFullDag( traverser );
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderRemoveBeforeNext() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        iterator.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderRemoveRoot() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A" );
        iterator.remove();
    }

    @Test
    public void preOrderRemoveB() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B" );
        iterator.remove();
        assertNextVerticesAre( iterator, "C", "D", "G" );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.preOrder( "A", adjacency ), "A", "C", "D", "G" );
    }

    @Test
    public void preOrderRemoveD() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B", "D" );
        iterator.remove();
        assertNextVerticesAre( iterator, "E", "C", "D", "G" );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.preOrder( "A", adjacency ), "A", "B", "E", "C", "D", "G" );
    }

    @Test
    public void preOrderRemoveE() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B", "D", "G", "E" );
        iterator.remove();
        assertNextVerticesAre( iterator, "C", "D", "G" );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.preOrder( "A", adjacency ), "A", "B", "D", "G", "C", "D", "G" );
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderRemoveTwice() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B" );
        iterator.remove();
        iterator.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderPruneBeforeNext() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        ( (PruningIterator) iterator ).prune();
    }

    @Test
    public void preOrderPruneRoot() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A" );
        ( (PruningIterator) iterator ).prune();
        assertIteratorEmpty( iterator );

        // Check that data structure is unchanged
        assertPreOrderFullDag( Traversers.preOrder( "A", adjacency ) );
    }

    @Test
    public void preOrderPruneB() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B" );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, "C", "D", "G" );

        // Check that data structure is unchanged
        assertPreOrderFullDag( Traversers.preOrder( "A", adjacency ) );
    }

    @Test
    public void preOrderPruneD() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B", "D" );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, "E", "C", "D", "G" );

        // Check that data structure is unchanged
        assertPreOrderFullDag( Traversers.preOrder( "A", adjacency ) );
    }

    @Test
    public void preOrderPruneE() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B", "D", "G", "E" );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, "C", "D", "G" );

        // Check that data structure is unchanged
        assertPreOrderFullDag( Traversers.preOrder( "A", adjacency ) );
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderPruneTwice() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B" );
        ( (PruningIterator) iterator ).prune();
        ( (PruningIterator) iterator ).prune();
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderRemoveThenPrune() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B" );
        ( (PruningIterator) iterator ).remove();
        ( (PruningIterator) iterator ).prune();
    }

    @Test( expected = IllegalStateException.class )
    public void preOrderPruneThenRemove() {
        final TreeTraverser<String> adjacency = mutableAdjacencyFor( dag );
        final Iterable<String> traverser = Traversers.preOrder( "A", adjacency );
        final Iterator<String> iterator = traverser.iterator();
        assertNextVerticesAre( iterator, "A", "B" );
        ( (PruningIterator) iterator ).prune();
        ( (PruningIterator) iterator ).remove();
    }

/*
    // postOrder( Traverser )

    @Test
    public void postOrderEmpty() {
        final Traverser<String, String> traverser = Traversers.postOrder( adjacencyFor( empty ) );
        assertTraversalContains( traverser.apply( "A" ), new Object[][] { { "A" } } );
    }

    @Test
    public void postOrderSingleEdge() {
        final Traverser<String, String> traverser = Traversers.postOrder( adjacencyFor( singleEdge ) );
        assertTraversalContains( traverser.apply( "A" ), new Object[][] { { "A", "A->B", "B" }, { "A" } } );
    }

    @Test
    public void postOrderTree() {
        final Traverser<String, String> traverser = Traversers.postOrder( adjacencyFor( tree ) );
        assertTraversalContains( traverser.apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C", "C->F", "F" },
                                                  { "A", "A->C", "C", "C->G", "G" },
                                                  { "A", "A->C", "C" },
                                                  { "A" } } );
    }

    @Test
    public void postOrderDag() {
        final Traverser<String, String> traverser = Traversers.postOrder( adjacencyFor( dag ) );
        assertTraversalContains( traverser.apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->C", "C" },
                                                  { "A" } } );
    }

    @Test( expected = IllegalStateException.class )
    public void postOrderRemoveBeforeNext() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.postOrder( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        iterator.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void postOrderRemoveRoot() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.postOrder( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C" },
                                             { "A" } } );
        iterator.remove();
    }

    @Test
    public void postOrderRemoveB() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.postOrder( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->B", "B" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C" },
                                             { "A" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.postOrder( adjacency ).apply( "A" ),
                                 new Object[][] { { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->C", "C" },
                                                  { "A" } } );
    }

    @Test
    public void postOrderRemoveD() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.postOrder( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->D", "D" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C" },
                                             { "A" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.postOrder( adjacency ).apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->C", "C" },
                                                  { "A" } } );
    }

    @Test
    public void postOrderRemoveE() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.postOrder( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C" },
                                             { "A" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.postOrder( adjacency ).apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->C", "C" },
                                                  { "A" } } );
    }

    @Test( expected = IllegalStateException.class )
    public void postOrderRemoveTwice() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.postOrder( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->B", "B" } } );
        iterator.remove();
        iterator.remove();
    }

    // breadthFirst( Traverser )

    @Test
    public void breadthFirstEmpty() {
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacencyFor( empty ) );
        assertTraversalContains( traverser.apply( "A" ), new Object[][] { { "A" } } );
    }

    @Test
    public void breadthFirstSingleEdge() {
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacencyFor( singleEdge ) );
        assertTraversalContains( traverser.apply( "A" ), new Object[][] { { "A" }, { "A", "A->B", "B" } } );
    }

    @Test
    public void breadthFirstLoop() {
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacencyFor( loop ) );
        assertTraversalBegins( traverser.apply( "A" ),
                               new Object[][] { { "A" },
                                                { "A", "A->A", "A" },
                                                { "A", "A->A", "A", "A->A", "A" },
                                                { "A", "A->A", "A", "A->A", "A", "A->A", "A" } } );
    }

    @Test
    public void breadthFirstCycle() {
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacencyFor( cycle ) );
        assertTraversalBegins( traverser.apply( "A" ),
                               new Object[][] { { "A" },
                                                { "A", "A->B", "B" },
                                                { "A", "A->B", "B", "B->C", "C" },
                                                { "A", "A->B", "B", "B->C", "C", "C->A", "A" },
                                                { "A", "A->B", "B", "B->C", "C", "C->A", "A", "A->B", "B" } } );
    }

    @Test
    public void breadthFirstTree() {
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacencyFor( tree ) );
        assertTraversalContains( traverser.apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->F", "F" },
                                                  { "A", "A->C", "C", "C->G", "G" } } );
    }

    private static void assertBreadthFirstFullDag( final Iterable<Walk<String, String>> traversal ) {
        assertTraversalContains( traversal,
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
    }

    @Test
    public void breadthFirstDag() {
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacencyFor( dag ) );
        assertBreadthFirstFullDag( traverser.apply( "A" ) );
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstRemoveBeforeNext() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        iterator.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstRemoveRoot() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator, "A" );
        iterator.remove();
    }

    @Test
    public void breadthFirstRemoveB() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->C", "C" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
    }

    @Test
    public void breadthFirstRemoveC() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" }, { "A", "A->C", "C" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );
    }

    @Test
    public void breadthFirstRemoveFirstD() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
    }

    @Test
    public void breadthFirstRemoveE() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->C", "C", "C->D", "D" },
                                                  { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
    }

    @Test
    public void breadthFirstRemoveSecondD() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" } } );
        iterator.remove();
        assertNextVerticesAre( iterator, new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );
    }

    @Test
    public void breadthFirstRemoveFirstG() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );
        iterator.remove();
        assertNextVerticesAre( iterator, EMPTY_EXPECTED_VERTICES );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D" } } );
    }

    @Test
    public void breadthFirstRemoveSecondG() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
        iterator.remove();
        assertNextVerticesAre( iterator, EMPTY_EXPECTED_VERTICES );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.breadthFirst( adjacency ).apply( "A" ),
                                 new Object[][] { { "A" },
                                                  { "A", "A->B", "B" },
                                                  { "A", "A->C", "C" },
                                                  { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D" } } );
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstRemoveTwice() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" } } );
        iterator.remove();
        iterator.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstPruneBeforeNext() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        ( (PruningIterator) iterator ).prune();
    }

    @Test
    public void breadthFirstPruneRoot() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator, "A" );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, EMPTY_EXPECTED_VERTICES );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneB() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->C", "C" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneC() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" }, { "A", "A->C", "C" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneFirstD() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneE() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneSecondD() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneFirstG() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, EMPTY_EXPECTED_VERTICES );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test
    public void breadthFirstPruneSecondG() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A" },
                                             { "A", "A->B", "B" },
                                             { "A", "A->C", "C" },
                                             { "A", "A->B", "B", "B->D", "D" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" },
                                             { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
        ( (PruningIterator) iterator ).prune();
        assertNextVerticesAre( iterator, EMPTY_EXPECTED_VERTICES );

        // Check that data structure is unchanged
        assertBreadthFirstFullDag( Traversers.breadthFirst( adjacency ).apply( "A" ) );
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstPruneTwice() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Iterator<Walk<String, String>> iterator = Traversers.breadthFirst( adjacency ).apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" } } );
        ( (PruningIterator) iterator ).prune();
        ( (PruningIterator) iterator ).prune();
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstRemoveThenPrune() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" } } );
        ( (PruningIterator) iterator ).remove();
        ( (PruningIterator) iterator ).prune();
    }

    @Test( expected = IllegalStateException.class )
    public void breadthFirstPruneThenRemove() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.breadthFirst( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator, new Object[][] { { "A" }, { "A", "A->B", "B" } } );
        ( (PruningIterator) iterator ).prune();
        ( (PruningIterator) iterator ).remove();
    }

    // leaves( Traverser )

    @Test
    public void leavesEmpty() {
        final Traverser<String, String> traverser = Traversers.leaves( adjacencyFor( empty ) );
        assertTraversalContains( traverser.apply( "A" ), new Object[][] { { "A" } } );
    }

    @Test
    public void leavesSingleEdge() {
        final Traverser<String, String> traverser = Traversers.leaves( adjacencyFor( singleEdge ) );
        assertTraversalContains( traverser.apply( "A" ), new Object[][] { { "A", "A->B", "B" } } );
    }

    @Test
    public void leavesTree() {
        final Traverser<String, String> traverser = Traversers.leaves( adjacencyFor( tree ) );
        assertTraversalContains( traverser.apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->F", "F" },
                                                  { "A", "A->C", "C", "C->G", "G" } } );
    }

    @Test
    public void leavesDag() {
        final Traverser<String, String> traverser = Traversers.leaves( adjacencyFor( dag ) );
        assertTraversalContains( traverser.apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
    }

    @Test( expected = IllegalStateException.class )
    public void leavesRemoveBeforeNext() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.leaves( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        iterator.remove();
    }

    @Test
    public void leavesRemoveFirstG() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.leaves( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.leaves( adjacency ).apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D" } } );
    }

    @Test
    public void leavesRemoveSecondG() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.leaves( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
        iterator.remove();
        assertIteratorEmpty( iterator );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.leaves( adjacency ).apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D" },
                                                  { "A", "A->B", "B", "B->E", "E" },
                                                  { "A", "A->C", "C", "C->D", "D" } } );
    }

    @Test
    public void leavesRemoveE() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.leaves( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->E", "E" } } );
        iterator.remove();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );

        // Check that data structure was actually changed
        assertTraversalContains( Traversers.leaves( adjacency ).apply( "A" ),
                                 new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                                  { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
    }

    @Test( expected = IllegalStateException.class )
    public void leavesRemoveTwice() {
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.leaves( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->E", "E" } } );
        iterator.remove();
        iterator.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void leavesNextAfterLastThenRemove() {
        // This use case is slightly different, because it exercises different logic in the traverser.
        final Traverser<String, String> adjacency = mutableAdjacencyFor( dag );
        final Traverser<String, String> traverser = Traversers.leaves( adjacency );
        final Iterator<Walk<String, String>> iterator = traverser.apply( "A" ).iterator();
        assertNextVerticesAre( iterator,
                            new Object[][] { { "A", "A->B", "B", "B->D", "D", "D->G", "G" },
                                             { "A", "A->B", "B", "B->E", "E" },
                                             { "A", "A->C", "C", "C->D", "D", "D->G", "G" } } );
        try {
            iterator.next();
        } catch( NoSuchElementException ignored ) {
            // expected
        }
        iterator.remove();
    }

    // elements()

    private static void assertPathWalksAre( final Iterable<Walk<Object, String>> traversal,
                                            final Object[][] expectedElements ) {
        final Iterator<Walk<Object, String>> iterator = traversal.iterator();
        for( final Object[] element : expectedElements ) {
            assertThat( iterator.hasNext(), is( true ) );
            final Walk<Object, String> walk = iterator.next();
            assertThat( Traversers.elementPath( walk ), is( element[ 0 ] ) );
            assertThat( walk.getTo(), is( element[ 1 ] ) );
        }
        assertIteratorEmpty( iterator );
    }

    @Test
    public void elementsNull() {
        final Object root = null;
        assertPathWalksAre( Traversers.elements().apply( root ), EMPTY_EXPECTED_VERTICES );
    }

    @Test
    public void elementsString() {
        final Object root = "abc";
        assertPathWalksAre( Traversers.elements().apply( root ), EMPTY_EXPECTED_VERTICES );
    }

    @Test
    public void elementsEmptyMap() {
        final Object root = Collections.emptyMap();
        assertPathWalksAre( Traversers.elements().apply( root ), EMPTY_EXPECTED_VERTICES );
    }

    @Test
    public void elementsEmptyList() {
        final Object root = Collections.emptyList();
        assertPathWalksAre( Traversers.elements().apply( root ), EMPTY_EXPECTED_VERTICES );
    }

    private static final int[] EMPTY_INT_ARRAY = new int[ 0 ];

    @Test
    public void elementsEmptyArray() {
        final Object root = EMPTY_INT_ARRAY;
        assertPathWalksAre( Traversers.elements().apply( root ), EMPTY_EXPECTED_VERTICES );
    }

    @Test
    public void elementsSimpleMap() {
        final Object root = ImmutableMap.of( "name", "Alice", "age", 37, "deceased", false );
        assertPathWalksAre( Traversers.elements().apply( root ),
                            new Object[][] { { "name", "Alice" }, { "age", 37 }, { "deceased", false } } );
    }

    @Test
    public void elementsSimpleList() {
        final Object root = Arrays.<Object>asList( "Alice", 37, false );
        assertPathWalksAre( Traversers.elements().apply( root ),
                            new Object[][] { { "[0]", "Alice" }, { "[1]", 37 }, { "[2]", false } } );
    }

    @Test
    public void elementsSimpleArray() {
        final Object root = new int[] { 42, 99, 256 };
        assertPathWalksAre( Traversers.elements().apply( root ),
                            new Object[][] { { "[0]", 42 }, { "[1]", 99 }, { "[2]", 256 } } );
    }

    @Test
    public void elementsMap() {
        final Map<String, Object> root = ImmutableMap.<String, Object>of( "names", Arrays.asList( "Alice", "Becky", "Carol" ),
                                                                          "ages", Arrays.asList( 37, 42, 32 ) );
        assertPathWalksAre( Traversers.elements().apply( root ),
                            new Object[][] { { "names", root.get( "names" ) },
                                             { "ages", root.get( "ages" ) } } );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void elementsList() {
        final List<Object> root = Arrays.<Object>asList( ImmutableMap.of( "name", "Alice", "age", 37 ),
                                                         ImmutableMap.of( "name", "Becky", "age", 42 ) );
        assertPathWalksAre( Traversers.elements().apply( root ),
                            new Object[][] { { "[0]", root.get( 0 ) },
                                             { "[1]", root.get( 1 ) } } );
    }

    @Test
    public void elementsArray() {
        final Object[] root = new Object[] { ImmutableMap.of( "name", "Alice", "age", 37 ),
                                       ImmutableMap.of( "name", "Becky", "age", 42 ) };
        assertPathWalksAre( Traversers.elements().apply( root ),
                            new Object[][] { { "[0]", root[ 0 ] },
                                             { "[1]", root[ 1 ] } } );
    }

    // leafElements()
    // elementPath()

    // Note that an empty map/iterable/array *is* a leaf.

    @Test
    public void leafElementsNull() {
        final Object root = null;
        assertPathWalksAre( Traversers.leafElements().apply( root ), new Object[][] { { "", root } } );
    }

    @Test
    public void leafElementsString() {
        final Object root = "abc";
        assertPathWalksAre( Traversers.leafElements().apply( root ), new Object[][] { { "", root } } );
    }

    @Test
    public void leafElementsEmptyMap() {
        final Object root = Collections.emptyMap();
        assertPathWalksAre( Traversers.leafElements().apply( root ), new Object[][] { { "", root } } );
    }

    @Test
    public void leafElementsEmptyList() {
        final Object root = Collections.emptyList();
        assertPathWalksAre( Traversers.leafElements().apply( root ), new Object[][] { { "", root } } );
    }

    @Test
    public void leafElementsEmptyArray() {
        final Object root = EMPTY_INT_ARRAY;
        assertPathWalksAre( Traversers.leafElements().apply( root ), new Object[][] { { "", root } } );
    }

    @Test
    public void leafElementsSimpleMap() {
        final Object root = ImmutableMap.of( "name", "Alice", "age", 37, "deceased", false );
        assertPathWalksAre( Traversers.leafElements().apply( root ),
                            new Object[][] { { "name", "Alice" }, { "age", 37 }, { "deceased", false } } );
    }

    @Test
    public void leafElementsSimpleList() {
        final Object root = Arrays.<Object>asList( "Alice", 37, false );
        assertPathWalksAre( Traversers.leafElements().apply( root ),
                            new Object[][] { { "[0]", "Alice" }, { "[1]", 37 }, { "[2]", false } } );
    }

    @Test
    public void leafElementsSimpleArray() {
        final Object root = new int[] { 42, 99, 256 };
        assertPathWalksAre( Traversers.leafElements().apply( root ),
                            new Object[][] { { "[0]", 42 }, { "[1]", 99 }, { "[2]", 256 } } );
    }

    @Test
    public void leafElementsMap() {
        final Map<String, Object> root = ImmutableMap.<String, Object>of( "names", Arrays.asList( "Alice", "Becky", "Carol" ),
                                                                          "ages", Arrays.asList( 37, 42, 32 ) );
        assertPathWalksAre( Traversers.leafElements().apply( root ),
                            new Object[][] { { "names[0]", "Alice" },
                                             { "names[1]", "Becky" },
                                             { "names[2]", "Carol" },
                                             { "ages[0]", 37 },
                                             { "ages[1]", 42 },
                                             { "ages[2]", 32 } } );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void leafElementsList() {
        final List<Object> root = Arrays.<Object>asList( ImmutableMap.of( "name", "Alice", "age", 37 ),
                                                         ImmutableMap.of( "name", "Becky", "age", 42 ) );
        assertPathWalksAre( Traversers.leafElements().apply( root ),
                            new Object[][] { { "[0].name", "Alice" },
                                             { "[0].age", 37 },
                                             { "[1].name", "Becky" },
                                             { "[1].age", 42 } } );
    }

    @Test
    public void leafElementsArray() {
        final Object[] root = new Object[] { ImmutableMap.of( "name", "Alice", "age", 37 ),
                                       ImmutableMap.of( "name", "Becky", "age", 42 ) };
        assertPathWalksAre( Traversers.leafElements().apply( root ),
                            new Object[][] { { "[0].name", "Alice" },
                                             { "[0].age", 37 },
                                             { "[1].name", "Becky" },
                                             { "[1].age", 42 } } );
    }

    // Test a more complex graph

    @SuppressWarnings( "unchecked" )
    @Test
    public void leafElementsComplex() {
        final Map<?, ?> map = ImmutableMap.builder()
                .put( "string", "A String" )
                .put( "integer", 42 )
                .put( "list", Arrays.asList( "zero", "one", "two", "three" ) )
                .put( "array", new Object[] { "four", "five", "six" } )
                .put( "boolean.array", new boolean[] { false, true, true, false, true } )
                .put( "map",
                      ImmutableMap.builder()
                              .put( "foo[abc]bar", "Another String" )
                              .put( "people",
                                    Arrays.asList( ImmutableMap.of( "name", "Alice", "age", 37 ),
                                                   ImmutableMap.of( "name", "Bob", "age", 55 ),
                                                   ImmutableMap.of( "name", "Carol", "age", 23 ),
                                                   ImmutableMap.of( "name", "Dave", "age", 27 ) ) )
                              .put( "owner", ImmutableMap.of( "name", "Elise", "age", 43 ) )
                              .build() )
                .build();

        assertPathWalksAre( Traversers.leafElements().apply( map ),
                            new Object[][] { { "string", "A String" },
                                             { "integer", 42 },
                                             { "list[0]", "zero" },
                                             { "list[1]", "one" },
                                             { "list[2]", "two" },
                                             { "list[3]", "three" },
                                             { "array[0]", "four" },
                                             { "array[1]", "five" },
                                             { "array[2]", "six" },
                                             { "boolean\\.array[0]", false },
                                             { "boolean\\.array[1]", true },
                                             { "boolean\\.array[2]", true },
                                             { "boolean\\.array[3]", false },
                                             { "boolean\\.array[4]", true },
                                             { "map.foo\\[abc\\]bar", "Another String" },
                                             { "map.people[0].name", "Alice" },
                                             { "map.people[0].age", 37 },
                                             { "map.people[1].name", "Bob" },
                                             { "map.people[1].age", 55 },
                                             { "map.people[2].name", "Carol" },
                                             { "map.people[2].age", 23 },
                                             { "map.people[3].name", "Dave" },
                                             { "map.people[3].age", 27 },
                                             { "map.owner.name", "Elise" },
                                             { "map.owner.age", 43 } } );
    }
*/
}
