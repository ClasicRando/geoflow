package html

import kotlinx.html.FlowContent
import kotlinx.html.script
import kotlinx.html.unsafe

val FlowContent.postObject get() = this.script {
    unsafe {
        raw("""
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
        """.trimIndent())
    }
}

fun FlowContent.postValue() {
    script {
        unsafe {
            raw("""
                function postValue(url) {
                    const options = {
                        method: 'POST',
                    };
                    fetch(url, options).then(result => {
                        result.json()
                    }).then(json => {
                        console.log(json);
                    });
                }
            """.trimIndent())
        }
    }
}