var socket = new SockJS('/ws');  // Ponto de entrada do WebSocket
var stompClient = Stomp.over(socket);

function connectToWebSocket() {
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/lista', function (response) {
            var data = JSON.parse(response.body);

            let downContent = '';
            let upContent = '';
            let countDown = 0;
            let countUp = 0;

            data.forEach(function (item, index) {
                const status = item.status.toUpperCase();

                const content = `
                    <div class="area1">
                        <p id="profile">${item.localizacao}</p>
                        <div class="gauge-container">
                            <div class="gauge-arc ${status === 'UP' ? 'percent-label' : 'percent-labelRED'}">
                                <div class="gauge-fill" style="width: 100%;"></div>
                            </div>
                            <div class="gauge-text ${status === 'UP' ? 'clildrenOn' : 'clildrenOff'}">${status}</div>
                        </div>
                    </div>
                `;

                if (status === 'UP') {
                    upContent += content;
                    countUp++;
                } else {
                    downContent += content;
                    countDown++;
                }
            });

            const output = `
                <span>OFF : <small>${countDown}</small></span>
                <section class="ls">
                    ${downContent}
                </section>

                <span>ON : <small>${countUp}</small></span>
                <section class="ls">
                    ${upContent}
                </section>
            `;

            document.getElementById("main").innerHTML = output;

        });
    }, function (error) {
        setTimeout(connectToWebSocket, 5000);  // Tentar reconectar após 5 segundos
    });
}

connectToWebSocket();
