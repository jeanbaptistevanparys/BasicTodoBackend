package org.example.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public class JwtUtil {
  private static final ObjectMapper mapper = new ObjectMapper();

  private static String b64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static byte[] b64UrlDecode(String s) {
    return Base64.getUrlDecoder().decode(s);
  }

  public static String sign(Map<String, Object> claims, String secret) {
    try {
      String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
      String payloadJson = mapper.writeValueAsString(claims);
      String header64 = b64Url(headerJson.getBytes(StandardCharsets.UTF_8));
      String payload64 = b64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
      String signingInput = header64 + "." + payload64;
      String sig = hmacSha256(signingInput, secret);
      return signingInput + "." + sig;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, Object> verify(String token, String secret) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) throw new IllegalArgumentException("Invalid token");
      String signingInput = parts[0] + "." + parts[1];
      String expectedSig = hmacSha256(signingInput, secret);
      if (!constantTimeEquals(expectedSig, parts[2])) throw new IllegalArgumentException("Bad signature");
      byte[] payloadBytes = b64UrlDecode(parts[1]);
      @SuppressWarnings("unchecked") Map<String, Object> claims = mapper.readValue(payloadBytes, Map.class);
      Object exp = claims.get("exp");
      if (exp instanceof Number) {
        long expEpoch = ((Number) exp).longValue();
        if (Instant.now().getEpochSecond() > expEpoch) throw new IllegalArgumentException("Token expired");
      }
      return claims;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String hmacSha256(String data, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return b64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a.length() != b.length()) return false;
    int res = 0;
    for (int i = 0; i < a.length(); i++) res |= a.charAt(i) ^ b.charAt(i);
    return res == 0;
  }
}

