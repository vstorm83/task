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

package org.exoplatform.task.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import java.io.Serializable;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Table(name = "TASK_PROJECT_MANAGERS")
@NamedQueries({
  @NamedQuery(name = "Manager.getManager",
      query = "SELECT m.manager FROM Manager m WHERE m.project.id = :projectId")
})
public class Manager implements Serializable {

  private static final long serialVersionUID = -8500184494249475284L;

  @Id
  @ManyToOne
  @JoinColumn(name = "PROJECT_ID")
  private Project project;

  @Id
  private String    manager;

  public Manager(Project project, String manager) {
    this.project = project;
    this.manager = manager;
  }

  public Project getProject() {
    return project;
  }

  public String getManager() {
    return manager;
  }
}
