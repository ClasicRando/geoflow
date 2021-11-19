let tasksSubscriber;
$(document).ready(function() {
    tasksSubscriber = subscriberTables[taskTableId];
});

function getRunId() {
    return window.location.href.match(/(?<=\/)[^/]+$/g)[0]||'';
}

function showSourceTables() {
    $(`#${sourceTablesTableId}`).bootstrapTable('refresh');
    $(`#${sourceTableModalId}`).modal('show');
}

function clickRunTask() {
    let $table = $(`#${taskTableId}`);
    if (!tasksSubscriber.isActive) {
        showMessageBox('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        return;
    }
    let data = $table.bootstrapTable('getData');
    if (data.find(row => row.task_status === 'Running' || row.task_status === 'Scheduled') !== undefined) {
        showMessageBox('Error', 'Task already running');
        return;
    }
    let row = data.find(row => row.task_status === 'Waiting');
    if (row == undefined) {
        showMessageBox('Error', 'No task to run');
        return;
    }
    postValue(`/api/run-task/${getRunId()}`);
}

function clickRunAllTasks() {
    let $table = $(`#${taskTableId}`);
    if (!tasksSubscriber.isActive) {
        showMessageBox('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        return;
    }
    let data = $table.bootstrapTable('getData');
    if (data.find(row => row.task_status === 'Running' || row.task_status === 'Scheduled') !== undefined) {
        showMessageBox('Error', 'Task already running');
        return;
    }
    let row = data.find(row => row.task_status === 'Waiting');
    if (row == undefined) {
        showMessageBox('Error', 'No task to run');
        return;
    }
    postValue(`/api/run-all/${getRunId()}`);
}

function statusFormatter(value, row) {
    switch(value) {
        case 'Waiting':
            return '';
        case 'Scheduled':
            return '<i class="fas fa-arrow-circle-right"></i>';
        case 'Running':
            return '<i class="fas fa-cogs fa-spin"></i>';
        case 'Complete':
            return '<i class="fas fa-check"></i>';
        case 'Failed':
            return '<i class="fas fa-exclamation"></i>';
    }
}

function titleCase(title) {
    return title.replace(
        /\w\S*/g,
        function(txt) {
            return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
        }
    ).replace(
        'Id',
        'ID'
    );
}

function showTaskInfo(prTaskId) {
    if (!tasksSubscriber.isActive) {
        showMessageBox('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        return;
    }
    let data = $(`#${taskTableId}`).bootstrapTable('getData').find(row => row.pipeline_run_task_id === prTaskId);
    let $modalBody = $(`#${taskDataModalId}Body`);
    $modalBody.empty();
    const div = document.createElement('div');
    for (const [key, value] of Object.entries(data)) {
        const label = document.createElement('label');
        label['for'] = key.replace(/\s+/g, '_');
        label.innerHTML = titleCase(key.replace(/_+/g, ' '));
        div.appendChild(label);
        const textValue = document.createElement('p');
        textValue.id = key.replace(/\s+/g, '_');
        textValue.innerHTML = value === '' ? ' ' : value;
        textValue.classList.add('border', 'rounded', 'p-3');
        div.appendChild(textValue);
    }
    $modalBody.append(div);
    $(`#${taskDataModalId}`).modal('show');
}

function reworkTask(prTaskId) {
    if (!tasksSubscriber.isActive) {
        showMessageBox('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        return;
    }
    postValue(`/api/reset-task/${getRunId()}/${prTaskId}`);
}

function taskActionFormatter(value, row) {
    const prTaskId = row.pipeline_run_task_id;
    const redoButton = row.task_status === 'Complete' || row.task_status === 'Failed' ? `<i class="fas fa-redo p-1 inTableButton" onClick="reworkTask(${prTaskId})"></i>` : '';
    return `<span style="display: inline;"><i class="fas fa-info-circle  p-1 inTableButton" onClick="showTaskInfo(${prTaskId})"></i>${redoButton}</span>`;
}