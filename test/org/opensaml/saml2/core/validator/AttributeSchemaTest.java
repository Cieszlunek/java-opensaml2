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

package org.opensaml.saml2.core.validator;

import javax.xml.namespace.QName;

import org.opensaml.common.SAMLObjectValidatorBaseTestCase;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.validation.ValidationException;

/**
 * Test case for {@link org.opensaml.saml2.core.validator.AttributeSchemaValidator}.
 */
public class AttributeSchemaTest extends SAMLObjectValidatorBaseTestCase {

    /** Constructor */
    public AttributeSchemaTest() {
        targetQName = new QName(SAMLConstants.SAML20_NS, Attribute.LOCAL_NAME, SAMLConstants.SAML20_PREFIX);
        validator = new AttributeSchemaValidator();
    }

    /*
     * @see org.opensaml.common.SAMLObjectValidatorBaseTestCase#populateRequiredData()
     */
    protected void populateRequiredData() {
        super.populateRequiredData();
        Attribute attribute = (Attribute) target;
        attribute.setName("name");
    }

    /**
     * Tests absent Name failure.
     * 
     * @throws ValidationException
     */
    public void testNameFailure() throws ValidationException {
        Attribute attribute = (Attribute) target;

        attribute.setName(null);
        assertValidationFail("Name was null, should raise a Validation Exception");

        attribute.setName("");
        assertValidationFail("Name was empty string, should raise a Validation Exception");
        
        attribute.setName("    ");
        assertValidationFail("Name was white space, should raise a Validation Exception");
    }
}