/*
 * Copyright (C) 2000-2013 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Writer Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.passwordencryption;

/**
 * A cryptographic function to encrypt a user password. Usually, it is a one-way hashing function or
 * better an adaptive key derivation function. In order to encrypt user passwords, a one-way hashing
 * function like MD5 or SHA-256/SHA-512 must use additional techniques like the salting and the
 * stretching. Theses functions must implement this interface to be used with DBBuilder.
 * @author mmoquillon
 */
public interface CryptographicFunction {

  /**
   * Encrypts the specified password.
   * @param password the password to encrypt.
   * @return a digest of the password computed by the algorithm implemented in this function.
   */
  String encrypt(String password);
}
