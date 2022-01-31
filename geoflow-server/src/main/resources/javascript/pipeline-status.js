let runId = -1;
let code;
const operations = JSON.parse(operationsJson);

document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    code = params.get('code');
    addOptions(statusSelect, operations.payload||[], 'href', 'name');
    // const $status = $(`#${statusSelectId}`);
    // for (const operation of operations.payload||[]) {
    //     $status.append(`<option value="${operation.href}">${operation.name}</option>`);
    // }
    statusSelect.value = `/pipeline-status?code=${code}`;
    statusSelect.addEventListener('change', (e) => {
        const href = e.target.value;
        const code = href.match(/(?<=\?code=).+/g)[0];
        $table.bootstrapTable('refreshOptions', {
            url: `http://localhost:8080/data/pipeline-runs/${code}`,
        });
    });
});

async function pickup() {
    $confirmPickup.modal('hide');
    const response = await fetchPOST(`/data/pipeline-runs/pickup/${runId}`);
    const json = await response.json();
    if ('payload' in json) {
        $table.bootstrapTable('refresh');
        showToast('Run Picked Up', json.payload);
    } else {
        showToast('Error During Pickup', json.errors);
    }
}

async function forward() {
    $confirmForward.modal('hide');
    const response = await fetchPOST(`/data/pipeline-runs/move-forward/${runId}`);
    const json = await response.json();
    if ('payload' in json) {
        $table.bootstrapTable('refresh');
        showToast('Run Moved Forward', json.payload);
    } else {
        showToast('Error During Move Forward', json.errors);
    }
}

async function back() {
    $confirmBack.modal('hide');
    const response = await fetchPOST(`/data/pipeline-runs/move-back/${runId}`);
    const json = await response.json();
    if ('payload' in json) {
        $table.bootstrapTable('refresh');
        showToast('Run Moved Back', json.payload);
    } else {
        showToast('Error During Move Back', json.errors);
    }
}

function showPickupModal(value) {
    runId = value;
    $confirmPickup.modal('show');
}

function showForwardModal(value) {
    runId = value;
    $confirmForward.modal('show');
}

function showBackModal(value) {
    runId = value;
    $confirmBack.modal('show');
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
