package org.openmrs.module.kenyaemrml.api.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.dmg.pmml.FieldName;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.Days;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.OutputField;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.util.EncounterBasedRegimenUtils;
import org.openmrs.module.kenyaemr.wrapper.PatientWrapper;
import org.openmrs.module.kenyaemrml.api.HTSMLService;
import org.openmrs.module.kenyaemrml.api.IITMLService;
import org.openmrs.module.kenyaemrml.api.MLUtils;
import org.openmrs.module.kenyaemrml.api.MLinKenyaEMRService;
import org.openmrs.module.kenyaemrml.api.ModelService;
import org.openmrs.module.kenyaemrml.domain.ModelInputFields;
import org.openmrs.module.kenyaemrml.domain.ScoringResult;
import org.openmrs.module.kenyaemrml.iit.Appointment;
import org.openmrs.module.kenyaemrml.iit.PatientRiskScore;
import org.openmrs.module.kenyaemrml.iit.Treatment;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.common.Age;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.ui.framework.SimpleObject;

/**
 * Service class used to prepare and score models
 */
public class ModelServiceImpl extends BaseOpenmrsService implements ModelService {
	
	private Log log = LogFactory.getLog(this.getClass());

	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	public ScoringResult htsscore(String modelId, String facilityName, String encounterDate, ModelInputFields inputFields, boolean debug) {
		try {
			Evaluator evaluator;
			HTSMLService hTSMLService = Context.getService(HTSMLService.class);
			evaluator = hTSMLService.getEvaluator();
			// evaluator.verify();
			ScoringResult scoringResult = new ScoringResult(score(evaluator, inputFields, debug));
			return scoringResult;
		}
		catch (Exception e) {
			log.error("IIT ML: Exception during preparation of input parameters or scoring of values for IIT model: " + e.getMessage());
			System.err.println("IIT ML: Exception during preparation of input parameters or scoring of values for IIT model: " + e.getMessage());
			e.printStackTrace();
			return(null);
		}
	}

	public ScoringResult iitscore(String modelId, String facilityName, String encounterDate, ModelInputFields inputFields, boolean debug) {
		try {
			Evaluator evaluator;
			IITMLService iITMLService = Context.getService(IITMLService.class);
			evaluator = iITMLService.getEvaluator();
			// evaluator.verify();
			ScoringResult scoringResult = new ScoringResult(score(evaluator, inputFields, debug));
			return scoringResult;
		}
		catch (Exception e) {
			log.error("IIT ML: Exception during preparation of input parameters or scoring of values for IIT model: " + e.getMessage());
			System.err.println("IIT ML: Exception during preparation of input parameters or scoring of values for IIT model: " + e.getMessage());
			e.printStackTrace();
			return(null);
		}
	}
	
	/**
	 * A method that scores a model
	 * 
	 * @param evaluator
	 * @param inputFields
	 * @return
	 */
	public Map<String, Object> score(Evaluator evaluator, ModelInputFields inputFields, boolean debug) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		Map<FieldName, ?> evaluationResultFromEvaluator = evaluator.evaluate(prepareEvaluationArgs(evaluator, inputFields));
		
		List<OutputField> outputFields = evaluator.getOutputFields();
		//List<TargetField> targetFields = evaluator.getTargetFields();
		
		for (OutputField targetField : outputFields) {
			FieldName targetFieldName = targetField.getName();
			Object targetFieldValue = evaluationResultFromEvaluator.get(targetField.getName());
			
			if (targetFieldValue instanceof Computable) {
				targetFieldValue = ((Computable) targetFieldValue).getResult();
			}
			
			result.put(targetFieldName.getValue(), targetFieldValue);
		}
		//TODO: this is purely for debugging
		if (debug) {
			Map<String, Object> modelInputs = new HashMap<String, Object>();
			Map<String, Object> combinedResult = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : inputFields.getFields().entrySet()) {
				modelInputs.put(entry.getKey(), entry.getValue());
			}
			combinedResult.put("predictions", result);
			combinedResult.put("ModelInputs", modelInputs);
			
