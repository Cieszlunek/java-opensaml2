/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.]
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
 */

package org.opensaml.saml2.metadata.impl;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.io.Marshaller;
import org.opensaml.common.io.MarshallingException;
import org.opensaml.common.io.impl.AbstractMarshaller;
import org.opensaml.saml2.common.CacheableSAMLObject;
import org.opensaml.saml2.common.TimeBoundSAMLObject;
import org.opensaml.saml2.common.impl.TimeBoundSAMLObjectHelper;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.w3c.dom.Element;

/**
 * A thread safe {@link org.opensaml.common.io.Marshaller} for {@link org.opensaml.saml2.metadata.RoleDescriptor} objects.
 * 
 * Note, this only works with {@link org.opensaml.saml2.metadata.RoleDescriptor} implementations that extend {@link org.opensaml.saml2.common.impl.AbstractSAMLObject}.
 */
public abstract class RoleDescriptorMarshaller extends AbstractMarshaller implements Marshaller {

    /**
     * Logger
     */
    private static Logger log = Logger.getLogger(EntityDescriptorMarshaller.class);
    
    /**
     * Constructor
     * 
     * @param target the QName of the elment or type this marshaller operates on
     */
    protected RoleDescriptorMarshaller(QName target) {
        super(target);
    }
    
    /*
     * @see org.opensaml.common.io.impl.AbstractMarshaller#marshallAttributes(org.opensaml.common.SAMLObject, org.w3c.dom.Element)
     */
    protected void marshallAttributes(SAMLObject samlElement, Element domElement) throws MarshallingException  {
        RoleDescriptor roleDescriptor = (RoleDescriptor)samlElement;
        
        // Set the validUntil attribute
        if(roleDescriptor.getValidUntil() != null){
            if(log.isDebugEnabled()){
                log.debug("Writting validUntil attribute to RoleDescriptor DOM element");
            }
            String validUntilStr = TimeBoundSAMLObjectHelper.calendarToString(roleDescriptor.getValidUntil());
            domElement.setAttribute(TimeBoundSAMLObject.VALID_UNTIL_ATTRIB_NAME, validUntilStr);
        }
        
        // Set the cacheDuration attribute
        if(roleDescriptor.getCacheDuration() != null){
            if(log.isDebugEnabled()){
                log.debug("Writting cacheDuration attribute to RoleDescriptor DOM element");
            }
            String cacheDuration = roleDescriptor.getCacheDuration().toString();
            domElement.setAttribute(CacheableSAMLObject.CACHE_DURATION_ATTRIB_NAME, cacheDuration);
        }
        
        // Set the protocolSupportEnumeration attribute
        Set<String> supportedProtocols = roleDescriptor.getSupportedProtocols();
        if(supportedProtocols != null && supportedProtocols.size() > 0) {
            if(log.isDebugEnabled()){
                log.debug("Writting protocolSupportEnumberation attribute to RoleDescriptor DOM element");
            }
            
            StringBuilder builder = new StringBuilder();
            for(String protocol : supportedProtocols) {
                builder.append(protocol);
                builder.append(" ");
            }
            
            domElement.setAttribute(RoleDescriptor.PROTOCOL_ENUMERATION_ATTRIB_NAME, builder.toString());
        }
        
        // Set errorURL attribute
        if(roleDescriptor.getErrorURL() != null) {
            if(log.isDebugEnabled()){
                log.debug("Writting errorURL attribute to RoleDescriptor DOM element");
            }
            domElement.setAttribute(RoleDescriptor.ERROR_URL_ATTRIB_NAME, roleDescriptor.getErrorURL());
        }
    }
}