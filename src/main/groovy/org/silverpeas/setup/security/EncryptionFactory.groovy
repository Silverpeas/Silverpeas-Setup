package org.silverpeas.setup.security

/**
 * A factory to creates encryption instances by taking into account the default implementation to
 * use.
 * @author mmoquillon
 */
@Singleton
class EncryptionFactory {

  /**
   * Creates a new default encryption instance.
   * @return an instance of the default implementation of the {@code Encryption} interface.
   */
  Encryption createDefaultEncryption() {
    return new UnixSHA512Encryption()
  }
}
