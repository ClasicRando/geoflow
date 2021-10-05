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

var tableRow = {};
var sourceTablesTableId = 'source-tables';
var sourceTableModalId = 'sourceTableData';

function saveSourceTableChanges() {
    const runId = new URLSearchParams(window.location.href.replace(/^[^?]+/g, '')).get('runId');
    const params = $(`#${sourceTableModalId}EditRowBody`).serialize();
    postValue(
        `/api/source-tables?stOid=${tableRow.st_oid}&runId=${runId}&${params}`,
        function(response) {
            $(`#${sourceTableModalId}EditRow`).modal('hide');
            $(`#${sourceTablesTableId}`).bootstrapTable('refresh');
            response.json().then(body => {
                if (body.error !== undefined) {
                    showMessageBox('Error', body.error);
                }
            });
        }
    );
}

function editSourceTableRow(row) {
    tableRow = row;
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