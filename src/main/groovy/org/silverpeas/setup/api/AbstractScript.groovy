/*
  Copyright (C) 2000 - 2022 Silverpeas

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  As a special exception to the terms and conditions of version 3.0 of
  the GPL, you may redistribute this Program in connection with Free/Libre
  Open Source Software ("FLOSS") applications as described in Silverpeas's
  FLOSS exception.  You should have recieved a copy of the text describing
  the FLOSS exception, and it is also available here:
  "http://www.silverpeas.org/docs/core/legal/floss_exception.html"

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.setup.api

/**
 * A default abstract implementation of {@code org.silverpeas.setup.api.Script}. It defines the
 * common structure of all concrete scripts that are used in Silverpeas Setup.
 * @author miguel
 */
abstract class AbstractScript implements Script {

  protected File script
  protected FileLogger logger
  protected Map<String, String> settings

  /**
   * Constructs a new script instance that refers the script located at the specified path.
   * @param path the absolute path of a Groovy script.
   */
  AbstractScript(String path) {
    script = new File(path)
  }

  /**
   * Uses the specified logger to trace the execution of this script.
   * @param logger a logger.
   * @return itself.
   */
  @Override
  AbstractScript useLogger(final FileLogger logger) {
    this.logger = logger
    return this
  }

  /**
   * Uses the specified settings to parametrize the execution of this script.
   * @param settings a collection of key-value pairs defining all the settings.
   * @return itself.
   */
  @Override
  AbstractScript useSettings(final Map<String, String> settings) {
    this.settings = settings
    return this
  }

  /**
   * Gets this script as a File instance into which are stored the script's code.
   * @return a File with the code of this script.
   */
  File toFile() {
    return script
  }

  /**
   * Returns a hash code value for the object. This method is
   * supported for the benefit of hash tables such as those provided by
   * {@link java.util.HashMap}.
   * <p>
   * The general contract of {@code hashCode} is:
   * <ul>
   * <li>Whenever it is invoked on the same object more than once during
   *     an execution of a Java application, the {@code hashCode} method
   *     must consistently return the same integer, provided no information
   *     used in {@code equals} comparisons on the object is modified.
   *     This integer need not remain consistent from one execution of an
   *     application to another execution of the same application.
   * <li>If two objects are equal according to the {@code equals ( Object )}
   *     method, then calling the {@code hashCode} method on each of
   *     the two objects must produce the same integer result.
   * <li>It is <em>not</em> required that if two objects are unequal
   *     according to the {@link java.lang.Object#equals(java.lang.Object)}
   *     method, then calling the {@code hashCode} method on each of the
   *     two objects must produce distinct integer results.  However, the
   *     programmer should be aware that producing distinct integer results
   *     for unequal objects may improve the performance of hash tables.
   * </ul>
   * <p>
   * As much as is reasonably practical, the hashCode method defined by
   * class {@code Object} does return distinct integers for distinct
   * objects. (This is typically implemented by converting the internal
   * address of the object into an integer, but this implementation
   * technique is not required by the
   * Java&trade; programming language.)
   *
   * @return a hash code value for this object.
   * @see java.lang.Object#equals(java.lang.Object)
   * @see java.lang.System#identityHashCode
   */
  @Override
  int hashCode() {
    return this.script.path.hashCode()
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * <p>
   * The {@code equals} method implements an equivalence relation
   * on non-null object references:
   * <ul>
   * <li>It is <i>reflexive</i>: for any non-null reference value
   * {@code x}, {@code x.equals ( x )} should return
   * {@code true}.
   * <li>It is <i>symmetric</i>: for any non-null reference values
   * {@code x} and {@code y}, {@code x.equals ( y )}
   *     should return {@code true} if and only if
   * {@code y.equals ( x )} returns {@code true}.
   * <li>It is <i>transitive</i>: for any non-null reference values
   * {@code x}, {@code y}, and {@code z}, if
   * {@code x.equals ( y )} returns {@code true} and
   * {@code y.equals ( z )} returns {@code true}, then
   * {@code x.equals ( z )} should return {@code true}.
   * <li>It is <i>consistent</i>: for any non-null reference values
   * {@code x} and {@code y}, multiple invocations of
   * {@code x.equals ( y )} consistently return {@code true}
   *     or consistently return {@code false}, provided no
   *     information used in {@code equals} comparisons on the
   *     objects is modified.
   * <li>For any non-null reference value {@code x},
   * {@code x.equals ( null )} should return {@code false}.
   * </ul>
   * <p>
   * The {@code equals} method for class {@code Object} implements
   * the most discriminating possible equivalence relation on objects;
   * that is, for any non-null reference values {@code x} and
   * {@code y}, this method returns {@code true} if and only
   * if {@code x} and {@code y} refer to the same object
   * ({@code x == y} has the value {@code true}).
   * <p>
   * Note that it is generally necessary to override the {@code hashCode}
   * method whenever this method is overridden, so as to maintain the
   * general contract for the {@code hashCode} method, which states
   * that equal objects must have equal hash codes.
   *
   * @param obj the reference object with which to compare.
   * @return {@code true} if this object is the same as the obj
   *          argument; {@code false} otherwise.
   * @see #hashCode()
   * @see java.util.HashMap
   */
  @Override
  boolean equals(final Object obj) {
    return (obj.class.name == this.class.name && obj.hashCode() == this.hashCode())
  }
}
