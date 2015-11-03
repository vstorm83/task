/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.task.dao.jpa;

import javax.persistence.TypedQuery;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.task.dao.ManagerHandler;
import org.exoplatform.task.domain.Manager;

public class ManagerDAOImpl extends CommonJPADAO<Manager, Serializable> implements ManagerHandler {
  @Override
  public Set<String> getManager(long projectId) {
    TypedQuery<String> query = getEntityManager().createNamedQuery("Manager.getManager", String.class);
    query.setParameter("taskId", projectId);

    return new HashSet<String>(query.getResultList());
  }
}
