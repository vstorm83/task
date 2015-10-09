/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.task.dao.jpa;

import static org.exoplatform.task.dao.condition.Conditions.TASK_COWORKER;
import static org.exoplatform.task.dao.condition.Conditions.TASK_MANAGER;
import static org.exoplatform.task.dao.condition.Conditions.TASK_PARTICIPATOR;
import static org.exoplatform.task.dao.condition.Conditions.TASK_TAG;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.task.dao.OrderBy;
import org.exoplatform.task.dao.TaskHandler;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.dao.condition.SingleCondition;
import org.exoplatform.task.domain.Label;
import org.exoplatform.task.domain.Status;
import org.exoplatform.task.domain.Task;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 4/8/15
 */
public class TaskDAOImpl extends CommonJPADAO<Task, Long> implements TaskHandler {

  public TaskDAOImpl() {
  }

  @Override
  public void delete(Task entity) {
    EntityManager em = getEntityManager();
    Task task = em.find(Task.class, entity.getId());

    // Delete all task log relate to this task
    Query query = em.createNamedQuery("TaskChangeLog.removeChangeLogByTaskId");
    query.setParameter("taskId", entity.getId());
    query.executeUpdate();

    // Delete all comments of task
    query = em.createNamedQuery("Comment.deleteCommentOfTask");
    query.setParameter("taskId", entity.getId());
    query.executeUpdate();

    em.remove(task);
  }

  @Override
  public List<Task> findByUser(String user) {

    List<String> memberships = new ArrayList<String>();
    memberships.add(user);

    return  findAllByMembership(user, memberships);
  }

  public List<Task> findAllByMembership(String user, List<String> memberships) {

    Query query = getEntityManager().createNamedQuery("Task.findByMemberships", Task.class);
    query.setParameter("userName", user);
    query.setParameter("memberships", memberships);

    return cloneEntities(query.getResultList());
  }

  @Override
  public ListAccess<Task> findTasks(TaskQuery query) {
    return findEntities(query, Task.class);
  }

  @Override
  public <T> List<T> selectTaskField(TaskQuery query, String fieldName) {
    EntityManager em = getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery q = cb.createQuery();

    Root<Task> task = q.from(Task.class);

    //List<Predicate> predicates = this.buildPredicate(query, task, cb);
    Predicate predicate = this.buildQuery(query.getCondition(), task, cb, q);

    if(predicate != null) {
      q.where(predicate);
    }

    //
    Path path = null;
    if (fieldName.indexOf('.') != -1) {
      String[] strs = fieldName.split("\\.");
      Join join = null;
      for (int i = 0; i < strs.length - 1; i++) {
        String s = strs[i];
        if (join == null) {
          join = task.join(s);
        } else {
          join = join.join(s);
        }
      }
      path = join.get(strs[strs.length - 1]);
    } else {
      path = task.get(fieldName);
    }
    q.select(path).distinct(true);

    if(query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
      List<OrderBy> orderBies = query.getOrderBy();
      List<Order> orders = new ArrayList<Order>();
      for(OrderBy orderBy : orderBies) {
        if (!orderBy.getFieldName().equals(fieldName)) {
          continue;
        }
        Path p = task.get(orderBy.getFieldName());
        orders.add(orderBy.isAscending() ? cb.asc(p) : cb.desc(p));
      }
      if (!orders.isEmpty()) {
        q.orderBy(orders);
      }
    }

    final TypedQuery<T> selectQuery = em.createQuery(q);
    return cloneEntities(selectQuery.getResultList());
  }

