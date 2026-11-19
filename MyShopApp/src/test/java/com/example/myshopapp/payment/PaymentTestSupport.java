package com.example.myshopapp.payment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/** 支付测试用的 RSA 密钥生成与 PEM 写入工具。 */
public final class PaymentTestSupport {

    private PaymentTestSupport() {
    }

    public static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    public static Path writePrivateKey(Path dir, KeyPair pair) throws IOException {
        return writePem(dir.resolve("private.pem"), "PRIVATE KEY", pair.getPrivate().getEncoded());
    }

    public static Path writePublicKey(Path dir, KeyPair pair) throws IOException {
        return writePem(dir.resolve("public.pem"), "PUBLIC KEY", pair.getPublic().getEncoded());
    }

    public static String sign(String content, PrivateKey key) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verify(String content, String signatureBase64, PublicKey key) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(key);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(signatureBase64));
    }

    private static Path writePem(Path path, String label, byte[] der) throws IOException {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        String pem = "-----BEGIN " + label + "-----\n" + body
                + "\n-----END " + label + "-----\n";
        Files.writeString(path, pem, StandardCharsets.UTF_8);
        return path;
    }
}
