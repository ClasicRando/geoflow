function showSourceTables() {
    $(`#${sourceTablesTableId}`).bootstrapTable('refresh');
    $(`#${sourceTableModalId}`).modal('show');
}

function clickRunTask() {
    let $table = $(`#${taskTableId}`);
    let options = $table.bootstrapTable('getOptions');
    if (options.autoRefreshStatus === false) {
        showMessageBox('Error', 'Please turn on auto refresh to run tasks');
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
    const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
    postValue(`/api/run-task?runId=${params.get('runId')}&prTaskId=${row.pipeline_run_task_id}`);
}

function clickRunAllTasks() {
    let $table = $(`#${taskTableId}`);
    let options =  $table.bootstrapTable('getOptions');
    if (options.autoRefreshStatus === false) {
        showMessageBox('Error', 'Please turn on auto refresh to run tasks');
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
    const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
    postValue(`/api/run-all?runId=${params.get('runId')}&prTaskId=${row.pipeline_run_task_id}`);
}

function statusFormatter(value, row) {
    switch(value) {
        case 'Waiting':
            return '';
        case 'Scheduled':
            return '<i class="fa fa-arrow-circle-right"></i>';
        case 'Running':
            return '<i class="fa fa-cog fa-spin"></i>';
        case 'Complete':
            return '<i class="fa fa-check"></i>';
        case 'Failed':
            return '<i class="fa fa-exclamation"></i>';
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

function showDataDisplayModal(action, data) {
    let options = $(`#${taskTableId}`).bootstrapTable('getOptions');
    if (options.autoRefreshStatus === false) {
        showMessageBox('Error', 'Please turn on auto refresh to select tasks');
        return;
    }
    let $modalBody = $(`#${taskDataModalId}Body`);
    $modalBody.empty();
    const div = document.createElement('div');
    switch(action) {
        case 'choice':
            const btnInfo = document.createElement('button');
            const btnReset = document.createElement('button');
            btnInfo.innerHTML = 'Task Info';
            btnReset.innerHTML = 'Rest Task';
            btnInfo.classList.add('btn','btn-secondary', 'mx-2');
            btnReset.classList.add('btn','btn-secondary', 'mx-2');
            btnInfo.onclick = () => {showDataDisplayModal('info', data);};
            btnReset.onclick = () => {showDataDisplayModal('reset', data);};
            div.appendChild(btnInfo);
            div.appendChild(btnReset);
            $modalBody.append(div);
            $(`#${taskDataModalId}`).modal('show');
            break;
        case 'reset':
            const params = new URLSearchParams(window.location.href.replace(/^[^?]+/g, ''));
            postValue(`/api/reset-task?runId=${params.get('runId')}&prTaskId=${data.pipeline_run_task_id}`);
            $(`#${taskDataModalId}`).modal('hide');
            break;
        case 'info':
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
            break;
    }
}

$(`#${taskTableId}`).on('click-row.bs.table', (e, row, element, field) => {
    showDataDisplayModal('choice', row);
});
// $(`#${sourceTablesTableId}`).on('click-row.bs.table', (e, row, element, field) => {
//     editSourceTableRow(row);
// });