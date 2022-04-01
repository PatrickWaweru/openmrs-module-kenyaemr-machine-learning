package org.openmrs.module.kenyaemrml.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemrml.api.MLinKenyaEMRService;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.openmrs.module.kenyaemrml.iit.PatientRiskScore;
import org.openmrs.User;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MLDataExchange {
	
	PersonService personService = Context.getPersonService();
	
	PatientService patientService = Context.getPatientService();
	
	MLinKenyaEMRService mLinKenyaEMRService = Context.getService(MLinKenyaEMRService.class);
	
	//OAuth variables
	private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
	
	private String strClientId = ""; // clientId
	
	private String strClientSecret = ""; // client secret
	
	private String strScope = ""; // scope
	
	private String strTokenUrl = ""; // Token URL
	
	private String strDWHbackEndURL = ""; // DWH backend URL
	
	private String strAuthURL = ""; // DWH auth URL
	
	private String strFacilityCode = ""; // Facility Code
	
	private final long recordsPerPull = 50; // Total number of records per request
	
	/**
	 * Initialize the OAuth variables
	 * 
	 * @return true on success or false on failure
	 */
	public boolean initAuthVars() {
		String dWHbackEndURL = "kenyaemr.iit.machine.learning.backend.url";
		GlobalProperty globalDWHbackEndURL = Context.getAdministrationService().getGlobalPropertyObject(dWHbackEndURL);
		strDWHbackEndURL = globalDWHbackEndURL.getPropertyValue();
		
		String tokenUrl = "kenyaemr.iit.machine.learning.token.url";
		GlobalProperty globalTokenUrl = Context.getAdministrationService().getGlobalPropertyObject(tokenUrl);
		strTokenUrl = globalTokenUrl.getPropertyValue();
		
		String scope = "kenyaemr.iit.machine.learning.scope";
		GlobalProperty globalScope = Context.getAdministrationService().getGlobalPropertyObject(scope);
		strScope = globalScope.getPropertyValue();
		
		String clientSecret = "kenyaemr.iit.machine.learning.client.secret";
		GlobalProperty globalClientSecret = Context.getAdministrationService().getGlobalPropertyObject(clientSecret);
		strClientSecret = globalClientSecret.getPropertyValue();
		
		String clientId = "kenyaemr.iit.machine.learning.client.id";
		GlobalProperty globalClientId = Context.getAdministrationService().getGlobalPropertyObject(clientId);
		strClientId = globalClientId.getPropertyValue();
		
		String authURL = "kenyaemr.iit.machine.learning.authorization.url";
		GlobalProperty globalAuthURL = Context.getAdministrationService().getGlobalPropertyObject(authURL);
		strAuthURL = globalAuthURL.getPropertyValue();
		
		String facilityCode = "facility.mflcode";
		GlobalProperty globalFacilityCode = Context.getAdministrationService().getGlobalPropertyObject(facilityCode);
		strFacilityCode = globalFacilityCode.getPropertyValue();
		
		if (strDWHbackEndURL == null || strTokenUrl == null || strScope == null || strClientSecret == null
		        || strClientId == null || strAuthURL == null) {
			System.err.println("ITT ML - get data: Please set DWH OAuth credentials");
			return (false);
		}
		return (true);
	}
	
	/**
	 * Get the Token
	 * 
	 * @return the token as a string and null on failure
	 */
	private String getClientCredentials() {
		
		String auth = strClientId + ":" + strClientSecret;
		String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
		BufferedReader reader = null;
		HttpsURLConnection connection = null;
		String returnValue = "";
		try {
			StringBuilder parameters = new StringBuilder();
			parameters.append("grant_type=" + URLEncoder.encode("client_credentials", "UTF-8"));
			parameters.append("&");
			parameters.append("scope=" + URLEncoder.encode(strScope, "UTF-8"));
			URL url = new URL(strTokenUrl);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Basic " + authentication);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Accept", "application/json");
			PrintStream os = new PrintStream(connection.getOutputStream());
			os.print(parameters);
			os.close();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line = null;
			StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
			while ((line = reader.readLine()) != null) {
				out.append(line);
			}
			String response = out.toString();
			Matcher matcher = pat.matcher(response);
			if (matcher.matches() && matcher.groupCount() > 0) {
				returnValue = matcher.group(1);
			} else {
				System.err.println("IIT ML - Error : Token pattern mismatch");
			}
			
		}
		catch (Exception e) {
			System.err.println("IIT ML - Error : " + e.getMessage());
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {}
			}
			connection.disconnect();
		}
		return returnValue;
	}
	
	/**
	 * Get the total records on remote side
	 * 
	 * @param bearerToken the OAuth2 token
	 * @return total number of records
	 */
	private long getTotalRecordsOnRemote(String bearerToken) {
		BufferedReader reader = null;
		HttpsURLConnection connection = null;
		try {
			URL url = new URL(strDWHbackEndURL + "?code=FND&name=predictions&pageNumber=1&pageSize=1&siteCode="
			        + strFacilityCode);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line = null;
			StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
			while ((line = reader.readLine()) != null) {
				out.append(line);
			}
			String response = out.toString();
			System.err.println("ITT ML - Total Remote Records JSON: " + response);
			
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode jsonNode = (ObjectNode) mapper.readTree(response);
			if (jsonNode != null) {
				long pageNumber = jsonNode.get("pageNumber").getLongValue();
				long pageSize = jsonNode.get("pageSize").getLongValue();
				long pageCount = jsonNode.get("pageCount").getLongValue();
				long totalItemCount = jsonNode.get("totalItemCount").getLongValue();
				
				System.err.println("ITT ML - Total Remote Records pageNumber: " + pageNumber);
				System.err.println("ITT ML - Total Remote Records pageSize: " + pageSize);
				System.err.println("ITT ML - Total Remote Records pageCount: " + pageCount);
				System.err.println("ITT ML - Total Remote Records totalItemCount: " + totalItemCount);
				
				return (pageCount);
			} else {
				return (0);
			}
		}
		catch (Exception e) {
			System.err.println("ITT ML - Error getting total remote records: " + e.getMessage());
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {}
			}
			connection.disconnect();
		}
		return (0);
	}
	
	/**
	 * Get the total records on local side
	 * 
	 * @return total number of records
	 */
	private long getTotalRecordsOnLocal() {
		Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		
		String strTotalCount = "select count(*) from kenyaemr_ml_patient_risk_score;";
		
		Long totalCount = (Long) Context.getAdministrationService().executeSQL(strTotalCount, true).get(0).get(0);
		
		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		return (totalCount);
	}
	
	/**
	 * Pulls records and saves locally
	 * 
	 * @param bearerToken the OAuth2 token
	 * @param totalRemote the total number of records in DWH
	 * @return true when successfull and false on failure
	 */
	private boolean pullAndSave(String bearerToken, long totalRemote) {
		
		long totalPages = (long) (Math.ceil((totalRemote * 1.0) / (recordsPerPull * 1.0)));
		System.err.println("ITT ML - Calculated Total Pages: " + totalPages);
		
		long currentPage = 1;
		for (int i = 0; i < totalPages; i++) {
			if (!getContinuePullingData()) {
				return (false);
			}
			BufferedReader reader = null;
			HttpsURLConnection connection = null;
			try {
				String fullURL = strDWHbackEndURL + "?code=FND&name=predictions&pageNumber=" + currentPage + "&pageSize="
				        + recordsPerPull + "&siteCode=" + strFacilityCode;
				System.err.println("ITT ML - Full URL: " + fullURL + " and page: " + currentPage);
				URL url = new URL(fullURL);
				connection = (HttpsURLConnection) url.openConnection();
				connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
				connection.setDoOutput(true);
				connection.setRequestMethod("GET");
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = null;
				StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
				while ((line = reader.readLine()) != null) {
					out.append(line);
				}
				String response = out.toString();
				System.err.println("ITT ML - Total Remote Records JSON: " + response);
				
				ObjectMapper mapper = new ObjectMapper();
				ObjectNode jsonNode = (ObjectNode) mapper.readTree(response);
				if (jsonNode != null) {
					long pageNumber = jsonNode.get("pageNumber").getLongValue();
					long pageSize = jsonNode.get("pageSize").getLongValue();
					long pageCount = jsonNode.get("pageCount").getLongValue();
					long totalItemCount = jsonNode.get("totalItemCount").getLongValue();
					
					System.err.println("ITT ML - Total Remote Records pageNumber: " + pageNumber);
					System.err.println("ITT ML - Total Remote Records pageSize: " + pageSize);
					System.err.println("ITT ML - Total Remote Records pageCount: " + pageCount);
					System.err.println("ITT ML - Total Remote Records totalItemCount: " + totalItemCount);
					
					JsonNode extract = jsonNode.get("extract");
					if (extract.isArray() && extract.size() > 0) {
						for (JsonNode person : extract) {
							if (!getContinuePullingData()) {
								return (false);
							}
							try {
								String riskScore = person.get("risk_score").asText();
								String uuid = person.get("id").asText();
								String patientId = person.get("PatientPID").asText();
								
								System.err.println("ITT ML - Risk Score: " + riskScore);
								System.err.println("ITT ML - UUID: " + uuid);
								System.err.println("ITT ML - PatientID: " + patientId);
								
								Patient patient = patientService.getPatient(Integer.valueOf(patientId));
								PatientRiskScore patientRiskScore = new PatientRiskScore();
								
								patientRiskScore.setRiskScore(Double.valueOf(riskScore));
								patientRiskScore.setSourceSystemUuid(uuid);
								patientRiskScore.setPatient(patient);
								
								mLinKenyaEMRService.saveOrUpdateRiskScore(patientRiskScore);
							}
							catch (Exception ex) {
								//Failed to save record
								System.err.println("ITT ML - Error getting and saving remote records: " + ex.getMessage());
							}
						}
					} else {
						System.err.println("ITT ML - JSON Data extraction problem. Exiting");
						if (reader != null) {
							try {
								reader.close();
							}
							catch (Exception ex) {}
						}
						if (reader != null) {
							try {
								connection.disconnect();
							}
							catch (Exception er) {}
						}
						return(false);
					}
				}
			}
			catch (Exception e) {
				System.err.println("ITT ML - Error getting and saving remote records: " + e.getMessage());
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					}
					catch (IOException e) {}
				}
				if (reader != null) {
					try {
						connection.disconnect();
					}
					catch (Exception er) {}
				}
			}
			try {
				System.err.println("ITT ML - Going to sleep for 1 seconds");
				Thread.sleep(1000);
			}
			catch (Exception ie) {
				Thread.currentThread().interrupt();
			}
			currentPage++;
		}
		return (true);
	}
	
	/**
	 * Fetches the data from data warehouse and saves it locally
	 * 
	 * @return true when successfull and false on failure
	 */
	public boolean fetchDataFromDWH() {
		// Init the auth vars
		boolean varsOk = initAuthVars();
		if (varsOk) {
			//Get the OAuth Token
			String credentials = getClientCredentials();
			//Get the data
			if (credentials != null) {
				System.err.println("ITT ML - Received a Token: " + credentials);
				//get total count by fetching only one record from remote
				long totalRemote = getTotalRecordsOnRemote(credentials);
				System.err.println("ITT ML - Total Remote Records: " + totalRemote);
				if (totalRemote > 0) {
					//We have remote records - get local total records
					long totalLocal = getTotalRecordsOnLocal();
					System.err.println("ITT ML - Total Local Records: " + totalLocal);
					//if remote records are greater than local, we pull
					if (totalRemote > totalLocal) {
						//We now pull and save
						pullAndSave(credentials, totalRemote);
						// pullThread pt = new pullThread(credentials, totalRemote);
						// pt.start();
					} else {
						System.err.println("ITT ML - Records are already in sync");
						return (false);
					}
				} else {
					System.err.println("ITT ML - No records on remote side");
					return (false);
				}
			} else {
				System.err.println("ITT ML - Failed to get the OAuth token");
				return (false);
			}
		} else {
			System.err.println("ITT ML - Failed to get the OAuth Vars");
			return (false);
		}
		return (true);
	}
	
	/**
	 * Enables or disables the pull data thread
	 * 
	 * @return false if pulling should be stopped and true if pull should continue
	 */
	public boolean getContinuePullingData() {
		//User user = Context.getUserService().getUser(Context.getUserContext().getAuthenticatedUser().getId());
		User user = Context.getUserContext().getAuthenticatedUser();
		if (user != null) {
			String stopIITMLPull = user.getUserProperty("stopIITMLPull");
			if (stopIITMLPull != null) {
				System.err.println("ITT ML - got the stop pull var as: " + stopIITMLPull);
				stopIITMLPull = stopIITMLPull.trim();
				if (stopIITMLPull.equalsIgnoreCase("0")) {
					return (true);
				} else if (stopIITMLPull.equalsIgnoreCase("1")) {
					return (false);
				}
			} else {
				System.err.println("ITT ML - Failed to get the stop pull var");
			}
		} else {
			//User has logged out, stop pulling
			System.err.println("ITT ML - User has logged out, stop the pull");
			return (false);
		}
		return (true);
	}
	
	class pullThread extends Thread {
		
		String token = "";
		
		long total = 0;
		
		public pullThread(String token, long total) {
			this.token = token;
			this.total = total;
		}
		
		public void run() {
			pullAndSave(token, total);
		}
	}
}
