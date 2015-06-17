package swift.net.lfs;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;

public class LFSConnection {
	static public String SERVER_HOST = "0.0.0.0";
	static public int SERVER_PORT = 9002;

	static private LinkedList<Socket> _list;

	public static synchronized Socket getConnection() {
		if (null == _list) {
			_list = new LinkedList<Socket>();
		}
		if (_list.size() > 0) {
			return _list.removeFirst();
		}
		else {
			try {
				return new Socket(SERVER_HOST, SERVER_PORT);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static synchronized void put(Socket connection) {
		if (null != connection && connection.isClosed() == false) {
			_list.add(connection);
		}
	}

	public static synchronized void clear() {
		if (null != _list) {
			Iterator<Socket> i = _list.iterator();
			while (i.hasNext()) {
				try {
					i.next().close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			i = null;
			_list = null;
		}
	}

}