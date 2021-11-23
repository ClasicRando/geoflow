var subscriberTables = {};
$(document).ready(function() {
    for (element of document.querySelectorAll('[data-sub=true]')) {
        const wsUrl = element.attributes['data-sub-url'].value;
        subscriberTables[element.attributes.id.value] = new TableRefreshSubscriber(wsUrl, $(element));
    }
});