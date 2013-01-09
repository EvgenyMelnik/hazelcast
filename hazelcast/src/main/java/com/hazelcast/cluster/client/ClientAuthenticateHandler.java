/*
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.cluster.client;

import com.hazelcast.client.ClientCommandHandler;
import com.hazelcast.client.ClientEndpoint;
import com.hazelcast.cluster.BindOperation;
import com.hazelcast.instance.Node;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.Connection;
import com.hazelcast.nio.Protocol;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.SerializationConstants;
import com.hazelcast.security.Credentials;
import com.hazelcast.security.UsernamePasswordCredentials;
import com.hazelcast.spi.NodeEngine;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.net.Socket;
import java.util.logging.Level;

public class ClientAuthenticateHandler extends ClientCommandHandler {
    ILogger logger;

    public ClientAuthenticateHandler(NodeEngine node) {
        super(node);
    }

    @Override
    public Protocol processCall(Node node, Protocol protocol) {
        logger = node.loggingService.getLogger(this.getClass().getName());
        Credentials credentials;
        String[] args = protocol.args;
        if (node.securityContext == null) {
            if (args.length < 2) {
                throw new RuntimeException("Should provide both username and password");
            }
            credentials = new UsernamePasswordCredentials(args[0], args[1]);
        } else {
            // TODO: !!! FIX ME !!!
            final byte[] array = protocol.buffers[0].array();
            Data data = new Data(SerializationConstants.CONSTANT_TYPE_BYTE_ARRAY, array);
            credentials = (Credentials) node.nodeEngine.toObject(data);
        }
        boolean authenticated = doAuthenticate(node, credentials, protocol.conn);
        return authenticated ? protocol.success() : protocol.error(null);
    }

    private boolean doAuthenticate(Node node, Credentials credentials, Connection conn) {
        boolean authenticated;
        if (credentials == null) {
            authenticated = false;
            logger.log(Level.SEVERE, "Could not retrieve Credentials object!");
        } else if (node.securityContext != null) {
            final Socket endpointSocket = conn.getSocketChannelWrapper().socket();
            // TODO: check!!!
            credentials.setEndpoint(endpointSocket.getInetAddress().getHostAddress());
            try {
                LoginContext lc = node.securityContext.createClientLoginContext(credentials);
                lc.login();
                node.clientCommandService.getClientEndpoint(conn).setLoginContext(lc);
                authenticated = true;
            } catch (LoginException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                authenticated = false;
            }
        } else {
            if (credentials instanceof UsernamePasswordCredentials) {
                final UsernamePasswordCredentials usernamePasswordCredentials = (UsernamePasswordCredentials) credentials;
                final String nodeGroupName = node.getConfig().getGroupConfig().getName();
                final String nodeGroupPassword = node.getConfig().getGroupConfig().getPassword();
                authenticated = (nodeGroupName.equals(usernamePasswordCredentials.getUsername())
                        && nodeGroupPassword.equals(usernamePasswordCredentials.getPassword()));
            } else {
                authenticated = false;
                logger.log(Level.SEVERE, "Hazelcast security is disabled.\nUsernamePasswordCredentials or cluster group-name" +
                        " and group-password should be used for authentication!\n" +
                        "Current credentials type is: " + credentials.getClass().getName());
            }
        }
        logger.log((authenticated ? Level.INFO : Level.WARNING), "received auth from " + conn
                + ", " + (authenticated ?
                "successfully authenticated" : "authentication failed"));
        if (!authenticated) {
            node.clientCommandService.removeClientEndpoint(conn);
        } else {
            ClientEndpoint clientEndpoint = node.clientCommandService.getClientEndpoint(conn);
            clientEndpoint.authenticated();
//            Bind bind = new Bind(new Address(conn.getSocketChannelWrapper().socket().getInetAddress(), conn.getSocketChannelWrapper().socket().getPort()));
//            bind.setConnection(conn);
//            bind.setNode(node);
//            node.clusterService.enqueueAndWait(bind);
            BindOperation bind = new BindOperation(new Address(conn.getSocketChannelWrapper().socket().getInetAddress(), conn.getSocketChannelWrapper().socket().getPort()));
            bind.setConnection(conn);
            bind.setNodeEngine(node.nodeEngine);
            node.nodeEngine.getOperationService().runOperation(bind);
            ///?????
        }
        return authenticated;
    }
}
