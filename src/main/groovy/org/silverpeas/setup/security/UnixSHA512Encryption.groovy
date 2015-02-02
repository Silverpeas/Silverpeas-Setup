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
