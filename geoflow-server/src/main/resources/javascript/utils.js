class TableRefreshSubscriber {

    constructor(url, $table) {
        this.$table = $table
        this.socket = new WebSocket(url);
        this.socket.addEventListener('message', function(event) {
            console.log(event);
            $table.bootstrapTable('refresh',{silent: true});
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
    const runId = new URLSearchParams(window.location.href.replace(/^[^?]+/g, '')).get('runId');
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
    return value ? '<i class="fa fa-check"></i>' : '';
}

function actionFormatter(value, row) {
    return `<span style="display: inline;"><i class="fa fa-edit p-1 inTableButton" onclick="editSourceTableRow(${row.st_oid})"></i><i class="fa fa-trash p-1 inTableButton" onclick="confirmSourceTableDelete(${row.st_oid})"></i></span>`;
}

function clickableTd(value, row, index) {
    return {classes: 'clickCell'};
}