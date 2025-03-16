 var socket = new SockJS('/ws'); // Conecta ao servidor WebSocket
        var stompClient = Stomp.over(socket);
        stompClient.connect({}, function (frame) {
            stompClient.subscribe('/topic/processamento', function (messageOutput) {
//                var statusDiv = document.getElementById('status');
                var progressoDiv = document.getElementById('modals');
//                statusDiv.innerHTML = messageOutput.body; // Atualiza o status
                progressoDiv.innerHTML = messageOutput.body; // Exibe o progresso detalhado
            });
        });
      document.addEventListener("DOMContentLoaded", function() {
      let form = document.querySelector("");
      form.addEventListener("input",function(){
         let value = form.value.replace(/[^0-9A-Fa-f]/g, '');
         let formn = "";
         for(let i = 0;i<value.length;i++){
            if(i > 0 && i%2===0){
                formn += ":";
            }
            formn += value[i];
         }
         form.value = formn.toUpperCase();
      })
})
