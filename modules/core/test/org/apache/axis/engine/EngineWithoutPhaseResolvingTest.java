/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis.engine;

import org.apache.axis.addressing.AddressingConstants;
import org.apache.axis.addressing.EndpointReference;
import org.apache.axis.context.ConfigurationContext;
import org.apache.axis.context.MessageContext;
import org.apache.axis.context.OperationContextFactory;
import org.apache.axis.context.ServiceContext;
import org.apache.axis.description.OperationDescription;
import org.apache.axis.description.ServiceDescription;
import org.apache.axis.description.TransportInDescription;
import org.apache.axis.description.TransportOutDescription;
import org.apache.axis.om.OMAbstractFactory;
import org.apache.axis.soap.SOAPFactory;
import org.apache.axis.transport.http.HTTPTransportSender;
import org.apache.wsdl.WSDLConstants;
import org.apache.wsdl.WSDLService;

import javax.xml.namespace.QName;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class EngineWithoutPhaseResolvingTest extends AbstractEngineTest {
    private MessageContext mc;
    private ArrayList executedHandlers = new ArrayList();
    private AxisConfiguration engineRegistry;
    private QName serviceName = new QName("axis/services/NullService");
    private QName opearationName = new QName("NullOperation");
    private ServiceDescription service;
    private ConfigurationContext engineContext;
    private OperationDescription axisOp;

    public EngineWithoutPhaseResolvingTest() {
    }

    public EngineWithoutPhaseResolvingTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {

        engineRegistry = new AxisSystemImpl();
        engineContext = new ConfigurationContext(engineRegistry);

        TransportOutDescription transport = new TransportOutDescription(new QName("null"));
        transport.setSender(new HTTPTransportSender());

        TransportInDescription transportIn = new TransportInDescription(new QName("null"));
        axisOp = new OperationDescription(opearationName);

        service = new ServiceDescription(serviceName);
        axisOp.setMessageReciever(new MessageReceiver() {
            public void recieve(MessageContext messgeCtx) throws AxisFault {
                // TODO Auto-generated method stub

            }
        });
        engineRegistry.addService(service);
        service.setStyle(WSDLService.STYLE_DOC);
        service.addOperation(axisOp);

        ServiceContext serviceContext = engineContext.createServiceContext(serviceName);

        mc =
                new MessageContext(engineContext,
                        null,
                        transportIn,
                        transport,
                        OperationContextFactory.createMEPContext(WSDLConstants.MEP_CONSTANT_IN_OUT, false, axisOp, serviceContext));
        mc.setTransportOut(transport);
        mc.setProperty(MessageContext.TRANSPORT_WRITER, new OutputStreamWriter(System.out));
        mc.setServerSide(true);
        SOAPFactory omFac = OMAbstractFactory.getSOAP11Factory();
        mc.setEnvelope(omFac.getDefaultEnvelope());


        mc.setWSAAction(opearationName.getLocalPart());
        System.out.flush();
    }

//    public void testServerSend() throws Exception {
//        AxisEngine engine = new AxisEngine(engineContext);
//        mc.setServerSide(true);
//        
//        mc.setServiceContext(engineContext.createServiceContext(serviceName));
//        engine.send(mc);
//    }
//
//    public void testClientSend() throws Exception {
//        AxisEngine engine = new AxisEngine(engineContext);
//        mc.setServerSide(false); 
//        mc.setServiceContext(engineContext.createServiceContext(serviceName));
//        engine.send(mc);
//    }

    public void testServerReceive() throws Exception {
        mc.setTo(new EndpointReference(AddressingConstants.WSA_TO, "axis/services/NullService"));
        AxisEngine engine = new AxisEngine(engineContext);
        mc.setServerSide(true);
        engine.receive(mc);
    }
//
//    public void testClientReceive() throws Exception {
//        mc.setServiceContext(engineContext.createServiceContext(serviceName));
//        mc.setOperationContext(axisOp.findOperationContext(mc,mc.getServiceContext(),false));
//        AxisEngine engine = new AxisEngine(engineContext);
//        mc.setServerSide(false);
//        engine.receive(mc);
//    }
}
