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
package de.marcelsauer.jmsloadtester;

import de.marcelsauer.jmsloadtester.config.Config;
import de.marcelsauer.jmsloadtester.core.Constants;
import de.marcelsauer.jmsloadtester.output.OutputStrategyFactory;
import de.marcelsauer.jmsloadtester.result.ResultContainer;
import de.marcelsauer.jmsloadtester.spring.SpringFactory;
import de.marcelsauer.jmsloadtester.tools.Logger;
import de.marcelsauer.jmsloadtester.tools.StringUtils;
import de.marcelsauer.jmsloadtester.tracker.MessageTracker;
import de.marcelsauer.jmsloadtester.tracker.ThreadTracker;
import de.marcelsauer.jmsloadtester.tracker.TimeTracker;

public class Main {

    private ResultContainer resultContainer;
    private MessageTracker messageTracker;
    private TimeTracker listenerTimeTracker;
    private TimeTracker senderTimeTracker;
    private ThreadTracker threadTracker;
    private Config config;
    private boolean exitGracefully;

    public Main(final boolean exitGracefully) {
        this.exitGracefully = exitGracefully;
        setup();
    }

    public Main() {
        this(true);
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.doRun();
    }

    private void setup() {
        // default logging output strategy
        Logger.setOut(OutputStrategyFactory.getOutputStrategy(OutputStrategyFactory.STDOUT));

        config = SpringFactory.getBean("config");

        listenerTimeTracker = SpringFactory.getBean("listenerTimeTracker");
        senderTimeTracker = SpringFactory.getBean("senderTimeTracker");
        messageTracker = SpringFactory.getBean("messageTracker");
        threadTracker = SpringFactory.getBean("threadTracker");
        resultContainer = SpringFactory.getBean("resultContainer");

        messageTracker.setTotalMessagesToBeReceived(config.getSubscriberWaitForTotalMessages());
        messageTracker.setTotalMessagesToBeSent(config.getExpectedMessageSentCount());

        Logger.info("using debug output strategy: " + config.getDebugOutputStrategy());
        Logger.info("using result output strategy: " + config.getResultOutputStrategy());
        Logger.info("using message output strategy: " + config.getMessageOutputStrategy());
        Logger.info("using message content strategy: " + config.getMessageContentStrategy());

        // init logger
        Logger.setOut(config.getDebugOutputStrategy());
    }

    // package private for testcase
    void doRun() {

        checkState();
        printIntro();

        try {
            // start of subscribers
        	long listenerRampupSleep = config.getListenerRampup();
            for (int i = 1, count = config.getSubscribersToStart(); i <= count; i++) {
                threadTracker.createListenerThread("Listener Thread " + i);
                if (listenerRampupSleep > 0) {
                	Thread.sleep(listenerRampupSleep);
                }
            }

            // TODO how much sleep?
            if (config.getSubscribersToStart() > 0) {
                Thread.sleep(2000);
            }

            // start of publishers
            long senderRampupSleep = config.getSenderRampup();
            for (int i = 1, count = config.getSendersToStart(); i <= count; i++) {
                threadTracker.createSenderThread("Sender Thread " + i);
                if (senderRampupSleep > 0) {
                	Thread.sleep(senderRampupSleep);
                }
            }

            // loop until all is done
            long progressSleep = getSleepCount();
            while (!(messageTracker.isAllReceived() && messageTracker.isAllSent())) {
                printProgress();
                if (progressSleep > 0) {
                	Thread.sleep(progressSleep);
                }
                Thread.yield();
            }

            Logger.info(Constants.EOL + "****************** MAIN DONE ******************");

        } catch (Exception e) {
            Logger.error("could not execute application", e);
        } finally {
            printProgress();
            outputResult();
            Logger.debug("exiting");
            exitGracefully();
        }
    }

    private void printIntro() {
        // listener
        Logger.info("creating " + config.getSubscribersToStart() + " listener threads. each waiting for " + config.getEachSubscriberWaitFor() + " messages.");
        Logger.info("waiting for " + config.getSubscriberWaitForTotalMessages() + " messages in total");
        Logger.info("lister ramp up time: " + config.getListenerRampup() + " ms");
        Logger.info("printing progress every " + getSleepCount() / Constants.MILLIS_FACTOR + " seconds");

        // sender
        Logger.info("creating " + config.getSendersToStart() + " sender threads. each sending " + config.getMessagesToSend() + " messages");
        Logger.info("sending " + config.getExpectedMessageSentCount() + " messages in total");
        Logger.info("sender ramp up time: " + config.getSenderRampup() + " ms");
    }

