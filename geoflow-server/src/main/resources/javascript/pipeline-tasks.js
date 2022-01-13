let tasksSubscriber;
let runId = -1;
let waitingForUpdate = false;
let timeUnit = 'mins';
let deletePlottingFieldsStOid = null;
$(document).ready(() => {
    tasksSubscriber = subscriberTables[taskTableId];
    tasksSubscriber.socket.addEventListener('message', (e) => { waitingForUpdate = false; })
    runId = window.location.href.match(/(?<=\/)[^/]+$/g)[0];
    $('button[name="btnConnected"]').click(async (e) => {
        if (tasksSubscriber.isActive) {
            tasksSubscriber.handleEvent('open');
        } else {
            const isActive = await tasksSubscriber.attemptRestart();
            if (isActive) {
                showToast('Reconnected', 'Connected to subsriber!');
            } else {
                showToast('Error', 'Attempted restart of subscriber failed');
            }
        }
    });
});

async function clickRunTask() {
    if (waitingForUpdate) {
        return;
    }
    if (!tasksSubscriber.isActive) {
        showToast('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        waitingForUpdate = false;
        return;
    }
    waitingForUpdate = true;
    let data = tasksSubscriber.$table.bootstrapTable('getData');
    if (data.filter(row => row.task_status === 'Running' || row.task_status === 'Scheduled').length > 1) {
        showToast('Error', 'Task already running');
        waitingForUpdate = false;
        return;
    }
    if (data.filter(row => row.task_status === 'Waiting').length === 0) {
        showToast('Error', 'No task to run');
        waitingForUpdate = false;
        return;
    }
    const response = await fetchPOST(`/data/pipeline-run-tasks/run-next/${runId}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', formatErrors(json.errors));
        waitingForUpdate = false;
    } else {
        showToast('Next Task Scheduled', `Successfully scheduled ${json.payload.pipeline_run_task_id} to run`);
    }
}

async function clickRunAllTasks() {
    if (waitingForUpdate) {
        return;
    }
    waitingForUpdate = true;
    if (!tasksSubscriber.isActive) {
        showToast('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        waitingForUpdate = false;
        return;
    }
    let data = tasksSubscriber.$table.bootstrapTable('getData');
    if (data.find(row => row.task_status === 'Running' || row.task_status === 'Scheduled') !== undefined) {
        showToast('Error', 'Task already running');
        waitingForUpdate = false;
        return;
    }
    let row = data.find(row => row.task_status === 'Waiting');
    if (row == undefined) {
        showToast('Error', 'No task to run');
        waitingForUpdate = false;
        return;
    }
    const response = await fetchPOST(`/data/pipeline-run-tasks/run-all/${runId}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', formatErrors(json.errors));
        waitingForUpdate = false;
    } else {
        showToast('Run All Scheduled', `Successfully scheduled to run all tasks`);
    }
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

function taskTimeFormatter(value, row) {
    if (row.task_completed === null || row.task_start === null) {
        return null;
    }
    const timeFactor = 1000 * (timeUnit === 'mins' ? 60 : 1);
    const dif = (new Date(row.task_completed) - new Date(row.task_start))/timeFactor;
    if (dif === 0) {
        return 0;
    }
    return dif.toFixed(dif > 0.01 ? 2 : 4);
}

async function reworkTask(prTaskId) {
    if (waitingForUpdate) {
        return;
    }
    waitingForUpdate = true;
    if (!tasksSubscriber.isActive) {
        showToast('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        waitingForUpdate = false;
        return;
    }
    let data = tasksSubscriber.$table.bootstrapTable('getData');
    if (data.filter(row => row.task_status === 'Running' || row.task_status === 'Scheduled').length > 1) {
        showToast('Error', 'Cannot perform rework while task is active');
        return;
    }
    const response = await fetchPOST(`/data/pipeline-run-tasks/reset-task/${prTaskId}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', formatErrors(json.errors));
        waitingForUpdate = false;
    } else {
        showToast('Task Reworked', `Successfully reworked ${prTaskId}`);
    }
}

function taskActionFormatter(value, row) {
    const prTaskId = row.pipeline_run_task_id;
    const outputButton = row.modal_html === null ? '' : `
        <a class="p-2" href="javascript:void(0)" onClick="showOutputModal(${prTaskId})">
            <i class="fas fa-table"></i>
        </a>
    `;
    const redoButton = row.task_status !== 'Complete' && row.task_status !== 'Failed' ? '' : `
        <a class="p-2" href="javascript:void(0)" onClick="reworkTask(${prTaskId})">
            <i class="fas fa-redo"></i>
        </a>
    `;
    return `${redoButton}${outputButton}`;
}

function changeTimeUnit() {
    timeUnit = timeUnit === 'mins' ? 'secs' : 'mins';
    const data = tasksSubscriber.$table.bootstrapTable('getData');
    tasksSubscriber.$table.bootstrapTable('load', data);
    $('th[data-field=time]').find('.th-inner').text(`Time (${timeUnit})`);
}

function showOutputModal(prTaskId) {
    const $modal = $(`#${taskOutputId}`);
    const $modalBody = $(`#${taskOutputId}Body`);
    const modalHtml = tasksSubscriber.$table.bootstrapTable('getData').find(row => row.pipeline_run_task_id === prTaskId).modal_html;
    $modalBody.empty();
    $modalBody.append(modalHtml);
    $modalBody.find('table').bootstrapTable();
    $modal.modal('show');
}

function editPlottingMethods() {
    return '';
}

function plottingFieldsActions(value, row) {
    const editButton = `
        <a class="p-2" href="javascript:void(0)" onClick="plottingFields(${row.st_oid})">
            <i class="fas fa-edit"></i>
        </a>
    `;
    const deleteButton = `
        <a class="p-2" href="javascript:void(0)" onClick="confirmDeletePlottingFields(${row.st_oid})">
            <i class="fas fa-trash"></i>
        </a>
    `;
    return `${editButton}${deleteButton}`;
}

function confirmDeletePlottingFields(stOid) {
    deletePlottingFieldsStOid = stOid;
    $(`#${confirmDeletePlottingFieldsId}`).modal('show');
}

async function deletePlottingFields() {
    const response = await fetchDELETE(`/data/plotting-fields/${deletePlottingFieldsStOid}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', formatErrors(json.errors));
    } else {
        $(`#${plottingFieldsTableId}`).bootstrapTable('refresh');
        showToast('Deleted Plotting Fields', `Deleted plotting fields for st_oid = ${deletePlottingFieldsStOid}`);
        deletePlottingFieldsStOid = null;
    }
    $(`#${confirmDeletePlottingFieldsId}`).modal('hide');
}
