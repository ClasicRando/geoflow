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

var stOid = 0;
var sourceTablesTableId = 'source-tables';
var sourceTableModalId = 'sourceTableData';
var saveChnagesId = 'saveChanges';
var deleteRecordId = 'deleteRecord';
var sourceTableRecordLabelId = 'sourceTableRecordLabel';

function postSourceTableChanges(method) {
    const runId = new URLSearchParams(window.location.href.replace(/^[^?]+/g, '')).get('runId');
    const params = $(`#${sourceTableModalId}EditRowBody`).serialize();
    postValue(
        `/api/source-tables?method=${method}&stOid=${stOid}&runId=${runId}&${params}`,
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
    postSourceTableChanges('delete');
}

function editSourceTableRow(row) {
    stOid = row.st_oid;
    $(`#${deleteRecordId}`).prop('hidden', false)
    $(`#${sourceTableRecordLabelId}`).html('Edit Row');
    let $table = $(`#${sourceTablesTableId}`);
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
    $(`#${deleteRecordId}`).prop('hidden', true)
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