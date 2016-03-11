/**
 * Copyright (C) 2000-2016 Atomikos <info@atomikos.com>
 *
 * LICENSE CONDITIONS
 *
 * See http://www.atomikos.com/Main/WhichLicenseApplies for details.
 */

package com.atomikos.recovery.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atomikos.icatch.CoordinatorLogEntry;
import com.atomikos.icatch.ParticipantLogEntry;
import com.atomikos.icatch.TxState;

public class Deserializer {

	private static final String JSON_ARRAY_END = "]";

	private static final String JSON_ARRAY_START = "[";

	
	List<String> tokenize(String content) {
		List<String> result = new ArrayList<String>();
		int endObject = content.indexOf("}");
		while(endObject >0){
			String object = content.substring(0,endObject+1);
			result.add(object);
			content = content.substring(endObject+1);
			endObject = content.indexOf("}");
		}
		return result;
	}

	String extractArrayPart(String content) {
		if(!content.contains(JSON_ARRAY_START) && !content.contains(JSON_ARRAY_END)) {
			//no array...
			return "";
		}
		//else
		int start=content.indexOf(JSON_ARRAY_START);
		int end=content.indexOf(JSON_ARRAY_END);
		
		return content.substring(start+1, end);
	}
	public CoordinatorLogEntry fromJSON(String coordinatorLogEntryStr) {
		Map<String, String> header = extractHeader(coordinatorLogEntryStr);
		String coordinatorId = header.get("id");
		String arrayContent = extractArrayPart(coordinatorLogEntryStr);
		List<String> elements = tokenize(arrayContent);
		
		ParticipantLogEntry[] participantLogEntries = new ParticipantLogEntry[elements.size()];
		
		for (int i = 0; i < participantLogEntries.length; i++) {
			participantLogEntries[i]=recreateParticipantLogEntry(coordinatorId,elements.get(i));
		}
		
		
		CoordinatorLogEntry actual = new CoordinatorLogEntry(header.get("id"),Boolean.valueOf(header.get("wasCommitted")),  participantLogEntries,header.get("superiorCoordinatorId"));
		return actual;
	}

	private Map<String, String> extractHeader(String coordinatorLogEntryStr) {
		Map<String,String> header = new HashMap<String, String>(2);
		String[] attributes = coordinatorLogEntryStr.split(",");
		for (String attribute : attributes) {
			String[] pair = attribute.split(":");
			header.put(pair[0].replaceAll("\\{", "").replace("\"", ""), pair[1].replace("\"", ""));
		}
		return header;
	}
	
	ParticipantLogEntry recreateParticipantLogEntry(String coordinatorId,
			String participantLogEntry) {
		participantLogEntry = participantLogEntry.replaceAll("\\{", "").replaceAll("\\}", "");
		
		Map<String,String> content = new HashMap<String, String>(5);
		String[] attributes = participantLogEntry.split(",");
		for (String attribute : attributes) {
			String[] pair = attribute.split(":");
			if(pair.length>1){
				content.put(pair[0].replace("\"", ""), pair[1].replace("\"", ""));	
			}
			
		}
		
		ParticipantLogEntry actual = new ParticipantLogEntry(coordinatorId,
				content.get("uri"), Long.valueOf(content.get("expires")), content.get("resourceName"), TxState.valueOf(content.get("state")));
		return actual;
	}
}
