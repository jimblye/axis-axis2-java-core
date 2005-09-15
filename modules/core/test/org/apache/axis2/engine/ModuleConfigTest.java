package org.apache.axis2.engine;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.ServiceBuilder;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ServiceDescription;
import org.apache.axis2.AxisFault;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
*
*
*/

/**
 * Author: Deepal Jayasinghe
 * Date: Sep 1, 2005
 * Time: 3:42:25 PM
 */
public class ModuleConfigTest extends TestCase {

    AxisConfiguration ar;
    String repo ="./test-resources/deployment/moduleConfig";



    public void testModuleConfigAtAxisConfig() {
        try {
            ConfigurationContextFactory builder = new ConfigurationContextFactory();
            ar = builder.buildConfigurationContext(repo).getAxisConfiguration();
            ModuleConfiguration moduleConfiguration =
                    ((AxisConfigurationImpl)ar).getModuleConfig(new QName("testModule"));
            assertNotNull(moduleConfiguration);
            Parameter para = moduleConfiguration.getParameter("testModulePara");
            assertNotNull(para);

            moduleConfiguration =
                    ((AxisConfigurationImpl)ar).getModuleConfig(new QName("testModule2"));
            assertNotNull(moduleConfiguration);
            para = moduleConfiguration.getParameter("testModulePara2");
            assertNotNull(para);
        } catch (DeploymentException e) {
            fail("This can not fail with this DeploymentException " + e) ;
        }
    }


     public void testModuleConfigAtService() {
        try {
            ConfigurationContextFactory builder = new ConfigurationContextFactory();
            ar = builder.buildConfigurationContext(repo).getAxisConfiguration();


            ServiceDescription service = new ServiceDescription();
            service.setName(new QName("testService"));
            ar.addService(service);
//            service.setParent(ar);
            InputStream in = new FileInputStream(repo + "/service1.xml");
            ServiceBuilder sbuilder = new ServiceBuilder(in,null,service);
            sbuilder.populateService(sbuilder.buildOM());

            ModuleConfiguration moduleConfiguration = service.getModuleConfig(new QName("Servie_module"));
            assertNotNull(moduleConfiguration);
            Parameter para = moduleConfiguration.getParameter("Servie_module_para");
            assertNotNull(para);

            OperationDescription op = service.getOperation("echoString");
            assertNotNull(op);

            moduleConfiguration = op.getModuleConfig(new QName("Op_Module"));
            assertNotNull(moduleConfiguration);
            para = moduleConfiguration.getParameter("Op_Module_para");
            assertNotNull(para);


        } catch (DeploymentException e) {
            fail("This can not fail with this DeploymentException " + e) ;
        } catch (FileNotFoundException e) {
             fail("This can not fail with this FileNotFoundException  " + e) ;
        } catch (AxisFault axisFault) {
            fail("This can not fail with this AxisFault  " + axisFault) ;
        } catch (XMLStreamException e) {
            fail("This can not fail with this AxisFault  " + e) ;
        }
     }
}
