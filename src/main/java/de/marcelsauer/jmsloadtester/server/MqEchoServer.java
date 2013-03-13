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

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class MqEchoServer implements MessageListener
{
    private int ackMode = Session.AUTO_ACKNOWLEDGE;
    private int deliveryMode = DeliveryMode.NON_PERSISTENT;
    private String messageQueueName;

    private Connection connection;
    private Session session;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private ConnectionFactory connectionFactory;
    private boolean isStarted = false;

    public MqEchoServer(ConnectionFactory connectionFactory,
                        String messageQueueName) {
        this.connectionFactory = connectionFactory;
        this.messageQueueName = messageQueueName;
    }
    
    public synchronized void start() {
        setupMessageQueueConsumer();
        isStarted = true;
    }

    private void setupMessageQueueConsumer() {
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ackMode);
            Destination adminQueue = this.session.createQueue(getMessageQueueName());

            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(deliveryMode);

            //Set up a consumer to consume messages off of the admin queue
            MessageConsumer consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            //TODO: Handle the exception appropriately
            e.printStackTrace();
            try {
                stop();
            } catch (JMSException e1) {
                // ignore
            }
        }
    }
    
    public void stop() throws JMSException {
        try {
            if (session != null) {
                session.close();
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    public void onMessage(Message message) {
        try {
            String correlationId = message.getJMSCorrelationID();
            System.out.println("Message received. CorrelationId=" + String.valueOf(correlationId));

            Message response = createEchoMessage(message);

            //Set the correlation ID from the received message to be the correlation id of the response message
            //this lets the client identify which message this is a response to if it has more than
            //one outstanding message to the server
            response.setJMSCorrelationID(message.getJMSCorrelationID());

            //Send the response to the Destination specified by the JMSReplyTo field of the received message,
            //this is presumably a temporary queue created by the client
            System.out.println("Sending response to: " + message.getJMSReplyTo());
            this.replyProducer.send(message.getJMSReplyTo(), response);
        } catch (JMSException e) {
            //TODO: Handle the exception appropriately
        }
    }
    
    protected Message createEchoMessage(Message message) throws JMSException {
        TextMessage response = this.session.createTextMessage();
        if (message instanceof TextMessage) {
            TextMessage txtMsg = (TextMessage) message;
            String messageText = txtMsg.getText();
            
            response.setText(messageText);
        } else if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            Enumeration<?> names = mapMessage.getMapNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                System.out.println(name.toString() + ": " + mapMessage.getObject(name.toString()));
            }
        }
            
        return response;
    }
    
    public boolean isStarted() {
        return isStarted;
    }
    
    public void setAckMode(int ackMode) {
        if (isStarted()) {
            throw new IllegalStateException("Can not change the ack mode once the server is started");
        }
        this.ackMode = ackMode;
    }
    
    public int getAckMode() {
        return ackMode;
    }
    
    public int getDeliveryMode() {
        return deliveryMode;
    }
    
    protected String getMessageQueueName() {
        return messageQueueName;
    }
}


