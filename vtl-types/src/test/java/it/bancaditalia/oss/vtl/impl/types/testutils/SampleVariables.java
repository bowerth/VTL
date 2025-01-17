/*
 * Copyright © 2020 Banca D'Italia
 *
 * Licensed under the EUPL, Version 1.2 (the "License");
 * You may not use this work except in compliance with the
 * License.
 * You may obtain a copy of the License at:
 *
 * https://joinup.ec.europa.eu/sites/default/files/custom-page/attachment/2020-03/EUPL-1.2%20EN.txt
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 *
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */
package it.bancaditalia.oss.vtl.impl.types.testutils;

import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.BOOLEANDS;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.INTEGERDS;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.NUMBERDS;
import static it.bancaditalia.oss.vtl.impl.types.domain.Domains.STRINGDS;

import it.bancaditalia.oss.vtl.impl.types.dataset.DataStructureComponentImpl;
import it.bancaditalia.oss.vtl.model.data.ComponentRole;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Attribute;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Identifier;
import it.bancaditalia.oss.vtl.model.data.ComponentRole.Measure;
import it.bancaditalia.oss.vtl.model.data.DataStructureComponent;

public enum SampleVariables
{
	MEASURE_INTEGER_1, MEASURE_INTEGER_2, MEASURE_INTEGER_3, 
	MEASURE_NUMBER_1, MEASURE_NUMBER_2, MEASURE_NUMBER_3, MEASURE_NUMBER_4,
	MEASURE_STRING_1, MEASURE_STRING_2, MEASURE_STRING_3, MEASURE_STRING_4, 
	MEASURE_BOOLEAN_1, MEASURE_BOOLEAN_2, MEASURE_BOOLEAN_3, MEASURE_BOOLEAN_4, 
	IDENT_INTEGER_1, IDENT_INTEGER_2, IDENT_INTEGER_3, 
	IDENT_NUMBER_1, IDENT_NUMBER_2, IDENT_NUMBER_3, IDENT_NUMBER_4, 
	IDENT_STRING_1, IDENT_STRING_2, IDENT_STRING_3, IDENT_STRING_4, 
	IDENT_BOOLEAN_1, IDENT_BOOLEAN_2, IDENT_BOOLEAN_3, IDENT_BOOLEAN_4, 
	ATTRIB_INTEGER_1, ATTRIB_INTEGER_2, ATTRIB_INTEGER_3, 
	ATTRIB_NUMBER_1, ATTRIB_NUMBER_2, ATTRIB_NUMBER_3, ATTRIB_NUMBER_4, 
	ATTRIB_STRING_1, ATTRIB_STRING_2, ATTRIB_STRING_3, ATTRIB_STRING_4, 
	ATTRIB_BOOLEAN_1, ATTRIB_BOOLEAN_2, ATTRIB_BOOLEAN_3, ATTRIB_BOOLEAN_4;

	private final DataStructureComponent<?, ?, ?> component;
	
	private SampleVariables()
	{
		String elem[] = name().split("_");
		Class<? extends ComponentRole> role = null;
		switch (elem[0])
		{
			case "MEASURE": role = Measure.class; break;
			case "ATTRIB": role = Attribute.class; break;
			case "IDENT": role = Identifier.class; break;
		}
		
		switch (elem[1])
		{
			case "NUMBER": component = new DataStructureComponentImpl<>(name().split("_", 2)[1], role, NUMBERDS); break;
			case "INTEGER": component = new DataStructureComponentImpl<>(name().split("_", 2)[1], role, INTEGERDS); break;
			case "STRING": component = new DataStructureComponentImpl<>(name().split("_", 2)[1], role, STRINGDS); break;
			case "BOOLEAN": component = new DataStructureComponentImpl<>(name().split("_", 2)[1], role, BOOLEANDS); break;
			default: throw new UnsupportedOperationException("Unsupported domain in unit test");
		}
	}
	
	public DataStructureComponent<?, ?, ?> getComponent()
	{
		return component;
	}
	
	public String getType()
	{
		return toString().split("_")[1];
	}
	
	public int getIndex()
	{
		return Integer.parseInt(toString().split("_")[2]);
	}
}
