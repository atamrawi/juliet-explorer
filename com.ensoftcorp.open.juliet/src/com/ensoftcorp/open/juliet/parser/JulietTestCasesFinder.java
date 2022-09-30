package com.ensoftcorp.open.juliet.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;

public class JulietTestCasesFinder {
	
	/**
	 * The actual directory where original Juliet test cases are.
	 */
	private static final String JULIET_TEST_CASES_DIRECTORY = "/Users/atamrawi/Desktop/juliet-test-cases/2022-08-11-juliet-c-cplusplus-v1-3-1-with-extra-support/";

	/**
	 * The actual JSON file used for parsing the Juliet test cases.
	 */
	private static final String JULIET_TEST_CASES_JSON_FILE_PATH = JULIET_TEST_CASES_DIRECTORY + "sarifs.json";
	
	/**
	 * The target directory where we will write the specific CWE Juliet test cases to with the build script.
	 */
	private static final String OUTPUT_DIRECTORY_PATH = "/Users/atamrawi/git/test-cases-atlas-project/com.ensoftcorp.open.juliet.testcases/src/";
	
	/**
	 * The root directory for source packages for each Juliet test case.
	 */
	private static final String SOURCE_PACKAGE_ROOT_DIRECTORY_NAME= "src/";
	
	/**
	 * The source directory containing the source code for a Juliet test case.
	 */
	private static final String TEST_CASE_SOURCE_DIRECTORY_PATH = SOURCE_PACKAGE_ROOT_DIRECTORY_NAME + "testcases";
	
	/**
	 * The directory for header/source files supporting each source file in {@link #TEST_CASE_SOURCE_DIRECTORY_PATH}.
	 */
	private static final String TEST_CASE_SUPPORT_SOURCE_DIRECTORY_PATH = SOURCE_PACKAGE_ROOT_DIRECTORY_NAME + "testcasesupport";
	
	/**
	 * The compilation command String formatter.
	 */
	private static final String COMPILE_COMMAND_STRING_FORMATTER = "echo \"gcc -I %s -c %s\"";
	
