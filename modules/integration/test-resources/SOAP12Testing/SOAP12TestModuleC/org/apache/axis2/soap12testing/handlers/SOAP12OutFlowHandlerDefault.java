package org.apache.axis2.soap12testing.handlers;

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
 * Author : Deepal Jayasinghe
 * Date: Jul 26, 2005
 * Time: 2:56:42 PM
 */

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.soap.SOAPHeaderBlock;
import org.apache.axis2.soap.SOAPHeader;
import org.apache.axis2.handlers.AbstractHandler;

import java.util.Iterator;

public class SOAP12OutFlowHandlerDefault extends AbstractHandler {


    public void revoke(MessageContext msgContext) {

    }

    public void invoke(MessageContext msgContext) {
        Integer headerBlockPresent = (Integer) msgContext.getOperationContext().getProperty("HEADER_BLOCK_PRESENT", true);
        if (headerBlockPresent.equals(new Integer(1))) {
            SOAPHeader headerAdd = (SOAPHeader) msgContext.getOperationContext().getProperty("HEADER_BLOCK", true);
           Iterator headerBlocks = headerAdd.examineAllHeaderBlocks();
            while(headerBlocks.hasNext()){
                SOAPHeaderBlock headerBlock=(SOAPHeaderBlock) headerBlocks.next();
                msgContext.getEnvelope().getHeader().addChild(headerBlock);
            }
        } else {
            msgContext.getEnvelope().getHeader().discard();
        }
    }
}
