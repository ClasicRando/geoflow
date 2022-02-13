let sourceTables = [];

document.addEventListener('DOMContentLoaded', async () => {
    const loadingLogicTab = document.querySelector('#loading-logic-tab');
    if (loadingLogicTab != null) {
        $(loadingLogicTab).on('show.bs.tab', async (e) => {
            const response = await fetchApi(`/data/source-tables/${runId}`, FetchMethods.GET);
            if (response.success) {
                sourceTables = response.payload;
            } else {
                showToast('Error', response.errors);
            }
            removeAllChildren(sourceTableSelector);
            removeAllChildren(parentTableSelector);
            addOptions(sourceTableSelector, sourceTables, 'st_oid', 'table_name');
            const event = new Event('change');
            sourceTableSelector.dispatchEvent(event);
            addOptions(parentTableSelector, [{st_oid: null, table_name: ''}, ...sourceTables], 'st_oid', 'table_name');
        });
    }
    sourceTableSelector.addEventListener('change', sourceTableChange);
    parentTableSelector.addEventListener('change', parentTableChange);
});

async function sourceTableChange() {
    removeAllChildren(sourceFieldsList);
    removeAllChildren(linkingKey);
    const stOid = sourceTableSelector.value;
    const response = await fetchApi(`/data/source-table-columns/${stOid}`, FetchMethods.GET);
    if (response.success) {
        $sourceFieldsList.bootstrapTable(
            'refreshOptions',
            {
                showHeader: true,
                url: `http://localhost:8080/data/source-table-columns/${stOid}`,
            }
        ).bootstrapTable('refresh');
        addOptions(linkingKey, response.payload, 'stc_oid', 'name');
    } else {
        showToast('Error', response.errors);
    }
}

async function parentTableChange() {
    removeAllChildren(parentLinkingKey);
    const stOid = parentTableSelector.value;
    if (stOid === 'null') {
        parentLinkingKeyRow.classList.add('hidden');
        return;
    }
    parentLinkingKeyRow.classList.remove('hidden');
    const response = await fetchApi(`/data/source-table-columns/${stOid}`, FetchMethods.GET);
    if (response.success) {
        addOptions(parentLinkingKey, response.payload, 'stc_oid', 'name');
    } else {
        showToast('Error', response.errors);
    }
}
