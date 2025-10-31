package org.example.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
  // Keep the existing public API name `hash(String)` used across the codebase.
  // Implement with bcrypt and add a verify method.

  private PasswordUtil() {}

  /**
   * Hashes the provided password using bcrypt.
   * @param password plain password
   * @return bcrypt hash (includes salt)
   */
  public static String hash(String password) {
    if (password == null) throw new IllegalArgumentException("password must not be null");
    // gensalt default log rounds is 10; increase if you need more CPU work
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

  /**
   * Verifies a plain password against a stored bcrypt hash.
   * @param password plain password
   * @param hash bcrypt hash
   * @return true if matches
   */
  public static boolean verify(String password, String hash) {
    if (password == null || hash == null) return false;
    try {
      return BCrypt.checkpw(password, hash);
    } catch (Exception e) {
      // malformed hash or other error
      return false;
    }
  }

}