  /*private List<Predicate> buildPredicate(TaskQuery query, Root<Task> task, CriteriaBuilder cb) {
    List<Predicate> predicates = new ArrayList<Predicate>();

    // Incoming?
    final String username = query.getUsername();
    if (query.getIsIncoming() != null && query.getIsIncoming()) {
      // Case query for IN-COMING tasks
      Join<Task, String> join = task.join("coworker", JoinType.LEFT);

      Predicate nullStatus = cb.isNull(task.get("status"));
      Predicate isAssingee = cb.equal(task.get("assignee"), username);
      Predicate isCreator = cb.equal(task.get("createdBy"), username);
      Predicate isCoworker = cb.equal(join, username);

      predicates.add(cb.and(nullStatus, cb.or(isAssingee, isCreator, isCoworker)));


    } else if (query.getIsTodo() != null && query.getIsTodo()) {
      // Case TO-DO tasks
      Predicate isAssignee = cb.equal(task.get("assignee"), username);
      predicates.add(isAssignee);
    }

    //.
    if(query.getTaskId() > 0) {
      predicates.add(cb.equal(task.get("id"), query.getTaskId()));
    }

    //
    if (query.getTitle() != null && !query.getTitle().isEmpty()) {
      predicates.add(cb.like(task.<String>get("title"), "%" + query.getTitle() + "%"));
    }

    //
    if (query.getDescription() != null && !query.getDescription().isEmpty()) {
      predicates.add(cb.like(task.<String>get("description"), '%' + query.getDescription() + '%'));
    }

<<<<<<< HEAD
    if (query.getLabelIds() != null && !query.getLabelIds().isEmpty()) {
      predicates.add(task.join("labels").get("id").in(query.getLabelIds()));
    }

    if (query.getTags() != null && !query.getTags().isEmpty()) {
      predicates.add(task.join("tag").in(query.getTags()));
    }

    if (query.getStatusId() == null) {
      predicates.add(cb.isNull(task.get("status")));
    } else if (query.getStatusId() != -1) {      
      predicates.add(cb.equal(task.get("status").get("id"), query.getStatusId()));
    }
    
    if (query.getPriority() != null) {
      predicates.add(cb.equal(task.get("priority"), query.getPriority()));
    }

=======
    //
    if(query.getKeyword() != null && !query.getKeyword().isEmpty()) {
      List<Predicate> keyConditions = new LinkedList<Predicate>();
      for (String k : query.getKeyword().split(" ")) {
        if (!(k = k.trim()).isEmpty()) {
          k = "%" + k.toLowerCase() + "%";
          keyConditions.add(cb.or(
                  cb.like(cb.lower(task.<String>get("title")), k),
                  cb.like(cb.lower(task.<String>get("description")), k),
                  cb.like(cb.lower(task.<String>get("assignee")), k)
          ));
        }
      }
      predicates.add(cb.or(keyConditions.toArray(new Predicate[keyConditions.size()])));
    }

    //
>>>>>>> 2d43c8a... Refactoring/Improvement of Task Management services
    Predicate assignPred = null;
    if (query.getAssignee() != null && !query.getAssignee().isEmpty()) {
      assignPred = cb.like(task.<String>get("assignee"), '%' + query.getAssignee() + '%');
    }
<<<<<<< HEAD
    
    Predicate createdByPred = null;
    if (query.getCreatedBy() != null && !query.getCreatedBy().isEmpty()) {
      createdByPred = cb.equal(task.<String>get("createdBy"), query.getCreatedBy());
    }
    
    Predicate coworkerPred = null;
    if (query.getCoworker() != null && !query.getCoworker().isEmpty()) {
      coworkerPred = cb.equal(task.join("coworker", JoinType.LEFT), query.getCoworker());
    }
    
=======

    //
>>>>>>> 2d43c8a... Refactoring/Improvement of Task Management services
    Predicate msPred = null;
    if (query.getMemberships() != null) {
<<<<<<< HEAD
      Subquery<Long> subm = q.subquery(Long.class);
      Root<Status> m = subm.from(Status.class);
      subm.select(m.<Long>get("id")).where(m.join("project").join("manager").in(query.getMemberships()));

      Subquery<Long> subp = q.subquery(Long.class);
      Root<Status> p = subp.from(Status.class);
      subp.select(p.<Long>get("id")).where(p.join("project").join("participator").in(query.getMemberships()));      
      
      msPred = cb.or(cb.in(task.get("status").get("id")).value(subm), cb.in(task.get("status").get("id")).value(subp));
    }

=======
      msPred = cb.or(task.join("status").join("project").join("manager", JoinType.LEFT).in(query.getMemberships()),
              task.join("status").join("project").join("participator", JoinType.LEFT).in(query.getMemberships()));
    }

    //
>>>>>>> Refactoring/Improvement of Task Management services
    Predicate projectPred = null;
    if (query.getProjectIds() != null) {
      if (query.getProjectIds().isEmpty()) {
        //TODO: How is this case?
        return null;
      } else if (query.getProjectIds().size() == 1 && query.getProjectIds().get(0) == 0) {
        projectPred = cb.isNotNull(task.get("status"));
      } else {
<<<<<<< HEAD
        Subquery<Long> subp = q.subquery(Long.class);
        Root<Status> p = subp.from(Status.class);
        subp.select(p.<Long>get("id")).where(p.join("project").get("id").in(query.getProjectIds()));
        
        projectPred = cb.in(task.get("status").get("id")).value(subp);
      }             
=======
        projectPred = task.get("status").get("project").get("id").in(query.getProjectIds());
      }
>>>>>>> Refactoring/Improvement of Task Management services
    }

    //
    List<Predicate> tmp = new LinkedList<Predicate>();
    for (String or : query.getOrFields()) {
      if (or.equals(TaskUtil.ASSIGNEE)) {
        tmp.add(assignPred);
      }
      if (or.equals(TaskUtil.MEMBERSHIP)) {
        tmp.add(msPred);
      }
      if (or.equals(TaskUtil.PROJECT)) {
        tmp.add(projectPred);
      }
      if (or.equals(TaskUtil.CREATED_BY)) {
        tmp.add(createdByPred);
      }
      if (or.equals(TaskUtil.COWORKER)) {
        tmp.add(coworkerPred);
      }
    }

    if (!tmp.isEmpty()) {
      predicates.add(cb.or(tmp.toArray(new Predicate[tmp.size()])));
    }

    if (!query.getOrFields().contains(TaskUtil.ASSIGNEE) && assignPred != null) {
      predicates.add(assignPred);
    }
    if (!query.getOrFields().contains(TaskUtil.MEMBERSHIP) && msPred != null) {
      predicates.add(msPred);
    }
    if (!query.getOrFields().contains(TaskUtil.PROJECT) && projectPred != null) {
<<<<<<< HEAD
      predicates.add(projectPred);      
    }
    if (!query.getOrFields().contains(TaskUtil.CREATED_BY) && createdByPred != null) {
      predicates.add(createdByPred);      
    }
    if (!query.getOrFields().contains(TaskUtil.COWORKER) && coworkerPred != null) {
      predicates.add(coworkerPred);      
    }

    if(query.getKeyword() != null && !query.getKeyword().isEmpty()) {      
      List<Predicate> keyConditions = new LinkedList<Predicate>();
      Join<Task, String> tagJoin = task.<Task, String>join("tag", JoinType.LEFT);
      
      for (String k : query.getKeyword().split(" ")) {
        if (!(k = k.trim()).isEmpty()) {
          k = "%" + k.toLowerCase() + "%";
          keyConditions.add(cb.or(
                                  cb.like(cb.lower(task.<String>get("title")), k),
                                  cb.like(cb.lower(task.<String>get("description")), k),
                                  cb.like(cb.lower(task.<String>get("assignee")), k),
                                  cb.like(cb.lower(tagJoin), k)
                              ));
        }
      }
      predicates.add(cb.or(keyConditions.toArray(new Predicate[keyConditions.size()])));
=======
      predicates.add(projectPred);
>>>>>>> Refactoring/Improvement of Task Management services
    }

    //
    if (query.getCompleted() != null) {
      predicates.add(cb.equal(task.get("completed"), query.getCompleted()));
    }

    //
    if (query.getCalendarIntegrated() != null) {
      predicates.add(cb.equal(task.get("calendarIntegrated"), query.getCalendarIntegrated()));
    }
    
    if (query.getDueDateFrom() != null) {
      predicates.add(cb.greaterThanOrEqualTo(task.<Date>get("dueDate"), new Date(query.getDueDateFrom())));
    }
<<<<<<< HEAD
    if (query.getDueDateTo() != null) {
      predicates.add(cb.lessThanOrEqualTo(task.<Date>get("dueDate"), new Date(query.getDueDateTo())));
    }    
    
=======

    //
>>>>>>> 2d43c8a... Refactoring/Improvement of Task Management services
    if (query.getStartDate() != null) {
      predicates.add(cb.greaterThanOrEqualTo(task.<Date>get("endDate"), query.getStartDate()));
    }
    if (query.getEndDate() != null) {
      predicates.add(cb.lessThanOrEqualTo(task.<Date>get("startDate"), query.getEndDate()));
    }

    //
    if (query.getDueDateFrom() != null || query.getDueDateTo() != null) {
      predicates.add(cb.isNotNull(task.get("dueDate")));
    }
    if (query.getDueDateFrom() != null) {
      predicates.add(cb.greaterThanOrEqualTo(task.<Date>get("dueDate"), query.getDueDateFrom()));
    }
    if (query.getDueDateTo() != null) {
      predicates.add(cb.lessThanOrEqualTo(task.<Date>get("dueDate"), query.getDueDateTo()));
    }

    //
    if (query.getStatus() != null) {
      predicates.add(cb.equal(task.get("status").<Long>get("id"), query.getStatus().getId()));
    }

    //
    if (query.getNullField() != null) {
      predicates.add(cb.isNull(task.get(query.getNullField())));
    }

    return predicates;
  }*/

