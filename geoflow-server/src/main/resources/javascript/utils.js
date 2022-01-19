class TableSubscriber {

    constructor(url, $table) {
        this.url = url;
        this.$table = $table;
        this.socket = new WebSocket(url);
        const handler = (e) => { this.handleEvent(e); }
        this.socket.addEventListener('message', handler);
        this.socket.addEventListener('open', handler);
        this.socket.addEventListener('error', handler);
        this.socket.addEventListener('close', handler);
    }

    get isActive() {
        return this.socket.readyState === WebSocket.prototype.OPEN;
    }

    attemptRestart() {
        try {
            this.socket.close();
        } catch (ex) {
            console.log(ex);
        }
        const $button = this.$table.parent().parent().parent().find('button[name="btnConnected"]');
        $button.find('.fa-layers').addClass('hidden');
        $button.prop('disabled', true);
        const spinner = document.createElement('span');
        spinner.classList.add('spinner-border', 'spinner-border-sm');
        spinner.setAttribute('role', 'status');
        spinner.setAttribute('aria-hidden', 'true');
        $button.append(spinner);
        this.socket = new WebSocket(this.url);
        const handler = (e) => { this.handleEvent(e); };
        const callback = () => {
            this.socket.addEventListener('error', handler);
            this.socket.addEventListener('close', handler);
            this.socket.addEventListener('message', handler);
        }
        return new Promise((resolve) => {
            setTimeout(() => {
                $button.prop('disabled', false);
                $button.find('.spinner-border').remove();
                $button.find('.fa-layers').removeClass('hidden');
                if (this.socket.readyState === WebSocket.prototype.OPEN) {
                    handler(new Event('open'));
                    callback();
                    resolve(true);
                } else {
                    handler(new Event('close'));
                    resolve(false);
                }
            }, 3000)
        });
    }

    handleEvent(event) {
        const $slashIcon = $('.fa-slash');
        const isHidden = $slashIcon.hasClass('hidden');
        switch(event.type) {
            case 'open':
                if (!isHidden) {
                    $slashIcon.addClass('hidden');
                }
                break;
            case 'message':
                const data = JSON.parse(event.data);
                this.$table.bootstrapTable('load', data);
                break;
            case 'error':
                if (isHidden) {
                    $slashIcon.removeClass('hidden');
                }
                break;
            case 'close':
                if (isHidden) {
                    $slashIcon.removeClass('hidden');
                }
                break;
        }
    }
}

$(document).ready(function() {
    if (typeof sourceTableModalId !== "undefined") {
        $(`#${sourceTableModalId}`).on('hidden.bs.modal', () => {
            $(`#${sourceTableModalId}ResponseErrorMessage`).text('');
        });
    }
});

const apiFetchOptions = {
    headers: {
        'Content-Type': 'application/json',
    },
    credentials: "include",
}

function redirect(route) {
    window.location.assign(route);
}

