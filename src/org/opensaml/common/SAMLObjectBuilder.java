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
package org.opensaml.common;

/**
 * An interface for the creation of SAMLElements implementations. Builders are
 * unique to a particular SAML element and can be retrieved by element QName
 * from the {@link org.opensaml.common.SAMLObjectBuilderFactory}.  Implementations 
 * of this interface MUST be stateless.
 */
public interface SAMLObjectBuilder {

	/**
	 * Creates a SAMLElement.
	 * 
	 * @return the SAMLElement
	 */
	public SAMLObject buildObject();
}