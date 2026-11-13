package com.example.myshopapp.payment.support;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 读取支付平台密钥文件：支持 PKCS8 私钥、X509 公钥以及 X509 证书。
 */
public final class PemKeyLoader {

    private PemKeyLoader() {
    }

    public static PrivateKey loadPrivateKey(String path) {
        try {
            byte[] content = Base64.getDecoder().decode(stripPemHeader(Files.readString(Path.of(path))));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(content));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付私钥加载失败，请检查密钥文件");
        }
    }

    public static PublicKey loadPublicKey(String path) {
        try {
            String raw = Files.readString(Path.of(path));
            if (raw.contains("BEGIN CERTIFICATE")) {
                return loadCertificateContent(raw).getPublicKey();
            }
            byte[] content = Base64.getDecoder().decode(stripPemHeader(raw));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(content));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付公钥加载失败，请检查密钥文件");
        }
    }

    public static X509Certificate loadCertificate(String path) {
        try {
            return loadCertificateContent(Files.readString(Path.of(path)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付平台证书加载失败，请检查证书文件");
        }
    }

    private static X509Certificate loadCertificateContent(String content) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
    }

    /** 去掉 PEM 头尾和所有空白，得到纯 Base64 内容 */
    private static String stripPemHeader(String content) {
        return content.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
    }
}
