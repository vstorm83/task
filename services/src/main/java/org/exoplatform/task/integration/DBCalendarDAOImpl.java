/**
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 **/
  
package org.exoplatform.task.integration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.exoplatform.calendar.service.AbstractCalendarDAO;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarDAO;
import org.exoplatform.calendar.service.CalendarQuery;
import org.exoplatform.calendar.service.Utils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.ProjectHandler;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.service.TaskService;

public class DBCalendarDAOImpl extends AbstractCalendarDAO {

  private static final Set<Integer> TYPES = new HashSet<Integer>();
  static {
    TYPES.add(DBCalendar.TYPE_DB);
  }
  
  private ProjectHandler projectHandler;

  public DBCalendarDAOImpl(TaskService taskService) {
    this.projectHandler = taskService.getProjectHandler();
  }

  @Override
  public Set<Integer> getCalendarTypes() {
    return TYPES;
  }

  @Override
  public Calendar getCalendarById(String calId) {
    Project project = projectHandler.find(Long.valueOf(calId));
    Calendar cal = newCalendarInstance(DBCalendar.TYPE_DB);
    cal = setCalProperties(cal, project);
    return cal;
  }

  @Override
  public Calendar getCalendarById(String calId, int calType) {
    return getCalendarById(calId);
  }

  @Override
  public ListAccess<Calendar> findCalendarsByQuery(CalendarQuery query) {
    final List<Calendar> calendars = new LinkedList<Calendar>();
    int size = 0;
    
    if (query == null || query.getCalType() == Calendar.TYPE_ALL || query.getCalType() == DBCalendar.TYPE_DB) {
      //TODO: make project handler findByQuery function
      List<Project> project = projectHandler.findAll();
      size = project.size();
      for (Project p : project) {
        Calendar cal = newCalendarInstance(DBCalendar.TYPE_DB);
        calendars.add(setCalProperties(cal, p));
      }
    }
    
    final int s = size;
    return new ListAccess<Calendar>() {
      @Override
      public Calendar[] load(int index, int length) throws Exception, IllegalArgumentException {
        return Utils.subList(calendars, index, length).toArray(new Calendar[calendars.size()]);
      }

      @Override
      public int getSize() throws Exception {
        return s;
      }
    };
  }

  @Override
  public Calendar saveCalendar(Calendar calendar, boolean isNew) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeCalendar(String calendarId, int calType) {
    throw new UnsupportedOperationException();
  }

  private Calendar setCalProperties(Calendar cal, Project project) {
    cal.setId(String.valueOf(project.getId()));
    cal.setCalendarColor(project.getColor());
    cal.setCalType(DBCalendar.TYPE_DB);
    cal.setDescription(project.getDescription());
    cal.setEditPermission(null);
    cal.setLastModified(Long.MAX_VALUE);
    cal.setName(project.getName());
    cal.setViewPermission(null);
    return cal;
  }
  
  @Override
  public Calendar newCalendarInstance(int type) {
    return new DBCalendar();
  }
}