  @Override
  public Task findTaskByActivityId(String activityId) {
    if (activityId == null || activityId.isEmpty()) {
      return null;
    }
    EntityManager em = getEntityManager();
    Query query = em.createNamedQuery("Task.findTaskByActivityId", Task.class);
    query.setParameter("activityId", activityId);
    try {
      return cloneEntity((Task) query.getSingleResult());
    } catch (PersistenceException e) {
      return null;
    }
  }

  @Override
  public void updateTaskOrder(long currentTaskId, Status newStatus, long[] orders) {
      int currentTaskIndex = -1;
      for (int i = 0; i < orders.length; i++) {
          if (orders[i] == currentTaskId) {
              currentTaskIndex = i;
              break;
          }
      }
      if (currentTaskIndex == -1) {
          return;
      }

      Task currentTask = find(currentTaskId);
      Task prevTask = null;
      Task nextTask = null;
      if (currentTaskIndex < orders.length - 1) {
          prevTask = find(orders[currentTaskIndex + 1]);
      }
      if (currentTaskIndex > 0) {
          nextTask = find(orders[currentTaskIndex - 1]);
      }

      int oldRank = currentTask.getRank();
      int prevRank = prevTask != null ? prevTask.getRank() : 0;
      int nextRank = nextTask != null ? nextTask.getRank() : 0;
      int newRank = prevRank + 1;
      if (newStatus != null && currentTask.getStatus().getId() != newStatus.getId()) {
          oldRank = 0;
          currentTask.setStatus(newStatus);
      }

      EntityManager em = getEntityManager();
      StringBuilder sql = null;

      if (newRank == 1 || oldRank == 0) {
          int increment = 1;
          StringBuilder exclude = new StringBuilder();
          if (nextRank == 0) {
              for (int i = currentTaskIndex - 1; i >= 0; i--) {
                  Task task = find(orders[i]);
                  if (task.getRank() > 0) {
                    break;
                  }
                  task.setRank(newRank + currentTaskIndex - i);
                  update(task);
                  if (exclude.length() > 0) {
                      exclude.append(',');
                  }
                  exclude.append(task.getId());
                  increment++;
              }
          }
          //Update rank of tasks have rank >= newRank with rank := rank + increment
          sql = new StringBuilder("UPDATE Task as ta SET ta.rank = ta.rank + ").append(increment)
                                .append(" WHERE ta.rank >= ").append(newRank);
          if (exclude.length() > 0) {
              sql.append(" AND ta.id NOT IN (").append(exclude.toString()).append(")");
          }

      } else if (oldRank < newRank) {
          //Update all task where oldRank < rank < newRank: rank = rank - 1
          sql = new StringBuilder("UPDATE Task as ta SET ta.rank = ta.rank - 1")
                                .append(" WHERE ta.rank > ").append(oldRank)
                                .append(" AND ta.rank < ").append(newRank);
          newRank --;
      } else if (oldRank > newRank) {
          //Update all task where newRank <= rank < oldRank: rank = rank + 1
          sql = new StringBuilder("UPDATE Task as ta SET ta.rank = ta.rank + 1")
                  .append(" WHERE ta.rank >= ").append(newRank)
                  .append(" AND ta.rank < ").append(oldRank);
          newRank ++;
      }

      if (sql != null && sql.length() > 0) {
          // Add common condition
          sql.append(" AND ta.completed = FALSE AND ta.status.id = ").append(currentTask.getStatus().getId());

          //TODO: This block code is temporary workaround because the update is require transaction
          EntityTransaction trans = em.getTransaction();
          boolean active = false;
          if (!trans.isActive()) {
            trans.begin();
            active = true;
          }

          em.createQuery(sql.toString()).executeUpdate();

          if (active) {
            trans.commit();
          }
      }
      currentTask.setRank(newRank);
      update(currentTask);
  }

