package de.marcelsauer.jmsloadtester.handler;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * JMS Load Tester Copyright (C) 2008 Marcel Sauer
 * <marcel[underscore]sauer[at]gmx.de>
 * 
 * This file is part of JMS Load Tester.
 * 
 * JMS Load Tester is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * JMS Load Tester is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * JMS Load Tester. If not, see <http://www.gnu.org/licenses/>.
 */
public class DefaultSessionHandlerImpl extends AbstractThreadAwareSessionHandler {

    private String ackMode;
    
    public DefaultSessionHandlerImpl(String ackMode){
        this.ackMode = ackMode;
    }

    @Override
    final Session getThreadSession(Connection connection) throws JMSException {
        return connection.createSession(false, ACK_MODE.valueOf(ackMode).getMode());
    }
}