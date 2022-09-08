package com.ensoftcorp.open.juliet.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JulietTestCasesHTMLGenerator {

	private static final String TEST_CASE_LIST_ITEM_TAG = "<li data-jstree='{\"icon\":\"jstree-file\"}'>\n";
	private static final String TEST_CASE_HREF_TAG_STRING_FORMATTER = "<a href=\"%s\" target=\"_blank\">%s</a>\n";
	private static final String CLOSE_LIST_ITEM_TAG = "</li>\n";
	private static final String OPEN_LIST_ITEM_TAG = "<li>\n";
	private static final String CLOSE_UNORDERED_LIST_TAG = "</ul>\n";
	private static final String OPEN_UNORDERED_LIST_TAG = "<ul>\n";
	private static final String DIV_TREE_ROOT_STRING_FORMATTER = "<div id=\"jstree\">\n%s\n</div>\n";

	public static String toHTML(List<JulietTestCase> julietTestCases) {
		StringBuilder julietTestCasesHTMLBuilder = new StringBuilder();
		
		Map<String, List<JulietTestCase>> cweToJulietTestCasesMap = getCWEToJulietTestCasesMap(julietTestCases);
		
		julietTestCasesHTMLBuilder.append(OPEN_UNORDERED_LIST_TAG);
		for(String cwe: cweToJulietTestCasesMap.keySet()) {
			List<JulietTestCase> cweTestCases = cweToJulietTestCasesMap.get(cwe);
			julietTestCasesHTMLBuilder.append(OPEN_LIST_ITEM_TAG);
			julietTestCasesHTMLBuilder.append(cwe + " [" + cweTestCases.size() + "]\n");
			
			julietTestCasesHTMLBuilder.append(OPEN_UNORDERED_LIST_TAG);
			String flowVariantHTMLBody = constructFlowVariantHTMLItems(cweTestCases);
			julietTestCasesHTMLBuilder.append(flowVariantHTMLBody);
			
			julietTestCasesHTMLBuilder.append(CLOSE_UNORDERED_LIST_TAG);
			
			julietTestCasesHTMLBuilder.append(CLOSE_LIST_ITEM_TAG);
		}
		julietTestCasesHTMLBuilder.append(CLOSE_UNORDERED_LIST_TAG);
		
		String generatedHTMLBody = String.format(DIV_TREE_ROOT_STRING_FORMATTER, julietTestCasesHTMLBuilder.toString());
		return generatedHTMLBody;
	}

	private static String constructFlowVariantHTMLItems(List<JulietTestCase> cweTestCases) {
		StringBuilder flowVariantHTMLBuilder = new StringBuilder();
		Map<String, List<JulietTestCase>> flowVariantToTestCasesMap = getFlowVariantToJulietTestCasesMap(cweTestCases);
		constructFlowVariantSubItem(FlowVariantConstants.BASELINE_FLOW_VARIANT, flowVariantHTMLBuilder, flowVariantToTestCasesMap);
		constructFlowVariantSubItem(FlowVariantConstants.CONTROLFLOW_FLOW_VARIANT, flowVariantHTMLBuilder, flowVariantToTestCasesMap);
		constructFlowVariantSubItem(FlowVariantConstants.DATAFLOW_FLOW_VARIANT, flowVariantHTMLBuilder, flowVariantToTestCasesMap);
		constructFlowVariantSubItem(FlowVariantConstants.DATA_CONTROL_FLOW_FLOW_VARIANT, flowVariantHTMLBuilder, flowVariantToTestCasesMap);
		constructFlowVariantSubItem(FlowVariantConstants.MISC_FLOW_VARIANT, flowVariantHTMLBuilder, flowVariantToTestCasesMap);
		return flowVariantHTMLBuilder.toString();
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
	 * Constructs a mapping from each CWE to the corresponding list of {@link JulietTestCase}s.
	 * 
	 * @param julietTestCases A list of parsed {@link JulietTestCase}s.
	 * @return A mapping from a CWE to the corresponding list of {@link JulietTestCase}s.
	 */
	private static Map<String, List<JulietTestCase>> getCWEToJulietTestCasesMap(List<JulietTestCase> julietTestCases) {
		Map<String, List<JulietTestCase>> cweToJulietTestCasesMap = new HashMap<String, List<JulietTestCase>>();
		for(JulietTestCase julietTestCase: julietTestCases) {
			List<JulietTestCase> testCases = new ArrayList<JulietTestCase>();
			if(cweToJulietTestCasesMap.containsKey(julietTestCase.getCwe())) {
				testCases = cweToJulietTestCasesMap.get(julietTestCase.getCwe());
			}
			testCases.add(julietTestCase);
			cweToJulietTestCasesMap.put(julietTestCase.getCwe(), testCases);
		}
		return cweToJulietTestCasesMap;
	}
	
	private static void constructFlowVariantSubItem(String key, StringBuilder treeBuilder, Map<String, List<JulietTestCase>> flowVariantToTestCasesMap) {
		if(flowVariantToTestCasesMap.containsKey(key)) {
			List<JulietTestCase> flowTestCases = flowVariantToTestCasesMap.get(key);
			treeBuilder.append(OPEN_LIST_ITEM_TAG);
			treeBuilder.append(key + " [" + flowTestCases.size() + "]\n");
				treeBuilder.append(OPEN_UNORDERED_LIST_TAG);
				Map<String, List<JulietTestCase>> flowTypeToTestCasesMap = new HashMap<String, List<JulietTestCase>>();
				for(JulietTestCase test: flowTestCases) {
					List<JulietTestCase> tests = new ArrayList<JulietTestCase>();
					if(flowTypeToTestCasesMap.containsKey(test.getFlowVariantCategory())) {
						tests = flowTypeToTestCasesMap.get(test.getFlowVariantCategory());
					}
					tests.add(test);
					flowTypeToTestCasesMap.put(test.getFlowVariantCategory(), tests);
				}
				List<String> keySet = new ArrayList<String>();
				keySet.addAll(flowTypeToTestCasesMap.keySet());
				Collections.sort(keySet);
				for(String keyItem: keySet) {
					List<JulietTestCase> tests = flowTypeToTestCasesMap.get(keyItem);
					treeBuilder.append(OPEN_LIST_ITEM_TAG);
					treeBuilder.append(keyItem + " [" + tests.size() + "]\n");
						treeBuilder.append(OPEN_UNORDERED_LIST_TAG);
						for(JulietTestCase testCase: tests) {
							treeBuilder.append(TEST_CASE_LIST_ITEM_TAG);
							String linkString = String.format(TEST_CASE_HREF_TAG_STRING_FORMATTER, testCase.getLink(), testCase.getIdentifier());
							treeBuilder.append(linkString);
							treeBuilder.append(CLOSE_LIST_ITEM_TAG);
						}
						treeBuilder.append(CLOSE_UNORDERED_LIST_TAG);
					treeBuilder.append(CLOSE_LIST_ITEM_TAG);
				}
				treeBuilder.append(CLOSE_UNORDERED_LIST_TAG);
			treeBuilder.append(CLOSE_LIST_ITEM_TAG);
		}
	}
	
}
