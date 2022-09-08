package com.ensoftcorp.open.juliet.parser;

import java.io.File;
import java.util.List;

public class Driver {
	
	private static final String JULIET_TEST_CASES_JSON_FILE_PATH = 
			"C:\\Users\\Ahmed Tamrawi\\Downloads\\2022-08-11-juliet-c-cplusplus-v1-3-1-with-extra-support\\sarifs.json";
	
	public static void main(String[] args) {
		File jsonFile = new File(JULIET_TEST_CASES_JSON_FILE_PATH);
		List<JulietTestCase> julietTestCases = JulietTestSuiteParser.parse(jsonFile);
		String htmlBody = JulietTestCasesHTMLGenerator.toHTML(julietTestCases);
		System.out.println(htmlBody);
	}
	
}
