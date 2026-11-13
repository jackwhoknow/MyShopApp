package com.example.myshopapp.auth;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码：生成简单算术题并输出 PNG，登录时必须带回正确答案，防止脚本批量登录。
 * 验证码存放在 Redis，2 分钟有效，使用一次后立即失效，多实例共享同一份数据。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String KEY_PREFIX = "myshop:captcha:";
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final int WIDTH = 110;
    private static final int HEIGHT = 38;

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;

    public CaptchaVO create() {
        int left = random.nextInt(10) + 1;
        int right = random.nextInt(10) + 1;
        boolean plus = random.nextBoolean();
        // 减法保证结果非负，避免用户输入负号
        if (!plus && left < right) {
            int temp = left;
            left = right;
            right = temp;
        }
        int answer = plus ? left + right : left - right;
        String expression = left + (plus ? " + " : " - ") + right + " = ?";

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + captchaId, String.valueOf(answer), TTL);
        return new CaptchaVO(captchaId, "data:image/png;base64," + render(expression));
    }

    /**
     * 校验并立即作废验证码。
     */
    public void verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入验证码");
        }
        // 取出即删除：同一验证码只能使用一次
        String answer = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + captchaId);
        if (answer == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码已过期，请刷新");
        }
        if (!answer.equals(captchaCode.trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不正确");
        }
    }

    private String render(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(247, 249, 252));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            // 干扰线
            for (int i = 0; i < 6; i++) {
                graphics.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            graphics.setColor(new Color(200, 22, 35));
            graphics.drawString(text, 12, 27);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码生成失败");
        }
    }
}
