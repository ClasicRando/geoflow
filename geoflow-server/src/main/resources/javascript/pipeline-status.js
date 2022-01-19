let runId = -1;
let code;
const operations = JSON.parse(operationsJson);

$(document).ready(function() {
    const params = new URLSearchParams(window.location.search);
    code = params.get('code');
    const $status = $(`#${statusSelectId}`);
    for (const operation of operations.payload||[]) {
        $status.append(`<option value="${operation.href}">${operation.name}</option>`);
    }
    $status.val(`/pipeline-status?code=${code}`);
    $status.change((e) => { 
        const href = $(e.target).val();
        const code = href.match(/(?<=\?code=).+/g)[0];
        $(`#${tableId}`).bootstrapTable('refreshOptions', {
            url: `http://localhost:8080/data/pipeline-runs/${code}`,
        });
    });
});

async function pickup() {
    $(`#${confirmPickupId}`).modal('hide');
    const response = await fetchPOST(`/data/pipeline-runs/pickup/${runId}`);
    const json = await response.json();
    if ('payload' in json) {
        $(`#${tableId}`).bootstrapTable('refresh');
        showToast('Run Picked Up', json.payload);
    } else {
        showToast('Error During Pickup', json.errors);
    }
}

async function forward() {
    $(`#${confirmForwardId}`).modal('hide');
    const response = await fetchPOST(`/data/pipeline-runs/move-forward/${runId}`);
    const json = await response.json();
    if ('payload' in json) {
        $(`#${tableId}`).bootstrapTable('refresh');
        showToast('Run Moved Forward', json.payload);
    } else {
        showToast('Error During Move Forward', json.errors);
    }
}

async function back() {
    $(`#${confirmBackId}`).modal('hide');
    const response = await fetchPOST(`/data/pipeline-runs/move-back/${runId}`);
    const json = await response.json();
    if ('payload' in json) {
        $(`#${tableId}`).bootstrapTable('refresh');
        showToast('Run Moved Back', json.payload);
    } else {
        showToast('Error During Move Back', json.errors);
    }
}

function showPickupModal(value) {
    runId = value;
    $(`#${confirmPickupId}`).modal('show');
}

function showForwardModal(value) {
    runId = value;
    $(`#${confirmForwardId}`).modal('show');
}

function showBackModal(value) {
    runId = value;
    $(`#${confirmBackId}`).modal('show');
}

function actionsFormatter(value, row) {
    const userId = row[`${code}_user`];
    const backButton = `<a class="p-2" href="javascript:void(0)" onClick="showBackModal(${row.run_id})"><i class="fas fa-redo"></i></a>`;
    if (userId === null) {
        const pickupButton = `<a class="p-2" href="javascript:void(0)" onClick="showPickupModal(${row.run_id})"><i class="fas fa-play"></i></a>`;
        return `${pickupButton}${backButton}`;
    } else {
        const forwardButton = row.operation_state === 'Active' ? `<a href="javascript:void(0)" onClick="showForwardModal(${row.run_id})"><i class="fas fa-fast-forward"></i></a>` : '';
        const enterButton = `<a class="p-2" href="/tasks/${row.run_id}"><i class="fas fa-sign-in-alt"></i></a>`;
        return `${enterButton}${forwardButton}${backButton}`;
    }
}
