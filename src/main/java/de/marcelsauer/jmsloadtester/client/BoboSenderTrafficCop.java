package de.marcelsauer.jmsloadtester.client;

public class BoboSenderTrafficCop implements SenderTrafficCop {

	@Override
	public void waitUntilSendable() {
		return;
	}

	@Override
	public boolean canSend() {
		return true;
	}

}
