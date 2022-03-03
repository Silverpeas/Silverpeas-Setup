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
package org.silverpeas.setup.security

import org.apache.commons.codec.digest.Crypt

/**
 * An encryption using the SHA-512 cryptographic algorithm with the Unix password encryption
 * mechanism: the algorithm is enhanced with salting and stretching technicals.
 * @author mmoquillon
 */
class UnixSHA512Encryption implements Encryption {

  private static final String SALTCHARS =
      'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890/.'
  private static final String ENCRYPTION_METHOD_ID = '$6$'

  /**
   * Generates a SHA-512 cryptographic digest of the specified plaintext expression by using a
   * randomly generated salt.
   * @param expression the expression to encrypt in SHA-512.
   * @return the digest, result of the expression encryption.
   */
  @Override
  String encrypt(final String expression) {
    return Crypt.crypt(expression, generateSalt())
  }

  private String generateSalt() {
    Random random = new Random()
    StringBuilder salt = new StringBuilder(ENCRYPTION_METHOD_ID)

    while (salt.length() < 16) {
      int index = (int) (random.nextFloat() * SALTCHARS.length())
      salt.append(SALTCHARS.substring(index, index + 1))
    }

    return salt.toString()
  }

}
