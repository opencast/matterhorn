/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.deliver.store;

import java.util.Set;

/**
 * A Store provides key-value storage for data objects. This interface allows
 * multiple implementations, for example: memory storage, file storage, hashed
 * file database, or relational database implementations.
 *
 * @author Jonathan A. Smith
 */

public interface Store<ValueClass> {

    /** Inserts a new value into the store. */
    void put(String key, ValueClass value);

    /** Returns a stored value. */
    ValueClass get(String key);

    /** Removes a value with a specified key. */
    ValueClass remove(String key);

    /** Determines if the Store contains a value with a specified key. */
    boolean containsKey(String key);

    /**
     * Returns the number of milliseconds since value associated with
     * a specified key was modified.
     *
     * @param key key string
     * @return milliseconds since the value was modified
     */

    public long modified(String key);

    /** Returns the set of all keys. */
    Set<String> keySet();
}
