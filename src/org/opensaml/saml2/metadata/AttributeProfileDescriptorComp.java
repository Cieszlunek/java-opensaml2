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

package org.opensaml.saml2.metadata;

import java.net.URI;
import java.util.Collection;

import org.opensaml.common.util.UnmodifiableOrderedSet;

/**
 * A functional interface RoleDescriptors may use to deal with "AttributeProfile" elements.
 *
 */
public interface AttributeProfileDescriptorComp {

    /**
     * Checks if the given attribute profile is supported by this authority.
     * 
     * @param format the attribute profile
     * 
     * @return true if the given attribute profile is supported, false if not
     */
    public boolean isSupportedAttributeProfile(String profileURI);

    /**
     * Gets an immutable list of attribute profile URIs supported by this authority.
     * 
     * @return list of NameID format {@link URI}s
     */
    public UnmodifiableOrderedSet<String> getAttributeProfiles();

    /**
     * Adds an attribute profile URIs supported by this authority.
     * 
     * @param profile an attribute profile
     */
    public void addAttributeProfile(String profileURI);

    /**
     * Removes an attribute profile URIs supported by this authority.
     * 
     * @param profile an attribute profile
     */
    public void removeAttributeProfile(String profileURI);

    /**
     * Removes a list of attribute profile URIs supported by this authority.
     * 
     * @param profiles a list attribute profiles
     */
    public void removeAttributeProfiles(Collection<String> profileURIs);

    /**
     * Removes all the attribute profiles supported by this authority.
     */
    public void removeAllAttributeProfiles();

}