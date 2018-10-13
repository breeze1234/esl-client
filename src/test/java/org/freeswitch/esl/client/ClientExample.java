package org.freeswitch.esl.client;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.internal.IModEslApi.EventFormat;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class ClientExample {
    private static final Logger L = LoggerFactory.getLogger(ClientExample.class);
    
//    private static String host = "10.100.57.78";
    private static String host = "192.168.1.104";
    private static int port = 8021;
    private static String password = "ClueCon"; 
    private static LinkedBlockingQueue<String> toDoCmdQueue = new LinkedBlockingQueue<String>();
    private static ConcurrentHashMap<String,CompletableFuture<EslEvent>> ayncEventResult = new ConcurrentHashMap<String,CompletableFuture<EslEvent>>();
    
    private static Client client = new Client();
   /*
    sendmsg 8cf3f692-cedd-4470-98b8-5cc8225d22f7
    call-command: execute
    execute-app-name: playback
    execute-app-arg: /usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav

    sendmsg 8cf3f692-cedd-4470-98b8-5cc8225d22f7
    call-command: nomedia
    nomedia-uuid: mute_test
    
    conference list
    
    conference freeswitch play /usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav
    conference 3000-192.168.1.104 stop all
    
    uuid_broadcast <uuid> app[![hangup_cause]]::args [aleg|bleg|both]
    uuid_broadcast fb000695-b74d-42de-a543-eaf3a287aca1 /usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav
    
    pause
Pause <uuid> playback of recorded media that was started with uuid_broadcast.
Usage: pause <uuid> <on|off>
Turning pause "on" activates the pause function, i.e. it pauses the playback of recorded media. Turning pause "off" deactivates the pause function and resumes playback of recorded media at the same point where it was paused.
Note: always returns -ERR no reply when successful; returns -ERR No such channel! when uuid is invalid.

pause 7285cf3e-7fe7-4629-927b-b637213e9e83 on
     
    uuid_displace d702ee20-7276-44c4-b848-67a01c7ad6e9 start /usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav 60
     
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

            client.connect(new InetSocketAddress(host, port), password, 10);
//            client.setEventSubscriptions(EventFormat.PLAIN, "CHANNEL_BRIDGE|CHANNEL_STATE|CALL_UPDATE");
            client.setEventSubscriptions(EventFormat.PLAIN, "BACKGROUND_JOB");
//            client.setEventSubscriptions(EventFormat.PLAIN, "all");
            
            new Thread(()-> {
            	L.info("excute task thread start.");
            	while(true) {
            		String cmd;
					try {
						cmd = toDoCmdQueue.take();
						L.info("====>>send cmd: {}", cmd);
						String[] cmdArr = cmd.split(",");
	            		CompletableFuture<EslEvent> eslEvent = client.sendBackgroundApiCommand(cmdArr[0], cmdArr[1]);
	            		
//	            		confMap.put(eslEvent.get().getEventBodyLines().toString(), cmd);
//	       	          	L.info("Async uuid:{}", eslEvent.get().getEventBodyLines().toString());
	            		
	            		ayncEventResult.put(cmd, eslEvent);
	            		
	            		
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						L.error("InterruptedException:",e);
//					} catch (ExecutionException e) {
//						// TODO Auto-generated catch block
//						L.error("ExecutionException:",e);
					}
            	}
            }).start();
            
            
            new Thread(()-> {
            	L.info("get aync task result thread start.");
            	while(true) {
					try {
						for(Entry<String, CompletableFuture<EslEvent>> iter: ayncEventResult.entrySet()) {
							try {
								String cmd = iter.getKey();
								
								String[] cmdArr = cmd.split(",");
								EslEvent eslEvent = iter.getValue().get();
								if(null != eslEvent) {
									L.info("<<==== recv msg: {}", eslEvent.getEventHeaders().toString());
									L.info("<<==== recv msg body: {}", eslEvent.getEventBodyLines().toString());
									String msgBody = eslEvent.getEventBodyLines().toString();
									if(msgBody.contains("+OK")) {
										String msgBodyArr[] = msgBody.split("\\s+");
										if( cmd.contains("user/1000") && msgBodyArr.length==2 ) {
											toDoCmdQueue.add("uuid_broadcast,".concat(msgBodyArr[1].substring(0,msgBodyArr[1].length()-1)).concat(" /usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav"));
											Thread.sleep(60*1000);
											toDoCmdQueue.add("pause,".concat(msgBodyArr[1].substring(0,msgBodyArr[1].length()-1)).concat(" on"));
										}
									}
								}
								ayncEventResult.remove(cmd);  // it's ok in concurrentHashMap
							} catch (ExecutionException e) {
								// TODO Auto-generated catch block
								L.error("ExecutionException:",e);
							}
							
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						L.error("InterruptedException:",e);
					}
            	}
            }).start();
            
            
//            CompletableFuture<EslEvent> eslEvent = client.sendBackgroundApiCommand( "status", "" );
//            L.info("Async return getEventName:{}", eslEvent.get().getEventName().toString());
//            L.info("Async return getEventHeaders:{}", eslEvent.get().getEventHeaders().toString());
//            L.info("Async return getEventBodyLines:{}", eslEvent.get().getEventBodyLines().toString());
//            
//            EslMessage response = client.sendApiCommand( "sofia status", "" );
//            L.info("Sync return getContentType:{}", response.getContentType().toString());
//            L.info("Sync return getHeaders:{}", response.getHeaders().toString());
//            L.info("Sync return getBodyLines:{}", response.getBodyLines().toString());
//            
//	          CompletableFuture<EslEvent> eslEvent = client.sendBackgroundApiCommand( "originate", "user/1000 3000" );
//	          confMap.put(eslEvent.get().getEventBodyLines().toString(), eslEvent.get());
//	          L.info("Async uuid:{}", eslEvent.get().getEventBodyLines().toString());
	          
              toDoCmdQueue.add("originate".concat(",user/1000 3000")); 
              toDoCmdQueue.add("originate".concat(",user/1001 3000"));
              
              
//	          client.sendBackgroundApiCommand( "originate", "user/1001 3000" );
            
//            SendMsg msg = getSendMsg("48f3b785-08fb-4174-9f5a-e814e1ec44f1", "execute", "playback", "/usr/share/freeswitch/sounds/music/8000/suite-espanola-op-47-leyenda.wav");
//            client.sendMessage(msg);
            
        } catch (Throwable t) {
            Throwables.propagate(t);
        }
    }
}
