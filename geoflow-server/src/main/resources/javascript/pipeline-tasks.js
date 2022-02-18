/** @type {TableSubscriber} */
let tasksSubscriber;
/** @type {number} */
let runId = -1;
/** @type {boolean} */
let waitingForUpdate = false;
/** @type {string} */
let timeUnit = 'mins';

document.addEventListener('DOMContentLoaded', () => {
    tasksSubscriber = subscriberTables[taskTableId];
    tasksSubscriber.socket.addEventListener('message', (e) => { waitingForUpdate = false; })
    runId = window.location.href.match(/(?<=\/)[^/]+$/g)[0];
    $('#source-tables-tab').on('show.bs.tab', () => {
        $sourceTablesTable.bootstrapTable('refresh');
    });
    $('#plotting-fields-tab').on('show.bs.tab', () => {
        $plottingFieldsTable.bootstrapTable('refresh');
    });
    $('#plotting-methods-tab').on('show.bs.tab', () => {
        $plottingMethodsTable.bootstrapTable('refresh');
    });
});

async function clickRunTask(isAll=false) {
    if (waitingForUpdate) {
        return;
    }
    waitingForUpdate = true;
    if (!tasksSubscriber.isActive) {
        showToast('Error', 'Task change listener is not currently running. Refresh page to reconnect');
        waitingForUpdate = false;
        return;
    }
    const data = tasksSubscriber.tableData;
    const runningTasks = data.filter(row => row.task_status === 'Running' || row.task_status === 'Scheduled');
    if (runningTasks.length > 1) {
        showToast('Warning', 'Task already running');
        waitingForUpdate = false;
        return;
    }
    const taskQueue = data.filter(row => row.task_status === 'Waiting');
    if (taskQueue.length === 0) {
        showToast('Warning', 'No task to run');
        waitingForUpdate = false;
        return;
    }
    const response = isAll ? await fetchPOST(`/data/pipeline-run-tasks/run-all/${runId}`) : await fetchPOST(`/data/pipeline-run-tasks/run-next/${runId}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', json.errors);
        waitingForUpdate = false;
    } else {
        showToast(
            isAll ? 'Run All Scheduled' : 'Next Task Scheduled',
            isAll ? `Successfully scheduled to run all tasks` : `Successfully scheduled ${json.payload.pipeline_run_task_id} to run`
        );
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
    const data = tasksSubscriber.tableData;
    const runningTasks = data.filter(row => row.task_status === 'Running' || row.task_status === 'Scheduled');
    if (runningTasks.length > 1) {
        showToast('Error', 'Cannot perform rework while task is active');
        return;
    }
    const response = await fetchPOST(`/data/pipeline-run-tasks/reset-task/${prTaskId}`);
    const json = await response.json();
    if ('errors' in json) {
        showToast('Error', json.errors);
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
    tasksSubscriber.reloadData();
    document.querySelector('th[data-field=time]').querySelector('.th-inner').textContent = `Time (${timeUnit})`;
}

function showOutputModal(prTaskId) {
    const modalBody = taskOutput.querySelector('.modal-body');
    const modalHtml = tasksSubscriber.tableData.find(row => row.pipeline_run_task_id === prTaskId).modal_html;
    modalBody.innerHTML = modalHtml;
    for (const modalTable of modalBody.querySelectorAll('table')) {
        $(modalTable).bootstrapTable();
    }
    $taskOutput.modal('show');
}
