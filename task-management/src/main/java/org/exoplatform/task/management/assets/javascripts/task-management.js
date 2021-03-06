// TODO: Move juzu-ajax, mentionsPlugin module into task management project if need
require(['SHARED/jquery', 'SHARED/edit_inline_js', 'SHARED/juzu-ajax', 'SHARED/mentionsPlugin', 'SHARED/bts_modal'], function(jQuery) {
jQuery(document).ready(function($) {
    var $taskManagement = $('#taskManagement');
    var $leftPanel = $('#taskManagement > .leftPanel');
    var $centerPanel = $('#taskManagement > .centerPanel');
    var $rightPanel = $('#taskManagement > .rightPanel');
    var $rightPanelContent = $rightPanel.find('.rightPanelContent');
    var $centerPanelContent = $centerPanel.find('.centerPanelContent');

    $.fn.editableform.buttons = '<button type="submit" class="btn btn-primary editable-submit"><i class="uiIconTick icon-white"></i></button>'+
        '<button type="button" class="btn editable-cancel"><i class="uiIconClose"></i></button>';

    var showRightPanel = function() {
        $centerPanel.removeClass('span10').addClass('span5');
        $rightPanel.show();
    };
    var hideRightPanel = function() {
        $rightPanelContent.html('');
        $rightPanel.hide();
        $centerPanel.removeClass('span5').addClass('span10');
    };

    var saveTaskDetailFunction = function(params) {
        var d = new $.Deferred;
        var data = params;
        data.taskId = params.pk;
        $('#taskDetailContainer').jzAjax('TaskController.saveTaskInfo()',{
            data: data,
            method: 'POST',
            traditional: true,
            success: function(response) {
                d.resolve();
            },
            error: function(jqXHR, textStatus, errorThrown ) {
                d.reject('update failure: ' + jqXHR.responseText);
            }
        });
        return d.promise();
    };

    var initEditInline = function(taskId) {
        var $taskDetailContainer = $('#taskDetailContainer');
        $taskDetailContainer.find('.editable').each(function(){
            var $this = $(this);
            var dataType = $this.attr('data-type');
            var fieldName = $this.attr('data-name');
            var editOptions = {
                mode: 'inline',
                showbuttons: false,
                pk: taskId,
                url: saveTaskDetailFunction
            };

            if(dataType == 'textarea') {
                editOptions.showbuttons = 'bottom';
                editOptions.emptytext = "Description";
            }
            if(fieldName == 'assignee' || fieldName == 'coworker') {
                var findUserURL = $this.jzURL('UserController.findUser');
                var getDisplayNameURL = $this.jzURL('UserController.getDisplayNameOfUser');
                //editOptions.source = findUserURL;
                editOptions.showbuttons = true;
                editOptions.emptytext = "Unassigned";
                editOptions.source = findUserURL;
                editOptions.select2= {
                    multiple: (fieldName == 'coworker'),
                    allowClear: true,
                    placeholder: 'Select an user',
                    tokenSeparators:[","],
                    minimumInputLength: 1,
                    initSelection: function (element, callback) {
                        return $.get(getDisplayNameURL, { usernames: element.val() }, function (data) {
                            callback((fieldName == 'coworker') ? data : data[0]);
                        });
                    }
                };

                //. This is workaround for issue of xEditable: https://github.com/vitalets/x-editable/issues/431
                if(fieldName == 'coworker') {
                    editOptions.display = function (value, sourceData) {
                        //display checklist as comma-separated values
                        if (!value || !value.length) {
                            $(this).empty();
                            return;
                        }
                        if (value && value.length > 0) {
                            //. Temporary display username in text field. It will be replace with displayName after ajax Get success
                            $(this).html(value.join(', '));
                            var $this = $(this);
                            $.get(getDisplayNameURL, { usernames: value.join(',') }, function (data) {
                                var html = [];
                                $.each(data, function (i, v) {
                                    html.push($.fn.editableutils.escape(v.text));
                                });
                                $this.html(html.join(', '));
                            });
                        }
                    };
                }
            }
            if(fieldName == 'dueDate') {
                editOptions.emptytext = "no Duedate";
                editOptions.mode = 'popup';
            }
            if(fieldName == 'status') {
                //var allStatusURL = $this.jzURL('StatusController.getAllStatus');
                var currentStatus = $this.attr('data-val');
                //editOptions.source = allStatusURL;
                editOptions.value = currentStatus;
            }
            if(fieldName == 'tags') {
                editOptions.showbuttons = true;
                editOptions.emptytext = "No Tags";
                editOptions.display = function(value, sourceData, response) {
                    if(value && value.length > 0) {
                        var html = [];
                        $.each(value, function(i, v) {
                            if(typeof v == 'string') {
                                html.push('<span class="badgeDefault badgePrimary">' + v + '</span>');
                            } else {
                                html.push('<span class="badgeDefault badgePrimary">' + v.text + '</span>');
                            }
                        });
                        $(this).html(html.join(' '));
                    } else {
                        $(this).empty();
                    }
                };
                editOptions.select2 = {
                    tags: [],
                    tokenSeparators: [',']
                };
            }
            $this.editable(editOptions);
        });
    };

    $rightPanel.on('click', '.control a.close', function(e) {
        hideRightPanel();
        return false;
    });

    $centerPanel.on('click', 'li.task', function(e) {
        var $li = $(e.target || e.srcElement).closest('li.task');
        var taskId = $li.attr('task-id');
        var currentTask = $rightPanelContent.find('.task-detail').attr('task-id');
        if (taskId != currentTask || $rightPanel.is(':hidden')) {
            $rightPanelContent.jzLoad('TaskController.detail()', {id: taskId}, function(html) {
                showRightPanel();
                initEditInline(taskId);
                $rightPanelContent.find('textarea').exoMentions({
                    onDataRequest:function (mode, query, callback) {
                        var _this = this;
                        $('#taskDetailContainer').jzAjax('UserController.findUsersToMention()', {
                            data: {query: query},
                            success: function(data) {
                                callback.call(_this, data);
                            }
                        });
                    },
                    idAction : 'taskCommentButton',
                    elasticStyle : {
                        maxHeight : '52px',
                        minHeight : '22px',
                        marginButton: '4px',
                        enableMargin: false
                    }
                });
                return false;
            });
        }
    });

    $rightPanel.on('click', 'a.task-completed-field', function(e){
        e.preventDefault();
        var $a = $(e.target || e.srcElement).closest('a');
        var isCompleted = $a.hasClass('icon-completed');
        var taskId = $a.closest('.task-detail').attr('task-id');
        var data = {taskId: taskId, completed: !isCompleted};
        $a.jzAjax('TaskController.updateCompleted()', {
            data: data,
            success: function(message) {
                $a.toggleClass('icon-completed');
            }
        });
        return false;
    });
    $rightPanel.on('click', 'a.action-clone-task', function(e){
        var $a = $(e.target).closest('a');
        var taskId = $a.closest('.task-detail').attr('task-id');
        $a.jzAjax('TaskController.clone()', {
            data: {id: taskId},
            success: function(response) {
                window.location.reload();
            }
        });
    });
    $rightPanel.on('click', 'a.action-delete-task', function(e){
        var $a = $(e.target).closest('a');
        var taskId = $a.closest('.task-detail').attr('task-id');
        $a.jzAjax('TaskController.delete()', {
            data: {id: taskId},
            success: function(response) {
                window.location.reload();
            }
        });
    });

    $rightPanel.on('submit', '.comment-form form', function(e) {
        e.preventDefault();
        var $form = $(e.target).closest('form');
        var $listComments = $form.closest('.task-detail').find('.list-comments');
        var taskId = $form.closest('.task-detail').attr('task-id');
        var comment = $.trim($form.find('textarea').val());
        if (comment == '') {
            alert('Please fill your comment!');
            return false;
        }
        var postCommentURL = $form.jzURL('TaskController.comment');
        $.post(postCommentURL, { taskId: taskId, comment: comment}, function(data) {
            var html = [];
            html.push('<li class="comment media">');
            html.push('    <a class="pull-left avatarXSmall" href="#">');
            html.push('     <img class="media-object" src="'+ data.author.avatar +'" alt="'+ data.author.displayName +'">');
            html.push('    </a>');
            html.push('    <div class="media-body">');
            html.push('    <div class="pull-right">');
            html.push('        <span class="muted">'+data.createdTimeString+'</span>');
            html.push('        <span class="comment-action">');
            html.push('            <a href="#" class="action-link delete-comment" commen-id="'+data.id+'"><i class="uiIconLightGray uiIconTrashMini"></i></a>');
            html.push('        </span>');
            html.push('    </div>');
            html.push('    <h6 class="media-heading"><a href="#">'+data.author.displayName+'</a></h6>');
            html.push('<div>');
            html.push(      data.formattedComment);
            html.push('</div>');
            html.push('</div>');
            html.push('</li>');
            var $html = $(html.join("\n"));
            $listComments.append($html);
            $listComments.find('li.no-comment').remove();
        },'json');

        return false;
    });

    $rightPanel.on('click', 'a.delete-comment', function(e) {
        e.preventDefault();
        var $a = $(e.target).closest('a');
        var commentId = $a.attr('commen-id');
        var deleteURL = $a.jzURL('TaskController.deleteComment');
        $.ajax({
            url: deleteURL,
            data: {commentId: commentId},
            type: 'POST',
            success: function(data) {
                $a.closest('li.comment').remove();
            },
            error: function() {
                alert('Error while delete comment, please try again.');
            }
        });
    });

    $rightPanel.on('click', 'a.load-all-comments', function(e) {
        e.preventDefault();
        var $a = $(e.target).closest('a');
        var $taskContainer = $a.closest('.task-detail');
        var taskId = $taskContainer.attr('task-id');
        var getAllCommentsURL = $a.jzURL('TaskController.loadAllComments');
        var $commentList = $taskContainer.find('ul.list-comments');
        $commentList.jzLoad(getAllCommentsURL, {taskId: taskId}, function(data) {
            $a.remove();
        });
    });

    $leftPanel.on('click', 'a.new-project', function(e) {
        var parentId = $(e.target).closest('a').attr('data-projectId');
        $rightPanelContent.jzLoad('ProjectController.projectForm()', {parentId: parentId}, showRightPanel);
        return true;
    });
    
    var $cloneProject = $('.confirmCloneProject');
    function showCloneProject(pId, projectName) {
    	$cloneProject.find('.pId').val(pId);
    	var msg = $cloneProject.find('.msg');
    	msg.html(msg.attr('data-msg').replace('{}', projectName));
        $cloneProject.modal({'backdrop': false});
    }
    
    $leftPanel.on('click', 'a.clone-project', function(e) {
    	var pId = $(e.target).closest('a').attr('data-projectId');    	    	
    	var projectName = $(this).closest('.project-item').find('.project-name').html();
    	
    	//
    	showCloneProject(pId, projectName);    	
    });
    
    $rightPanel.on('click', '.projectDetail .action-clone-project', function() {
    	var $detail = $(this).closest('.projectDetail');
    	var pId = $detail.attr('data-projectId');
    	var projectName = $detail.find('.projectName').html();
    	
    	showCloneProject(pId, projectName);
    });
    
    $cloneProject.find('.btn-primary').click(function(e) {
    	var pId = $cloneProject.find('.pId').val();
    	var cloneTask = $cloneProject.find('.cloneTask').is(':checked');
        
        var cloneURL = $rightPanel.jzURL('ProjectController.cloneProject');
        $.ajax({
            type: 'POST',
            url: cloneURL,
            data: {'id': pId, 'cloneTask': cloneTask},
            success: function(data) {
                window.location.reload();
            },
            error: function() {
                alert('error while create new project. Please try again.')
            }
        });
    });
    
    $rightPanel.on('submit', 'form.create-project-form', function(e) {
        var $form = $(e.target).closest('form');
        var name = $form.find('input[name="name"]').val();
        var description = $form.find('textarea[name="description"]').val();
        var parentId = $form.find('input[name="parentId"]').val();

        if(name == '') {
            alert('Please input the project name');
            return false;
        }

        var createURL = $rightPanel.jzURL('ProjectController.createProject');
        var $listProject = $leftPanel.find('ul.list-projects');
        $.ajax({
            type: 'POST',
            url: createURL,
            data: {name: name, description: description, parentId: parentId},
            success: function(data) {
                // Reload project tree;
                var $div = $('<div></div>').hide();
                $listProject.parent().append($div);
                $div.jzLoad('ProjectController.projectTree()', {current: 0}, function(content) {
                    $div.remove();
                    $listProject.html($(content).html());

                    $listProject.find('a.project-name[data-id="'+data.id+'"]').click();
                });
            },
            error: function() {
                alert('error while create new project. Please try again.')
            }
        });

        return false;
    });

    var saveProjectDetailFunction = function(params) {
        var d = new $.Deferred;
        var data = params;
        data.projectId = params.pk;
        $rightPanel.jzAjax('ProjectController.saveProjectInfo()',{
            data: data,
            method: 'POST',
            traditional: true,
            success: function(response) {
                d.resolve();
                //
                if (params.name == 'name') {
                    $leftPanel
                        .find('li.project-item a.project-name[data-id="'+ data.projectId +'"]')
                        .html(data.value);
                }
            },
            error: function(jqXHR, textStatus, errorThrown ) {
                d.reject('update failure: ' + jqXHR.responseText);
            }
        });
        return d.promise();
    };
    var initEditInlineForProject = function(projectId) {
        var $project = $rightPanel.find('.projectDetail');
        $project.find('.editable').each(function(){
            var $this = $(this);
            var dataType = $this.attr('data-type');
            var fieldName = $this.attr('data-name');
            var editOptions = {
                mode: 'inline',
                showbuttons: false,
                pk: projectId,
                url: saveProjectDetailFunction
            };

            if(dataType == 'textarea') {
                editOptions.showbuttons = 'bottom';
                editOptions.emptytext = "Description";
            }
            if(fieldName == 'manager' || fieldName == 'participator') {
                var findUserURL = $this.jzURL('UserController.findUser');
                var getDisplayNameURL = $this.jzURL('UserController.getDisplayNameOfUser');
                editOptions.showbuttons = true;
                editOptions.emptytext = (fieldName == 'manager' ? "No Manager" : "No Participator");
                editOptions.source = findUserURL;
                editOptions.select2= {
                    multiple: true,
                    allowClear: true,
                    placeholder: 'Select an user',
                    tokenSeparators:[","],
                    minimumInputLength: 1,
                    initSelection: function (element, callback) {
                        return $.get(getDisplayNameURL, { usernames: element.val() }, function (data) {
                            callback(data);
                        });
                    }
                };

                //. This is workaround for issue of xEditable: https://github.com/vitalets/x-editable/issues/431
                editOptions.display = function (value, sourceData) {
                    //display checklist as comma-separated values
                    if (!value || !value.length) {
                        $(this).empty();
                        return;
                    }
                    if (value && value.length > 0) {
                        //. Temporary display username in text field. It will be replace with displayName after ajax Get success
                        $(this).html(value.join(', '));
                        var $this = $(this);
                        $.get(getDisplayNameURL, { usernames: value.join(',') }, function (data) {
                            var html = [];
                            $.each(data, function (i, v) {
                                html.push($.fn.editableutils.escape(v.text));
                            });
                            $this.html(html.join(', '));
                        });
                    }
                };
            }
            if(fieldName == 'dueDate') {
                editOptions.emptytext = "no Duedate";
                editOptions.mode = 'popup';
            }
            $this.editable(editOptions);
        });
    };

    $leftPanel.on('click', 'a.project-name', function(e) {
        var $a = $(e.target).closest('a');
        var projectId = $a.data('id');
        var currentProject = $centerPanel.find('.projectListView').attr('data-projectId');
        if (currentProject != projectId || ($rightPanel.is(':hidden') && projectId > 0)) {

            $centerPanelContent.jzLoad('TaskController.listTasks()', {projectId: projectId}, function() {
                $a.closest('.leftPanel > ul').find('li.active').removeClass('active');
                $a.closest('li').addClass('active');
            });

            // Show project summary at right panel
            if(projectId > 0) {
                $rightPanelContent.jzLoad('ProjectController.projectDetail()', {id: projectId}, function () {
                    $a.closest('ul.list-projects[parentid="0"]').find('li.active').removeClass('active');
                    $a.closest('li').addClass('active');
                    showRightPanel();
                    initEditInlineForProject(projectId);
                });
            } else {
                hideRightPanel();
            }
        }
        return false;
    });

    $rightPanel.on('click', 'a.action-delete-project', function(e) {
        var $projectDetail = $(e.target).closest('.projectDetail');
        var projectId = $projectDetail.attr('data-projectId');
        var deleteProjectURL = $projectDetail.jzURL('ProjectController.deleteProject');
        var projectName = $projectDetail.find('a.editable[data-name="name"]').html();
        var confirmed = confirm('Are you sure you want to delete project: ' + projectName);
        if (confirmed) {
            $.ajax({
                type: 'POST',
                url: deleteProjectURL,
                data: {projectId: projectId},
                success: function (resp) {
                    $projectDetail.remove();
                    hideRightPanel();
                    $leftPanel
                        .find('li.project-item a.project-name[data-id="' + projectId + '"]')
                        .closest('li.project-item').remove();
                },
                error: function () {
                    alert('Delete project failure, please try again.')
                }
            });
        }
        return true;
    });

    $centerPanel.on('click', 'a.btn-add-task', function(e) {
        var $projectListView =  $(e.target).closest('.projectListView');
        $projectListView.find('.taskList li.item-add-new-task').show(500, function() {
            $projectListView.find('form.form-create-task input').focus();
        });
        return false;
    });

    $centerPanel.on('submit', 'form.form-create-task', function(e) {
        var $form = $(e.target).closest('form');
        var projectId = $form.closest('.projectListView').attr('data-projectId');
        var taskInput = $form.find('input[name="taskTitle"]').val();
        $form.jzAjax('TaskController.createTask()', {
            method: 'POST',
            data: {projectId: projectId, taskInput: taskInput},
            success: function(html) {
                $centerPanelContent.html(html);
            }
        });
        return false;
    });

    var submitFilter = function(e) {
        var $projectListView =  $(e.target).closest('.projectListView');
        var projectId = $projectListView.attr('data-projectId');
        var groupBy = $projectListView.find('select[name="groupBy"]').val();
        if(groupBy == undefined) {
            groupBy = '';
        }
        var orderBy = $projectListView.find('select[name="orderBy"]').val();
        if(orderBy == undefined) {
            orderBy = '';
        }
        var keyword = $projectListView.closest('.projectListView').find('input[name="keyword"]').val();
        $centerPanelContent.jzLoad('TaskController.listTasks()',
            {
                projectId: projectId,
                keyword: keyword,
                groupBy: groupBy,
                orderBy: orderBy
            },
            function() {
                hideRightPanel();
            }
        );
    };

    $centerPanel.on('submit', '.projectListView form.form-search', function(e) {
        submitFilter(e);
        return false;
    });

    $centerPanel.on('change', '.taskList form.filter-form select', function(e) {
        submitFilter(e);
        return false;
    });
});
});