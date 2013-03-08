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
package org.opencastproject.episode.api;

import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import static org.opencastproject.util.data.functions.Misc.chuck;

/** Exception that is thrown by the episode service to indicate any unrecoverable error. */
public class EpisodeServiceException extends RuntimeException {

  /** Serial version uid */
  private static final long serialVersionUID = -7411693851983157126L;

  public EpisodeServiceException(String message) {
    super(message);
  }

  public EpisodeServiceException(Throwable cause) {
    super(cause);
  }

  /** Returns true if the exception is caused by a {@link org.opencastproject.security.api.UnauthorizedException}. */
  // todo is an authorization failure really unrecoverable?
  public boolean isCauseNotAuthorized() {
    return getCause() instanceof UnauthorizedException;
  }

  /** Returns true if the exception is caused by a {@link org.opencastproject.util.NotFoundException}. */
  public boolean isCauseNotFound() {
    return getCause() instanceof NotFoundException;
  }

  /**
   * If the exception is caused by an {@link org.opencastproject.security.api.UnauthorizedException}
   * rethrow it, otherwise do nothing.
   */
  public void rethrowUnauthorizedException() {
    if (isCauseNotAuthorized()) {
      chuck(getCause());
    }
  }
}
