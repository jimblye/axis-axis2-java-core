package org.apache.axis2.soap;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.soap.impl.llom.SOAPProcessingException;

/**
 * Copyright 2001-2004 The Apache Software Foundation.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * <p/>
 */
public interface SOAPFaultCode extends OMElement {
    /**
     * Eran Chinthaka (chinthaka@apache.org)
     */

    /**
     * @param value
     */
    public void setValue(SOAPFaultValue value) throws SOAPProcessingException;

    /**
     * @return
     */
    public SOAPFaultValue getValue();

    /**
     * @param value
     */
    public void setSubCode(SOAPFaultSubCode value) throws SOAPProcessingException;

    /**
     * @return
     */
    public SOAPFaultSubCode getSubCode();

}
