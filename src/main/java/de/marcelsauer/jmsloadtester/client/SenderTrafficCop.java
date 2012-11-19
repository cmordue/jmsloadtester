package de.marcelsauer.jmsloadtester.client;

public interface SenderTrafficCop {

	public void waitUntilSendable();
	
	public boolean canSend();
}
