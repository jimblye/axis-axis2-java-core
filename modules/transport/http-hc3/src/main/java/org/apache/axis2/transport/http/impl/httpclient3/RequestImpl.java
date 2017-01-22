/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.transport.http.impl.httpclient3;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.axiom.mime.Header;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.http.AxisRequestEntity;
import org.apache.axis2.transport.http.HTTPAuthenticator;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportConstants;
import org.apache.axis2.transport.http.Request;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

final class RequestImpl implements Request {
    private static final Log log = LogFactory.getLog(RequestImpl.class);

    protected final HTTPSenderImpl sender;
    protected final MessageContext msgContext;
    protected final URL url;
    protected final HttpMethodBase method;
    protected final HttpClient httpClient;
    private final HostConfiguration config;

    RequestImpl(HTTPSenderImpl sender, MessageContext msgContext, final String methodName, URL url,
            AxisRequestEntity requestEntity) throws AxisFault {
        this.sender = sender;
        this.msgContext = msgContext;
        this.url = url;
        httpClient = sender.getHttpClient(msgContext);
        if (requestEntity == null) {
            method = new HttpMethodBase() {
                @Override
                public String getName() {
                    return methodName;
                }
            };
            // This mimicks GetMethod
            if (methodName.equals(HTTPConstants.HTTP_METHOD_GET)) {
                method.setFollowRedirects(true);
            }
        } else {
            EntityEnclosingMethod entityEnclosingMethod = new EntityEnclosingMethod() {
                @Override
                public String getName() {
                    return methodName;
                }
            };
            entityEnclosingMethod.setRequestEntity(new AxisRequestEntityImpl(requestEntity));
            if (!sender.getHttpVersion().equals(HTTPConstants.HEADER_PROTOCOL_10) && sender.isChunked()) {
                entityEnclosingMethod.setContentChunked(true);
            }
            method = entityEnclosingMethod;
        }
        method.setPath(url.getPath());
        method.setQueryString(url.getQuery());
        // TODO: this is fishy; it means that we may end up modifying a HostConfiguration from a cached HTTP client
        HostConfiguration config = httpClient.getHostConfiguration();
        if (config == null) {
            config = new HostConfiguration();
        }
        this.config = config;
    }

    @Override
    public void enableHTTP10() {
        httpClient.getParams().setVersion(HttpVersion.HTTP_1_0);
    }

    @Override
    public void setHeader(String name, String value) {
        method.setRequestHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        method.addRequestHeader(name, value);
    }

    private static Header[] convertHeaders(org.apache.commons.httpclient.Header[] headers) {
        Header[] result = new Header[headers.length];
        for (int i=0; i<headers.length; i++) {
            result[i] = new Header(headers[i].getName(), headers[i].getValue());
        }
        return result;
    }

    @Override
    public Header[] getRequestHeaders() {
        return convertHeaders(method.getRequestHeaders());
    }

    @Override
    public int getStatusCode() {
        return method.getStatusCode();
    }

    @Override
    public String getStatusText() {
        return method.getStatusText();
    }

    @Override
    public Header[] getResponseHeaders() {
        return convertHeaders(method.getResponseHeaders());
    }

    @Override
    public void execute() throws AxisFault {
        try {
            executeMethod();
            handleResponse();
        } catch (IOException e) {
            log.info("Unable to send to url[" + url + "]", e);
            throw AxisFault.makeFault(e);
        } finally {
            cleanup();
        }
    }

    private void executeMethod() throws IOException {
        populateHostConfiguration();

        // add compression headers if needed
        if (msgContext.isPropertyTrue(HTTPConstants.MC_ACCEPT_GZIP)) {
            method.addRequestHeader(HTTPConstants.HEADER_ACCEPT_ENCODING,
                    HTTPConstants.COMPRESSION_GZIP);
        }

        if (msgContext.getProperty(HTTPConstants.HTTP_METHOD_PARAMS) != null) {
            HttpMethodParams params = (HttpMethodParams) msgContext
                    .getProperty(HTTPConstants.HTTP_METHOD_PARAMS);
            method.setParams(params);
        }

        String cookiePolicy = (String) msgContext.getProperty(HTTPConstants.COOKIE_POLICY);
        if (cookiePolicy != null) {
            method.getParams().setCookiePolicy(cookiePolicy);
        }
        HttpState httpState = (HttpState) msgContext.getProperty(HTTPConstants.CACHED_HTTP_STATE);

        sender.setTimeouts(msgContext, method);

        httpClient.executeMethod(config, method, httpState);
    }

    private void handleResponse() throws IOException {
        int statusCode = getStatusCode();
        log.trace("Handling response - " + statusCode);
        if (statusCode == HttpStatus.SC_ACCEPTED) {
            /* When an HTTP 202 Accepted code has been received, this will be the case of an execution 
             * of an in-only operation. In such a scenario, the HTTP response headers should be returned,
             * i.e. session cookies. */
            sender.obtainHTTPHeaderInformation(this, method, msgContext);
            // Since we don't expect any content with a 202 response, we must release the connection
            method.releaseConnection();            
        } else if (statusCode >= 200 && statusCode < 300) {
            // Save the HttpMethod so that we can release the connection when cleaning up
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, method);
            sender.processResponse(this, method, msgContext);
        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
                || statusCode == HttpStatus.SC_BAD_REQUEST) {
            // Save the HttpMethod so that we can release the connection when
            // cleaning up
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, method);
            org.apache.commons.httpclient.Header contenttypeHeader = method.getResponseHeader(HTTPConstants.HEADER_CONTENT_TYPE);
            String value = null;
            if (contenttypeHeader != null) {
                value = contenttypeHeader.getValue();
            }
            OperationContext opContext = msgContext.getOperationContext();
            if (opContext != null) {
                MessageContext inMessageContext = opContext
                        .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
                if (inMessageContext != null) {
                    inMessageContext.setProcessingFault(true);
                }
            }
            if (value != null) {

                sender.processResponse(this, method, msgContext);
            }

