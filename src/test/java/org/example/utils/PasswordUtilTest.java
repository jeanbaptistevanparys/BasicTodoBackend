package org.example.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PasswordUtilTest {

  @Test
  public void hashAndVerify_success() {
    String pw = "s3cr3t-password";
    String hash = PasswordUtil.hash(pw);
    assertThat(hash).isNotNull();
    boolean ok = PasswordUtil.verify(pw, hash);
    assertThat(ok).isTrue();
  }

  @Test
  public void verify_wrongPassword_returnsFalse() {
    String hash = PasswordUtil.hash("abc123");
    boolean ok = PasswordUtil.verify("wrong", hash);
    assertThat(ok).isFalse();
  }

  @Test
  public void hash_null_throws() {
    assertThatThrownBy(() -> PasswordUtil.hash(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void verify_nulls_returnFalse() {
    assertThat(PasswordUtil.verify(null, "$2a$something")).isFalse();
    assertThat(PasswordUtil.verify("pw", null)).isFalse();
    assertThat(PasswordUtil.verify(null, null)).isFalse();
  }
}

