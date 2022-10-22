class HttpClientRequest {
    variables = new RequestVariables();
}

class RequestVariables {
    set(varName, varValue) {
        console.log("__variable:" + varName + "," + varValue);
    }
}

const request = new HttpClientRequest();
