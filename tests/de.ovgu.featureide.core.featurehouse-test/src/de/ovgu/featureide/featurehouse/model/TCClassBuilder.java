/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2013  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.featurehouse.model;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;

import org.junit.Test;

/**
 * Test for {@link FeatureHouseModelBuilder} of <code>FeatureHouse</code> C files.
 * 
 * @author Jens Meinicke
 */
public class TCClassBuilder {
	private CClassBuilder builder = new CClassBuilder(null);
	
	// METHOD TEST 1
	private String TEST_METHOD_1 = "int\nfindUserPublicKeyPair (void *listdata, void *searchdata){\n}"; 
	private String EXPECTED_NAME_METHOD_1 = "findUserPublicKeyPair";
	private String EXPECTED_RETURNTYPE_METHOD_1 = "int";
	private String EXPECTED_MODIFIER_METHOD_1 = ""; 
	private String EXPECTED_PARAMETER_1_METHOD_1 = "void *listdata";
	private String EXPECTED_PARAMETER_2_METHOD_1 = "void *searchdata";
	
	@Test
	public void MethodTest1() {
		LinkedList<String> method = builder.getMethod(TEST_METHOD_1);
		assertEquals(EXPECTED_NAME_METHOD_1, method.get(0));
		assertEquals(EXPECTED_RETURNTYPE_METHOD_1, method.get(1));
		assertEquals(EXPECTED_MODIFIER_METHOD_1, method.get(2));
		assertEquals(EXPECTED_PARAMETER_1_METHOD_1, method.get(3));
		assertEquals(EXPECTED_PARAMETER_2_METHOD_1, method.get(4));
	}
	
	// METHOD TEST 2
	private String TEST_METHOD_2 = "unsigned int uni_int(unsigned int n){}"; 
	private String EXPECTED_NAME_METHOD_2 = "uni_int";
	private String EXPECTED_RETURNTYPE_METHOD_2 = "unsigned int";
	private String EXPECTED_MODIFIER_METHOD_2 = ""; 
	private String EXPECTED_PARAMETER_1_METHOD_2 = "unsigned int n";
	
	@Test
	public void MethodTest2() {
		LinkedList<String> method = builder.getMethod(TEST_METHOD_2);
		assertEquals(EXPECTED_NAME_METHOD_2, method.get(0));
		assertEquals(EXPECTED_RETURNTYPE_METHOD_2, method.get(1));
		assertEquals(EXPECTED_MODIFIER_METHOD_2, method.get(2));
		assertEquals(EXPECTED_PARAMETER_1_METHOD_2, method.get(3));
	}

	// FIELD TEST 1
	private String TEST_FIELD_1 = "static long tps;"; 
	private String EXPECTED_MODIFIER_FIELD_1 = "static"; 
	private String EXPECTED_TYPE_FIELD_1 = "long";
	private String EXPECTED_NAME_FIELD_1 = "tps";
	
	@Test
	public void FieldTest1() {
		LinkedList<String> fields = builder.getFields(TEST_FIELD_1);
		assertEquals(EXPECTED_MODIFIER_FIELD_1, fields.get(0));
		assertEquals(EXPECTED_TYPE_FIELD_1, fields.get(1));
		assertEquals(EXPECTED_NAME_FIELD_1, fields.get(2));
	}
	
	// FIELD TEST 2
	private String TEST_FIELD_2 = "static int a, b , c ;"; 
	private String EXPECTED_MODIFIER_FIELD_2 = "static"; 
	private String EXPECTED_TYPE_FIELD_2 = "int";
	private String EXPECTED_NAME_FIELD_2_1 = "a";
	private String EXPECTED_NAME_FIELD_2_2 = "b";
	private String EXPECTED_NAME_FIELD_2_3 = "c";
	
	@Test
	public void FieldTest2() {
		LinkedList<String> fields = builder.getFields(TEST_FIELD_2);
		assertEquals(EXPECTED_MODIFIER_FIELD_2, fields.get(0));
		assertEquals(EXPECTED_TYPE_FIELD_2, fields.get(1));
		assertEquals(EXPECTED_NAME_FIELD_2_1, fields.get(2));
		assertEquals(EXPECTED_NAME_FIELD_2_2, fields.get(3));
		assertEquals(EXPECTED_NAME_FIELD_2_3, fields.get(4));
	}
}
