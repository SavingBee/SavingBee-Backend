package com.project.savingbee.productAlert.channel.exception;

public class RetryableChannelException extends ChannelException {
  public RetryableChannelException(String message, Throwable cause) {
    super(message, cause);
  }

  public RetryableChannelException(String message) {
    super(message);
  }
}
