/*
 * iDART: The Intelligent Dispensing of Antiretroviral Treatment
 * Copyright (C) 2006 Cell-Life
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version
 * 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.celllife.idart.database.hibernate.util;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class HibernateExecutor {

	private static final Logger log = Logger.getLogger(HibernateExecutor.class.getName());

	public static <T> T execute(HibernateCallback<T> callback) {
		return execute(callback, HibernateUtil.getNewSession(), true, true, true);
	}

	public static <T> T execute(HibernateCallback<T> callback, Session session,
			boolean beginTransaction, boolean flush, boolean closeSession) {
		Transaction tx = null;
		T result = null;
		try {
			if (beginTransaction) {
				tx = session.beginTransaction();
			}
			result = callback.doInHibernate(session);
			if (flush) {
				session.flush();
			}
			if (tx != null) {
				tx.commit();
			}
		} catch (Exception e) {
			log.error("Error executing hibernate callback", e);
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			if (closeSession) {
				session.close();
			}
		}
		return result;
	}

}
