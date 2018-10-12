package org.freeswitch.esl.client;

import com.google.common.base.Throwables;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.internal.IModEslApi.EventFormat;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientExample {
    private static final Logger L = LoggerFactory.getLogger(ClientExample.class);
    
    private static String host = "10.100.57.78";
    private static int port = 8021;
    private static String password = "ClueCon"; 
    
    private static ConcurrentHashMap<String,EslEvent> confMap = new ConcurrentHashMap<String,EslEvent>();
   /*
    sendmsg 8cf3f692-cedd-4470-98b8-5cc8225d22f7
    call-command: execute
    execute-app-name: playback
    execute-app-arg: /usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav

    sendmsg 8cf3f692-cedd-4470-98b8-5cc8225d22f7
    call-command: nomedia
    nomedia-uuid: mute_test
    
    
    show calls
    
    */
    
    public static SendMsg getSendMsg(String uuid, String commond, String AppName, String Args) {
    	SendMsg msg = new SendMsg(uuid);
        msg.addCallCommand(commond);
        msg.addExecuteAppName(AppName);
        msg.addExecuteAppArg(Args);
        return msg;
    }
    
    public static void main(String[] args) {
        try {
//            if (args.length < 1) {
//                System.out.println("Usage: java ClientExample PASSWORD");
//                return;
//            }
//
//            String password = args[0];

            Client client = new Client();

            client.addEventListener(
            		(ctx, event) ->{
            			L.info("Received event - name: {}", event.getEventName());
            			L.info("Received event - event header:  {}", event.getEventHeaders().toString());
//            			L.info("Received event - event body:  {}", event.getEventBodyLines().toString());
            			
//            			Map<String, String> vars = event.getEventHeaders();
//            			String uuid = vars.get("unique-id");
//            			L.info("Received event - unique-id:  {}", uuid);
//                        if (event.getEventName().equals("CHANNEL_EXECUTE_COMPLETE") 
//                        		&& vars.get("Application").equals("conference")) {
//                        	L.info("Received event - CHANNEL_EXECUTE_COMPLETE:  {}", event.getEventBodyLines().toString());
//                        }
                        
            		}
            );

            client.connect(new InetSocketAddress(host, 8021), password, 10);
//            client.setEventSubscriptions(EventFormat.PLAIN, "CHANNEL_BRIDGE|CHANNEL_STATE|CALL_UPDATE");
            client.setEventSubscriptions(EventFormat.PLAIN, "all");

            
            CompletableFuture<EslEvent> eslEvent = client.sendBackgroundApiCommand( "status", "" );
            L.info("Async return getEventName:{}", eslEvent.get().getEventName().toString());
            L.info("Async return getEventHeaders:{}", eslEvent.get().getEventHeaders().toString());
            L.info("Async return getEventBodyLines:{}", eslEvent.get().getEventBodyLines().toString());
//            
//            EslMessage response = client.sendApiCommand( "sofia status", "" );
//            L.info("Sync return getContentType:{}", response.getContentType().toString());
//            L.info("Sync return getHeaders:{}", response.getHeaders().toString());
//            L.info("Sync return getBodyLines:{}", response.getBodyLines().toString());
//            
//	          CompletableFuture<EslEvent> eslEvent = client.sendBackgroundApiCommand( "originate", "user/1000 3000" );
//	          confMap.put(eslEvent.get().getEventBodyLines().toString(), eslEvent.get());
//	          L.info("Async uuid:{}", eslEvent.get().getEventBodyLines().toString());
            
//            SendMsg msg = getSendMsg("48f3b785-08fb-4174-9f5a-e814e1ec44f1", "execute", "playback", "/usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav");
//            client.sendMessage(msg);
            
        } catch (Throwable t) {
            Throwables.propagate(t);
        }
    }
}
