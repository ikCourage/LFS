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
			return _list.removeLast();
		}
		else {
			try {
				ByteArray b = new ByteArray(12);
				Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
				socket.getInputStream().read(b.buf, 0, 4);
				int size = b.readInt() + 4;
				b.ensureCapacity(size);
				while (b.position < size) {
					b.position += socket.getInputStream().read(b.buf, b.position, size - b.position);
				}
				b.position = 4;
				int status = b.readInt();
				b.clear();
				if (status != 0) {
					System.out.println("connection closed. status: " + status);
					socket.close();
					socket = null;
				}
				return socket;
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