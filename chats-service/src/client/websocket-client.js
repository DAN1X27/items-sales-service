async function login() {
    const username = prompt("Username");
    const password = prompt("Password")
    const url = "http://localhost:8080/auth/login";
    const response = await fetch(url, {
        method: "POST",
        body: JSON.stringify({
            username: username,
            password: password
        }),
        headers: {
            "Content-Type" : "application/json"
        }
    });
    const data = await response.json();
    if (!response.ok) {
        console.log(`Login error: ${data}`);
    } else {
        let accessToken = data["access_token"];
        let refreshToken = data["refresh_token"];
        localStorage.setItem("refresh_token", refreshToken);
        return accessToken;
    }
}

async function refreshAccessToken(refreshToken) {
    const url = `http://localhost:8080/auth/refresh-token?refresh_token=${refreshToken}`;
    const response = await fetch(url, {
        method: "POST"
    });
    if (response.status === 401) {
        return login();
    }
    const data = await response.json();
    let accessToken = data["access_token"];
    let newRefreshToken = data["refresh_token"];
    localStorage.setItem("refresh_token", newRefreshToken);
    return accessToken;
}

async function connect() {
    let refreshToken = localStorage.getItem("refresh_token");
    let accessToken;
    if (!refreshToken) {
        accessToken = await login();
    } else {
        accessToken = await refreshAccessToken(refreshToken);
    }
    const id = Number(prompt("Chat id"))
    const url = "http://localhost:8080/ws";
    const socket = new SockJS(url);
    const stompClient = Stomp.over(socket);
    const headers = {
        Authorization: `Bearer ${accessToken}`
    };
    stompClient.connect(headers, (frame) => {
        console.log(`Connected: ${frame}`);
        stompClient.subscribe(`/topic/chat/${id}`, message => {
            console.log(`Message: ${message}`);
        })
    })
}
connect();