  @Override
  public List<Task> findTasksByLabel(long labelId, List<Long> projectIds, String username, OrderBy orderBy) {
    EntityManager em = getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Task> query = cb.createQuery(Task.class);
    From task = query.from(Task.class);
    //
    Join<Task, Label> label = task.join("labels", JoinType.INNER);
    Predicate labelPred;
    if (labelId > 0) {
      labelPred = cb.equal(label.get("id"), labelId);
    } else {
      labelPred = cb.equal(label.get("username"), username);
    }
    //
    Predicate projectPred = null;
    if (projectIds != null && !projectIds.isEmpty()) {      
      projectPred = cb.in(task.join("status", JoinType.LEFT).get("project").get("id")).value(projectIds);
    }
    query.select(task).distinct(true);
    if (projectPred == null) {
      query.where(labelPred);
    } else {
      query.where(cb.and(labelPred, projectPred));      
    }

    if (orderBy != null) {
      Order order = orderBy.isAscending() ? cb.asc(task.get(orderBy.getFieldName())) : cb.desc(task.get(orderBy.getFieldName()));
      query.orderBy(order);
    }

    try {
      return em.createQuery(query).getResultList();
    } catch (PersistenceException e) {
      return Collections.emptyList();
    }
  }

  protected Path buildPath(SingleCondition condition, Root<Task> root) {
    String field = condition.getField();
    
    Join join = null;
    if (field.indexOf('.') > 0) {
      String[] arr = field.split("\\.");
      for (int i = 0; i < arr.length - 1; i++) {
        String s = arr[i];
        if (join == null) {
          join = root.join(s, JoinType.INNER);
        } else {
          join = join.join(s, JoinType.INNER);
        }
      }
      field = arr[arr.length - 1];
    }    

    Path path = join == null ? root.get(field) : join.get(field);
    
    if (TASK_COWORKER.equals(field)) {
      path = root.join(field, JoinType.LEFT);
    } else if (TASK_MANAGER.equals(condition.getField())) {
      path = join.join("manager", JoinType.LEFT);
    } else if (TASK_PARTICIPATOR.equals(condition.getField())) {
      path = join.join("participator", JoinType.LEFT);
    } else if (TASK_TAG.equals(condition.getField())) {
      path = root.join("tag", JoinType.INNER);
    }
    
    return path;
  }

  private static final ListAccess<Task> EMPTY = new ListAccess<Task>() {
    @Override
    public Task[] load(int index, int length) throws Exception, IllegalArgumentException {
      return new Task[0];
    }

    @Override
    public int getSize() throws Exception {
      return 0;
    }
  };
}

