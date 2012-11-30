/*
 * Copyright (c) 2012 Ray A. Conner
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

import com.github.rconner.util.ImmutableStack;
import com.google.common.annotations.Beta;

/**
 * A walk from one vertex to another, optionally over some implementation-specific object. It is not uncommon for a Walk
 * to be over an Iterable of (sub) Walks.
 *
 * @param <V>
 * @param <E>
 *
 * @author rconner
 */
@Beta
public abstract class Walk<V, E> {

    /**
     * @return
     */
    public abstract V getFrom();

    /**
     * @return
     */
    public abstract V getTo();

    /**
     * @return
     */
    public abstract E getOver();

    /**
     * Creates a new immutable Walk, with an over of null.
     *
     * @param from
     * @param to
     * @param <V>
     * @param <E>
     *
     * @return
     */
    public static <V, E> Walk<V, E> newInstance( final V from, final V to ) {
        return new TrivialWalk<V, E>( from, to, null );
    }

    /**
     * Creates a new immutable Walk.
     *
     * @param from
     * @param to
     * @param over
     * @param <V>
     * @param <E>
     *
     * @return
     */
    public static <V, E> Walk<V, E> newInstance( final V from, final V to, final E over ) {
        return new TrivialWalk<V, E>( from, to, over );
    }

    private static final class TrivialWalk<V, E> extends Walk<V, E> {
        private final V from;
        private final V to;
        private final E over;

        TrivialWalk( final V from, final V to, final E over ) {
            this.from = from;
            this.to = to;
            this.over = over;
        }

        @Override
        public V getFrom() {
            return from;
        }

        @Override
        public V getTo() {
            return to;
        }

        @Override
        public E getOver() {
            return over;
        }
    }

    /**
     * Creates a builder used to create a Walk with sub-walks.
     *
     * @param from
     * @param <V>
     * @param <E>
     *
     * @return
     */
    public static <V, E> Builder<V, E> from( final V from ) {
        return new Builder<V, E>( from );
    }

    public static final class Builder<V, E> {
        private final V from;
        @SuppressWarnings( "unchecked" )
        private ImmutableStack<Walk<V, E>> stack = ImmutableStack.of();

        Builder( final V from ) {
            this.from = from;
        }

        public Builder<V, E> add( final Walk<V, E> walk ) {
            stack = stack.push( walk );
            return this;
        }

        public Builder<V, E> pop() {
            stack = stack.pop();
            return this;
        }

        public Walk<V, Iterable<Walk<V, E>>> build() {
            // if stack is empty, do not know from/to
            // otherwise:
            //   from := stack.last.from
            //   to := stack.head.to
            // FIXME: Instead, build a *really* lazy walk? b/c often the caller will only
            // be interested in walk.to anyway. So just keep the stack around in the walk.
            V to = stack.isEmpty() ? from : stack.peek().getTo();
            return Walk.newInstance( from, to, stack.reverse() );
        }
    }

}