/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemrml.iit;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Patient;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * A model for IIT - interruption in treatment score for a patient IIT evaluates the possibility of
 * a patient to have interruption in ARV treatment based on a number of factors It is a model class.
 * It should extend either {@link BaseOpenmrsObject} or {@link BaseOpenmrsMetadata}.
 */
public class PatientRiskScore extends BaseOpenmrsData implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Integer id;
	
	private Patient patient;
	
	private String sourceSystemUuid;
	
	private Double riskScore;
	
	private Date evaluationDate;

	private String description;

	private String riskFactors;

	private String payload;

	private String Age;
    private String births;
    private String pregnancies;
    private String literacy;
    private String poverty;
    private String anc;
    private String pnc;
    private String sba;
    private String hiv_prev;
    private String hiv_count;
    private String condom;
    private String intercourse;
    private String in_union;
    private String circumcision;
    private String partner_away;
    private String partner_men;
    private String partner_women;
    private String sti;
    private String fb;
    private String n_appts;
    private String missed1;
    private String missed5;
    private String missed30;
    private String missed1_last5;
    private String missed5_last5;
    private String missed30_last5;
    private String num_hiv_regimens;
    private String n_visits_lastfive;
    private String n_unscheduled_lastfive;
    private String BMI;
    private String changeInBMI;
    private String Weight;
    private String changeInWeight;
    private String num_adherence_ART;
    private String num_adherence_CTX;
    private String num_poor_ART;
    private String num_poor_CTX;
    private String num_fair_ART;
    private String num_fair_CTX;
    private String n_tests_all;
    private String n_hvl_all;
    private String n_tests_threeyears;
    private String n_hvl_threeyears;
    private String timeOnArt;
    private String AgeARTStart;
    private String recent_hvl_rate;
    private String total_hvl_rate;
    private String art_poor_adherence_rate;
    private String art_fair_adherence_rate;
    private String ctx_poor_adherence_rate;
    private String ctx_fair_adherence_rate;
    private String unscheduled_rate;
    private String all_late30_rate;
    private String all_late5_rate;
    private String all_late1_rate;
    private String recent_late30_rate;
    private String recent_late5_rate;
    private String recent_late1_rate;
    private String GenderMale;
    private String GenderFemale;
    private String PatientSourceCCC;
    private String PatientSourceIPDAdult;
    private String PatientSourceMCH;
    private String PatientSourceOPD;
    private String PatientSourceOther;
    private String PatientSourceTBClinic;
    private String PatientSourceVCT;
    private String MaritalStatusDivorced;
    private String MaritalStatusMarried;
    private String MaritalStatusOther;
    private String MaritalStatusPolygamous;
    private String MaritalStatusSingle;
    private String MaritalStatusWidow;
    private String PopulationTypeGeneralPopulation;
    private String PopulationTypeKeyPopulation;
    private String PopulationTypePriorityPopulation;
    private String TreatmentTypeART;
    private String TreatmentTypePMTCT;
    private String OptimizedHIVRegimenNo;
    private String OptimizedHIVRegimenYes;
    private String Other_RegimenNo;
    private String Other_RegimenYes;
    private String PregnantNo;
    private String PregnantYes;
    private String PregnantNR;
    private String DifferentiatedCareCommunityARTDistributionHCWLed;
    private String DifferentiatedCareCommunityARTDistributionpeerled;
    private String DifferentiatedCareFacilityARTdistributionGroup;
    private String DifferentiatedCareFastTrack;
    private String DifferentiatedCareStandardCare;
    private String most_recent_art_adherencefair;
    private String most_recent_art_adherencegood;
    private String most_recent_art_adherencepoor;
    private String most_recent_ctx_adherencefair;
    private String most_recent_ctx_adherencegood;
    private String most_recent_ctx_adherencepoor;
    private String StabilityAssessmentStable;
    private String StabilityAssessmentUnstable;
    private String most_recent_vlHVL;
    private String most_recent_vlLVL;
    private String most_recent_vlSuppressed;
    private String label;

	private String mflCode;
	private String cccNumber;
	
	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(final Integer id) {
		this.id = id;
	}
	
	public Patient getPatient() {
		return patient;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	public String getSourceSystemUuid() {
		return sourceSystemUuid;
	}
	
	public void setSourceSystemUuid(String sourceSystemUuid) {
		this.sourceSystemUuid = sourceSystemUuid;
	}
	
	public Double getRiskScore() {
		return riskScore;
	}
	
	public void setRiskScore(Double riskScore) {
		this.riskScore = riskScore;
	}
	
	public Date getEvaluationDate() {
		return evaluationDate;
	}
	
	public void setEvaluationDate(Date evaluationDate) {
		this.evaluationDate = evaluationDate;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRiskFactors() {
		return riskFactors;
	}

	public void setRiskFactors(String riskFactors) {
		this.riskFactors = riskFactors;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getMflCode() {
		return mflCode;
	}

	public void setMflCode(String mflCode) {
		this.mflCode = mflCode;
	}

	public String getCccNumber() {
		return cccNumber;
	}

	public void setCccNumber(String cccNumber) {
		this.cccNumber = cccNumber;
	}

	public String getAge() {
		return Age;
	}

	public void setAge(String age) {
		Age = age;
	}

	public String getBirths() {
		return births;
	}

	public void setBirths(String births) {
		this.births = births;
	}

	public String getPregnancies() {
		return pregnancies;
	}

	public void setPregnancies(String pregnancies) {
		this.pregnancies = pregnancies;
	}

	public String getLiteracy() {
		return literacy;
	}

	public void setLiteracy(String literacy) {
		this.literacy = literacy;
	}

	public String getPoverty() {
		return poverty;
	}

	public void setPoverty(String poverty) {
		this.poverty = poverty;
	}

	public String getAnc() {
		return anc;
	}

	public void setAnc(String anc) {
		this.anc = anc;
	}

	public String getPnc() {
		return pnc;
	}

	public void setPnc(String pnc) {
		this.pnc = pnc;
	}

	public String getSba() {
		return sba;
	}

	public void setSba(String sba) {
		this.sba = sba;
	}

	public String getHiv_prev() {
		return hiv_prev;
	}

	public void setHiv_prev(String hiv_prev) {
		this.hiv_prev = hiv_prev;
	}

	public String getHiv_count() {
		return hiv_count;
	}

	public void setHiv_count(String hiv_count) {
		this.hiv_count = hiv_count;
	}

	public String getCondom() {
		return condom;
	}

	public void setCondom(String condom) {
		this.condom = condom;
	}

	public String getIntercourse() {
		return intercourse;
	}

	public void setIntercourse(String intercourse) {
		this.intercourse = intercourse;
	}

	public String getIn_union() {
		return in_union;
	}

	public void setIn_union(String in_union) {
		this.in_union = in_union;
	}

	public String getCircumcision() {
		return circumcision;
	}

	public void setCircumcision(String circumcision) {
		this.circumcision = circumcision;
	}

	public String getPartner_away() {
		return partner_away;
	}

	public void setPartner_away(String partner_away) {
		this.partner_away = partner_away;
	}

	public String getPartner_men() {
		return partner_men;
	}

	public void setPartner_men(String partner_men) {
		this.partner_men = partner_men;
	}

	public String getPartner_women() {
		return partner_women;
	}

	public void setPartner_women(String partner_women) {
		this.partner_women = partner_women;
	}

	public String getSti() {
		return sti;
	}

	public void setSti(String sti) {
		this.sti = sti;
	}

	public String getFb() {
		return fb;
	}

	public void setFb(String fb) {
		this.fb = fb;
	}

	public String getN_appts() {
		return n_appts;
	}

	public void setN_appts(String n_appts) {
		this.n_appts = n_appts;
	}

	public String getMissed1() {
		return missed1;
	}

	public void setMissed1(String missed1) {
		this.missed1 = missed1;
	}

	public String getMissed5() {
		return missed5;
	}

	public void setMissed5(String missed5) {
		this.missed5 = missed5;
	}

	public String getMissed30() {
		return missed30;
	}

	public void setMissed30(String missed30) {
		this.missed30 = missed30;
	}

	public String getMissed1_last5() {
		return missed1_last5;
	}

	public void setMissed1_last5(String missed1_last5) {
		this.missed1_last5 = missed1_last5;
	}

	public String getMissed5_last5() {
		return missed5_last5;
	}

	public void setMissed5_last5(String missed5_last5) {
		this.missed5_last5 = missed5_last5;
	}

	public String getMissed30_last5() {
		return missed30_last5;
	}

	public void setMissed30_last5(String missed30_last5) {
		this.missed30_last5 = missed30_last5;
	}

	public String getNum_hiv_regimens() {
		return num_hiv_regimens;
	}

	public void setNum_hiv_regimens(String num_hiv_regimens) {
		this.num_hiv_regimens = num_hiv_regimens;
	}

	public String getN_visits_lastfive() {
		return n_visits_lastfive;
	}

	public void setN_visits_lastfive(String n_visits_lastfive) {
		this.n_visits_lastfive = n_visits_lastfive;
	}

	public String getN_unscheduled_lastfive() {
		return n_unscheduled_lastfive;
	}

	public void setN_unscheduled_lastfive(String n_unscheduled_lastfive) {
		this.n_unscheduled_lastfive = n_unscheduled_lastfive;
	}

	public String getBMI() {
		return BMI;
	}

	public void setBMI(String bMI) {
		BMI = bMI;
	}

	public String getChangeInBMI() {
		return changeInBMI;
	}

	public void setChangeInBMI(String changeInBMI) {
		this.changeInBMI = changeInBMI;
	}

	public String getWeight() {
		return Weight;
	}

	public void setWeight(String weight) {
		Weight = weight;
	}

	public String getChangeInWeight() {
		return changeInWeight;
	}

	public void setChangeInWeight(String changeInWeight) {
		this.changeInWeight = changeInWeight;
	}

	public String getNum_adherence_ART() {
		return num_adherence_ART;
	}

	public void setNum_adherence_ART(String num_adherence_ART) {
		this.num_adherence_ART = num_adherence_ART;
	}

	public String getNum_adherence_CTX() {
		return num_adherence_CTX;
	}

	public void setNum_adherence_CTX(String num_adherence_CTX) {
		this.num_adherence_CTX = num_adherence_CTX;
	}

	public String getNum_poor_ART() {
		return num_poor_ART;
	}

	public void setNum_poor_ART(String num_poor_ART) {
		this.num_poor_ART = num_poor_ART;
	}

	public String getNum_poor_CTX() {
		return num_poor_CTX;
	}

	public void setNum_poor_CTX(String num_poor_CTX) {
		this.num_poor_CTX = num_poor_CTX;
	}

	public String getNum_fair_ART() {
		return num_fair_ART;
	}

	public void setNum_fair_ART(String num_fair_ART) {
		this.num_fair_ART = num_fair_ART;
	}

	public String getNum_fair_CTX() {
		return num_fair_CTX;
	}

	public void setNum_fair_CTX(String num_fair_CTX) {
		this.num_fair_CTX = num_fair_CTX;
	}

	public String getN_tests_all() {
		return n_tests_all;
	}

	public void setN_tests_all(String n_tests_all) {
		this.n_tests_all = n_tests_all;
	}

	public String getN_hvl_all() {
		return n_hvl_all;
	}

	public void setN_hvl_all(String n_hvl_all) {
		this.n_hvl_all = n_hvl_all;
	}

	public String getN_tests_threeyears() {
		return n_tests_threeyears;
	}

	public void setN_tests_threeyears(String n_tests_threeyears) {
		this.n_tests_threeyears = n_tests_threeyears;
	}

	public String getN_hvl_threeyears() {
		return n_hvl_threeyears;
	}

	public void setN_hvl_threeyears(String n_hvl_threeyears) {
		this.n_hvl_threeyears = n_hvl_threeyears;
	}

	public String getTimeOnArt() {
		return timeOnArt;
	}

	public void setTimeOnArt(String timeOnArt) {
		this.timeOnArt = timeOnArt;
	}

	public String getAgeARTStart() {
		return AgeARTStart;
	}

	public void setAgeARTStart(String ageARTStart) {
		AgeARTStart = ageARTStart;
	}

	public String getRecent_hvl_rate() {
		return recent_hvl_rate;
	}

	public void setRecent_hvl_rate(String recent_hvl_rate) {
		this.recent_hvl_rate = recent_hvl_rate;
	}

	public String getTotal_hvl_rate() {
		return total_hvl_rate;
	}

	public void setTotal_hvl_rate(String total_hvl_rate) {
		this.total_hvl_rate = total_hvl_rate;
	}

	public String getArt_poor_adherence_rate() {
		return art_poor_adherence_rate;
	}

	public void setArt_poor_adherence_rate(String art_poor_adherence_rate) {
		this.art_poor_adherence_rate = art_poor_adherence_rate;
	}

	public String getArt_fair_adherence_rate() {
		return art_fair_adherence_rate;
	}

	public void setArt_fair_adherence_rate(String art_fair_adherence_rate) {
		this.art_fair_adherence_rate = art_fair_adherence_rate;
	}

	public String getCtx_poor_adherence_rate() {
		return ctx_poor_adherence_rate;
	}

	public void setCtx_poor_adherence_rate(String ctx_poor_adherence_rate) {
		this.ctx_poor_adherence_rate = ctx_poor_adherence_rate;
	}

	public String getCtx_fair_adherence_rate() {
		return ctx_fair_adherence_rate;
	}

	public void setCtx_fair_adherence_rate(String ctx_fair_adherence_rate) {
		this.ctx_fair_adherence_rate = ctx_fair_adherence_rate;
	}

	public String getUnscheduled_rate() {
		return unscheduled_rate;
	}

	public void setUnscheduled_rate(String unscheduled_rate) {
		this.unscheduled_rate = unscheduled_rate;
	}

	public String getAll_late30_rate() {
		return all_late30_rate;
	}

	public void setAll_late30_rate(String all_late30_rate) {
		this.all_late30_rate = all_late30_rate;
	}

	public String getAll_late5_rate() {
		return all_late5_rate;
	}

	public void setAll_late5_rate(String all_late5_rate) {
		this.all_late5_rate = all_late5_rate;
	}

	public String getAll_late1_rate() {
		return all_late1_rate;
	}

	public void setAll_late1_rate(String all_late1_rate) {
		this.all_late1_rate = all_late1_rate;
	}

	public String getRecent_late30_rate() {
		return recent_late30_rate;
	}

	public void setRecent_late30_rate(String recent_late30_rate) {
		this.recent_late30_rate = recent_late30_rate;
	}

	public String getRecent_late5_rate() {
		return recent_late5_rate;
	}

	public void setRecent_late5_rate(String recent_late5_rate) {
		this.recent_late5_rate = recent_late5_rate;
	}

	public String getRecent_late1_rate() {
		return recent_late1_rate;
	}

	public void setRecent_late1_rate(String recent_late1_rate) {
		this.recent_late1_rate = recent_late1_rate;
	}

	public String getGenderMale() {
		return GenderMale;
	}

	public void setGenderMale(String genderMale) {
		GenderMale = genderMale;
	}

	public String getGenderFemale() {
		return GenderFemale;
	}

	public void setGenderFemale(String genderFemale) {
		GenderFemale = genderFemale;
	}

	public String getPatientSourceCCC() {
		return PatientSourceCCC;
	}

	public void setPatientSourceCCC(String patientSourceCCC) {
		PatientSourceCCC = patientSourceCCC;
	}

	public String getPatientSourceIPDAdult() {
		return PatientSourceIPDAdult;
	}

	public void setPatientSourceIPDAdult(String patientSourceIPDAdult) {
		PatientSourceIPDAdult = patientSourceIPDAdult;
	}

	public String getPatientSourceMCH() {
		return PatientSourceMCH;
	}

	public void setPatientSourceMCH(String patientSourceMCH) {
		PatientSourceMCH = patientSourceMCH;
	}

	public String getPatientSourceOPD() {
		return PatientSourceOPD;
	}

	public void setPatientSourceOPD(String patientSourceOPD) {
		PatientSourceOPD = patientSourceOPD;
	}

	public String getPatientSourceOther() {
		return PatientSourceOther;
	}

	public void setPatientSourceOther(String patientSourceOther) {
		PatientSourceOther = patientSourceOther;
	}

	public String getPatientSourceTBClinic() {
		return PatientSourceTBClinic;
	}

	public void setPatientSourceTBClinic(String patientSourceTBClinic) {
		PatientSourceTBClinic = patientSourceTBClinic;
	}

	public String getPatientSourceVCT() {
		return PatientSourceVCT;
	}

	public void setPatientSourceVCT(String patientSourceVCT) {
		PatientSourceVCT = patientSourceVCT;
	}

	public String getMaritalStatusDivorced() {
		return MaritalStatusDivorced;
	}

	public void setMaritalStatusDivorced(String maritalStatusDivorced) {
		MaritalStatusDivorced = maritalStatusDivorced;
	}

	public String getMaritalStatusMarried() {
		return MaritalStatusMarried;
	}

	public void setMaritalStatusMarried(String maritalStatusMarried) {
		MaritalStatusMarried = maritalStatusMarried;
	}

	public String getMaritalStatusOther() {
		return MaritalStatusOther;
	}

	public void setMaritalStatusOther(String maritalStatusOther) {
		MaritalStatusOther = maritalStatusOther;
	}

	public String getMaritalStatusPolygamous() {
		return MaritalStatusPolygamous;
	}

	public void setMaritalStatusPolygamous(String maritalStatusPolygamous) {
		MaritalStatusPolygamous = maritalStatusPolygamous;
	}

	public String getMaritalStatusSingle() {
		return MaritalStatusSingle;
	}

	public void setMaritalStatusSingle(String maritalStatusSingle) {
		MaritalStatusSingle = maritalStatusSingle;
	}

	public String getMaritalStatusWidow() {
		return MaritalStatusWidow;
	}

	public void setMaritalStatusWidow(String maritalStatusWidow) {
		MaritalStatusWidow = maritalStatusWidow;
	}

	public String getPopulationTypeGeneralPopulation() {
		return PopulationTypeGeneralPopulation;
	}

	public void setPopulationTypeGeneralPopulation(String populationTypeGeneralPopulation) {
		PopulationTypeGeneralPopulation = populationTypeGeneralPopulation;
	}

	public String getPopulationTypeKeyPopulation() {
		return PopulationTypeKeyPopulation;
	}

	public void setPopulationTypeKeyPopulation(String populationTypeKeyPopulation) {
		PopulationTypeKeyPopulation = populationTypeKeyPopulation;
	}

	public String getPopulationTypePriorityPopulation() {
		return PopulationTypePriorityPopulation;
	}

	public void setPopulationTypePriorityPopulation(String populationTypePriorityPopulation) {
		PopulationTypePriorityPopulation = populationTypePriorityPopulation;
	}

	public String getTreatmentTypeART() {
		return TreatmentTypeART;
	}

	public void setTreatmentTypeART(String treatmentTypeART) {
		TreatmentTypeART = treatmentTypeART;
	}

	public String getTreatmentTypePMTCT() {
		return TreatmentTypePMTCT;
	}

	public void setTreatmentTypePMTCT(String treatmentTypePMTCT) {
		TreatmentTypePMTCT = treatmentTypePMTCT;
	}

	public String getOptimizedHIVRegimenNo() {
		return OptimizedHIVRegimenNo;
	}

	public void setOptimizedHIVRegimenNo(String optimizedHIVRegimenNo) {
		OptimizedHIVRegimenNo = optimizedHIVRegimenNo;
	}

	public String getOptimizedHIVRegimenYes() {
		return OptimizedHIVRegimenYes;
	}

	public void setOptimizedHIVRegimenYes(String optimizedHIVRegimenYes) {
		OptimizedHIVRegimenYes = optimizedHIVRegimenYes;
	}

	public String getOther_RegimenNo() {
		return Other_RegimenNo;
	}

	public void setOther_RegimenNo(String other_RegimenNo) {
		Other_RegimenNo = other_RegimenNo;
	}

	public String getOther_RegimenYes() {
		return Other_RegimenYes;
	}

	public void setOther_RegimenYes(String other_RegimenYes) {
		Other_RegimenYes = other_RegimenYes;
	}

	public String getPregnantNo() {
		return PregnantNo;
	}

	public void setPregnantNo(String pregnantNo) {
		PregnantNo = pregnantNo;
	}

	public String getPregnantYes() {
		return PregnantYes;
	}

	public void setPregnantYes(String pregnantYes) {
		PregnantYes = pregnantYes;
	}

	public String getPregnantNR() {
		return PregnantNR;
	}

	public void setPregnantNR(String pregnantNR) {
		PregnantNR = pregnantNR;
	}

	public String getDifferentiatedCareCommunityARTDistributionHCWLed() {
		return DifferentiatedCareCommunityARTDistributionHCWLed;
	}

	public void setDifferentiatedCareCommunityARTDistributionHCWLed(
			String differentiatedCareCommunityARTDistributionHCWLed) {
		DifferentiatedCareCommunityARTDistributionHCWLed = differentiatedCareCommunityARTDistributionHCWLed;
	}

	public String getDifferentiatedCareCommunityARTDistributionpeerled() {
		return DifferentiatedCareCommunityARTDistributionpeerled;
	}

	public void setDifferentiatedCareCommunityARTDistributionpeerled(
			String differentiatedCareCommunityARTDistributionpeerled) {
		DifferentiatedCareCommunityARTDistributionpeerled = differentiatedCareCommunityARTDistributionpeerled;
	}

	public String getDifferentiatedCareFacilityARTdistributionGroup() {
		return DifferentiatedCareFacilityARTdistributionGroup;
	}

	public void setDifferentiatedCareFacilityARTdistributionGroup(String differentiatedCareFacilityARTdistributionGroup) {
		DifferentiatedCareFacilityARTdistributionGroup = differentiatedCareFacilityARTdistributionGroup;
	}

	public String getDifferentiatedCareFastTrack() {
		return DifferentiatedCareFastTrack;
	}

	public void setDifferentiatedCareFastTrack(String differentiatedCareFastTrack) {
		DifferentiatedCareFastTrack = differentiatedCareFastTrack;
	}

	public String getDifferentiatedCareStandardCare() {
		return DifferentiatedCareStandardCare;
	}

	public void setDifferentiatedCareStandardCare(String differentiatedCareStandardCare) {
		DifferentiatedCareStandardCare = differentiatedCareStandardCare;
	}

	public String getMost_recent_art_adherencefair() {
		return most_recent_art_adherencefair;
	}

	public void setMost_recent_art_adherencefair(String most_recent_art_adherencefair) {
		this.most_recent_art_adherencefair = most_recent_art_adherencefair;
	}

	public String getMost_recent_art_adherencegood() {
		return most_recent_art_adherencegood;
	}

	public void setMost_recent_art_adherencegood(String most_recent_art_adherencegood) {
		this.most_recent_art_adherencegood = most_recent_art_adherencegood;
	}

	public String getMost_recent_art_adherencepoor() {
		return most_recent_art_adherencepoor;
	}

	public void setMost_recent_art_adherencepoor(String most_recent_art_adherencepoor) {
		this.most_recent_art_adherencepoor = most_recent_art_adherencepoor;
	}

	public String getMost_recent_ctx_adherencefair() {
		return most_recent_ctx_adherencefair;
	}

	public void setMost_recent_ctx_adherencefair(String most_recent_ctx_adherencefair) {
		this.most_recent_ctx_adherencefair = most_recent_ctx_adherencefair;
	}

	public String getMost_recent_ctx_adherencegood() {
		return most_recent_ctx_adherencegood;
	}

	public void setMost_recent_ctx_adherencegood(String most_recent_ctx_adherencegood) {
		this.most_recent_ctx_adherencegood = most_recent_ctx_adherencegood;
	}

	public String getMost_recent_ctx_adherencepoor() {
		return most_recent_ctx_adherencepoor;
	}

	public void setMost_recent_ctx_adherencepoor(String most_recent_ctx_adherencepoor) {
		this.most_recent_ctx_adherencepoor = most_recent_ctx_adherencepoor;
	}

	public String getStabilityAssessmentStable() {
		return StabilityAssessmentStable;
	}

	public void setStabilityAssessmentStable(String stabilityAssessmentStable) {
		StabilityAssessmentStable = stabilityAssessmentStable;
	}

	public String getStabilityAssessmentUnstable() {
		return StabilityAssessmentUnstable;
	}

	public void setStabilityAssessmentUnstable(String stabilityAssessmentUnstable) {
		StabilityAssessmentUnstable = stabilityAssessmentUnstable;
	}

	public String getMost_recent_vlHVL() {
		return most_recent_vlHVL;
	}

	public void setMost_recent_vlHVL(String most_recent_vlHVL) {
		this.most_recent_vlHVL = most_recent_vlHVL;
	}

	public String getMost_recent_vlLVL() {
		return most_recent_vlLVL;
	}

	public void setMost_recent_vlLVL(String most_recent_vlLVL) {
		this.most_recent_vlLVL = most_recent_vlLVL;
	}

	public String getMost_recent_vlSuppressed() {
		return most_recent_vlSuppressed;
	}

	public void setMost_recent_vlSuppressed(String most_recent_vlSuppressed) {
		this.most_recent_vlSuppressed = most_recent_vlSuppressed;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		PatientRiskScore that = (PatientRiskScore) o;
		return id.equals(that.id) && patient.equals(that.patient) && sourceSystemUuid.equals(that.sourceSystemUuid)
		        && riskScore.equals(that.riskScore) && evaluationDate.equals(that.evaluationDate);
	}
	
	@Override
	public String toString() {
		return "PatientRiskScore [description=" + description + ", evaluationDate=" + evaluationDate + ", id=" + id
				+ ", patient=" + patient + ", riskFactors=" + riskFactors + ", riskScore=" + riskScore
				+ ", sourceSystemUuid=" + sourceSystemUuid + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, patient, sourceSystemUuid, riskScore, evaluationDate);
	}
}
