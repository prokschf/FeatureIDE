/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.antenna;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.prop4j.And;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.Not;

import antenna.preprocessor.v3.PPException;
import antenna.preprocessor.v3.Preprocessor;
import de.ovgu.featureide.antenna.model.AntennaModelBuilder;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.preprocessor.PPComposerExtensionClass;
import de.ovgu.featureide.fm.core.editing.NodeCreator;

/**
 * Antenna: a purposely-simple Java preprocessor.
 * 
 * @author Christoph Giesel
 * @author Marcus Kamieth
 */
public class AntennaPreprocessor extends PPComposerExtensionClass {

	/** antenna preprocessor used from external library */
	private Preprocessor preprocessor;

	private AntennaModelBuilder antennaModelBuilder;
	
	/** pattern for replacing preprocessor commands like "//#if" */
	static final Pattern replaceCommandPattern = Pattern.compile("//#(.+?)\\s");
	
	public AntennaPreprocessor() {
		super();
		
		pluginName = "Antenna";
	}

	@Override
	public void initialize(IFeatureProject project) {
		super.initialize(project);
		antennaModelBuilder = new AntennaModelBuilder(project);
		preprocessor = new Preprocessor(new AntennaLogger(),
				new AntennaLineFilter());

		if (project.getProjectSourcePath() == null
				|| project.getProjectSourcePath().equals("")) {
			project.setPaths(project.getBuildPath(), project.getBuildPath(),
					project.getConfigPath());
		}
	}

	@Override
	public ArrayList<String> extensions() {
		ArrayList<String> extensions = new ArrayList<String>();
		extensions.add(".java");
		return extensions;
	}

	@Override
	public void performFullBuild(IFile config) {
		if (!prepareFullBuild(config))
			return;

		// generate comma separated string of activated features
		StringBuilder featureList = new StringBuilder();
		for (String feature : activatedFeatures) {
			featureList.append(feature + ",");
		}
		
		featureList.deleteCharAt(featureList.length()-1);

		// add source files
		try {
			// add activated features as definitions to preprocessor
			preprocessor.addDefines(featureList.toString());

			// preprocess for all files in source folder
			preprocessSourceFiles(featureProject.getBuildFolder());
		} catch (Exception e) {
			AntennaCorePlugin.getDefault().logError(e);
		}

		if (antennaModelBuilder != null)
			antennaModelBuilder.buildModel();
	}

	/*
	 * buildFile is not set to derived
	 * 
	 * @see
	 * de.ovgu.featureide.core.builder.ComposerExtensionClass#postCompile(org
	 * .eclipse.core.resources.IResourceDelta, org.eclipse.core.resources.IFile)
	 */
	@Override
	public void postCompile(IResourceDelta delta, IFile buildFile) {
	}

	/**
	 * preprocess all files in folder
	 * 
	 * @param sourceFolder folder with files to preprocess
	 * @throws CoreException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void preprocessSourceFiles(IFolder sourceFolder) throws CoreException,
			FileNotFoundException, IOException {
		
		for (final IResource res : sourceFolder.members()) {
			if (res instanceof IFolder) {
				// for folders do recursively 
				preprocessSourceFiles((IFolder) res);
			} else if (res instanceof IFile) {
				// delete all existing builder markers 
				featureProject.deleteBuilderMarkers(res, 0);
				
				// get all lines from file
				final Vector<String> lines = loadStringsFromFile((IFile) res);
				
				// do checking and some stuff
				Job job = new Job("preprocessor annotation checking") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						processLinesOfFile(lines, (IFile) res);
						
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.SHORT);
				job.schedule();
				

				boolean changed = false;

				try {
					// run antenna preprocessor
					changed = preprocessor.preprocess(lines,
							((IFile) res).getCharset());
				} catch (PPException e) {
					featureProject.createBuilderMarker(
							res,
							e.getMessage().replace(
									"Line #" + e.getLineNumber() + " :",
									"Antenna:"), e.getLineNumber() + 1,
							IMarker.SEVERITY_ERROR);
					AntennaCorePlugin.getDefault().logError(e);
				}

				// if preprocessor changed file: save & refresh
				if (changed) {
					FileOutputStream ostr = new FileOutputStream(res
							.getRawLocation().toOSString());
					Preprocessor.saveStrings(lines, ostr,
							((IFile) res).getCharset());
					ostr.close();

					// use touch to support e.g. linux
					res.touch(null);
					res.refreshLocal(IResource.DEPTH_ZERO, null);
				}
			}
		}
	}
	
	/**
	 * Do checking for all lines of file.
	 * 
	 * @param lines all lines of file
	 * @param res file
	 */
	private void processLinesOfFile(Vector<String> lines, IFile res){
		expressionStack = new Stack<Node>();
		
		// count of if, ifelse and else to remove after processing of else from stack
		ifelseCountStack = new Stack<Integer>();
		ifelseCountStack.push(0);
		
		// go line for line
		for (int j = 0; j < lines.size(); ++j) {
			String line = lines.get(j);
			
			// if line is preprocessor directive
			if (line.contains("//#")) {
				if (line.contains("//#if ") ||
					line.contains("//#elif ") ||
					line.contains("//#condition ") ||
					line.contains("//#ifdef ") ||
					line.contains("//#ifndef ") ||
					line.contains("//#elifdef ") ||
					line.contains("//#elifndef ") ||
					line.contains("//#else")) {
					
					// if e1, elseif e2, ..., elseif en  ==  if -e1 && -e2 && ... && en
					// if e1, elseif e2, ..., else  ==  if -e1 && -e2 && ...
					if (line.contains("//#elif ") || line.contains("//#elifdef ") || line.contains("//#elifndef ") || line.contains("//#else")) {
						if(!expressionStack.isEmpty()) {
							Node lastElement = new Not(expressionStack.pop());
							expressionStack.push(lastElement);
						}
					} else if (line.contains("//#if ") || line.contains("//#ifdef ") || line.contains("//#ifndef ")) {
						ifelseCountStack.push(0);
					}
					
					ifelseCountStack.push(ifelseCountStack.pop() + 1);
					
					setMarkersContradictionalFeatures(line, res, j+1);
					
					setMarkersNotConcreteFeatures(line, res, j+1);
				} else if (line.contains("//#endif")) {
					for (; ifelseCountStack.peek() > 0; ifelseCountStack.push(ifelseCountStack.pop() - 1)) {
						if (!expressionStack.isEmpty())
							expressionStack.pop();
					}
					
					ifelseCountStack.pop();
				}
			}
		}
	}
	
