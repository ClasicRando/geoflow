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

$(document).ready(function() {
    $(`#${sourceTableModalId}EditRow`).on('hidden.bs.modal', () => {
        $(`#${sourceTableModalId}ResponseErrorMessage`).text('');
    });
});

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

function fetchPUT(url, data={}) {
    return fetchJSON('PUT', url, data);
}

function fetchPATCH(url, data={}) {
    return fetchJSON('PATCH', url, data);
}

function fetchPOST(url, data={}) {
    return fetchJSON('POST', url, data);
}

function fetchDELETE(url) {
    return fetchJSON('DELETE', url, {});
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
    insert: fetchPOST,
    update: fetchPATCH,
    delete: deleteValue,
}

var sourceTableRecord = {};
var sourceTablesTableId = 'sourceTables';
var sourceTableModalId = 'sourceTableData';
var saveChangesId = 'saveChanges';
var deleteRecordId = 'deleteRecord';
var sourceTableRecordLabelId = 'sourceTableRecordLabel';
var deleteSourceTableConfirmId = 'deleteSourceTable';

async function commitSourceTableChanges(method) {
    let response;
    let json;
    switch (method) {
        case 'update':
            response = await fetchPUT(`/api/v2/source-tables`, sourceTableRecord);
            json = await response.json();
            if ('errors' in json) {
                $(`#${sourceTableModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
            } else {
                $(`#${sourceTableModalId}EditRow`).modal('hide');
                showToast('Updated Source Table', `Updated source table record (${json.payload.st_oid})`);
            }
            break;
        case 'insert':
            const runId = window.location.href.match(/(?<=\/)\d+$/g)[0];
            response = await fetchPOST(`/api/v2/source-tables/${runId}`, sourceTableRecord);
            json = await response.json();
            if ('errors' in json) {
                $(`#${sourceTableModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
            } else {
                $(`#${sourceTableModalId}EditRow`).modal('hide');
                showToast('Added Source Table', `Created new source table record (${json.payload})`);
            }
            break;
        case 'delete':
            response = await fetchDELETE(`/api/v2/source-tables/${sourceTableRecord.st_oid}`);
            json = await response.json();
            if ('errors' in json) {
                showToast('Error Deleting Source Table', formatErrors(json.errors));
            } else {
                showToast('Deleted Source Table', json.payload);
            }
            break;
    }
    if (!('errors' in json)) {
        $(`#${sourceTablesTableId}`).bootstrapTable('refresh');
        sourceTableRecord = {};
    }
}

async function saveSourceTableChanges() {
    for (const key in sourceTableRecord) {
        const $formField = $(`#${key}`);
        if ($formField.length) {
            if ($formField.is(':checkbox')) {
                sourceTableRecord[key] = $formField.prop('checked');
            } else {
                const value = $formField.val()
                sourceTableRecord[key] = value === '' ? null : value;
            }
        }
    }
    commitSourceTableChanges(sourceTableRecord.st_oid !== 0 ? 'update' : 'insert');
}

async function deleteSourceTable() {
    $(`#${deleteSourceTableConfirmId}`).modal('hide');
    commitSourceTableChanges('delete');
}

function confirmSourceTableDelete(id) {
    sourceTableRecord = $(`#${sourceTablesTableId}`).bootstrapTable('getData').find(row => row.st_oid === id);
    $(`#${deleteSourceTableConfirmId}`).modal('show');
}

function editSourceTableRow(id) {
    sourceTableRecord = $(`#${sourceTablesTableId}`).bootstrapTable('getData').find(row => row.st_oid === id);
    showSourceTableRow('edit');
}

function newSourceTableRow() {
    sourceTableRecord = {
        st_oid: 0,
        table_name: '',
        file_id: '',
        file_name: '',
        sub_table: '',
        delimiter: '',
        qualified: false,
        encoding: 'utf8',
        url: '',
        comments: '',
        record_count: 0,
        collect_type: 'Download',
        analyze: true,
        load: true,
    }
    showSourceTableRow('new');
}

function showSourceTableRow(action) {
    $(`#${sourceTableRecordLabelId}`).html(action === 'edit' ? 'Edit Row' : 'New Source Table');
    const allColumns = $(`#${sourceTablesTableId}`).bootstrapTable('getOptions').columns[0];
    const columns = allColumns.filter(column => column.visible && column.editable);
    const columnNames = columns.map(column => column.field);

    for (const [key, value] of Object.entries(sourceTableRecord)) {
        if (!columnNames.includes(key)) {
            continue;
        }
        const $formField = $(`#${key}`);
        if ($formField.is(':checkbox')) {
            $formField.prop('checked', value);
        } else {
            $formField.val(value||'');
        }
    }

    $(`#${sourceTableModalId}EditRow`).modal('show');
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
                <img src="http://localhost:8080/favicon.ico" class="rounded mr-2">
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
