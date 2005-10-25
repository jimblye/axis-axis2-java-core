package org.apache.axis2.databinding.schema.populate;

import junit.framework.TestCase;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;


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

public class PopulateAnonComplexTypeTest extends TestCase {
    private String xmlString = "<tempElt>" +
            "<varInt>5</varInt>" +
            "<varString>Hello</varString>" +
            "<varFloat>3.3</varFloat>" +
            "</tempElt>";

    public void testPopulate() throws Exception{

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(xmlString.getBytes()));
        Class clazz = Class.forName("org1.soapinterop.tempElt");
        Method parseMethod = clazz.getMethod("parse",new Class[]{XMLStreamReader.class});
        Object obj = parseMethod.invoke(null,new Object[]{reader});


        BeanInfo beanInfo =  Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        Method readMethod;
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
            if ("varInt".equals(propertyDescriptor.getDisplayName())){
                readMethod = propertyDescriptor.getReadMethod();
                assertEquals(new Integer(5),readMethod.invoke(obj,null));
            } else if ("varFloat".equals(propertyDescriptor.getDisplayName())){
                readMethod = propertyDescriptor.getReadMethod();
                assertEquals(new Float(3.3),readMethod.invoke(obj,null));
            }  else if ("varString".equals(propertyDescriptor.getDisplayName())){
                readMethod = propertyDescriptor.getReadMethod();
                assertEquals("Hello",readMethod.invoke(obj,null));
            }

        }

    }



}

