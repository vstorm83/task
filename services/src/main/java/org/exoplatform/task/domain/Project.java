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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>
 */
@Entity
@ExoEntity
@Table(name = "TASK_PROJECTS")
public class Project {

  public static final String PREFIX_CLONE = "Copy of ";

  @Id
  @SequenceGenerator(name="SEQ_TASK_PROJECTS_PROJECT_ID", sequenceName="SEQ_TASK_PROJECTS_PROJECT_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_TASK_PROJECTS_PROJECT_ID")
  @Column(name = "PROJECT_ID")
  private long      id;

  private String    name;

  private String    description;

  private String    color;
  
  @Column(name = "CALENDAR_INTEGRATED")
  private boolean calendarIntegrated = false;

  //TODO: should remove cascade ALL on this field
  @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
  private Set<Status> status = new HashSet<Status>();

  @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval = true)
  private Set<Manager> manager = new HashSet<Manager>();

  @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval = true)
  private Set<Participator> participator = new HashSet<Participator>();

  @Temporal(TemporalType.DATE)
  @Column(name = "DUE_DATE")
  private Date dueDate;

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_PROJECT_ID", nullable = true)
  private Project parent;

  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
  private List<Project> children = new LinkedList<Project>();

  // This field is used for remove cascade
  @ManyToMany(mappedBy = "hiddenProjects", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  private Set<UserSetting> hiddenOn = new HashSet<UserSetting>();

  public Project() {
  }

  public Project(String name, String description, Set<Status> status) {
    this.name = name;
    this.description = description;
    this.status = status;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  //TODO: get list status of project via StatusService
  @Deprecated
  public Set<Status> getStatus() {
    return status;
  }

  @Deprecated
  public void setStatus(Set<Status> status) {
    this.status = status;
  }

  public void setParticipator(Set<Participator> participator) {
    this.participator = participator;
  }

  public void setManager(Set<Manager> manager) {
    this.manager = manager;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getDueDate() {
    return dueDate;
  }

  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public boolean isCalendarIntegrated() {
    return calendarIntegrated;
  }

  public void setCalendarIntegrated(boolean calendarIntegrated) {
    this.calendarIntegrated = calendarIntegrated;
  }

  public Project getParent() {
    return parent;
  }

  public void setParent(Project parent) {
    this.parent = parent;
  }

  @Deprecated
  public List<Project> getChildren() {
    return children;
  }

  @Deprecated
  public void setChildren(List<Project> children) {
    this.children = children;
  }

  public Project clone(boolean cloneTask) {
    Project project = new Project(this.getName(), this.getDescription(), new HashSet<Status>());

    project.setId(getId());
    project.setColor(this.getColor());
    project.setDueDate(this.getDueDate());
    if (this.getParent() != null) {
      project.setParent(getParent().clone(false));
    }
    project.setCalendarIntegrated(isCalendarIntegrated());

    //
    project.status = new HashSet<Status>();
    project.children = new LinkedList<Project>();

    return project;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (getId() ^ (getId() >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Project))
      return false;
    Project other = (Project) obj;
    if (getId() != other.getId())
      return false;
    return true;
  }

}
