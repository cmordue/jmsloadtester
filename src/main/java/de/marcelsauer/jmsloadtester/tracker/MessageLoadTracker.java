package de.marcelsauer.jmsloadtester.tracker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.Message;

import de.marcelsauer.jmsloadtester.client.SenderTrafficCop;
import de.marcelsauer.jmsloadtester.message.MessageNotifyable;
import de.marcelsauer.jmsloadtester.message.MessageSentAware;
import de.marcelsauer.jmsloadtester.tools.Logger;

public class MessageLoadTracker implements MessageSentAware, MessageNotifyable, SenderTrafficCop {

	private int maxOutstandingMessages;
	private final AtomicInteger outstandingMessages = new AtomicInteger(); 
	private Lock lock = new ReentrantLock();
	private Condition canSend = lock.newCondition();
	
	public MessageLoadTracker(int maxOutstandingMessages) {
		this.maxOutstandingMessages = maxOutstandingMessages;
	}

	@Override
	public void waitUntilSendable() {
		lock.lock();
		try {
			while (!canSend()) {
				try {
					canSend.await();
				} catch (InterruptedException e) {
					Logger.info("interrupted");
					Thread.currentThread().interrupt();
					return;
				}
			}
//			Logger.info("will send: " + outstandingMessages.get());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean canSend() {
		return outstandingMessages.get() < maxOutstandingMessages;
	}

	@Override
	public void onMessage(Message message) {
		outstandingMessages.decrementAndGet();
		lock.lock();
		try {
			canSend.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void messageSent(Message message) {
		outstandingMessages.incrementAndGet();
	}

}
