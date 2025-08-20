package com.project.savingbee.productAlert.channel.exception;

public class NonRetryableChannelException extends RuntimeException {
  public NonRetryableChannelException(String message, Throwable cause) {
    super(message, cause);
  }

  public NonRetryableChannelException(String message) {
    super(message);
  }
}
