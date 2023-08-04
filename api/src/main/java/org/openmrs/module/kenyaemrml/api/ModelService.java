package org.openmrs.module.kenyaemrml.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemrml.domain.ModelInputFields;
import org.openmrs.module.kenyaemrml.domain.ScoringResult;

public interface ModelService extends OpenmrsService {
	
	ScoringResult htsscore(String modelId, String facilityName, String encounterDate, ModelInputFields inputFields,
	        boolean debug);
	
	ScoringResult iitscore(String modelId, String facilityName, String encounterDate, ModelInputFields inputFields,
	        boolean debug);
}
