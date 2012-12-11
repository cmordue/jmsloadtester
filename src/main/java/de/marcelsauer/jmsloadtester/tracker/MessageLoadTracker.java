/**
 * Copyright (C) 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
