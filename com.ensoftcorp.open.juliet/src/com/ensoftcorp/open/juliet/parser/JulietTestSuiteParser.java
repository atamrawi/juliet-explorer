package com.ensoftcorp.open.juliet.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JulietTestSuiteParser {

	private static final String SPACE_DELIMITER = "\\s+";
	private static final String MULTIPLE_RUN_RESULTS_STRING_FORMATTER = "Found [%d] may run results for test case [%d]";
	private static final String NEWLINE_SPACES_DELIMITER = "\\n\\s+";
	private static final String ERROR_PARSING_FLOW_VARIANT_STRING_FORMATTER = "Error parsing flow variants [%s] for identifier [%s]";
	private static final String MULTIPLE_RUN_OBJECTS_STRING_FORMATTER = "The identifier [%d] had couple of 'runs' objects";
	private static final String ERROR_PARSING_JSON_FILE_STRING_FORMATTER = "An error occurred while parsing the JSON file at: %s";
	private static final String ERROR_READING_JSON_FILE_STRING_FORMATTER = "An error occurred while reading the JSON file at: %s";
	private static final String JSON_FILE_NOT_FOUND_STRING_FORMATTER = "Cannot find the JSON file at: %s";
	private static final String PARSED_TEST_CASES_SIZE_DOES_NOT_MATCH_REPORTED_SIZE_STRING_FORMATTER = "Parsed Juliet test cases size [%d] does not conform to overall reported size [%d] in JSON file.";
	private static final String JULIET_TEST_CASES_FOUND_STRING_FORMATTER = "Found [%d] test cases.";
	
	public static List<JulietTestCase> parse(File jsonFile) {
		try (FileReader reader = new FileReader(jsonFile)) {
			System.out.print("Parsing [" + jsonFile.getName() + "] ...");
			JSONParser jsonParser = new JSONParser();
			JSONObject obj = (JSONObject) jsonParser.parse(reader);
			System.out.println("Done.");
			
			JSONArray testCasesArray = (JSONArray) obj.get(JSONConstants.TEST_CASES_KEY);
			String testCasesFoundCountMessage = String.format(JULIET_TEST_CASES_FOUND_STRING_FORMATTER, testCasesArray.size());
			System.out.println(testCasesFoundCountMessage);
			
			List<JulietTestCase> parsedJulietTestCases = parseJulietTestCases(testCasesArray);
			if(parsedJulietTestCases.size() != testCasesArray.size()) {
				String errorMessage = String.format(PARSED_TEST_CASES_SIZE_DOES_NOT_MATCH_REPORTED_SIZE_STRING_FORMATTER, parsedJulietTestCases.size(), testCasesArray.size());
				System.err.println(errorMessage);
			}
			return parsedJulietTestCases;

		} catch (FileNotFoundException e) {
			String errorMessage = String.format(JSON_FILE_NOT_FOUND_STRING_FORMATTER, jsonFile.getPath());
			System.err.println(errorMessage);
			return null;
		} catch (IOException e) {
			String errorMessage = String.format(ERROR_READING_JSON_FILE_STRING_FORMATTER, jsonFile.getPath());
			System.err.println(errorMessage);
			return null;
		} catch (ParseException e) {
			String errorMessage = String.format(ERROR_PARSING_JSON_FILE_STRING_FORMATTER, jsonFile.getPath());
			System.err.println(errorMessage);
			return null;
		}
	}

	/**
	 * Parses the given <code>testCasesArray</code> containing {@link JulietTestCase}s.
	 * 
	 * @param testCasesArray An instance of {@link JSONArray}.
	 * @return A list of {@link JulietTestCase}s.
	 */
	private static List<JulietTestCase> parseJulietTestCases(JSONArray testCasesArray) {
		List<JulietTestCase> parsedTestCases = new ArrayList<JulietTestCase>();
		
		for(Object object: testCasesArray) {
			JSONObject testCaseObject = (JSONObject) object;
			String identifier = (String) testCaseObject.get(JSONConstants.TEST_CASE_IDENTIFIER_KEY);
			
			JulietTestCase julietTestCase = new JulietTestCase(identifier);
			
			String link = (String) testCaseObject.get(JSONConstants.TEST_CASE_LINK_KEY);
			julietTestCase.setLink(link);
			
			JSONObject sarifObject = (JSONObject) testCaseObject.get(JSONConstants.TEST_CASE_SARIF_KEY);
			JSONArray runsArray = (JSONArray) sarifObject.get(JSONConstants.TEST_CASE_RUNS_KEY);
			parseJulietTestCasesRuns(julietTestCase, runsArray);
			
			parsedTestCases.add(julietTestCase);
		}
		
		return parsedTestCases;
	}

	/**
	 * Updates the given <code>julietTestCase</code> by mining properties from the given <code>runsArray</code>.
	 * 
	 * @param julietTestCase An instance of {@link JulietTestCase}.
	 * @param runsArray An instance of {@link JSONArray}.
	 */
	private static void parseJulietTestCasesRuns(JulietTestCase julietTestCase, JSONArray runsArray) {
		if(runsArray.size() != 1) {
			System.err.println(String.format(MULTIPLE_RUN_OBJECTS_STRING_FORMATTER, julietTestCase.getIdentifier()));
			return;
		}
		
		JSONObject runObject = (JSONObject) runsArray.get(0);
		JSONObject propertiesObject = (JSONObject) runObject.get(JSONConstants.TEST_CASE_RUN_PROPERTIES_KEY);
		String description = (String) propertiesObject.get(JSONConstants.TEST_CASE_RUN_PROPERTIES_DESCRIPTION_KEY);
		String[] descriptionParts = description.split(NEWLINE_SPACES_DELIMITER);
		if(descriptionParts.length == 0) {
			return;
		}
		
		if(descriptionParts.length == 1) {
			List<String> runResults = getRunResults(runObject);
			if(runResults.size() == 1) {
				String runResult = runResults.get(0);
				julietTestCase.setCwe(runResult);
				julietTestCase.setFlowVariantCategory(FlowVariantConstants.MISC_FLOW_VARIANT_NUMBER + " " + runResult);
				julietTestCase.setFlowVariantType(FlowVariantConstants.MISC_FLOW_VARIANT);
			} else {
				String errorMessage = String.format(MULTIPLE_RUN_RESULTS_STRING_FORMATTER, runResults.size(), julietTestCase.getIdentifier());
				System.err.println(errorMessage);
			}
			return;
		}
		
		julietTestCase.setCwe(descriptionParts[0].trim());
		
		for(int i = 0; i < descriptionParts.length; i++) {
			if(descriptionParts[i].startsWith(FlowVariantConstants.FLOW_VARIANT_STRING_CONSTANT)) {
				String flowVariantCategory = descriptionParts[i].substring(FlowVariantConstants.FLOW_VARIANT_STRING_CONSTANT.length()).trim();
				julietTestCase.setFlowVariantCategory(flowVariantCategory);
				
				String[] flowVariantCategoryParts = flowVariantCategory.split(SPACE_DELIMITER);
				String flowVariantPart = flowVariantCategoryParts[1];
				if(flowVariantCategoryParts.length > 2) {
					flowVariantPart += " " + flowVariantCategoryParts[2];
				}
				if(flowVariantPart.contains(FlowVariantConstants.BASELINE_FLOW_VARIANT)) {
					julietTestCase.setFlowVariantType(FlowVariantConstants.BASELINE_FLOW_VARIANT);
				} else if(flowVariantPart.contains(FlowVariantConstants.CONTROLFLOW_FLOW_VARIANT)) {
					julietTestCase.setFlowVariantType(FlowVariantConstants.CONTROLFLOW_FLOW_VARIANT);
				} else if(flowVariantPart.contains(FlowVariantConstants.DATAFLOW_FLOW_VARIANT)) {
					julietTestCase.setFlowVariantType(FlowVariantConstants.DATAFLOW_FLOW_VARIANT);
				} else if(flowVariantPart.contains(FlowVariantConstants.DATA_CONTROL_FLOW_FLOW_VARIANT)) {
					julietTestCase.setFlowVariantType(FlowVariantConstants.DATA_CONTROL_FLOW_FLOW_VARIANT);
				} else {
					String errorMessage = String.format(ERROR_PARSING_FLOW_VARIANT_STRING_FORMATTER, flowVariantCategory, julietTestCase.getIdentifier());
					System.err.println(errorMessage);
				}
			}
		}	
	}

	/**
	 * Gets the run results from the given <code>runObject</code>.
	 * 
	 * @param runObject An instance of {@link JSONObject}.
	 * @return A list of {@link String}s.
	 */
	private static List<String> getRunResults(JSONObject runObject) {
		JSONArray resultsArray = (JSONArray) runObject.get(JSONConstants.TEST_CASE_RUN_RESULTS_KEY);
		List<String> results = new ArrayList<String>();
		for(Object resultObject: resultsArray) {
			String ruleId = (String) ((JSONObject) resultObject).get(JSONConstants.TEST_CASE_RUN_RULE_ID_KEY);
			JSONObject message = (JSONObject) ((JSONObject) resultObject).get(JSONConstants.TEST_CASE_RUN_MESSAGE_KEY);
			String messageText = (String) message.get(JSONConstants.TEST_CASE_RUN_MESSAGE_TEXT_KEY);
			String result = ruleId + " " + messageText;
			results.add(result);
		}
		return results;
	}
}
