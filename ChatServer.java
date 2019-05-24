import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			while(true){
				Socket sock = server.accept(); // accept() method 는 외부에서 어떤 포트로 소켓에 연결을 신청할 떄, 그걸 잡아서 연결시켜주는 method
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	private String[] forbiddenWord = {"captain", "vision", "avengers", "ironman", "hulk"};
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id, id + " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){
				if(line.equals("/quit")){
					System.out.println("[Server] User (" + id + ") exited.");
					break;
				}
				if(line.equals("/userlist")){
					send_userlist();
				}
				if(line.indexOf("/to ") == 0){
					sendmsg(line);
				}
				else
					broadcast(id, id + " : " + line);
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id, id + " exited.");
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println(id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg
	public void broadcast(String id, String msg){
		synchronized(hm){
			Set keySet = hm.keySet();
			Object userlist[] = keySet.toArray();
			for(Object s : userlist){
				if(s.toString().equals(id)){}
				else{
					Object obj = hm.get(s.toString());
					if(obj!= null){
						PrintWriter pw = (PrintWriter)obj;
						pw.println(msg);
						pw.flush();
					}	
				}
			}
		}
	} // broadcast
	public void send_userlist(){
		synchronized(hm){
			int count = 0;
			Collection collection = hm.values();
			Iterator iter = collection.iterator();
			Set keySet = hm.keySet();
			Object userlist[] = keySet.toArray();
			while(iter.hasNext()){
				PrintWriter pw = (PrintWriter)iter.next();
				for(Object s : userlist){
					count++;
					pw.println(s.toString());
					pw.flush();
				}
				pw.println("Number of User : " + count);
				count = 0;
			}
		}
	} // send_userlist
}
