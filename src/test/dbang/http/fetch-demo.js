async function httpPost() {
    const body = `
      <div>
        <span>Some HTML here</span>
      </div>
    `;
    const headers = {"Content-Type": "text/html"}
    const response = await fetch("https://httpbin.org/post", {
        method: 'POST',
        headers,
        body
    });
    return response.text();
}

const result = await httpPost();
console.log(result)

