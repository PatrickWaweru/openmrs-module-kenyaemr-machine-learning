/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrml.api.impl;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemrml.api.IITMLService;

public class IITMLServiceImpl extends BaseOpenmrsService implements IITMLService {
	
	private SessionFactory sessionFactory;
	private Evaluator evaluator;

	// Done only once
	public IITMLServiceImpl() {
		init();
	}
	
	@Override
	public void onStartup() {
		// Hold
		// init();
	}

	private void init() {
		try {
			if(!checkIfAlreadyLoaded()) {
				System.out.println("IIT ML: Model NOT loaded. Now loading");
				String modelId = "XGB_IIT_02232023";
				String fullModelZipFileName = modelId.concat(".pmml.zip");
				fullModelZipFileName = "iit/" + fullModelZipFileName;
				InputStream stream = IITMLServiceImpl.class.getClassLoader().getResourceAsStream(fullModelZipFileName);
				BufferedInputStream bistream = new BufferedInputStream(stream);
				// Model name
				String fullModelFileName = modelId.concat(".pmml");
				ZipInputStream zis = new ZipInputStream(bistream);
				ZipEntry ze = null;

				while ((ze = zis.getNextEntry()) != null) {
					System.out.println("IIT ML: Got entry: " + ze);
					if(ze.getName().trim().equalsIgnoreCase(fullModelFileName)) {
						// Building a model evaluator from a PMML file
						evaluator = new LoadingModelEvaluatorBuilder().load(zis).build();
						evaluator.verify();
						System.out.println("IIT ML: Created the IIT ML Evaluator. Should do this only once in a lifetime");
						break;
					}
				}
			} else {
				System.out.println("IIT ML: Model already loaded. Not loading again");
			}
		}
		catch (Exception e) {
			System.err.println("IIT ML. Init model error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the service is already loaded and avoids loading multiple times
	 * @return Boolean - true = loaded, false = not loaded
	 */
	private Boolean checkIfAlreadyLoaded() {
		Boolean ret = false;
		try {
			IITMLService iITMLService = Context.getService(IITMLService.class);
			if(iITMLService == null) {
				return(false);
			}
			Evaluator eval = iITMLService.getEvaluator();
			if(eval == null) {
				return(false);
			}
		} catch (Exception ex) {}
		return(ret);
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	@Override
	public Evaluator getEvaluator() {
		return evaluator;
	}

	@Override
	public void setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
	}
}
