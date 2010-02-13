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

package test.interop.whitemesa.round4.simple.utils;

import org.apache.ws.commons.om.OMAbstractFactory;
import org.apache.ws.commons.om.OMElement;
import org.apache.ws.commons.om.OMNamespace;
import org.apache.ws.commons.soap.SOAPEnvelope;
import org.apache.ws.commons.soap.SOAPFactory;
import test.interop.whitemesa.SunClientUtil;

public class EchoIntArrayFaultClientUtil implements SunClientUtil {
    public SOAPEnvelope getEchoSoapEnvelope() {

        SOAPFactory omfactory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope reqEnv = omfactory.getDefaultEnvelope();

        OMNamespace omNs = omfactory.createOMNamespace("http://soapinterop.org/wsdl", "m");
        OMNamespace envNs = reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV");
        OMNamespace encNs = reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/encoding/", "SOAP-ENC");
        OMNamespace typeNs = reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema", "m0");

        OMElement operation = omfactory.createOMElement("echoIntArrayFault", omNs);
        operation.declareNamespace(envNs);
        reqEnv.getBody().addChild(operation);
        operation.addAttribute("encodingStyle", "http://schemas.xmlsoap.org/soap/encoding/", envNs);

        OMElement part = omfactory.createOMElement("param", null);
        part.declareNamespace(encNs);
        part.declareNamespace(typeNs);
        part.addAttribute("arrayType", "m0:int[3]", encNs);
        part.addAttribute("type", "SOAP-ENC:Array", typeNs);
        OMElement item0 = omfactory.createOMElement("item0", null);
        item0.declareNamespace(typeNs);
        item0.addAttribute("type", "m0:int", typeNs);
        item0.addChild(omfactory.createText("451"));
        OMElement item1 = omfactory.createOMElement("item1", null);
        item1.declareNamespace(typeNs);
        item1.addAttribute("type", "m0:int", typeNs);
        item1.addChild(omfactory.createText("425"));
        OMElement item2 = omfactory.createOMElement("item2", null);
        item2.declareNamespace(typeNs);
        item2.addAttribute("type", "m0:int", typeNs);
        item2.addChild(omfactory.createText("2523"));

        part.addChild(item0);
        part.addChild(item1);
        part.addChild(item2);

        operation.addChild(part);
        return reqEnv;
    }
}