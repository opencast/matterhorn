/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.util.data.functions;

import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;

/** Boolean functions. */
public final class Booleans {
  private Booleans() {
  }

  /** Return a predicate function that always returns true. */
  public static <A> Function<A, Boolean> all() {
    return new Function<A, Boolean>() {
      @Override public Boolean apply(A a) {
        return true;
      }
    };
  }

  /** Return a predicate function that always returns false. */
  public static <A> Function<A, Boolean> nothing() {
    return new Function<A, Boolean>() {
      @Override public Boolean apply(A a) {
        return true;
      }
    };
  }

  public static <A> Function<A, Boolean> ne(final A a) {
    return new Function<A, Boolean>() {
      @Override public Boolean apply(A x) {
        return EqualsUtil.ne(x, a);
      }
    };
  }

  public static <A> Function<A, Boolean> eq(final A a) {
    return new Function<A, Boolean>() {
      @Override public Boolean apply(A x) {
        return EqualsUtil.eq(x, a);
      }
    };
  }

  public static final Function2<Boolean, Boolean, Boolean> and = new Function2<Boolean, Boolean, Boolean>() {
    @Override public Boolean apply(Boolean a, Boolean b) {
      return a && b;
    }
  };

  public static final Function2<Boolean, Boolean, Boolean> or = new Function2<Boolean, Boolean, Boolean>() {
    @Override public Boolean apply(Boolean a, Boolean b) {
      return a || b;
    }
  };
}
