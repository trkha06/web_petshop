package com.utepetshop.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendOtpEmail(String toEmail, String otp, String purpose) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail, "UTEPet Care & Spa");
      helper.setTo(toEmail);

      String title;
      String actionText;
      if ("REGISTER".equalsIgnoreCase(purpose)) {
        title = "Xac Thuc Dang Ky Tai Khoan";
        actionText = "hoan tat dang ky tai khoan tai UTEPet Care & Spa";
      } else {
        title = "Khoi Phuc Mat Khau";
        actionText = "thiet lap lai mat khau tai khoan cua ban";
      }

      helper.setSubject("[UTEPet] " + title + " - Ma OTP: " + otp);

      String template = """
          <!DOCTYPE html>
          <html>
          <head>
            <meta charset="utf-8">
            <style>
              body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #fbf9f4; margin: 0; padding: 20px; }
              .container { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 20px; padding: 35px; box-shadow: 0 4px 15px rgba(0,0,0,0.06); border: 1px solid #ebd9cc; }
              .header { text-align: center; margin-bottom: 25px; }
              .logo { font-size: 26px; font-weight: bold; color: #226f4f; }
              .title { font-size: 20px; font-weight: 700; color: #182820; margin-top: 10px; }
              .otp-box { background: #eaf6f0; border: 2px dashed #226f4f; border-radius: 14px; padding: 18px; text-align: center; margin: 25px 0; }
              .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #ff7d57; font-family: monospace; }
              .desc { font-size: 14px; color: #485c52; line-height: 1.6; }
              .warning { font-size: 13px; color: #8c7365; margin-top: 20px; border-top: 1px solid #ebd9cc; padding-top: 15px; }
              .footer { text-align: center; font-size: 12px; color: #999; margin-top: 25px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="header">
                <div class="logo">🐾 UTEPet Care & Spa</div>
                <div class="title">%s</div>
              </div>
              <p class="desc">Xin chao quy khach,</p>
              <p class="desc">Ban dang thuc hien thao tac %s. Vui long su dung ma xac thuc OTP ben duoi:</p>
              <div class="otp-box">
                <div class="otp-code">%s</div>
              </div>
              <p class="desc" style="text-align: center; font-weight: 600; color: #ff7d57;">Ma xac thuc co hieu luc trong vong 5 phut.</p>
              <div class="warning">
                Luu y an toan: Tuyet doi khong cung cap ma OTP nay cho bat ky ai de dam bao an toan cho tai khoan cua ban.
              </div>
              <div class="footer">
                UTEPet Shop & Care - He Thong Spa Thu Cung 5 Sao - Hotline: 1900 6868
              </div>
            </div>
          </body>
          </html>
          """;

      String html = String.format(template, title, actionText, otp);

      helper.setText(html, true);
      mailSender.send(message);
      log.info("Sent OTP email to {}", toEmail);
    } catch (Exception ex) {
      log.error("Failed to send OTP email to {}", toEmail, ex);
      throw new RuntimeException("Không thể gửi email OTP: " + ex.getMessage());
    }
  }
}
