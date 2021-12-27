let runId = -1;
let code;
const operations = JSON.parse(operationsJson);

$(document).ready(function() {
    code = window.location.href.match(/(?<=\/)[^/]+$/g);
    const $status = $(`#${statusSelectId}`);
    for (const operation of operations.payload||[]) {
        $status.append(`<option value="${operation.href}">${operation.name}</option>`);
    }
    $status.val(`/pipeline-status/${code}`);
    $status.change((e) => {
        redirect($(e.target).val())
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
        showToast('Error During Pickup', formatErrors(json.errors));
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
        showToast('Error During Move Forward', formatErrors(json.errors));
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
        showToast('Error During Move Back', formatErrors(json.errors));
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

function enterRun(value) {
    redirect(`/tasks/${value}`);
}

function actionsFormatter(value, row) {
    const userId = row[`${code}_user`];
    const backButton = `<i class="fas fa-redo p-1 inTableButton" onClick="showBackModal(${row.run_id})"></i>`;
    if (userId === null) {
        const pickupButton = `<i class="fas fa-play p-1 inTableButton" onClick="showPickupModal(${row.run_id})"></i>`;
        return `<span style="display: inline;">${pickupButton}${backButton}</span>`;
    } else {
        const forwardButton = row.operation_state === 'Active' ? `<i class="fas fa-fast-forward p-1 inTableButton" onClick="showForwardModal(${row.run_id})"></i>` : '';
        const enterButton = `<i class="fas fa-sign-in-alt p-1 inTableButton" onClick="enterRun(${row.run_id})"></i>`;
        return `<span style="display: inline;">${enterButton}${forwardButton}${backButton}</span>`;
    }
}
