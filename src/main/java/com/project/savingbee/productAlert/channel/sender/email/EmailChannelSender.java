package com.project.savingbee.productAlert.channel.sender.email;

import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.exception.NonRetryableChannelException;
import com.project.savingbee.productAlert.channel.exception.RetryableChannelException;
import com.project.savingbee.productAlert.channel.sender.ChannelSender;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component("emailSender")
@RequiredArgsConstructor
public class EmailChannelSender implements ChannelSender {
  private final JavaMailSender mailSender;

  @Value("${alert.mail.from:no-reply@savingbee.dev}")
  private String fromAddress;

  @Value("${alert.mail.fromName:SavingBee}")
  private String fromName;

  @Override
  public void send(AlertMessage message) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

      helper.setTo(message.getTo());
      helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));

      if (message.getSubject() != null) {
        helper.setSubject(message.getSubject());
      }

      helper.setText(message.getBody() == null ? "" : message.getBody(), true);

      mailSender.send(mimeMessage);
    } catch (MailAuthenticationException e) {
      throw new NonRetryableChannelException("SMTP 인증 오류", e);

    } catch (MailPreparationException e) {
      throw new NonRetryableChannelException("메일 구성 오류", e);

    } catch (MailSendException e) {
      if (addressProblem(e)) {
        throw new NonRetryableChannelException("메일 주소 오류", e);
      }
      throw new RetryableChannelException("메일 서버 오류", e);

    } catch (MessagingException e) {
      throw new RetryableChannelException("메일 전송 실패(네트워크)", e);

    } catch (Exception e) {
      throw new RetryableChannelException("메일 전송 실패(기타)", e);

    }
  }

  private boolean addressProblem(Throwable t) {
    while (t != null) {
      // 주소 형식 오류/수신자 오류면 재시도 X
      if (t instanceof AddressException || t instanceof SendFailedException) {
        return true;
      }
      t = t.getCause();
    }

    return false;
  }
}
