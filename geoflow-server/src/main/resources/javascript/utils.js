class TableSubscriber {

    constructor(url, $table) {
        this.$table = $table;
        this.socket = new WebSocket(url);
        this.socket.addEventListener('message', (event) => {
            const data = JSON.parse(event.data);
            $table.bootstrapTable('load', data);
        });
    }

    get isActive() {
        return this.socket.readyState == this.socket.OPEN;
    }

    attemptRestart() {
        if (this.isActive) {
            this.socket.close();
        }
        this.socket = new WebSocket(this.socket.url);
    }
}

function redirect(route) {
    window.location.assign(route);
}

function post(params) {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '';
    
    for (const [key, value] of Object.entries(params)) {
        const hiddenField = document.createElement('input');
        hiddenField.type = 'hidden';
        hiddenField.name = key;
        hiddenField.value = value;
        
        form.appendChild(hiddenField);
    }
    
    document.body.appendChild(form);
    form.submit();
}

function fetchJSON(method, url, data) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
    };
    return fetch(url, options);
}

function patchJSON(url, data) {
    return fetchJSON('PATCH', url, data);
}

function fetchPOST(url, data={}) {
    return fetchJSON('POST', url, data);
}

function postValue(url, func = function(value) {console.log(value)}) {
    const options = {
        method: 'POST',
    };
    fetch(url, options).then(func);
}

function patchValue(url, func = function(value) {console.log(value)}) {
    const options = {
        method: 'PATCH',
    };
    fetch(url, options).then(func);
}

function deleteValue(url, func = function(value) {console.log(value)}) {
    const options = {
        method: 'DELETE',
    };
    fetch(url, options).then(func);
}

var requestMethods = {
    insert: postValue,
    update: patchValue,
    delete: deleteValue,
}

var stOid = 0;
var sourceTablesTableId = 'sourceTables';
var sourceTableModalId = 'sourceTableData';
var saveChangesId = 'saveChanges';
var deleteRecordId = 'deleteRecord';
var sourceTableRecordLabelId = 'sourceTableRecordLabel';
var deleteSourceTableConfirmId = 'deleteSourceTable';

function postSourceTableChanges(method) {
    const runId = window.location.href.match(/(?<=\/)\d+$/g)[0]
    const params = $(`#${sourceTableModalId}EditRowBody`).serialize();
    requestMethods[method](
        `/api/source-tables?stOid=${stOid}&runId=${runId}&${params}`,
        function(response) {
            $(`#${sourceTablesTableId}`).bootstrapTable('refresh');
            response.json().then(body => {
                if (body.error !== undefined) {
                    showMessageBox('Error', body.error);
                } else {
                    $(`#${sourceTableModalId}EditRow`).modal('hide');
                }
            });
        }
    );
    stOid = 0;
}

function saveSourceTableChanges() {
    postSourceTableChanges(stOid !== 0 ? 'update' : 'insert');
}

function deleteSourceTable() {
    $(`#${deleteSourceTableConfirmId}`).modal('hide');
    postSourceTableChanges('delete');
}

function confirmSourceTableDelete(id) {
    stOid = id;
    $(`#${deleteSourceTableConfirmId}`).modal('show');
}

function editSourceTableRow(id) {
    stOid = id;
    $(`#${sourceTableRecordLabelId}`).html('Edit Row');
    let $table = $(`#${sourceTablesTableId}`);
    let row = $table.bootstrapTable('getData').find(row => row.st_oid === id);
    let allColumns = $table.bootstrapTable('getOptions')['columns'][0];
    let columns = allColumns.filter(column => column.visible && column.editable);
    let columnNames = columns.map(column => column.field);
    for (const [key, value] of Object.entries(row)) {
        if (!columnNames.includes(key)) {
            continue;
        }
        if (typeof(value) === 'boolean') {
            $(`#${key}`).prop('checked', value);
        } else {
            $(`#${key}`).val(value);
        }
    }
    $(`#${sourceTableModalId}EditRow`).modal('show');
}

function newSourceTableRow() {
    stOid = 0;
    $(`#${sourceTableRecordLabelId}`).html('New Source Table');
    $(`#${sourceTableModalId}EditRow`).modal('show');
    const columns = $(`#${sourceTablesTableId}`).bootstrapTable('getOptions').columns[0];
    for (const column of columns) {
        const $formField = $(`#${column.field}`);
        if ($formField.is(':checkbox')) {
            $formField.prop('checked', false);
        } else if ($formField.is('select')) {
            $formField.find('option:first-child').prop('selected', true);
        } else {
            $formField.val('');
        }
    }
}

function sourceTableRecordSorting(sortName, sortOrder, data) {
    let order = sortOrder === 'desc' ? -1 : 1;
    if (sortName === 'file_id') {
        data.sort((a,b) => {
            const aNum = +((a[sortName] + '').replace(/[^\d]/g, ''));
            const bNum = +((b[sortName] + '').replace(/[^\d]/g, ''));
            if (aNum < bNum) {
              return order * -1
            }
            if (aNum > bNum) {
              return order
            }
            return 0
        });
    } else if (order === 1) {
        data.sort();
    } else {
        data.sort().reverse();
    }
}

function boolFormatter(value, row) {
    return value ? '<i class="fas fa-check"></i>' : '';
}

function actionFormatter(value, row) {
    return `<span style="display: inline;"><i class="fas fa-edit p-1 inTableButton" onclick="editSourceTableRow(${row.st_oid})"></i><i class="fas fa-trash p-1 inTableButton" onclick="confirmSourceTableDelete(${row.st_oid})"></i></span>`;
}

function clickableTd(value, row, index) {
    return {classes: 'clickCell'};
}

function removeFeedback($control) {
    $control.parent().remove('.invalid-feedback').remove('.valid-feedback');
}

function addInvalidFeedback($control, message) {
    const $parent = $control.parent()
    $parent.find('.invalid-feedback').remove()
    $parent.find('.valid-feedback').remove()
    $parent.append(`<div class="invalid-feedback">${message}</div>`);
}

function addValidFeedback($control, message) {
    const $parent = $control.parent()
    $parent.find('.invalid-feedback').remove()
    $parent.find('.valid-feedback').remove()
    $parent.append(`<div class="valid-feedback">${message}</div>`);
}

function showToast(title, message) {
    const container = document.createElement('div');
    const toast = `
    <div class="position-fixed bottom-0 right-0 p-3" style="z-index: 5; right: 0; bottom: 0;">
        <div class="toast hide" role="alert" data-delay="10000" aria-live="assertive" aria-atomic="true">
            <div class="toast-header">
                <img src="favicon.ico" class="rounded mr-2">
                <strong class="mr-2">${title}</strong>
                <small>just now</small>
                <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    </div>
    `;
    container.innerHTML = toast;
    container.addEventListener('hidden.bs.toast', (e) => {
        e.target.remove();
    });
    document.body.append(container);
    const $toast = $(container).find('.toast');
    $toast.toast('show');
    $toast.on('hidden.bs.toast', (e) => {
        container.remove();
    });
}

function formatErrors(errors) {
    return errors.map(error => `${error.error_name} -> ${error.message}`).join('\n');
}