    private void outputResult() {
        // generic stuff
        resultContainer.addResultKeyValue("listeners started", threadTracker.getListenersStarted());
        resultContainer.addResultKeyValue("senders started", threadTracker.getSendersStarted());
        resultContainer.addResultKeyValue("expected total message count", config.getSubscriberWaitForTotalMessages());
        resultContainer.addResultKeyValue("total messages received", messageTracker.getTotalMessagesReceived());
        resultContainer.addResultKeyValue("total messages sent", messageTracker.getTotalMessagesSent());
        resultContainer.addResultKeyValue("number of missing messages", (config.getSubscriberWaitForTotalMessages() - messageTracker.getTotalMessagesReceived()));
        resultContainer.addResultKeyValue("uniqe JMSMessageID count of all incoming messages", messageTracker.getMessagesReceived().size());
        resultContainer.addResultKeyValue("uniqe JMSMessageID count of all sent messages", messageTracker.getMessagesSent().size());

        // sender stuff
        resultContainer.addResultKeyValue("Sender duration nanoseconds", senderTimeTracker.getDurationInNanoSeconds());
        resultContainer.addResultKeyValue("Sender duration milliseconds", senderTimeTracker.getDurationInMilliSeconds());
        resultContainer.addResultKeyValue("Sender duration sec", senderTimeTracker.getDurationInSeconds());
        resultContainer.addResultKeyValue("Sender messages / sec", messageTracker.getTotalMessagesSent() / senderTimeTracker.getDurationInSeconds());
        resultContainer.addResultKeyValue("Sender messages / milli sec", messageTracker.getTotalMessagesSent() / senderTimeTracker.getDurationInMilliSeconds());

        // listener stuff
        resultContainer.addResultKeyValue("Listener duration nanoseconds", listenerTimeTracker.getDurationInNanoSeconds());
        resultContainer.addResultKeyValue("Listener duration milliseconds", listenerTimeTracker.getDurationInMilliSeconds());
        resultContainer.addResultKeyValue("Listener duration sec", listenerTimeTracker.getDurationInSeconds());
        resultContainer.addResultKeyValue("Listener messages / sec", messageTracker.getTotalMessagesReceived() / listenerTimeTracker.getDurationInSeconds());
        resultContainer.addResultKeyValue("Listener messages / milli sec", messageTracker.getTotalMessagesReceived() / listenerTimeTracker.getDurationInMilliSeconds());

        resultContainer.outputResult(config.getResultOutputStrategy());
    }

    private void exitGracefully() {
        if (exitGracefully) {
            Runtime.getRuntime().exit(0);
        }
    }

    private void checkState() {
        if (config.getExpectedMessageSentCount() <= 0 && config.getSubscriberWaitForTotalMessages() <= 0) {
            Logger.info("no listeners and senders are configured or message count is 0. please check your config");
            exitGracefully();
        }
    }

    private void printProgress() {
        final String senderProgress = getSenderProgress();
        final String listenerProgess = getListenerProgress();
        if (!StringUtils.isEmpty(senderProgress)) {
            Logger.info(senderProgress);
        }
        if (!StringUtils.isEmpty(listenerProgess)) {
            Logger.info(listenerProgess);
        }
    }

    private String getSenderProgress() {
        final StringBuffer sb = new StringBuffer();
        if (config.getExpectedMessageSentCount() > 0) {
            sb.append("[SENDER] " + messageTracker.getTotalMessagesSent() + " of " + config.getExpectedMessageSentCount() + " messages sent so far to [" + config.getSendToDestination() + "] => ");
            sb.append(String.format("%2.4f msg/s, ", messageTracker.getTotalMessagesSent() / senderTimeTracker.getDurationInSeconds()));
            sb.append(String.format("%2.4f msg/ms ", messageTracker.getTotalMessagesSent() / senderTimeTracker.getDurationInMilliSeconds()));
        }
        return sb.toString();
    }

    private String getListenerProgress() {
        final StringBuffer sb = new StringBuffer();
        if (config.getSubscriberWaitForTotalMessages() > 0) {
            sb.append("[LISTENER] received " + messageTracker.getTotalMessagesReceived() + " of " + config.getSubscriberWaitForTotalMessages() + " expected messages on [" + config.getListenToDestination() + "], ");
            sb.append(String.format("%2.4f msg/s, ", messageTracker.getTotalMessagesReceived() / listenerTimeTracker.getDurationInSeconds()));
            sb.append(String.format("%2.4f msg/ms ", messageTracker.getTotalMessagesReceived() / listenerTimeTracker.getDurationInMilliSeconds()));
        }
        return sb.toString();
    }

    private int getSleepCount() {
        return config.getPauseBetweenPrintProgress();
    }

}
