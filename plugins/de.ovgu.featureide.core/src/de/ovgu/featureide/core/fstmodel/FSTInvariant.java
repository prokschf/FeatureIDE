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
package de.ovgu.featureide.core.fstmodel;

import java.util.LinkedList;

/**
 * description
 * 
 * @author Stefan Krueger
 * @author Florian Proksch
 */
public class FSTInvariant extends RoleElement {

	public enum RoleTypes {
		ROLE_TYPE_INVARIANT, MISC
	}

	RoleTypes parentRoleType;
	private LinkedList<String> parameterTypes;
	boolean hasProperIdentifier;
	boolean isAsmetalInvariant;
	
	/**
	 * @return the parentRoleType
	 */
	/*
	 * public RoleTypes getParentRoleType() { return parentRoleType; }
	 */

	/**
	 * @param name
	 * @param type
	 * @param modifiers
	 */
	public FSTInvariant(String name, String body) {
		super(name, "", "", body, -1, -1);
		hasProperIdentifier = false;
		isAsmetalInvariant = false;
	}

	/**
	 * @param name
	 * @param type
	 * @param modifiers
	 * @param body
	 * @param beginLine
	 * @param endLine
	 */

	public FSTInvariant(String name, String body, LinkedList<String> _parameterTypes, int beginLine, int endLine, boolean _hasProperIdentifier, boolean _isAsmetalInvaraint) 
	{
		super(name, "", "", body, beginLine, endLine);
		hasProperIdentifier = _hasProperIdentifier;
		isAsmetalInvariant = _isAsmetalInvaraint;
		parameterTypes = _parameterTypes;
	}

	public FSTInvariant(String name, String body, int beginLine, int endLine) {
		super(name, "", "", body, beginLine, endLine);
		hasProperIdentifier = false;
		isAsmetalInvariant = false;		
	}

	@Deprecated
	public int getUniqueIdentifier() {
			return (body + beginLine + getFile()).hashCode();
	}

	@Override
	public String getFullName() 
	{
		
		if (isAsmetalInvariant)
		{
			StringBuilder fullname = new StringBuilder();
			fullname.append(hasProperIdentifier ? name : "[line " + beginLine + "]");
			fullname.append(" over ");
			for (int i = 0; i < parameterTypes.size(); i++) {
				if (i > 0)
					fullname.append(", ");
				fullname.append(parameterTypes.get(i));
			}
			return fullname.toString();
			
		}else
		{
			//JML Invariant
			String name = body.replaceAll("  ", "").replace((char) 10, ' ').replaceFirst("invariant ", "");
			return ((name.length() > 25 ? name.substring(0, 25) + "..." : name));
		}
	}

	public boolean inRefinementGroup() {
		for (FSTRole role : getRole().getFSTClass().getRoles()) {
			if (role.getFeature().equals(getRole().getFeature())) {
				continue;
			}
			for (FSTInvariant invariant : role.getClassFragment().getInvariants()) {
				if (invariant.getName().equals(getName())) {
					return true;
				}
			}
		}
		return false;
	}

}