	public static void main(String[] args) {
		File jsonFile = new File(JULIET_TEST_CASES_JSON_FILE_PATH);
		List<JulietTestCase> julietTestCases = JulietTestSuiteParser.parse(jsonFile);
		
		String targetCWE = "CWE: 369 Divide by Zero";
		File outputDirectoryFile = new File(OUTPUT_DIRECTORY_PATH + sanitizeDirectoryName(targetCWE));
		if(outputDirectoryFile.exists()) {
			System.err.println("Directory already exists, make sure to save your work, then delete the directory: " + outputDirectoryFile.getAbsolutePath());
			return;
		} else {
			outputDirectoryFile.mkdirs();
		}
		
		File scriptFile = new File(outputDirectoryFile, "build.sh");
		List<String> fileContents = new ArrayList<String>();
		fileContents.add("#!/bin/sh");
		fileContents.add("echo \"#################################\"");
		fileContents.add(String.format("echo \"Compiling [%s] directory\"", outputDirectoryFile.getName()));
		fileContents.add("echo \"#################################\"");
		
		List<JulietTestCase> filteredTestCases = new ArrayList<JulietTestCase>();
		for(JulietTestCase testCase: julietTestCases) {
			if(targetCWE.equals(testCase.getCwe())) {
				filteredTestCases.add(testCase);
			}
		}
		
		System.out.println(String.format("There are [%d] test cases for the %s", filteredTestCases.size(), targetCWE));
		
		Map<String, List<JulietTestCase>> flowVariantToTestCasesMap = getFlowVariantToJulietTestCasesMap(filteredTestCases);
		for(String flowVariantString: flowVariantToTestCasesMap.keySet()) {
			File flowVariantDirectory = new File(outputDirectoryFile, sanitizeDirectoryName(flowVariantString));
			flowVariantDirectory.mkdir();
			
			List<JulietTestCase> currentTestCases = flowVariantToTestCasesMap.get(flowVariantString);
			System.out.println(String.format("Flow variant [%s] has [%d] test cases", flowVariantString, currentTestCases.size()));
			
			for(JulietTestCase testCase: currentTestCases) {
				File testCaseSourcePackageDirectoryFile = new File(JULIET_TEST_CASES_DIRECTORY + testCase.getIdentifier());
				if(!testCaseSourcePackageDirectoryFile.exists()) {
					System.err.println("Source package does not exist: " + testCaseSourcePackageDirectoryFile.getAbsolutePath());
				}
				
				File newSourcePackageDirectory = new File(flowVariantDirectory, testCase.getIdentifier());
				
				try {
					FileUtils.copyDirectory(testCaseSourcePackageDirectoryFile, newSourcePackageDirectory);
				} catch (IOException e) {
					System.err.println("An error occured: " + e.getMessage());
				}
				
				File testCasesSourceDirectory = new File(newSourcePackageDirectory, TEST_CASE_SOURCE_DIRECTORY_PATH);
				if(!testCasesSourceDirectory.exists()) {
					System.err.println("Could not find testcases source directory for: " + newSourcePackageDirectory.getName());
					continue;
				}
				
				File testCasesSupportSourceDirectory = new File(newSourcePackageDirectory, TEST_CASE_SUPPORT_SOURCE_DIRECTORY_PATH);
				if(!testCasesSupportSourceDirectory.exists()) {
					System.err.println("Could not find testcases support source directory for: " + newSourcePackageDirectory.getName());
					continue;
				}
				
				Collection<File> sourceFiles = FileUtils.listFiles(testCasesSourceDirectory, FileFileFilter.FILE, DirectoryFileFilter.DIRECTORY);
				if(sourceFiles.isEmpty()) {
					System.err.println("Could not find source files for: " + testCase.getIdentifier());
				}
				
				for(File sourceFile: sourceFiles) {
					String fileExtension = FilenameUtils.getExtension(sourceFile.getName());
					if(fileExtension.equals("c")) {
						String compilationCommand = String.format(COMPILE_COMMAND_STRING_FORMATTER, testCasesSupportSourceDirectory.getAbsolutePath(), sourceFile.getAbsolutePath());
						//System.out.println(compilationCommand);	
						fileContents.add(compilationCommand);
					}
				}
			}
		}
		
		try {
			FileUtils.writeLines(scriptFile, fileContents);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructs a mapping from each flow variant to the corresponding list of {@link JulietTestCase}s.
	 * 
	 * @param julietTestCases A list of parsed {@link JulietTestCase}s.
	 * @return A mapping from each flow variant to the corresponding list of {@link JulietTestCase}s.
	 */
	private static Map<String, List<JulietTestCase>> getFlowVariantToJulietTestCasesMap(List<JulietTestCase> julietTestCases) {
		Map<String, List<JulietTestCase>> flowVariantToJulietTestCasesMap = new HashMap<String, List<JulietTestCase>>();
		for(JulietTestCase julietTestCase: julietTestCases) {
			List<JulietTestCase> testCases = new ArrayList<JulietTestCase>();
			if(flowVariantToJulietTestCasesMap.containsKey(julietTestCase.getFlowVariantType())) {
				testCases = flowVariantToJulietTestCasesMap.get(julietTestCase.getFlowVariantType());
			}
			testCases.add(julietTestCase);
			flowVariantToJulietTestCasesMap.put(julietTestCase.getFlowVariantType(), testCases);
		}
		return flowVariantToJulietTestCasesMap;
	}
	
	/**
	 * Sanitizes the given <code>directoryName</code> for better directory name in the file system.
	 * 
	 * @param directoryName
	 * @return
	 */
	private static String sanitizeDirectoryName(String directoryName) {
		return directoryName.replaceAll("\\s+", "-").replaceAll(":", "").replaceAll("/", "-");
	}
	
}
