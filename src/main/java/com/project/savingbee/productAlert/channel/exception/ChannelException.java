package com.project.savingbee.productAlert.channel.exception;

public class ChannelException extends RuntimeException {
  public ChannelException(String message, Throwable cause) {
    super(message, cause);
  }

  public ChannelException(String message) {
    super(message);
  }
}
