/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.query.ir.result;


import java.util.Iterator;
import java.util.Set;


/** @author tnine */
public abstract class MergeIterator implements ResultIterator {


    /** kept private on purpose so advance must return the correct value */
    private Set<ScanColumn> next;

    /** Pointer to the last set.  Equal to "next" when returned.  Used to retain results after "next" is set to null */
    private Set<ScanColumn> last;
    /** The size of the pages */
    protected int pageSize;

    int loadCount = 0;


    /**
     *
     */
    public MergeIterator( int pageSize ) {
        this.pageSize = pageSize;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Set<ScanColumn>> iterator() {
        return this;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        //if next isn't set, try to advance
        if ( next == null ) {
            doAdvance();
        }

        boolean results = next != null && next.size() > 0;

        if ( results ) {
            last = next;
            loadCount++;
        }

        return results;
    }


    /** Advance to the next page */
    protected void doAdvance() {
        next = advance();
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    @Override
    public Set<ScanColumn> next() {
        if ( next == null ) {
            doAdvance();
        }

        Set<ScanColumn> returnVal = next;

        next = null;

        return returnVal;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException( "You can't remove from a union iterator" );
    }


    /* (non-Javadoc)
     * @see org.usergrid.persistence.query.ir.result.ResultIterator#reset()
     */
    @Override
    public void reset() {
        if ( loadCount == 1 && last != null ) {
            next = last;
            return;
        }
        //clean up the last pointer
        last = null;
        //reset in the child iterators
        doReset();
    }


    /** Advance the iterator to the next value.  Can return an empty set with signals no values */
    protected abstract Set<ScanColumn> advance();

    /** Perform the reset if required */
    protected abstract void doReset();
}
