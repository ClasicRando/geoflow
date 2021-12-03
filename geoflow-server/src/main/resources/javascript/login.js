const validationRules = {
    username: 'required',
    password: 'required',
};
const validationMessages = {
    username: 'Please provide a username',
    password: 'Please provide a password',
};

let validator;
$(document).ready(() => {
    validator = $('#loginForm').validate({
        rules: validationRules,
        messages: validationMessages,
        errorClass: 'invalidInput',
        validClass: 'validInput',
        submitHandler: () => {},
    });
    $('#submit').click(async () => {
        const user = {
            username: $('#username').val(),
            password: $('#password').val(),
        };
        const response = await fetchPOST('/login', user);
        if (response.status === 200) {
            if (response.headers.get('Content-Type').includes('application/json')) {
                const json = await response.json();
                if ('success' in json) {
                    redirect('/index');
                } else {
                    $('#message').text(json.error);
                }
            } else {
                console.log(await response.text());
                $('#message').text('Response content not json. See console for details');
            }
        } else {
            console.log(await response.text());
            $('#message').text('Invalid Response');
        }
    });
});