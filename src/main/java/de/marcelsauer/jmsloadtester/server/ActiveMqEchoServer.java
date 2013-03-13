/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package de.marcelsauer.jmsloadtester.server;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

public class ActiveMqEchoServer extends MqEchoServer {
    
    private boolean isActiveMqEmbedded = false;
    private String messageBrokerUrl;

    public ActiveMqEchoServer() {
        this("tcp://localhost:61616", "rr");
    }
    
    public ActiveMqEchoServer(String messageBrokerUrl,
                              String messageQueueName) {
        this(new ActiveMQConnectionFactory(messageBrokerUrl), messageBrokerUrl, messageQueueName);
    }
    
    public ActiveMqEchoServer(ConnectionFactory connectionFactory,
                              String messageBrokerUrl,
                              String messageQueueName) {
        super(connectionFactory, messageQueueName);
        this.messageBrokerUrl = messageBrokerUrl;
    }
    
    public void setRunActiveMqEmbedded(boolean runActiveMqEmbedded) {
        if (isStarted()) {
            throw new IllegalStateException("Can not change embedded state after the echo server has started");
        }
        isActiveMqEmbedded = runActiveMqEmbedded;
    }
    
    public void start() {
        if (isActiveMqEmbedded) {
            try {
                //This message broker is embedded
                BrokerService broker = new BrokerService();
                broker.setPersistent(false);
                broker.setUseJmx(false);
                broker.addConnector(getMessageBrokerUrl());
                broker.start();
            } catch (Exception e) {
                //TODO: Handle the exception appropriately
                e.printStackTrace();
            }
        }
        super.start();
    }
    
    protected String getMessageBrokerUrl() {
        return messageBrokerUrl;
    }

    public static void main(String[] args) {
        ActiveMqEchoServer echoServer = new ActiveMqEchoServer();
        echoServer.start();
    }
}

