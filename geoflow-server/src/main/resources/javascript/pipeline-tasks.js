/** @type {TableSubscriber} */
let tasksSubscriber;
/** @type {number} */
let runId = -1;
/** @type {boolean} */
let waitingForUpdate = false;
/** @type {string} */
let timeUnit = 'mins';
/** @type {number} */
let deletePlottingFieldsStOid = null;
/** @type {HTMLOListElement} */
let plottingMethodsList = null;
const plottingMethodTypesJson = JSON.parse(plottingMethodTypes);
const methodTypeOptions = plottingMethodTypesJson.payload.map(method => {
    return {
        id: method.method_id,
        text: method.name,
    }
});
let options = null;
/** @type {{id: number, text: string}[]} */
let tableNameOptions = null;

document.addEventListener('DOMContentLoaded', () => {
    tasksSubscriber = subscriberTables[taskTableId];
    tasksSubscriber.socket.addEventListener('message', (e) => { waitingForUpdate = false; })
    runId = window.location.href.match(/(?<=\/)[^/]+$/g)[0];
    plottingMethodsList = plottingMethodsModal.querySelector('ol');
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

/**
 * @param {number} i
 * @returns {HTMLLIElement}
 */
function plottingMethodListItem(i, order, methodType, stOid) {
    const index = i + 1;
    const liElement = document.createElement('li');
    liElement.classList.add('list-group-item');
    liElement.id = index.toString();
    const row = document.createElement('div');
    row.classList.add('row');
    const orderCol = document.createElement('div');
    orderCol.classList.add('col-2');
    const orderLabel = document.createElement('label');
    orderLabel.htmlFor = `order${index}`;
    orderLabel.textContent = 'Order';
    const orderSelect = document.createElement('select');
    orderSelect.id = `order${index}`;
    orderSelect.name = 'order';
    orderSelect.classList.add('custom-select');
    addOptions(orderSelect, options, 'id', 'text');
    orderSelect.value = order;
    orderCol.appendChild(orderLabel);
    orderCol.appendChild(orderSelect);
    row.appendChild(orderCol);

    const sourceTableCol = document.createElement('div');
    sourceTableCol.classList.add('col');
    const sourceTableLabel = document.createElement('label');
    sourceTableLabel.htmlFor = `sourceTable${index}`;
    sourceTableLabel.textContent = 'Source Table';
    const sourceTableSelect = document.createElement('select');
    sourceTableSelect.id = `sourceTable${index}`;
    sourceTableSelect.name = 'sourceTable';
    sourceTableSelect.classList.add('custom-select');
    addOptions(sourceTableSelect, tableNameOptions, 'id', 'text');
    sourceTableSelect.value = stOid;
    sourceTableCol.appendChild(sourceTableLabel);
    sourceTableCol.appendChild(sourceTableSelect);
    row.appendChild(sourceTableCol);

    const methodCol = document.createElement('div');
    methodCol.classList.add('col');
    const methodLabel = document.createElement('label');
    methodLabel.htmlFor = `method${index}`;
    methodLabel.textContent = 'Method';
    const methodSelect = document.createElement('select');
    methodSelect.id = `method${index}`;
    methodSelect.name = 'method';
    methodSelect.classList.add('custom-select');
    addOptions(methodSelect, methodTypeOptions, 'id', 'text');
    methodSelect.value = methodType;
    methodCol.appendChild(methodLabel);
    methodCol.appendChild(methodSelect);
    row.appendChild(methodCol);

    liElement.appendChild(row);
    return liElement;
}

async function editPlottingMethods() {
    const response = await fetchApi(`/plotting-methods/${runId}`, FetchMethods.GET);
    if (!response.success) {
        showToast('Error', response.errors);
        return;
    }
    const methods = response.payload;
    const methodsCount = methods.length;
    const sourceTablesResponse = await fetchApi(`/source-tables/${runId}`, FetchMethods.GET);
    if (!sourceTablesResponse.success) {
        showToast('Error', sourceTablesResponse.errors);
        return;
    }
    options = (new Array(methodsCount)).fill(undefined).map((_, i) => {
        return {
            id: i + 1,
            text: i + 1,
        }
    });
    tableNameOptions = sourceTablesResponse.payload.map(sourceTable => {
        return {
            id: sourceTable.st_oid,
            text: sourceTable.table_name,
        }
    });
    removeAllChildren(plottingMethodsList);
    for(let i = 0; i < methodsCount; i++) {
        const method = methods[i];
        const methodElement = plottingMethodListItem(i, method.order, method.method_type, method.st_oid);
        plottingMethodsList.appendChild(methodElement);
    }
    $plottingMethodsModal.modal('show');
}

function addPlottingMethod() {
    const lastIndex = plottingMethodsList.querySelectorAll('li').length;
    const methodElement = plottingMethodListItem(lastIndex, '', '', '');
    plottingMethodsList.appendChild(methodElement);
    setOrderNumberOptions();
}

function setOrderNumberOptions() {
    const optionCount = plottingMethodsList.querySelectorAll('li').length;
    options = (new Array(optionCount)).fill(undefined).map((_, i) => {
        return {
            id: i,
            text: i,
        }
    });
    for (const element of plottingMethodsList.querySelectorAll('select[name=order]')) {
        if (optionCount !== element.querySelectorAll('option').length) {
            const tempValue = element.value;
            removeAllChildren(element);
            addOptions(element, options, 'id', 'text');
            element.vaue = tempValue;
        }
    }
}
