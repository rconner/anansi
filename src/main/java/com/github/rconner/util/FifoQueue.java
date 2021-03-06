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

package com.github.rconner.util;

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.collect.UnmodifiableIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A minimal FIFO queue implementation, which does <strong>not</strong> implement {@link java.util.Collection}. This
 * implementation is not thread-safe, and does not even have a fail-fast iterator.
 *
 * @author rconner
 */
@Beta
public final class FifoQueue<E> implements Iterable<E> {

    private Node<E> head;
    private Node<E> tail;

    public static <E> FifoQueue<E> of() {
        return new FifoQueue<E>();
    }

    public static <E> FifoQueue<E> of( final E element ) {
        final FifoQueue<E> queue = new FifoQueue<E>();
        queue.enqueue( element );
        return queue;
    }

    public static <E> FifoQueue<E> of( final E... elements ) {
        final FifoQueue<E> queue = new FifoQueue<E>();
        for( final E element : elements ) {
            queue.enqueue( element );
        }
        return queue;
    }

    public void enqueue( final E element ) {
        final Node<E> node = new Node<E>( element );
        if( tail == null ) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
    }

    public E dequeue() {
        if( head == null ) {
            throw new NoSuchElementException();
        }
        final E element = head.element;
        head = head.next;
        if( head == null ) {
            tail = null;
        }
        return element;
    }

    public E head() {
        if( head == null ) {
            throw new NoSuchElementException();
        }
        return head.element;
    }

    public boolean isEmpty() {
        return head == null;
    }

    @Override
    public Iterator<E> iterator() {
        return new UnmodifiableIterator<E>() {
            private Node<E> node = head;

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            public E next() {
                if( node == null ) {
                    throw new NoSuchElementException();
                }
                final E element = node.element;
                node = node.next;
                return element;
            }
        };
    }

    private static final Joiner JOINER = Joiner.on( ", " ).useForNull( "null" );

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append( '[' );
        JOINER.appendTo( sb, this );
        sb.append( ']' );
        return sb.toString();
    }


    private static class Node<E> {
        final E element;
        Node<E> next;

        Node( final E element ) {
            this.element = element;
        }
    }
}
