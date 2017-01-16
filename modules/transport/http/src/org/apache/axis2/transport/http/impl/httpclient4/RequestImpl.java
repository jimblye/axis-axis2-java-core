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
package org.apache.axis2.transport.http.impl.httpclient4;

import java.io.IOException;
import java.net.URL;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.NamedValue;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.http.AxisRequestEntity;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.Request;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

final class RequestImpl implements Request {
    private static final Log log = LogFactory.getLog(RequestImpl.class);
    
    protected final HTTPSenderImpl sender;
    protected final MessageContext msgContext;
    protected final URL url;
    protected final HttpRequestBase method;
    protected final AbstractHttpClient httpClient;

    RequestImpl(HTTPSenderImpl sender, MessageContext msgContext, URL url, AxisRequestEntity requestEntity, HttpRequestBase method) throws AxisFault {
        this.sender = sender;
        this.msgContext = msgContext;
        this.url = url;
        this.method = method;
        httpClient = sender.getHttpClient(msgContext);
        sender.populateCommonProperties(msgContext, url, method, httpClient);
        if (requestEntity != null) {
            AxisRequestEntityImpl requestEntityAdapter = new AxisRequestEntityImpl(requestEntity);
            ((HttpEntityEnclosingRequest)method).setEntity(requestEntityAdapter);
    
            if (!sender.getHttpVersion().equals(HTTPConstants.HEADER_PROTOCOL_10) && sender.isChunked()) {
                requestEntity.setChunked(sender.isChunked());
            }
        }
    }

    @Override
    public void enableHTTP10() {
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
                HttpVersion.HTTP_1_0);
    }

    @Override
    public void setHeader(String name, String value) {
        method.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        method.addHeader(name, value);
    }

    @Override
    public NamedValue[] getRequestHeaders() {
        Header[] headers = method.getAllHeaders();
        NamedValue[] result = new NamedValue[headers.length];
        for (int i=0; i<headers.length; i++) {
            result[i] = new NamedValue(headers[i].getName(), headers[i].getValue());
        }
        return result;
    }

    @Override
    public void execute() throws AxisFault {
        HttpResponse response = null;
        try {
            response = executeMethod();
            handleResponse(response);
        } catch (IOException e) {
            log.info("Unable to send to url[" + url + "]", e);
            throw AxisFault.makeFault(e);
        } finally {
            cleanup(response);
        }
    }

    private HttpResponse executeMethod() throws IOException {
        HttpHost httpHost = sender.getHostConfiguration(httpClient, msgContext, url);

        // add compression headers if needed
        if (msgContext.isPropertyTrue(HTTPConstants.MC_ACCEPT_GZIP)) {
            method.addHeader(HTTPConstants.HEADER_ACCEPT_ENCODING,
                             HTTPConstants.COMPRESSION_GZIP);
        }

        if (msgContext.isPropertyTrue(HTTPConstants.MC_GZIP_REQUEST)) {
            method.addHeader(HTTPConstants.HEADER_CONTENT_ENCODING,
                             HTTPConstants.COMPRESSION_GZIP);
        }

        if (msgContext.getProperty(HTTPConstants.HTTP_METHOD_PARAMS) != null) {
            HttpParams params = (HttpParams) msgContext
                    .getProperty(HTTPConstants.HTTP_METHOD_PARAMS);
            method.setParams(params);
        }

        String cookiePolicy = (String) msgContext.getProperty(HTTPConstants.COOKIE_POLICY);
        if (cookiePolicy != null) {
            method.getParams().setParameter(ClientPNames.COOKIE_POLICY, cookiePolicy);
        }

        sender.setTimeouts(msgContext, method);
        HttpContext localContext = new BasicHttpContext();
        // Why do we have add context here
        return httpClient.execute(httpHost, method, localContext);
    }

    private void handleResponse(HttpResponse response)
            throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        log.trace("Handling response - " + statusCode);
        if (statusCode == HttpStatus.SC_ACCEPTED) {
            msgContext.setProperty(HTTPConstants.CLEANUP_RESPONSE, Boolean.TRUE);
            /*
            * When an HTTP 202 Accepted code has been received, this will be
            * the case of an execution of an in-only operation. In such a
            * scenario, the HTTP response headers should be returned, i.e.
            * session cookies.
            */
            sender.obtainHTTPHeaderInformation(response, msgContext);

        } else if (statusCode >= 200 && statusCode < 300) {
            // We don't clean the response here because the response will be used afterwards
            msgContext.setProperty(HTTPConstants.CLEANUP_RESPONSE, Boolean.FALSE);
            sender.processResponse(response, msgContext);

        } else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
                   || statusCode == HttpStatus.SC_BAD_REQUEST) {
            msgContext.setProperty(HTTPConstants.CLEANUP_RESPONSE, Boolean.TRUE);
            Header contentTypeHeader = response.getFirstHeader(HTTPConstants.HEADER_CONTENT_TYPE);
            String value = null;
            if (contentTypeHeader != null) {
                value = contentTypeHeader.getValue();
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
                msgContext.setProperty(HTTPConstants.CLEANUP_RESPONSE, Boolean.FALSE);
                sender.processResponse(response, msgContext);
            }

            if (org.apache.axis2.util.Utils.isClientThreadNonBlockingPropertySet(msgContext)) {
                throw new AxisFault(Messages.
                        getMessage("transportError",
                                   String.valueOf(statusCode),
                                   response.getStatusLine().toString()));
            }
        } else {
            msgContext.setProperty(HTTPConstants.CLEANUP_RESPONSE, Boolean.TRUE);
            throw new AxisFault(Messages.getMessage("transportError", String.valueOf(statusCode),
                                                    response.getStatusLine().toString()));
        }
    }

    private void cleanup(HttpResponse response) {
        if (msgContext.isPropertyTrue(HTTPConstants.CLEANUP_RESPONSE)) {
            log.trace("Cleaning response : " + response);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    EntityUtils.consume(entity);
                } catch (IOException e) {
                    log.error("Error while cleaning response : " + response, e);
                }
            }
        }
    }
}
