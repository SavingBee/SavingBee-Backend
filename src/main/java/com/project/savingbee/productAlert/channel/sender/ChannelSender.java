package com.project.savingbee.productAlert.channel.sender;

import com.project.savingbee.productAlert.channel.compose.AlertMessage;
import com.project.savingbee.productAlert.channel.exception.ChannelException;

public interface ChannelSender {

  void send(AlertMessage message) throws ChannelException;
}
