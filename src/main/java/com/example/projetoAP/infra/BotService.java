package com.example.projetoAP.infra;

import com.example.projetoAP.domain.model.Endpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
@Component
public class BotService extends TelegramLongPollingBot {

    @Value("${chatId}")
    private String  chatId;
    private String botName;
    public BotService(String botName,String token){
        super(token);
        this.botName = botName;
    }
    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Chat ID: " + update.getMessage().getChatId());
        if (update.hasMessage()) {
            Message message = update.getMessage();
            System.out.println("Chat ID: " + message.getChatId());
            System.out.println("Mensagem recebida: " + message.getText());
            System.out.println("Nome do remetente: " + message.getFrom().getFirstName());
        }
            Message message = update.getMessage();


    }
    public void notificationTelegram(Endpoint endpoint){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String alerta = """
[ALERTA] Equipamento 🔴 DOWN
⚙️ Equipamento: [%s]
📍 Localização: [%s]
🏘️ Rua: [%s]
🌐 IP: [%s]
🖥️ MAC: [%s]
⏱️ Offline desde: [%s]

""".formatted(endpoint.getGrupo().replace("_SERVICE",""), endpoint.getNome(), endpoint.getLocal(), endpoint.getIp(), endpoint.getMac(), endpoint.getUltimaVezOnline().format(formatter));
        SendMessage message = new SendMessage();
        message.setChatId("-4930867289");
        message.setText(alerta);
        message.enableMarkdown(true);
        CompletableFuture.runAsync(()->{
            try {
             execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void notificationTelegramS(){

        SendMessage message = new SendMessage();
        message.setChatId("-4930867289");
        message.setText("Atualizando dados...");
        message.enableMarkdown(true);
        CompletableFuture.runAsync(()->{
            try {
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }
}
