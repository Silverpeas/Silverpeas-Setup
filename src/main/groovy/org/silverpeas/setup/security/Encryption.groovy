package org.silverpeas.setup.security

/**
 * An encryption of an expression producing a digest.
 * @author mmoquillon
 */
interface Encryption {

  /**
   * Encrypts the specified expression according to the cryptographic algorithm used by this
   * encryption implementation.
   * @param expression the expression to encrypt.
   * @return the digest, result of the expression encryption.
   */
  String encrypt(String expression)
}