package org.apache.axis2.interopt.whitemesa.round2.util.soap12;

import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAPFactory;
import org.apache.axis2.soap.SOAPBody;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.interopt.whitemesa.round2.util.SunRound2ClientUtil;

/**
 * Created by IntelliJ IDEA.
 * User: Gayan
 * Date: Sep 5, 2005
 * Time: 8:09:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class GroupbSoap12EchoSimpleTypesAsStructUtil implements SunRound2ClientUtil {

    public SOAPEnvelope getEchoSoapEnvelope() {

        SOAPFactory omfactory = OMAbstractFactory.getSOAP12Factory();
        SOAPEnvelope reqEnv = omfactory.createSOAPEnvelope();
        //reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/envelope/", "soapenv");
        //reqEnv.declareNamespace("http://schemas.xmlsoap.org/wsdl/", "xmlns");
        //reqEnv.declareNamespace("http://schemas.xmlsoap.org/wsdl/soap/", "soap");
        reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema", "xsd");
        reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/encoding/", "SOAP-ENC");
        reqEnv.declareNamespace("http://soapinterop.org/", "tns");
        reqEnv.declareNamespace("http://soapinterop.org/xsd", "s");
        //reqEnv.declareNamespace("http://schemas.xmlsoap.org/wsdl/", "wsdl");
        reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema-instance","xsi");

        OMElement operation = omfactory.createOMElement("echoSimpleTypesAsStruct", "http://soapinterop.org/", null);
        SOAPBody body = omfactory.createSOAPBody(reqEnv);
        body.addChild(operation);
        operation.addAttribute("soapenv:encodingStyle", "http://www.w3.org/2003/05/soap-encoding", null);

        OMElement part0 = omfactory.createOMElement("inputString", "", null);
        part0.addAttribute("xsi:type", "xsd:string", null);
        part0.addChild(omfactory.createText("45ascasc  acasa asd52"));

        OMElement part1 = omfactory.createOMElement("inputInteger", "", null);
        part1.addAttribute("xsi:type", "xsd:int", null);
        part1.addChild(omfactory.createText("4552"));

        OMElement part2 = omfactory.createOMElement("inputFloat", "", null);
        part2.addAttribute("xsi:type", "xsd:float", null);
        part2.addChild(omfactory.createText("450.52"));

        operation.addChild(part0);
        operation.addChild(part1);
        operation.addChild(part2); //reqEnv.getBody().addChild(method);

        return reqEnv;
    }
}
