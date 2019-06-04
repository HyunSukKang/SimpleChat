
// Github : https://github.com/HyunSukKang/SimpleChat

import java.net.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap();
			ArrayList<String> forbiddenWord = new ArrayList<String>();
			while(true){
				Socket sock = server.accept(); // accept() method 는 외부에서 어떤 포트로 소켓에 연결을 신청할 떄, 그걸 잡아서 연결시켜주는 method
				ChatThread chatthread = new ChatThread(sock, hm, forbiddenWord);
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
	private PrintWriter pw;
	private HashMap hm;
	private Date Time;
	private SimpleDateFormat format;
	// private boolean initFlag = false;
	private ArrayList<String> forbiddenWord; // 금지어
	public ChatThread(Socket sock, HashMap hm, ArrayList<String> forbiddenWord){
		this.sock = sock;
		this.hm = hm;
		this.forbiddenWord = forbiddenWord;
		try{
			pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			Time = new Date();
			format = new SimpleDateFormat("HH:mm:ss");
			forbiddenWord = new ArrayList<String>();
			broadcast(id + " entered.", Time);
			System.out.println("[" + format.format(Time) + "] " + "[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			// initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	public void run(){
		try{
			String line = null;
			while((line = br.readLine()) != null){
				Time = new Date();
				if(line.equals("/quit")){
					System.out.println("[" + format.format(Time) + "] " + "[Server] User (" + id + ") exited.");
					break;
				}
				if(line.equals("/userlist")){ // userlist 입력했을 경우
					send_userlist(Time);
				}
				else if(line.equals("/spamlist")){
					spamlist(Time);
				}
				else if(line.indexOf("/addspam") == 0){
					addspam(line);
				}
				else if(line.indexOf("/to ") == 0){
					sendmsg(line, Time);
				}
				else
					broadcast(id + " : " + line, Time);
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.", Time);
			try{
				if(sock != null)
					sock.close();
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg, Date time){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		if(end != -1){
			String to = msg.substring(start, end);
			String msg2 = msg.substring(end+1);
			Object obj = hm.get(to);
			if(obj != null){
				PrintWriter pw = (PrintWriter)obj;
				pw.println("[" + format.format(time) + "] " + id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg

	/**
	 * 받은 msg를 wordTest를 통해 검사하여 금지어가 있으면 그 유저에게만 경고메시지 전달
	 * msg를 입력한 유저의 id를 이용하여, 그 유저에게는 msg를 전달하지 않음
	 * @param msg
	 */
	public void broadcast(String msg, Date time){
		int flag = 0;
		synchronized(hm){
			flag = wordTest(msg);
			if(flag == 1){
				Object obj = hm.get(id);
				PrintWriter pw = (PrintWriter)obj;
				pw.println("[" + format.format(time) + "] " + "Detected forbiddened word");
				pw.flush();
			}
			else{
				Set keySet = hm.keySet();
				Object userlist[] = keySet.toArray();
				for(Object s : userlist){
					if(s.toString().equals(id)) continue; // msg를 입력한 유저일 경우 지나감
					Object obj = hm.get(s.toString());
					if(obj!= null){
						PrintWriter pw = (PrintWriter)obj;
						pw.println("[" + format.format(time) + "] " + msg);
						pw.flush();
					}
				}
			}
		}
	} // broadcast

	/**
	 * "/userlist"를 입력한 유저의 id를 통해, 그 유저에게 userlist를 전달한다.
	 * userlist는 HashMap에서 keySet()을 통해 얻는다. 
	 */
	public void send_userlist(Date time){
		synchronized(hm){
			int count = 0;
			Set keySet = hm.keySet();
			Object userlist[] = keySet.toArray();

			Object obj = hm.get(id);
			PrintWriter pw = (PrintWriter)obj;
			for(Object s : userlist){
				count++;
				pw.println("[" + format.format(time) + "] " + s.toString());
				pw.flush();
			}
			pw.println("[" + format.format(time) + "] " + "Number of User : " + count);
			pw.flush();
		}
	} // send_userlist

	/**
	 * wordTest method는 유저에게 메시지를 입력받았을 때, 금지어가 존재하는지 검사하는 method이다.
	 * 금지어가 있으면 1, 없으면 0을 리턴한다.
	 * String class의 indexOf() 를 통해 검사한다. (indexOf()는 특정 String이 존재 할 경우 그 String의 인덱스를, 없을 경우 -1을 리턴)
	 */
	public int wordTest(String msg){
		int flag = 0;
		for(String s : forbiddenWord){
			if(msg.indexOf(s) != -1){
				flag = 1;
			}
		}

		return flag;
	} // wordTest

	public void spamlist(Date time){
		Object obj = hm.get(id);
		PrintWriter pw = (PrintWriter)obj;

		for(String s : forbiddenWord){
			pw.println("[" + format.format(time) + "] " + "spamword : " + s);
			pw.flush();
		}
	} // spamlist
	
	public void addspam(String msg){
		int start = msg.indexOf(" ") +1;
		if(start != -1){
			String msg2 = msg.substring(start);
			synchronized(forbiddenWord){
				forbiddenWord.add(msg2);
			}
		}
	} // addspam
}
