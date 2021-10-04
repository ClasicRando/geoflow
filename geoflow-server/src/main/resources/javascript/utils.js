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