            if (org.apache.axis2.util.Utils.isClientThreadNonBlockingPropertySet(msgContext)) {
                throw new AxisFault(Messages.getMessage("transportError",
                        String.valueOf(statusCode), getStatusText()));
            }
        } else {
            // Since we don't process the response, we must release the
            // connection immediately
            method.releaseConnection();
            throw new AxisFault(Messages.getMessage("transportError", String.valueOf(statusCode),
                    getStatusText()));
        }
    }

    private void cleanup() {
        if (msgContext.isPropertyTrue(HTTPConstants.AUTO_RELEASE_CONNECTION)) {
            log.trace("AutoReleasing " + method);
            method.releaseConnection();
        }
    }

    /**
     * getting host configuration to support standard http/s, proxy and NTLM
     * support
     * 
     * @return a HostConfiguration set up with proxy information
     * @throws AxisFault
     *             if problems occur
     */
    protected void populateHostConfiguration() throws AxisFault {

        int port = url.getPort();

        String protocol = url.getProtocol();
        if (port == -1) {
            if (HTTPTransportConstants.PROTOCOL_HTTP.equals(protocol)) {
                port = 80;
            } else if (HTTPTransportConstants.PROTOCOL_HTTPS.equals(protocol)) {
                port = 443;
            }

        }

        // one might need to set his own socket factory. Let's allow that case
        // as well.
        Protocol protocolHandler = (Protocol) msgContext.getOptions().getProperty(
                HTTPConstants.CUSTOM_PROTOCOL_HANDLER);

        // setting the real host configuration
        // I assume the 90% case, or even 99% case will be no protocol handler
        // case.
        if (protocolHandler == null) {
            config.setHost(url.getHost(), port, url.getProtocol());
        } else {
            config.setHost(url.getHost(), port, protocolHandler);
        }

        // proxy configuration

        if (HTTPProxyConfigurator.isProxyEnabled(msgContext, url)) {
            if (log.isDebugEnabled()) {
                log.debug("Configuring HTTP proxy.");
            }
            HTTPProxyConfigurator.configure(msgContext, httpClient, config);
        }
    }

    /*
     * This will handle server Authentication, It could be either NTLM, Digest
     * or Basic Authentication. Apart from that user can change the priory or
     * add a custom authentication scheme.
     */
    @Override
    public void enableAuthentication(HTTPAuthenticator authenticator) {
        method.setDoAuthentication(true);

        String username = authenticator.getUsername();
        String password = authenticator.getPassword();
        String host = authenticator.getHost();
        String domain = authenticator.getDomain();

        int port = authenticator.getPort();
        String realm = authenticator.getRealm();

        Credentials creds;

        HttpState tmpHttpState = null;
        HttpState httpState = (HttpState) msgContext
                .getProperty(HTTPConstants.CACHED_HTTP_STATE);
        if (httpState != null) {
            tmpHttpState = httpState;
        } else {
            tmpHttpState = httpClient.getState();
        }

        httpClient.getParams().setAuthenticationPreemptive(
                authenticator.getPreemptiveAuthentication());

        if (host != null) {
            if (domain != null) {
                /* Credentials for NTLM Authentication */
                creds = new NTCredentials(username, password, host, domain);
            } else {
                /* Credentials for Digest and Basic Authentication */
                creds = new UsernamePasswordCredentials(username, password);
            }
            tmpHttpState.setCredentials(new AuthScope(host, port, realm), creds);
        } else {
            if (domain != null) {
                /*
                 * Credentials for NTLM Authentication when host is
                 * ANY_HOST
                 */
                creds = new NTCredentials(username, password, AuthScope.ANY_HOST, domain);
                tmpHttpState.setCredentials(new AuthScope(AuthScope.ANY_HOST, port, realm),
                        creds);
            } else {
                /* Credentials only for Digest and Basic Authentication */
                creds = new UsernamePasswordCredentials(username, password);
                tmpHttpState.setCredentials(new AuthScope(AuthScope.ANY), creds);
            }
        }
        /* Customizing the priority Order */
        List schemes = authenticator.getAuthSchemes();
        if (schemes != null && schemes.size() > 0) {
            List authPrefs = new ArrayList(3);
            for (int i = 0; i < schemes.size(); i++) {
                if (schemes.get(i) instanceof AuthPolicy) {
                    authPrefs.add(schemes.get(i));
                    continue;
                }
                String scheme = (String) schemes.get(i);
                authPrefs.add(authenticator.getAuthPolicyPref(scheme));

            }
            httpClient.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
        }
    }
}