async function fetchApiGET(url) {
    const response = await fetch(url.startsWith('/data') ? url : `/data/${url.trimStart('/')}`);
    if (response.status !== 200) {
        return {
            success: false,
            status: response.status,
            payload: null,
            object: null,
            errorCode: response.status,
            errors: [{error: response.statusText}],
        };
    }
    try {
        const json = await response.json();
        return {
            success: 'payload' in json,
            status: response.status,
            payload: json.payload||null,
            object: json.object||null,
            errorCode: json.code||null,
            errors: json.errors||null,
        }
    } catch {
        return {
            success: false,
            status: response.status,
            payload: null,
            object: null,
            errorCode: response.status,
            errors: [{error: await response.text()}],
        };
    }
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

var sourceTableRecord = {};

async function commitSourceTableChanges(method) {
    let response;
    let json;
    switch (method) {
        case 'update':
            response = await fetchPUT(`/data/source-tables`, sourceTableRecord);
            json = await response.json();
            if ('errors' in json) {
                $(`#${sourceTableModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
            } else {
                $(`#${sourceTableModalId}`).modal('hide');
                showToast('Updated Source Table', `Updated source table record (${json.payload.st_oid})`);
            }
            break;
        case 'insert':
            const runId = window.location.href.match(/(?<=\/)\d+$/g)[0];
            response = await fetchPOST(`/data/source-tables/${runId}`, sourceTableRecord);
            json = await response.json();
            if ('errors' in json) {
                $(`#${sourceTableModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
            } else {
                $(`#${sourceTableModalId}`).modal('hide');
                showToast('Added Source Table', `Created new source table record (${json.payload})`);
            }
            break;
        case 'delete':
            response = await fetchDELETE(`/data/source-tables/${sourceTableRecord.st_oid}`);
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

async function deleteSourceTable($el) {
    $el.modal('hide');
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

    $(`#${sourceTableModalId}`).modal('show');
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
    const plottingButton = `
        <a class="p-2" href="javascript:void(0)" onclick="plottingFields(${row.st_oid})">
            <i class="fas fa-map-marker-alt"></i>
        </a>
    `;
    return `${editButton}${deleteButton}${plottingButton}`;
}

var plottingFieldsStOid = null;

async function plottingFields(stOid) {
    const $modal = $(`#${plottingFieldsModalId}`);
    const fieldsResponse = await fetch(`/data/plotting-fields/source-table/${stOid}`);
    let payload = {};
    if (fieldsResponse.status === 200) {
        const json = await fieldsResponse.json();
        payload = json.payload[0]||{};
    }
    const columnsResponse = await fetch(`/data/source-table-columns/${stOid}`);
    if (columnsResponse.status !== 200) {
        showToast('Error', `Could not find source columns for st_oid = ${stOid}`);
        return;
    }
    const columnsJson = await columnsResponse.json();
    if (typeof columnsJson.payload === "undefined") {
        showToast('Error', `Could not find source columns for st_oid = ${stOid}`);
        return;
    }
    plottingFieldsStOid = stOid;
    populatePlottingFieldsModal($modal, columnsJson.payload);
    $modal.find('#mergeKey').val(payload.merge_key||'');
    $modal.find('#companyName').val(payload.name||'');
    $modal.find('#addressLine1').val(payload.address_line1||'');
    $modal.find('#addressLine2').val(payload.address_line2||'');
    $modal.find('#city').val(payload.city||'');
    $modal.find('#alternateCities').val(payload.alternate_cities||[]);
    $modal.find('#mailCode').val(payload.mail_code||'');
    $modal.find('#prov').val(payload.prov||'');
    $modal.find('#latitude').val(payload.latitude||'');
    $modal.find('#longitude').val(payload.longitude||'');
    $modal.modal('show');
}

function populatePlottingFieldsModal($modal, columns) {
    const options = columns.map(column => `<option value="${column.name}">${column.name}</option>`).join('');
    $modal.find('select:not([multiple=multiple])').append(`<option value=""></option>${options}`);
    $modal.find('select[multiple=multiple]').append(options);
}

async function submitPlottingFields($modal) {
    const plottingFields = {
        st_oid: plottingFieldsStOid,
        merge_key: $modal.find('#mergeKey').val(),
        name: $modal.find('#companyName').val(),
        address_line1: $modal.find('#addressLine1').val(),
        address_line2: $modal.find('#addressLine2').val(),
        city: $modal.find('#city').val(),
        alternate_cities: $modal.find('#alternateCities').val(),
        mail_code: $modal.find('#mailCode').val(),
        prov: $modal.find('#prov').val(),
        latitude: $modal.find('#latitude').val(),
        longitude: $modal.find('#longitude').val(),
        clean_address: null,
        clean_city: null,
    }
    const response = await fetchPOST('/data/plotting-fields', plottingFields);
    const json = await response.json();
    if ('errors' in json) {
        $(`#${plottingFieldsModalId}ResponseErrorMessage`).text(formatErrors(json.errors));
    } else {
        $(`#${plottingFieldsTableId}`).bootstrapTable('refresh');
        $(`#${plottingFieldsModalId}`).modal('hide');
        showToast('Set Plotting Fields', `Set plotting fields for st_oid = ${plottingFieldsStOid}`);
        plottingFieldsStOid = null;
    }
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
    const container = document.getElementById('toasts');
    const toastDiv = document.createElement('div');
    toastDiv.innerHTML = `
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
    `;
    container.appendChild(toastDiv);
    const $toast = $(toastDiv).find('.toast');
    $toast.toast('show');
    $toast.on('hidden.bs.toast', (e) => {
        toastDiv.remove();
    });
}

function formatErrors(errors) {
    return errors.map(error => `${error.error_name} -> ${error.message}`).join('\n');
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

function alternateCitiesFormatter(value, row) {
    return value.join(',');
}

function resetForm($modal) {
    $modal.find('input[type=text]').val('');
    $modal.find('input[type=password]').val('');
    $modal.find('select').each(() => {
        const $el = $(this);
        $el.val($el.find(':first-child').val());
    });
    $modal.find('input[type=checkbox]').prop('checked', false);
    $modal.find('input[type=date]').val('');
    $('p.invalidInput').text('');
}