	/**
	 * Checks given line if it contains expressions which are always 
	 * <code>true</code> or <code>false</code>.<br /><br />
	 * 
	 * Check in three steps:
	 * <ol>
	 * <li>just the given line</li>
	 * <li>the given line and the feature model</li>
	 * <li>the given line, the surrounding lines and the feature model</li>
	 * </ol>
	 * 
	 * @param line content of line
	 * @param res file containing given line
	 * @param lineNumber line number of given line
	 */
	private void setMarkersContradictionalFeatures(String line, IFile res, int lineNumber){
		if (line.contains("//#else")) {
			if (!expressionStack.isEmpty()) {
				Node[] nestedExpressions = new Node[expressionStack.size()];
				nestedExpressions = expressionStack.toArray(nestedExpressions);
				
				And nestedExpressionsAnd = new And(nestedExpressions);
				
				isContradictionOrTautology(nestedExpressionsAnd.clone(), true, lineNumber, res);
			}
			
			return;
		}
		
		boolean conditionIsSet = line.contains("//#condition ");
		boolean negative = line.contains("//#ifndef ") || line.contains("//#elifndef ");
		
		// remove "//#if ", "//ifdef", ...
		line = replaceCommandPattern.matcher(line).replaceAll("");
		
		// prepare expression for NodeReader()
		line = line.trim();
		line = line.replace("&&", "&");
		line = line.replace("||", "|");
		line = line.replace("!", "-");
		line = line.replace("&", " and ");
		line = line.replace("|", " or ");
		line = line.replace("-", " not ");
		
		//get all features and generate Node expression for given line
		Node ppExpression = nodereader.stringToNode(line, featureList);
		
		if (ppExpression != null) {
			if (negative)
				ppExpression = new Not(ppExpression.clone());
			
			if (!conditionIsSet)
				expressionStack.push(ppExpression);
			
			doThreeStepExpressionCheck(ppExpression, lineNumber, res);
		} else {
			// if generating of expression failed, generate expression "true"
			if (!conditionIsSet)
				expressionStack.push(new Literal(NodeCreator.varTrue));
		}
		
	}
	
	/**
	 * Checks given line if it contains not existing or abstract features.
	 * 
	 * @param line content of line
	 * @param res file containing given line
	 * @param lineNumber line number of given line
	 */
	private void setMarkersNotConcreteFeatures(String line, IFile res, int lineNumber) {
		String[] splitted = line.split(AntennaModelBuilder.OPERATORS, 0);
		
		for (int i = 0; i < splitted.length; ++i) {
			if (!splitted[i].equals("") && !splitted[i].contains("//#")) {
				Matcher m = patternIsConcreteFeature.matcher(splitted[i]);
				
				if (!m.matches()) {
					featureProject.createBuilderMarker(res,
							"Antenna: " + splitted[i] + " is not a concrete feature", lineNumber,
							IMarker.SEVERITY_WARNING);
				}
			}
		}
	}

	@Override
	public ArrayList<String[]> getTemplates() {
		ArrayList<String[]> list = new ArrayList<String[]>();
		String[] java = { "Java", "java", "public class #classname# {\n\n}" };
		list.add(java);
		return list;
	}

	@Override
	public boolean clean() {
		return false;
	}

	@Override
	public boolean hasFeatureFolders() {
		return false;
	}

	@Override
	public boolean hasFeatureFolder() {
		return false;
	}

	@Override
	public boolean copyNotComposedFiles() {
		return true;
	}

	@Override
	public void buildFSTModel() {
		antennaModelBuilder.buildModel();
	}
}