			return combinedResult;
		} else {
			return result;
		}
	}
	
	/**
	 * Performs variable mapping
	 * 
	 * @param evaluator
	 * @param inputFields
	 * @return variable-value pair
	 */
	public Map<FieldName, FieldValue> prepareEvaluationArgs(Evaluator evaluator, ModelInputFields inputFields) {
		Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();
		
		List<InputField> evaluatorFields = evaluator.getActiveFields();
		
		for (InputField evaluatorField : evaluatorFields) {
			FieldName evaluatorFieldName = evaluatorField.getName();
			String evaluatorFieldNameValue = evaluatorFieldName.getValue();
			
			Object inputValue = inputFields.getFields().get(evaluatorFieldNameValue);
			
			if (inputValue == null) {
				log.warn("Model value not found for the following field: " + evaluatorFieldNameValue);
			}
			
			arguments.put(evaluatorFieldName, evaluatorField.prepare(inputValue));
		}
		return arguments;
	}

	/**
	 * Gets the latest patient IIT score
	 */
	public PatientRiskScore generatePatientRiskScore(Patient patient) {
		long startTime = System.currentTimeMillis();
		long stopTime = 0L;
		long startMemory = getMemoryConsumption();
		long stopMemory = 0L;

		PatientRiskScore patientRiskScore = new PatientRiskScore();
		SimpleObject modelConfigs = new SimpleObject();
		SimpleObject patientPredictionVariables = new SimpleObject();
		SimpleObject mlScoringRequestPayload = new SimpleObject();
		
		try {
			//Threshold
			String iitLowRiskThresholdGlobal = "kenyaemrml.iit.lowRiskThreshold";
			GlobalProperty globalIITLowRiskThreshold = Context.getAdministrationService().getGlobalPropertyObject(iitLowRiskThresholdGlobal);
			String strIITLowRiskThreshold = globalIITLowRiskThreshold.getPropertyValue();
			Double decIITLowRiskThreshold = Double.valueOf(strIITLowRiskThreshold);

			String iitMediumRiskThresholdGlobal = "kenyaemrml.iit.mediumRiskThreshold";
			GlobalProperty globalIITMediumRiskThreshold = Context.getAdministrationService().getGlobalPropertyObject(iitMediumRiskThresholdGlobal);
			String strIITMediumRiskThreshold = globalIITMediumRiskThreshold.getPropertyValue();
			Double decIITMediumRiskThreshold = Double.valueOf(strIITMediumRiskThreshold);

			String iitHighRiskThresholdGlobal = "kenyaemrml.iit.highRiskThreshold";
			GlobalProperty globalIITHighRiskThreshold = Context.getAdministrationService().getGlobalPropertyObject(iitHighRiskThresholdGlobal);
			String strIITHighRiskThreshold = globalIITHighRiskThreshold.getPropertyValue();
			Double decIITHighRiskThreshold = Double.valueOf(strIITHighRiskThreshold);
			// Model Configuration
			modelConfigs.put("modelId", "XGB_IIT_12152023");
			String today = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
			modelConfigs.put("encounterDate", today);
			modelConfigs.put("facilityId", "");
			modelConfigs.put("debug", "true");
			// Prediction Variables

			// Start Facility Profile Matrix
			// This is set to initial zeros. The real values will be picked from the location matrix file

			patientPredictionVariables.put("anc", 0);
			patientPredictionVariables.put("sti", 0);
			patientPredictionVariables.put("SumTXCurr", 0);
			patientPredictionVariables.put("poverty", 0);
			patientPredictionVariables.put("pregnancies", 0);
			patientPredictionVariables.put("pnc", 0);
			patientPredictionVariables.put("pop", 0);
			patientPredictionVariables.put("sba", 0);
			patientPredictionVariables.put("owner_typeFaith", 0);
			patientPredictionVariables.put("owner_typeNGO", 0);
			patientPredictionVariables.put("owner_typePrivate", 0);
			patientPredictionVariables.put("owner_typePublic", 0);
			patientPredictionVariables.put("partner_away", 0);
			patientPredictionVariables.put("partner_men", 0);
			patientPredictionVariables.put("partner_women", 0);
			patientPredictionVariables.put("literacy", 0);
			patientPredictionVariables.put("hiv_count", 0);
			patientPredictionVariables.put("hiv_prev", 0);
			patientPredictionVariables.put("in_union", 0);
			patientPredictionVariables.put("intercourse", 0);
			patientPredictionVariables.put("keph_level_nameLevel.2", 0);
			patientPredictionVariables.put("keph_level_nameLevel.3", 0);
			patientPredictionVariables.put("keph_level_nameLevel.4", 0);
			patientPredictionVariables.put("keph_level_nameLevel.5", 0);
			patientPredictionVariables.put("keph_level_nameLevel.6", 0);
			patientPredictionVariables.put("circumcision", 0);
			patientPredictionVariables.put("condom", 0);
			patientPredictionVariables.put("births", 0);

			// End Facility Profile Matrix

			patientPredictionVariables.put("Age", 0);
			patientPredictionVariables.put("AHDNo", 0);
			patientPredictionVariables.put("AHDYes", 0);
			patientPredictionVariables.put("average_tca_last5", 0);
			patientPredictionVariables.put("averagelateness", 0);
			patientPredictionVariables.put("averagelateness_last10", 0);
			patientPredictionVariables.put("averagelateness_last3", 0);
			patientPredictionVariables.put("averagelateness_last5", 0);
			patientPredictionVariables.put("BMI", 0);
			patientPredictionVariables.put("Breastfeedingno", 0);
			patientPredictionVariables.put("BreastfeedingNR", 0);
			patientPredictionVariables.put("Breastfeedingyes", 0);
			patientPredictionVariables.put("DayFri", 0);
			patientPredictionVariables.put("DayMon", 0);
			patientPredictionVariables.put("DaySat", 0);
			patientPredictionVariables.put("DaySun", 0);
			patientPredictionVariables.put("DayThu", 0);
			patientPredictionVariables.put("DayTue", 0);
			patientPredictionVariables.put("DayWed", 0);
			patientPredictionVariables.put("DifferentiatedCarecommunityartdistributionhcwled", 0);
			patientPredictionVariables.put("DifferentiatedCarecommunityartdistributionpeerled", 0);
			patientPredictionVariables.put("DifferentiatedCareexpress", 0);
			patientPredictionVariables.put("DifferentiatedCarefacilityartdistributiongroup", 0);
			patientPredictionVariables.put("DifferentiatedCarefasttrack", 0);
			patientPredictionVariables.put("DifferentiatedCarestandardcare", 0);
			patientPredictionVariables.put("GenderFemale", 0);
			patientPredictionVariables.put("GenderMale", 0);
			patientPredictionVariables.put("late", 0);
			patientPredictionVariables.put("late_last10", 0);
			patientPredictionVariables.put("late_last3", 0);
			patientPredictionVariables.put("late_last5", 0);
			patientPredictionVariables.put("late_rate", 0);
			patientPredictionVariables.put("late28", 0);
			patientPredictionVariables.put("late28_rate", 0);
			patientPredictionVariables.put("MaritalStatusDivorced", 0);
			patientPredictionVariables.put("MaritalStatusMarried", 0);
			patientPredictionVariables.put("MaritalStatusMinor", 0);
			patientPredictionVariables.put("MaritalStatusOther", 0);
			patientPredictionVariables.put("MaritalStatusPolygamous", 0);
			patientPredictionVariables.put("MaritalStatusSingle", 0);
			patientPredictionVariables.put("MaritalStatusWidow", 0);
			patientPredictionVariables.put("MonthApr", 0);
			patientPredictionVariables.put("MonthAug", 0);
			patientPredictionVariables.put("MonthDec", 0);
			patientPredictionVariables.put("MonthFeb", 0);
			patientPredictionVariables.put("MonthJan", 0);
			patientPredictionVariables.put("MonthJul", 0);
			patientPredictionVariables.put("MonthJun", 0);
			patientPredictionVariables.put("MonthMar", 0);
			patientPredictionVariables.put("MonthMay", 0);
			patientPredictionVariables.put("MonthNov", 0);
			patientPredictionVariables.put("MonthOct", 0);
			patientPredictionVariables.put("MonthSep", 0);
			patientPredictionVariables.put("most_recent_art_adherencefair", 0);
			patientPredictionVariables.put("most_recent_art_adherencegood", 0);
			patientPredictionVariables.put("most_recent_art_adherencepoor", 0);
			patientPredictionVariables.put("most_recent_vlsuppressed", 0);
			patientPredictionVariables.put("most_recent_vlunsuppressed", 0);
			patientPredictionVariables.put("n_appts", 0);
			patientPredictionVariables.put("n_hvl_threeyears", 0);
			patientPredictionVariables.put("n_lvl_threeyears", 0);
			patientPredictionVariables.put("n_tests_threeyears", 0);
			patientPredictionVariables.put("NextAppointmentDate", 0);
			patientPredictionVariables.put("num_hiv_regimens", 0);
			patientPredictionVariables.put("OptimizedHIVRegimenNo", 0);
			patientPredictionVariables.put("OptimizedHIVRegimenYes", 0);
			patientPredictionVariables.put("PatientSourceOPD", 0);
			patientPredictionVariables.put("PatientSourceOther", 0);
			patientPredictionVariables.put("PatientSourceVCT", 0);
			patientPredictionVariables.put("PopulationTypeGP", 0);
			patientPredictionVariables.put("PopulationTypeKP", 0);
			patientPredictionVariables.put("Pregnantno", 0);
			patientPredictionVariables.put("PregnantNR", 0);
			patientPredictionVariables.put("Pregnantyes", 0);
			patientPredictionVariables.put("recent_hvl_rate", 0);
			patientPredictionVariables.put("StabilityAssessmentStable", 0);
			patientPredictionVariables.put("StabilityAssessmentUnstable", 0);
			patientPredictionVariables.put("timeOnArt", 0);
			patientPredictionVariables.put("unscheduled_rate", 0);
			patientPredictionVariables.put("visit_1", 0);
			patientPredictionVariables.put("visit_2", 0);
			patientPredictionVariables.put("visit_3", 0);
			patientPredictionVariables.put("visit_4", 0);
			patientPredictionVariables.put("visit_5", 0);
			patientPredictionVariables.put("Weight", 0);


			// Load model configs and variables
			mlScoringRequestPayload.put("modelConfigs", modelConfigs);
			mlScoringRequestPayload.put("variableValues", patientPredictionVariables);

			// Get JSON Payload
			String payload = mlScoringRequestPayload.toJson();
			System.out.println("IIT ML: Prediction Payload: " + payload);
			
			// Get the IIT ML score
			try {
				//Extract score from payload
				String mlScoreResponse = MLUtils.generateIITMLScore(payload);

				if(mlScoreResponse != null && !mlScoreResponse.trim().equalsIgnoreCase("")) {
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode jsonNode = (ObjectNode) mapper.readTree(mlScoreResponse);
					if (jsonNode != null) {
						System.out.println("IIT ML: Got ML Score Payload as: " + mlScoreResponse);
						Double riskScore = jsonNode.get("result").get("predictions").get("Probability_1").getDoubleValue();
						
						System.out.println("IIT ML: Got ML score as: " + riskScore);
						if(riskScore == null) {
							riskScore = new Double(0.00);
						}

						// Check if there is an existing record. In case we want to save, we need to modify record instead of creating a new one
						PatientRiskScore currentPatientRiskScore = Context.getService(MLinKenyaEMRService.class).getLatestPatientRiskScoreByPatient(patient);
						if(currentPatientRiskScore != null) {
							patientRiskScore = currentPatientRiskScore;
						} else {
							patientRiskScore.setPatient(patient);
							String randUUID = UUID.randomUUID().toString(); 
							patientRiskScore.setSourceSystemUuid(randUUID);
						}

						patientRiskScore.setRiskFactors("");
						patientRiskScore.setRiskScore(riskScore);
						
						if(riskScore <= decIITLowRiskThreshold) {
							patientRiskScore.setDescription("Low Risk");
						} else if((riskScore > decIITLowRiskThreshold) && (riskScore <= decIITMediumRiskThreshold)) {
							patientRiskScore.setDescription("Medium Risk");
						} else if((riskScore > decIITMediumRiskThreshold) && (riskScore <= decIITHighRiskThreshold)) {
							patientRiskScore.setDescription("High Risk");
						} else if(riskScore > decIITHighRiskThreshold) {
							patientRiskScore.setDescription("Highest Risk");
						}

						System.out.println("IIT ML: Got ML Description as: " + patientRiskScore.getDescription());
						patientRiskScore.setEvaluationDate(new Date());

						try {
							patientRiskScore.setPayload(payload);
							patientRiskScore = extractPayload(patientRiskScore, mlScoringRequestPayload);

							String facilityMflCode = MLUtils.getDefaultMflCode();
							patientRiskScore.setMflCode(facilityMflCode);

							Hibernate.initialize(patient.getIdentifiers()); // fix lazy loading
							String UNIQUE_PATIENT_NUMBER = "05ee9cf4-7242-4a17-b4d4-00f707265c8a";
							PatientIdentifierType patientIdentifierType = Context.getPatientService().getPatientIdentifierTypeByUuid(UNIQUE_PATIENT_NUMBER);
							PatientIdentifier cccNumberId = patient.getPatientIdentifier(patientIdentifierType); // error with lazy loading
							String cccNumber = cccNumberId.getIdentifier();
							patientRiskScore.setCccNumber(cccNumber);
						} catch(Exception ex) {
							System.err.println("ITT ML: Could not add payload, ccc or mfl " + ex.getMessage());
							ex.printStackTrace();
						}
						
						System.out.println("IIT ML: PatientRiskScore is: " + patientRiskScore.toString());

						stopTime = System.currentTimeMillis();
						long elapsedTime = stopTime - startTime;
						System.out.println("Time taken: " + elapsedTime);
						System.out.println("Time taken sec: " + elapsedTime / 1000);

						stopMemory = getMemoryConsumption();
						long usedMemory = stopMemory - startMemory;
						System.out.println("Memory used: " + usedMemory);

						return(patientRiskScore);
					} else {
						System.err.println("IIT ML: Error: Unable to get ML score");
					}
				}
			}
			catch (Exception em) {
				System.err.println("ITT ML: Could not get the IIT Score: Error Calling IIT Model " + em.getMessage());
				em.printStackTrace();
			}
		}
		catch (Exception ex) {
			System.err.println("ITT ML: Could not get the IIT Score: Error sourcing model vars " + ex.getMessage());
			ex.printStackTrace();
		}

		//In case of an error
		patientRiskScore.setRiskFactors("");
		patientRiskScore.setRiskScore(0.00);
		patientRiskScore.setPatient(patient);
		patientRiskScore.setDescription("Unknown Risk");
		patientRiskScore.setEvaluationDate(new Date());
		String randUUID = UUID.randomUUID().toString(); 
		patientRiskScore.setSourceSystemUuid(randUUID);

		stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Time taken: " + elapsedTime);
		System.out.println("Time taken sec: " + elapsedTime / 1000);

		stopMemory = getMemoryConsumption();
		long usedMemory = stopMemory - startMemory;
		System.out.println("Memory used: " + usedMemory);

		return(patientRiskScore);
	}

	private PatientRiskScore extractPayload(PatientRiskScore prs, SimpleObject mlScoringRequestPayload) {
		SimpleObject load = (SimpleObject) mlScoringRequestPayload.get("variableValues");

		try {
			prs.setAge(convertToString(load.get("Age")));
			prs.setAHDNo(convertToString(load.get("AHDNo")));
			prs.setAHDYes(convertToString(load.get("AHDYes")));
			prs.setAverage_tca_last5(convertToString(load.get("average_tca_last5")));
			prs.setAveragelateness(convertToString(load.get("averagelateness")));
			prs.setAveragelateness_last10(convertToString(load.get("averagelateness_last10")));
			prs.setAveragelateness_last3(convertToString(load.get("averagelateness_last3")));
			prs.setAveragelateness_last5(convertToString(load.get("averagelateness_last5")));
			prs.setBMI(convertToString(load.get("BMI")));
			prs.setBreastfeedingno(convertToString(load.get("Breastfeedingno")));
			prs.setBreastfeedingNR(convertToString(load.get("BreastfeedingNR")));
			prs.setBreastfeedingyes(convertToString(load.get("Breastfeedingyes")));
			prs.setDayFri(convertToString(load.get("DayFri")));
			prs.setDayMon(convertToString(load.get("DayMon")));
			prs.setDaySat(convertToString(load.get("DaySat")));
			prs.setDaySun(convertToString(load.get("DaySun")));
			prs.setDayThu(convertToString(load.get("DayThu")));
			prs.setDayTue(convertToString(load.get("DayTue")));
			prs.setDayWed(convertToString(load.get("DayWed")));
			prs.setDifferentiatedCarecommunityartdistributionhcwled(convertToString(load.get("DifferentiatedCarecommunityartdistributionhcwled")));
			prs.setDifferentiatedCarecommunityartdistributionpeerled(convertToString(load.get("DifferentiatedCarecommunityartdistributionpeerled")));
			prs.setDifferentiatedCareexpress(convertToString(load.get("DifferentiatedCareexpress")));
			prs.setDifferentiatedCarefacilityartdistributiongroup(convertToString(load.get("DifferentiatedCarefacilityartdistributiongroup")));
			prs.setDifferentiatedCarefasttrack(convertToString(load.get("DifferentiatedCarefasttrack")));
			prs.setDifferentiatedCarestandardcare(convertToString(load.get("DifferentiatedCarestandardcare")));
			prs.setGenderFemale(convertToString(load.get("GenderFemale")));
			prs.setGenderMale(convertToString(load.get("GenderMale")));
			prs.setLate(convertToString(load.get("late")));
			prs.setLate_last10(convertToString(load.get("late_last10")));
			prs.setLate_last3(convertToString(load.get("late_last3")));
			prs.setLate_last5(convertToString(load.get("late_last5")));
			prs.setLate_rate(convertToString(load.get("late_rate")));
			prs.setLate28(convertToString(load.get("late28")));
			prs.setLate28_rate(convertToString(load.get("late28_rate")));
			prs.setMaritalStatusDivorced(convertToString(load.get("MaritalStatusDivorced")));
			prs.setMaritalStatusMarried(convertToString(load.get("MaritalStatusMarried")));
			prs.setMaritalStatusMinor(convertToString(load.get("MaritalStatusMinor")));
			prs.setMaritalStatusOther(convertToString(load.get("MaritalStatusOther")));
			prs.setMaritalStatusPolygamous(convertToString(load.get("MaritalStatusPolygamous")));
			prs.setMaritalStatusSingle(convertToString(load.get("MaritalStatusSingle")));
			prs.setMaritalStatusWidow(convertToString(load.get("MaritalStatusWidow")));
			prs.setMonthApr(convertToString(load.get("MonthApr")));
			prs.setMonthAug(convertToString(load.get("MonthAug")));
			prs.setMonthDec(convertToString(load.get("MonthDec")));
			prs.setMonthFeb(convertToString(load.get("MonthFeb")));
			prs.setMonthJan(convertToString(load.get("MonthJan")));
			prs.setMonthJul(convertToString(load.get("MonthJul")));
			prs.setMonthJun(convertToString(load.get("MonthJun")));
			prs.setMonthMar(convertToString(load.get("MonthMar")));
			prs.setMonthMay(convertToString(load.get("MonthMay")));
			prs.setMonthNov(convertToString(load.get("MonthNov")));
			prs.setMonthOct(convertToString(load.get("MonthOct")));
			prs.setMonthSep(convertToString(load.get("MonthSep")));
			prs.setMost_recent_art_adherencefair(convertToString(load.get("most_recent_art_adherencefair")));
			prs.setMost_recent_art_adherencegood(convertToString(load.get("most_recent_art_adherencegood")));
			prs.setMost_recent_art_adherencepoor(convertToString(load.get("most_recent_art_adherencepoor")));
			prs.setMost_recent_vlsuppressed(convertToString(load.get("most_recent_vlsuppressed")));
			prs.setMost_recent_vlunsuppressed(convertToString(load.get("most_recent_vlunsuppressed")));
			prs.setN_appts(convertToString(load.get("n_appts")));
			prs.setN_hvl_threeyears(convertToString(load.get("n_hvl_threeyears")));
			prs.setN_lvl_threeyears(convertToString(load.get("n_lvl_threeyears")));
			prs.setN_tests_threeyears(convertToString(load.get("n_tests_threeyears")));
			prs.setNextAppointmentDate(convertToString(load.get("NextAppointmentDate")));
			prs.setNum_hiv_regimens(convertToString(load.get("num_hiv_regimens")));
			prs.setOptimizedHIVRegimenNo(convertToString(load.get("OptimizedHIVRegimenNo")));
			prs.setOptimizedHIVRegimenYes(convertToString(load.get("OptimizedHIVRegimenYes")));
			prs.setPatientSourceOPD(convertToString(load.get("PatientSourceOPD")));
			prs.setPatientSourceOther(convertToString(load.get("PatientSourceOther")));
			prs.setPatientSourceVCT(convertToString(load.get("PatientSourceVCT")));
			prs.setPopulationTypeGP(convertToString(load.get("PopulationTypeGP")));
			prs.setPopulationTypeKP(convertToString(load.get("PopulationTypeKP")));
			prs.setPregnantno(convertToString(load.get("Pregnantno")));
			prs.setPregnantNR(convertToString(load.get("PregnantNR")));
			prs.setPregnantyes(convertToString(load.get("Pregnantyes")));
			prs.setRecent_hvl_rate(convertToString(load.get("recent_hvl_rate")));
			prs.setStabilityAssessmentStable(convertToString(load.get("StabilityAssessmentStable")));
			prs.setStabilityAssessmentUnstable(convertToString(load.get("StabilityAssessmentUnstable")));
			prs.setTimeOnArt(convertToString(load.get("timeOnArt")));
			prs.setUnscheduled_rate(convertToString(load.get("unscheduled_rate")));
			prs.setVisit_1(convertToString(load.get("visit_1")));
			prs.setVisit_2(convertToString(load.get("visit_2")));
			prs.setVisit_3(convertToString(load.get("visit_3")));
			prs.setVisit_4(convertToString(load.get("visit_4")));
			prs.setVisit_5(convertToString(load.get("visit_5")));
			prs.setWeight(convertToString(load.get("Weight")));


		} catch(Exception ex) {
			System.err.println("IIT ML: Got error inserting debug variables to DB: " + ex.getMessage());
			ex.printStackTrace();
		}

		return(prs);
	}

	/**
	 * Converts any input object into a string and returns "" if it is not possible
	 * @param input -- input Object
	 * @return String
	 */
	private String convertToString(Object input) {
		if (input == null) {
			return "";
		}
	
		if (input instanceof String || input instanceof Integer || input instanceof Double ||
			input instanceof Long || input instanceof Float) {
			return input.toString();
		}
	
		return "";
	}

	/**
	 * Get the current memory consumption in MB
	 * @return long - the RAM usage in MB
	 */
	public long getMemoryConsumption() {
		long ret = 0L;
		final long MEGABYTE = 1024L * 1024L;

		// Get the Java runtime
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();
        // Calculate the used memory
        long memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used memory is bytes: " + memory);

		// get the MB
		ret = memory / MEGABYTE;
        System.out.println("Used memory is megabytes: " + ret);

		return(ret);
	}

	// START variable calculations
	private Long getTimeOnArt(List<List<Object>> artRecord) {
		Long ret = 0L;
		if(artRecord != null) {
			// Get the last record
			if (artRecord.size() > 0) {
				List<Object> visitObject = artRecord.get(artRecord.size() - 1);
				if (visitObject.get(1) != null) {
					Date artStartDate = (Date) visitObject.get(1);
					Date now = new Date();
					// Instant artInstant = artStartDate.toInstant();
					// Instant nowInstant = now.toInstant();
					// Get the age in years
					// Duration duration = Duration.between(nowInstant, dobInstant);
					// long years = duration.toDays() / 365;
					java.time.LocalDate artLocal = dateToLocalDate(artStartDate);
					java.time.LocalDate nowLocal = dateToLocalDate(now);
					long months = Math.abs(ChronoUnit.MONTHS.between(nowLocal, artLocal));
					ret = months;
				}
			}
		}
		return(ret);
	}

	private String getAHDNo(List<List<Object>> visits, Long Age) {
		String ret = "NA";
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				// If WHO Stage in (1,2) and Age six and above, then 1, if Age five or below or
				// WHO stage in (3,4), then 0, if Age over 6 and WHO Stage is NULL, then NA
				if (visitObject.get(10) != null) {
					String whoStage = (String) visitObject.get(10);
					whoStage = whoStage.trim().toLowerCase();
					Integer whoStageInt = getIntegerValue(whoStage);
					if((whoStageInt == 1 || whoStageInt == 2) && Age >= 6) {
						ret = "1";
						return(ret);
					}
					if((whoStageInt == 3 || whoStageInt == 4) && Age <= 5) {
						ret = "0";
						return(ret);
					}
				}
			}
		}
		return(ret);
	}

	private String getAHDYes(List<List<Object>> visits, Long Age) {
		String ret = "NA";
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				// If WHO Stage in (3,4) or Age five or below, then 1, if Age is six or
				// over and WHO stage in (1,2), then 0, if Age 6 or over and WHO Stage is NULL, then NA
				if (visitObject.get(10) != null) {
					String whoStage = (String) visitObject.get(10);
					whoStage = whoStage.trim().toLowerCase();
					Integer whoStageInt = getIntegerValue(whoStage);
					if((whoStageInt == 3 || whoStageInt == 4) && Age <= 5) {
						ret = "1";
						return(ret);
					}
					if((whoStageInt == 1 || whoStageInt == 2) && Age >= 6) {
						ret = "0";
						return(ret);
					}
				}
			}
		}
		return(ret);
	}

	private Double getRecentHvlRate(Integer n_hvl_threeyears,Integer n_test_threeyears) {
		Double ret = 0.00;

		try {
			if (n_test_threeyears != 0) {
				ret = (n_hvl_threeyears * 1.00) / (n_test_threeyears * 1.00);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return(ret);
	}

	private Integer getOptimizedHIVRegimenNo(List<List<Object>> pharmacy) {
		Integer ret = 1;
		if(pharmacy != null) {
			if (pharmacy.size() > 0) {
				// The last record
				List<Object> labObject = pharmacy.get(pharmacy.size() - 1);
				// TreatmentType != NULL or Prophylaxis, Drug != NULL
				if (labObject.get(4) != null && labObject.get(3) != null) {
					// Get drug name
					String drugName = (String) labObject.get(3);
					drugName = drugName.toLowerCase();
					if(drugName.contains("dtg")) {
						ret = 0;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getOptimizedHIVRegimenYes(List<List<Object>> pharmacy) {
		Integer ret = 0;
		if(pharmacy != null) {
			if (pharmacy.size() > 0) {
				// The last record
				List<Object> labObject = pharmacy.get(pharmacy.size() - 1);
				// TreatmentType != NULL or Prophylaxis, Drug != NULL
				if (labObject.get(4) != null && labObject.get(3) != null) {
					// Get drug name
					String drugName = (String) labObject.get(3);
					drugName = drugName.toLowerCase();
					if(drugName.contains("dtg")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMostRecentVLsuppressed(List<List<Object>> lab) {
		Integer ret = 0;
		if(lab != null) {
			if (lab.size() > 0) {
				// The last record
				List<Object> labObject = lab.get(lab.size() - 1);
				if (labObject.get(2) != null) {
					String result = (String) labObject.get(2);
					if(getIntegerValue(result.trim()) < 200 || result.trim().equalsIgnoreCase("LDL")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMostRecentVLunsuppressed(List<List<Object>> lab) {
		Integer ret = 0;
		if(lab != null) {
			if (lab.size() > 0) {
				// The last record
				List<Object> labObject = lab.get(lab.size() - 1);
				if (labObject.get(2) != null) {
					String result = (String) labObject.get(2);
					if(getIntegerValue(result.trim()) >= 200) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getNtestsThreeYears(List<List<Object>> lab) {
		Integer ret = 0;
		if(lab != null) {
			// Get for the last 3 years
			if (lab.size() > 0) {
				// Reverse the list to loop from the first
				List<List<Object>> labRev = new ArrayList<>();
				labRev.addAll(lab);
				Collections.reverse(labRev);
				// Count number of tests for the last 3 years
				Date now = new Date();
				// Instant nowInstant = now.toInstant();
				java.time.LocalDate nowLocal = dateToLocalDate(now);
				for(List<Object> labObject: labRev) {
					if (labObject.get(1) != null) {
						Date testDate = (Date) labObject.get(1);
						//Instant testInstant = testDate.toInstant();
						java.time.LocalDate testLocal = dateToLocalDate(testDate);
						long years = Math.abs(ChronoUnit.YEARS.between(nowLocal, testLocal));
						if(years <= 3) {
							ret++;
						}
					}
				}
			}
		}
		return(ret);
	}

	private Integer getNHVLThreeYears(List<List<Object>> lab) {
		Integer ret = 0;
		if(lab != null) {
			// Get for the last 3 years
			if (lab.size() > 0) {
				// Reverse the list to loop from the first
				List<List<Object>> labRev = new ArrayList<>();
				labRev.addAll(lab);
				Collections.reverse(labRev);
				// Count number of tests for the last 3 years
				Date now = new Date();
				// Instant nowInstant = now.toInstant();
				java.time.LocalDate nowLocal = dateToLocalDate(now);
				for(List<Object> labObject: labRev) {
					if (labObject.get(1) != null && labObject.get(2) != null) {
						Date testDate = (Date) labObject.get(1);
						// Instant testInstant = testDate.toInstant();
						java.time.LocalDate testLocal = dateToLocalDate(testDate);
						long years = Math.abs(ChronoUnit.YEARS.between(nowLocal, testLocal));
						String result = (String) labObject.get(2);
						if(years <= 3 && getIntegerValue(result.trim()) >= 200) {
							ret++;
						}
					}
				}
			}
		}
		return(ret);
	}

	private Integer getNLVLThreeYears(List<List<Object>> lab) {
		Integer ret = 0;
		if(lab != null) {
			// Get for the last 3 years
			if (lab.size() > 0) {
				// Reverse the list to loop from the first
				List<List<Object>> labRev = new ArrayList<>();
				labRev.addAll(lab);
				Collections.reverse(labRev);
				// Count number of tests for the last 3 years
				Date now = new Date();
				// Instant nowInstant = now.toInstant();
				java.time.LocalDate nowLocal = dateToLocalDate(now);
				for(List<Object> labObject: labRev) {
					if (labObject.get(1) != null && labObject.get(2) != null) {
						Date testDate = (Date) labObject.get(1);
						//Instant testInstant = testDate.toInstant();
						java.time.LocalDate testLocal = dateToLocalDate(testDate);
						long years = Math.abs(ChronoUnit.YEARS.between(nowLocal, testLocal));
						String result = (String) labObject.get(2);
						if(years <= 3 && (getIntegerValue(result.trim()) < 200 || result.trim().equalsIgnoreCase("LDL"))) {
							ret++;
						}
					}
				}
			}
		}
		return(ret);
	}

	/**
	 * Gets the integer value of a string, otherwise returns zero
	 * @param val
	 * @return
	 */
	public static int getIntegerValue(String val) {
		int ret = 0;
		try {
			ret = (int) Math.ceil(Double.parseDouble(val));
		} catch(Exception ex) {}
		return(ret);
	}

	/**
	 * Gets the long value of a string, otherwise returns zero
	 * @param val
	 * @return
	 */
	public static long getLongValue(String val) {
		long ret = 0;
		try {
			ret = (long) Math.ceil(Double.parseDouble(val));
		} catch(Exception ex) {}
		return(ret);
	}

	private Integer getPopulationTypeKP(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last visit
			if (demographics.size() > 0) {
				List<Object> visitObject = demographics.get(demographics.size() - 1);
				if (visitObject.get(6) != null) {
					String differentiatedCare = (String) visitObject.get(6);
					if(differentiatedCare.trim().equalsIgnoreCase("Key Population")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getPopulationTypeGP(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last visit
			if (demographics.size() > 0) {
				List<Object> visitObject = demographics.get(demographics.size() - 1);
				if (visitObject.get(6) != null) {
					String differentiatedCare = (String) visitObject.get(6);
					if(differentiatedCare.trim().equalsIgnoreCase("General Population")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private String getBreastFeedingNo(List<List<Object>> visits, Integer gender, Long Age) {
		String ret = "NA";
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(11) != null) {
					String isBreastFeeding = (String) visitObject.get(11);
					if(isBreastFeeding.trim().equalsIgnoreCase("yes")) {
						ret = "0";
						return(ret);
					}
					if(isBreastFeeding.trim().equalsIgnoreCase("no") && gender == 2 && Age > 9 && Age < 50) {
						ret = "1";
						return(ret);
					}
				}
			}
		}
		return(ret);
	}

	private String getBreastFeedingNR(Integer gender, Long Age) {
		String ret = "0";
		if(gender == 1 || Age < 10 || Age > 50) {
			ret = "1";
			return(ret);
		}
		return(ret);
	}

	private String getBreastFeedingYes(List<List<Object>> visits, Integer gender, Long Age) {
		String ret = "NA";
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(11) != null) {
					String isBreastFeeding = (String) visitObject.get(11);
					if(isBreastFeeding.trim().equalsIgnoreCase("no")) {
						ret = "0";
						return(ret);
					}
					if(isBreastFeeding.trim().equalsIgnoreCase("yes") && gender == 2 && Age > 9 && Age < 50) {
						ret = "1";
						return(ret);
					}
				}
			}
		}
		return(ret);
	}

	private String getPregnantNo(List<List<Object>> visits, Integer gender, Long Age) {
		String ret = "NA";
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(6) != null) {
					String isPregnant = (String) visitObject.get(6);
					if(isPregnant.trim().equalsIgnoreCase("yes")) {
						ret = "0";
						return(ret);
					}
					if(isPregnant.trim().equalsIgnoreCase("no") && gender == 2 && Age > 9 && Age < 50) {
						ret = "1";
						return(ret);
					}
				}
			}
		}
		return(ret);
	}

	private String getPregnantNR(Integer gender, Long Age) {
		String ret = "0";
		if(gender == 1 || Age < 10 || Age > 50) {
			ret = "1";
			return(ret);
		}
		return(ret);
	}

	private String getPregnantYes(List<List<Object>> visits, Integer gender, Long Age) {
		String ret = "NA";
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(6) != null) {
					String isPregnant = (String) visitObject.get(6);
					if(isPregnant.trim().equalsIgnoreCase("no")) {
						ret = "0";
						return(ret);
					}
					if(isPregnant.trim().equalsIgnoreCase("yes") && gender == 2 && Age > 9 && Age < 50) {
						ret = "1";
						return(ret);
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMostRecentArtAdherenceFair(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				// Get adherence category (ART) position
				if (visitObject.get(12) != null) {
					String adherencePositions = (String) visitObject.get(12);
					String[] tokens = adherencePositions.split("\\|");

					int artPos = -1;
					for (int i = 0; i < tokens.length; i++) {
						if (tokens[i].trim().equalsIgnoreCase("ART")) {
							System.out.println("IIT ML: Position of 'ART': " + i);
							break;
						}
					}

					if(artPos > -1) {
						// We found ART adherence is covered we get the status
						if (visitObject.get(9) != null) {
							String adherenceString = (String) visitObject.get(9);
							String[] adherenceTokens = adherenceString.split("\\|");
							if(adherenceTokens.length > 0) {
								for (int i = 0; i < adherenceTokens.length; i++) {
									if(i == artPos) {
										if (adherenceTokens[i].trim().equalsIgnoreCase("fair")) {
											ret = 1;
											return(ret);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMostRecentArtAdherenceGood(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				// Get adherence category (ART) position
				if (visitObject.get(12) != null) {
					String adherencePositions = (String) visitObject.get(12);
					String[] tokens = adherencePositions.split("\\|");

					int artPos = -1;
					for (int i = 0; i < tokens.length; i++) {
						if (tokens[i].trim().equalsIgnoreCase("ART")) {
							System.out.println("IIT ML: Position of 'ART': " + i);
							break;
						}
					}

					if(artPos > -1) {
						// We found ART adherence is covered we get the status
						if (visitObject.get(9) != null) {
							String adherenceString = (String) visitObject.get(9);
							String[] adherenceTokens = adherenceString.split("\\|");
							if(adherenceTokens.length > 0) {
								for (int i = 0; i < adherenceTokens.length; i++) {
									if(i == artPos) {
										if (adherenceTokens[i].trim().equalsIgnoreCase("good")) {
											ret = 1;
											return(ret);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMostRecentArtAdherencePoor(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				// Get adherence category (ART) position
				if (visitObject.get(12) != null) {
					String adherencePositions = (String) visitObject.get(12);
					String[] tokens = adherencePositions.split("\\|");

					int artPos = -1;
					for (int i = 0; i < tokens.length; i++) {
						if (tokens[i].trim().equalsIgnoreCase("ART")) {
							System.out.println("IIT ML: Position of 'ART': " + i);
							break;
						}
					}

					if(artPos > -1) {
						// We found ART adherence is covered we get the status
						if (visitObject.get(9) != null) {
							String adherenceString = (String) visitObject.get(9);
							String[] adherenceTokens = adherenceString.split("\\|");
							if(adherenceTokens.length > 0) {
								for (int i = 0; i < adherenceTokens.length; i++) {
									if(i == artPos) {
										if (adherenceTokens[i].trim().equalsIgnoreCase("poor")) {
											ret = 1;
											return(ret);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return(ret);
	}

	private Integer getStabilityAssessmentStable(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(8) != null) {
					String differentiatedCare = (String) visitObject.get(8);
					if(differentiatedCare.trim().equalsIgnoreCase("stable")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getStabilityAssessmentUnstable(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(8) != null) {
					String differentiatedCare = (String) visitObject.get(8);
					if(differentiatedCare.trim().equalsIgnoreCase("not stable")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getDifferentiatedCarecommunityartdistributionhcwled(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(7) != null) {
					String differentiatedCare = (String) visitObject.get(7);
					if(differentiatedCare.trim().equalsIgnoreCase("Community ART Distribution - HCW Led")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getDifferentiatedCarecommunityartdistributionpeerled(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(7) != null) {
					String differentiatedCare = (String) visitObject.get(7);
					if(differentiatedCare.trim().equalsIgnoreCase("Community ART Distribution - Peer Led")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getDifferentiatedCareexpress(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(7) != null) {
					String differentiatedCare = (String) visitObject.get(7);
					if(differentiatedCare.trim().equalsIgnoreCase("Express")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getDifferentiatedCarefacilityartdistributiongroup(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(7) != null) {
					String differentiatedCare = (String) visitObject.get(7);
					if(differentiatedCare.trim().equalsIgnoreCase("Facility ART Distribution Group")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getDifferentiatedCarefasttrack(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(7) != null) {
					String differentiatedCare = (String) visitObject.get(7);
					if(differentiatedCare.trim().equalsIgnoreCase("Fast Track")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getDifferentiatedCarestandardcare(List<List<Object>> visits) {
		Integer ret = 0;
		if(visits != null) {
			// Get the last visit
			if (visits.size() > 0) {
				List<Object> visitObject = visits.get(visits.size() - 1);
				if (visitObject.get(7) != null) {
					String differentiatedCare = (String) visitObject.get(7);
					if(differentiatedCare.trim().equalsIgnoreCase("Standard Care")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusOther(List<List<Object>> demographics, Long Age) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(4) != null) {
					String marital = (String) maritalObject.get(4);
					if (!marital.trim().equalsIgnoreCase("single") &&
							!marital.trim().equalsIgnoreCase("divorced") &&
							!marital.trim().equalsIgnoreCase("widow") &&
							!marital.trim().equalsIgnoreCase("separated") &&
							!marital.trim().equalsIgnoreCase("married") &&
							!marital.trim().equalsIgnoreCase("monogamous") &&
							!marital.trim().equalsIgnoreCase("cohabiting") &&
							!marital.trim().equalsIgnoreCase("polygamous") &&
							Age > 15
					) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusSingle(List<List<Object>> demographics, Long Age) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(4) != null) {
					String gender = (String) maritalObject.get(4);
					if (gender.trim().equalsIgnoreCase("single") && Age > 15) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusWidow(List<List<Object>> demographics, Long Age) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(4) != null) {
					String marital = (String) maritalObject.get(4);
					if (marital.trim().equalsIgnoreCase("widow") && Age > 15) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusPolygamous(List<List<Object>> demographics, Long Age) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(4) != null) {
					String marital = (String) maritalObject.get(4);
					if (marital.trim().equalsIgnoreCase("polygamous") && Age > 15) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusDivorced(List<List<Object>> demographics, Long Age) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(4) != null) {
					String marital = (String) maritalObject.get(4);
					if ((marital.trim().equalsIgnoreCase("divorced") && Age > 15) ||
							(marital.trim().equalsIgnoreCase("separated") && Age > 15)
					) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusMarried(List<List<Object>> demographics, Long Age) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(4) != null){
					String marital = (String) maritalObject.get(4);
					if ((marital.trim().equalsIgnoreCase("married") && Age > 15) ||
							(marital.trim().equalsIgnoreCase("monogamous") && Age > 15) ||
							(marital.trim().equalsIgnoreCase("cohabiting") && Age > 15)
					) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMaritalStatusMinor(Long Age) {
		Integer ret = 0;
		if (Age <= 15) {
			ret = 1;
		}
		return(ret);
	}

	private Long getAgeYears(List<List<Object>> demographics) {
		Long ret = 0L;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> maritalObject = demographics.get(demographics.size() - 1);
				if(maritalObject.get(2) != null) {
					Date DOB = (Date) maritalObject.get(2);
					Date now = new Date();
					// Instant dobInstant = DOB.toInstant();
					// Instant nowInstant = now.toInstant();
					// Get the age in years
					// Duration duration = Duration.between(nowInstant, dobInstant);
					// long years = duration.toDays() / 365;
					java.time.LocalDate dobLocal = dateToLocalDate(DOB);
					java.time.LocalDate nowLocal = dateToLocalDate(now);
					long years = Math.abs(ChronoUnit.YEARS.between(nowLocal, dobLocal));
					ret = years;
				}
			}
		}
		return(ret);
	}

	private LocalDate dateToLocalDate(Date dateToConvert) {
		return Instant.ofEpochMilli(dateToConvert.getTime())
				.atZone(ZoneId.systemDefault())
				.toLocalDate();
	}

	private Integer getPatientSourceVCT(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> sourceObject = demographics.get(demographics.size() - 1);
				if(sourceObject.get(3) != null) {
					String source = (String) sourceObject.get(3);
					if (source.trim().equalsIgnoreCase("vct")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getPatientSourceOther(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> sourceObject = demographics.get(demographics.size() - 1);
				if(sourceObject.get(3) != null) {
					String source = (String) sourceObject.get(3);
					if (!source.trim().equalsIgnoreCase("opd") && !source.trim().equalsIgnoreCase("vct")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getPatientSourceOPD(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> sourceObject = demographics.get(demographics.size() - 1);
				if(sourceObject.get(3) != null) {
					String source = (String) sourceObject.get(3);
					if (source.trim().equalsIgnoreCase("opd")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getGenderFemale(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> genderObject = demographics.get(demographics.size() - 1);
				if(genderObject.get(1) != null) {
					String gender = (String) genderObject.get(1);
					if (gender.trim().equalsIgnoreCase("female")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getGenderMale(List<List<Object>> demographics) {
		Integer ret = 0;
		if(demographics != null) {
			// Get the last appointment
			if (demographics.size() > 0) {
				List<Object> genderObject = demographics.get(demographics.size() - 1);
				if(genderObject.get(1) != null) {
					String gender = (String) genderObject.get(1);
					if (gender.trim().equalsIgnoreCase("male")) {
						ret = 1;
					}
				}
			}
		}
		return(ret);
	}

	private Integer getMonthJan(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.JANUARY.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthFeb(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.FEBRUARY.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthMar(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.MARCH.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthApr(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = monthOfYear == Month.APRIL.getValue() ? 1: 0;
			}
		}
		return(ret);
	}

	private Integer getMonthMay(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.MAY.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthJun(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.JUNE.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthJul(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.JULY.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthAug(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.AUGUST.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthSep(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.SEPTEMBER.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthOct(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = monthOfYear == Month.OCTOBER.getValue() ? 1: 0;
			}
		}
		return(ret);
	}

	private Integer getMonthNov(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.NOVEMBER.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getMonthDec(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getMonthOfYear(NAD);
				ret = (monthOfYear == Month.DECEMBER.getValue()) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDayMon(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 1) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDayTue(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 2) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDayWed(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 3) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDayThu(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 4) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDayFri(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 5) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDaySat(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 6) ? 1 : 0;
			}
		}
		return(ret);
	}

	private Integer getDaySun(List<Appointment> appointments) {
		Integer ret = 0;
		if(appointments != null) {
			// Get the last appointment
			if(appointments.size() > 0) {
				Appointment latestAppointment = appointments.get(appointments.size() - 1);
				Date NAD = latestAppointment.getAppointmentDate();
				int monthOfYear = getDayOfWeek(NAD);
				ret = (monthOfYear == 7) ? 1 : 0;
			}
		}
		return(ret);
	}

	public static int getMonthOfYear(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// Calendar months are zero-based, so adding 1 to get the correct month.
		return calendar.get(Calendar.MONTH) + 1;
	}

	public static int getDayOfWeek(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// Calendar weeks are zero-based, so adding 1 to get the correct week.
		return calendar.get(Calendar.DAY_OF_WEEK) + 1;
	}

	private Double getBMI(Double height, Double weight) {
		// BMI is given by (Weight / ((Height/100)^2))
		Double ret = 0.00;
		if(height > 0 && weight > 0) {
			//constraints
			if(height >= 100 && height <= 250 && weight >= 30 && weight <= 200) {
				ret = weight / (Math.pow((height / 100), 2));
			}
		}
		return(ret);
	}

	private Double getWeight(List<List<Object>> visits) {
		Double ret = 0.00;
		if(visits != null) {
			// Flip the list -- the last becomes the first
			Collections.reverse(visits);
			for (List<Object> in : visits) {
				if(in.get(5) != null) {
					Double weight = (Double) in.get(5);
					return(weight);
				}
			}
		}
		return(ret);
	}

	private Double getHeight(List<List<Object>> visits) {
		Double ret = 0.00;
		if(visits != null) {
			// Flip the list -- the last becomes the first
			Collections.reverse(visits);
			for (List<Object> in : visits) {
				if(in.get(4) != null) {
					Double height = (Double) in.get(4);
					return(height);
				}
			}
		}
		return(ret);
	}

	private Double getUnscheduledRateLast5(List<List<Object>> visits) {
		Double ret = 0.00;
		if(visits != null) {
			Integer addition = 0;

			// Get last 5 visits
			List<List<Object>> workingList = new ArrayList<>();
			int size = visits.size();
			if (size > 5) {
				workingList.addAll(visits.subList(size - 5, size));
			} else {
				workingList.addAll(visits);
			}
			Integer divider = Math.max(workingList.size(), 1); // Avoid divide by zero

			for (List<Object> in : workingList) {
				String visitType = (String)in.get(3);
				if(visitType != null && visitType.trim().equalsIgnoreCase("unscheduled")) {
					addition++;
				}
			}
			ret = ((double)addition / (double)divider);
		}
		return(ret);
	}

	private Double getAverageTCALast5(List<Appointment> appointments) {
		Double ret = 0.00;
		if(appointments != null) {
			List<Appointment> workingList = new ArrayList<>();
			List<Appointment> holdingList = new ArrayList<>();
			// Apparently we should not consider the last encounter. No idea why.
			holdingList.addAll(appointments);
			if(holdingList.size() > 0) {
				holdingList.remove(holdingList.size() - 1);
			}
			int size = holdingList.size();
			if (size > 5) {
				workingList.addAll(holdingList.subList(size - 5, size));
			} else {
				workingList.addAll(holdingList);
			}
			Integer divider = workingList.size();
			if(divider > 0) {
				Integer totalDays = 0;
				for (Appointment in : workingList) {
					// Get the difference in days
					long differenceInMilliseconds = in.getAppointmentDate().getTime() - in.getEncounterDate().getTime();
					int differenceInDays = (int) (differenceInMilliseconds / (24 * 60 * 60 * 1000));
					totalDays += differenceInDays;
				}
				ret = ((double)totalDays / (double)divider);
			}
		}
		return(ret);
	}

	private Integer getNumHivRegimens(Set<Treatment> treatments) {
		Integer ret = 0;
		if(treatments != null) {
			Set<String> drugs = new HashSet<>(); // This will ensure we get unique drugs
			for (Treatment in : treatments) {
				System.err.println("IIT ML: got drug: " + in.getDrug() + " Treatment Type: " + in.getTreatmentType());
				if (in.getDrug() != null && in.getTreatmentType() != null && !in.getTreatmentType().trim().equalsIgnoreCase("Prophylaxis")) {
					String drug = in.getDrug().trim().toLowerCase();
					drugs.add(drug);
				}
			}
			ret = drugs.size();
		}
		return(ret);
	}

	private Double getAverageLatenessLast5(List<Integer> missed) {
		Double ret = 0.00;
		if(missed != null) {
			Integer addition = 0;
			Integer divider = 5;

			// Get last 5 missed
			List<Integer> workingList = new ArrayList<>();
			int size = missed.size();
			if (size > 5) {
				workingList.addAll(missed.subList(size - 5, size));
			} else {
				workingList.addAll(missed);
			}

			for (Integer in : workingList) {
				if(in > 0) {
					addition += in;
				}
			}
			ret = ((double)addition / (double)divider);
		}
		return(ret);
	}

	private Integer getLateLast5(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			List<Integer> workingList = new ArrayList<>();
			int size = missed.size();
			if (size > 5) {
				workingList.addAll(missed.subList(size - 5, size));
			} else {
				workingList.addAll(missed);
			}
			for (Integer in : workingList) {
				if (in >= 1) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Double getAverageLatenessLast10(List<Integer> missed) {
		Double ret = 0.00;
		if(missed != null) {
			Integer addition = 0;
			Integer divider = 10;

			// Get last 10 missed
			List<Integer> workingList = new ArrayList<>();
			int size = missed.size();
			if (size > 10) {
				workingList.addAll(missed.subList(size - 10, size));
			} else {
				workingList.addAll(missed);
			}

			for (Integer in : workingList) {
				if(in > 0) {
					addition += in;
				}
			}
			ret = ((double)addition / (double)divider);
		}
		return(ret);
	}

	private Double getAverageLatenessLast3(List<Integer> missed) {
		Double ret = 0.00;
		if(missed != null) {
			Integer addition = 0;
			Integer divider = 3;

			// Get last 3 missed
			List<Integer> workingList = new ArrayList<>();
			int size = missed.size();
			if (size > 3) {
				workingList.addAll(missed.subList(size - 3, size));
			} else {
				workingList.addAll(missed);
			}

			for (Integer in : workingList) {
				if(in > 0) {
					addition += in;
				}
			}
			ret = ((double)addition / (double)divider);
		}
		return(ret);
	}

	private Integer getNextAppointmentDate(List<Appointment> allAppts) {
		Integer ret = 0;
		if(allAppts != null) {
			int size = allAppts.size();
			if(size > 0) {
				// Get latest appointment
				Appointment last = allAppts.get(size - 1);
				Date lastNAD = last.getAppointmentDate();
				Date lastEncounterDate = last.getEncounterDate();
				// Get the difference in days
				long differenceInMilliseconds = lastNAD.getTime() - lastEncounterDate.getTime();
				int differenceInDays = (int) (differenceInMilliseconds / (24 * 60 * 60 * 1000));
				// Only positive integers
				ret = Math.max(differenceInDays, 0);
			}
		}
		return(ret);
	}

	private Integer getLateLast3(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			List<Integer> workingList = new ArrayList<>();
			int size = missed.size();
			if (size > 3) {
				workingList.addAll(missed.subList(size - 3, size));
			} else {
				workingList.addAll(missed);
			}
			for (Integer in : workingList) {
				if (in >= 1) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getLateLast10(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			List<Integer> workingList = new ArrayList<>();
			int size = missed.size();
			if (size > 10) {
				workingList.addAll(missed.subList(size - 10, size));
			} else {
				workingList.addAll(missed);
			}
			for (Integer in : workingList) {
				if (in >= 1) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getVisit1(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			ret = size > 0 ? missed.get(size - 1) : 0;
		}
		return(ret);
	}

	private Integer getVisit2(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			ret = size > 1 ? missed.get(size - 2) : 0;
		}
		return(ret);
	}

	private Integer getVisit3(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			ret = size > 2 ? missed.get(size - 3) : 0;
		}
		return(ret);
	}

	private Integer getVisit4(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			ret = size > 3 ? missed.get(size - 4) : 0;
		}
		return(ret);
	}

	private Integer getVisit5(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			ret = size > 4 ? missed.get(size - 5) : 0;
		}
		return(ret);
	}

	private Double getLate28Rate(Integer late28, Integer n_appts) {
		Double ret = 0.00;
		if(late28 > 0 && n_appts > 0) {
			ret = ((double)late28 / (double)n_appts);
		}
		return(ret);
	}

	private Double getLateRate(Integer late, Integer n_appts) {
		Double ret = 0.00;
		if(late > 0 && n_appts > 0) {
			ret = ((double)late / (double)n_appts);
		}
		return(ret);
	}

	private Double getAverageLateness(List<Integer> missed, Integer divider) {
		Double ret = 0.00;
		if(missed != null && divider > 0.00) {
			Integer addition = 0;
			for (Integer in : missed) {
				if(in > 0) {
					addition += in;
				}
			}
			ret = ((double)addition / (double)divider);
		}
		return(ret);
	}

	private Integer getLate28(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			for (Integer in : missed) {
				if (in >= 28) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getMissed1(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			for (Integer in : missed) {
				if (in >= 1) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getMissed5(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			for (Integer in : missed) {
				if (in >= 5) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getMissed30(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			for (Integer in : missed) {
				if (in >= 30) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getMissed1Last5(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			List<Integer> subList = missed.subList(Math.max(size - 5, 0), size);
			for (Integer in : subList) {
				if (in >= 1) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getMissed5Last5(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			List<Integer> subList = missed.subList(Math.max(size - 5, 0), size);
			for (Integer in : subList) {
				if (in >= 5) {
					ret++;
				}
			}
		}
		return(ret);
	}

	private Integer getMissed30Last5(List<Integer> missed) {
		Integer ret = 0;
		if(missed != null) {
			int size = missed.size();
			List<Integer> subList = missed.subList(Math.max(size - 5, 0), size);
			for (Integer in : subList) {
				if (in >= 30) {
					ret++;
				}
			}
		}
		return(ret);
	}

	static List<Integer> calculateLateness(List<Appointment> records) {
		List<Integer> dateDifferences = new ArrayList<>();

		for (int i = 0; i < records.size() - 1; i++) {
			Appointment currentRecord = records.get(i);
			Appointment nextRecord = records.get(i + 1);

			long differenceInMilliseconds = nextRecord.getEncounterDate().getTime() - currentRecord.getAppointmentDate().getTime();
			int differenceInDays = (int) (differenceInMilliseconds / (24 * 60 * 60 * 1000));

			// If difference is less than zero, make it zero
			differenceInDays = Math.max(differenceInDays, 0);

			dateDifferences.add(differenceInDays);
		}

		return dateDifferences;
	}

	static List<Appointment> sortAppointmentsByEncounterDate(Set<Appointment> records) {
		List<Appointment> sortedRecords = new ArrayList<>(records);
		sortedRecords.sort(Comparator.comparing(Appointment::getEncounterDate));
		return sortedRecords;
	}

	static void processRecords(Set<Appointment> records) {
		Map<Date, Appointment> resultMap = new HashMap<>();

		for (Appointment record : records) {
			if (!resultMap.containsKey(record.getEncounterDate()) || record.getAppointmentDate().after(resultMap.get(record.getEncounterDate()).getAppointmentDate())) {
				resultMap.put(record.getEncounterDate(), record);
			}
		}

		records.clear();
		records.addAll(resultMap.values());
	}

	/**
	 *
	 * @param dateString
	 * @return
	 */
	public static LocalDateTime convertStringToDate(String dateString) {
		try {
			return LocalDateTime.parse(dateString);
		} catch (Exception e) {
			// Conversion failed, return null
			return null;
		}
	}
	// END variable calculations
}
