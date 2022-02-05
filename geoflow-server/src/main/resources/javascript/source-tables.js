document.addEventListener('DOMContentLoaded', () => {
    if (typeof sourceTableModal !== "undefined") {
        sourceTableModal.addEventListener('hidden.bs.modal', () => {
            sourceTableModal.querySelector('p.invalidInput').textContent = '';
        });
    }
});

var sourceTableRecord = {};

async function commitSourceTableChanges(method) {
    let response;
    switch (method) {
        case 'update':
            response = await fetchApi(`/data/source-tables`, FetchMethods.PUT, sourceTableRecord);
            if (response.success) {
                $sourceTableModal.modal('hide');
                showToast('Updated Source Table', `Updated source table record (${response.payload.st_oid})`);
            } else {
                sourceTableModal.querySelector('p.invalidInput').textContent = formatErrors(response.errors);
            }
            break;
        case 'insert':
            const runId = window.location.href.match(/(?<=\/)\d+$/g)[0];
            response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.POST, sourceTableRecord);
            if (response.success) {
                $sourceTableModal.modal('hide');
                showToast('Added Source Table', `Created new source table record (${response.payload})`);
            } else {
                sourceTableModal.querySelector('p.invalidInput').textContent = formatErrors(response.errors);
            }
            break;
        case 'delete':
            response = await fetchDELETE(`/data/source-tables/${sourceTableRecord.st_oid}`);
            json = await response.json();
            if (response.success) {
                showToast('Deleted Source Table', response.payload);
            } else {
                showToast('Error Deleting Source Table', response.errors);
            }
            break;
    }
    if (response.success) {
        $sourceTablesTable.bootstrapTable('refresh');
        sourceTableRecord = {};
    }
}

async function saveSourceTableChanges() {
    for (const key in sourceTableRecord) {
        const formField = document.querySelector(`#${key}`);
        if (formField != null) {
            if (formField.type && formField.type === 'checkbox') {
                sourceTableRecord[key] = formField.checked;
            } else {
                sourceTableRecord[key] = formField.value === '' ? null : formField.value;
            }
        }
    }
    commitSourceTableChanges(sourceTableRecord.st_oid !== 0 ? 'update' : 'insert');
}

async function deleteSourceTable() {
    $deleteSourceTableConfirm.modal('hide');
    commitSourceTableChanges('delete');
}

function confirmSourceTableDelete(id) {
    sourceTableRecord = $sourceTablesTable.bootstrapTable('getData').find(row => row.st_oid === id);
    $deleteSourceTableConfirm.modal('show');
}

function editSourceTableRow(id) {
    sourceTableRecord = $sourceTablesTable.bootstrapTable('getData').find(row => row.st_oid === id);
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
    };
    showSourceTableRow('new');
}

function showSourceTableRow(action) {
    sourceTableModal.querySelector('h5.modal-title').textContent = action === 'edit' ? 'Edit Row' : 'New Source Table';
    const allColumns = $sourceTablesTable.bootstrapTable('getOptions').columns[0];
    const columnNames = allColumns.filter(column => column.visible && column.editable).map(column => column.field);
    for (const [key, value] of Object.entries(sourceTableRecord)) {
        if (!columnNames.includes(key)) {
            continue;
        }
        const formField = document.querySelector(`#${key}`);
        if (formField != null) {
            if (formField.type && formField.type === 'checkbox') {
                formField.checked = value;
            } else {
                formField.value = value||'';
            }
        }
    }
    $sourceTableModal.modal('show');
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
    const editButton = `
        <a class="p-2" href="javascript:void(0)" onclick="editSourceTableRow(${row.st_oid})">
            <i class="fas fa-edit" ></i>
        </a>
    `;
    const deleteButton = `
        <a class="p-2" href="javascript:void(0)" onclick="confirmSourceTableDelete(${row.st_oid})">
            <i class="fas fa-trash"></i>
        </a>
    `;
    const plottingButton = typeof $plottingFieldsTable !== 'undefined' ? `
        <a class="p-2" href="javascript:void(0)" onclick="plottingFields(${row.st_oid})">
            <i class="fas fa-map-marker-alt"></i>
        </a>
    ` : '';
    return `${editButton}${deleteButton}${plottingButton}`;
}
