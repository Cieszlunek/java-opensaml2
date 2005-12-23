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

import javax.xml.datatype.DatatypeFactory;

import org.apache.log4j.Logger;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.io.Marshaller;
import org.opensaml.common.io.impl.AbstractMarshaller;
import org.opensaml.common.util.xml.XMLParserException;
import org.opensaml.saml2.common.CacheableSAMLObject;
import org.opensaml.saml2.common.TimeBoundSAMLObject;
import org.opensaml.saml2.common.impl.CacheableSAMLObjectHelper;
import org.opensaml.saml2.common.impl.TimeBoundSAMLObjectHelper;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.w3c.dom.Element;

/**
 * A thread safe {@link org.opensaml.common.io.Marshaller} for {@link org.opensaml.saml2.metadata.EntitiesDescriptor} objects.
 * 
 * Note, this only works with {@link org.opensaml.saml2.metadata.EntitiesDescriptor} implementations that extend {@link org.opensaml.saml2.common.impl.AbstractSAMLObject}.
 */
public class EntitiesDescriptorMarshaller extends AbstractMarshaller implements Marshaller {

    /**
     * Logger
     */
    private static Logger log = Logger.getLogger(EntitiesDescriptorMarshaller.class);
    
    /**
     * 
     * Constructor
     *
     * @throws XMLParserException thrown if this Marshaller is unable to create a {@link DatatypeFactory}
     */
    public EntitiesDescriptorMarshaller(){
        super(EntitiesDescriptor.QNAME);
    }
    
    /*
     * @see org.opensaml.common.io.impl.AbstractMarshaller#marshall(org.opensaml.common.SAMLElement, org.w3c.dom.Document)
     */
    public void marshallAttributes(SAMLObject samlElement, Element domElement){
        
        EntitiesDescriptor entitiesDescriptor = (EntitiesDescriptor)samlElement;
        
        // Set the validUntil attribute
        if(entitiesDescriptor.getValidUntil() != null){
            if(log.isDebugEnabled()){
                log.debug("Writting validUntil attribute to EntitiesDescriptor DOM element");
            }
            String validUntilStr = TimeBoundSAMLObjectHelper.calendarToString(entitiesDescriptor.getValidUntil());
            domElement.setAttribute(TimeBoundSAMLObject.VALID_UNTIL_ATTRIB_NAME, validUntilStr);
        }
        
        // Set the cacheDuration attribute
        if(entitiesDescriptor.getCacheDuration() != null){
            if(log.isDebugEnabled()){
                log.debug("Writting cacheDuration attribute to EntitiesDescriptor DOM element");
            }
            String cacheDuration = CacheableSAMLObjectHelper.longToDuration(entitiesDescriptor.getCacheDuration());
            domElement.setAttribute(CacheableSAMLObject.CACHE_DURATION_ATTRIB_NAME, cacheDuration);
        }
        
        // Set the Name attribute
        if(entitiesDescriptor.getName() != null){
            if(log.isDebugEnabled()){
                log.debug("Writting Name attribute to EntitiesDescriptor DOM element");
            }
            domElement.setAttribute(EntitiesDescriptor.NAME_ATTRIB_NAME, entitiesDescriptor.getName());
        }
    }
}