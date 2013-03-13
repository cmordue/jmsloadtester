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

import java.util.ArrayList;
import java.util.List;

public class ActiveMqEchoServerApp
{
    private static final int DEFAULT_NUM_THREADS = 1;
    private static final String DEFAULT_QUEUE_NAME = "rr";
    private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

    private static final String PROPERTY_NUM_THREADS = "numThreads";
    private static final String PROPERTY_QUEUE_NAME = "queueName";
    private static final String PROPERTY_BROKER_URL = "brokerUrl";
    
    private int numThreads = DEFAULT_NUM_THREADS;
    private String queueName = DEFAULT_QUEUE_NAME;
    private String brokerUrl = DEFAULT_BROKER_URL;
    
    public void start()
    {
        List<ActiveMqEchoServer> servers = createServers();
        for (ActiveMqEchoServer echoServer : servers) {
            echoServer.start();
        }
    }
    
    public List<ActiveMqEchoServer> createServers() {
        int numServers = numThreads;
        
        List<ActiveMqEchoServer> servers = new ArrayList<ActiveMqEchoServer>(numServers);
        for (int i = 0; i < numServers; i++) {
            ActiveMqEchoServer echoServer = new ActiveMqEchoServer(brokerUrl, queueName);
            servers.add(echoServer);
        }
        return servers;
    }
    
    public void setNumThreads(int numThreads)
    {
        this.numThreads = numThreads;
    }
    
    public void setQueueName(String queueName)
    {
        this.queueName = queueName;
    }
    
    public void setBrokerUrl(String brokerUrl)
    {
        this.brokerUrl = brokerUrl;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        ActiveMqEchoServerApp app = new ActiveMqEchoServerApp();
        app.setNumThreads(Integer.parseInt(System.getProperty(PROPERTY_NUM_THREADS, Integer.toString(DEFAULT_NUM_THREADS))));
        app.setQueueName(System.getProperty(PROPERTY_QUEUE_NAME, DEFAULT_QUEUE_NAME));
        app.setBrokerUrl(System.getProperty(PROPERTY_BROKER_URL, DEFAULT_BROKER_URL));
        app.start();
    }

}


