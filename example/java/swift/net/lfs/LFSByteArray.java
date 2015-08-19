package swift.net.lfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class LFSByteArray extends ByteArray {
	static public final int ACTION_TYPE_STATEMENT = 102;

	static private final int EXTERN_TYPE_BYTE = 1;
	static private final int EXTERN_TYPE_BOOLEAN = -2;
	static private final int EXTERN_TYPE_SHORT = 2;
	static private final int EXTERN_TYPE_INT = 4;
	static private final int EXTERN_TYPE_LONG = 8;
	static private final int EXTERN_TYPE_FLOAT = 3;
	static private final int EXTERN_TYPE_DOUBLE = 7;
	static private final int EXTERN_TYPE_STRING = -4;
	static private final int EXTERN_TYPE_STRING_BYTES = -5;
	static private final int EXTERN_TYPE_BYTES = -3;
	static private final int EXTERN_TYPE_NULL = -1;

	protected String statement;
	protected int[] varsMeta;
	protected int varsLength;
	/**
	 * 系统状态
	 */
	protected int status;
	/**
	 * 用户状态
	 */
	protected int state;
	/**
	 * 类型
	 */
	protected int actionType;
	/**
	 * 时间
	 */
	protected long time;
	/**
	 * 全部时间
	 */
	protected long totalTime;

	public LFSByteArray() {
		this(32);
	}

	/**
	 * @param size
	 */
	public LFSByteArray(int size) {
		super(size);
		reset();
	}

	public void putByte(int v)
	{
		writeByte(EXTERN_TYPE_BYTE);
		writeByte(v);
		varsLength++;
	}

	public void putBytes(byte[] v)
	{
		writeByte(EXTERN_TYPE_BYTES);
		writeInt(v.length);
		write(v);
		writeByte(0);
		varsLength++;
	}

	public void putBytes(byte[] v, int off, int len)
	{
		writeByte(EXTERN_TYPE_BYTES);
		writeInt(len);
		write(v, off, len);
		writeByte(0);
		varsLength++;
	}

	public void putBoolean(boolean v)
	{
		writeByte(EXTERN_TYPE_BOOLEAN);
		writeBoolean(v);
		varsLength++;
	}

	public void putNull()
	{
		writeByte(EXTERN_TYPE_NULL);
		varsLength++;
	}

	public void putShort(int v)
	{
		writeByte(EXTERN_TYPE_SHORT);
		writeShort(v);
		varsLength++;
	}

	public void putChar(int v)
	{
		writeByte(EXTERN_TYPE_SHORT);
		writeChar(v);
		varsLength++;
	}

	public void putInt(int v)
	{
		writeByte(EXTERN_TYPE_INT);
		writeInt(v);
		varsLength++;
	}

	public void putLong(long v)
	{
		writeByte(EXTERN_TYPE_LONG);
		writeLong(v);
		varsLength++;
	}

	public void putFloat(float v)
	{
		writeByte(EXTERN_TYPE_FLOAT);
		writeFloat(v);
		varsLength++;
	}

	public void putDouble(double v)
	{
		writeByte(EXTERN_TYPE_DOUBLE);
		writeDouble(v);
		varsLength++;
	}

	public void putString(String v)
	{
		try {
			byte[] b = v.getBytes("UTF-8");
			writeByte(EXTERN_TYPE_STRING);
			writeInt(b.length);
			write(b);
			writeByte(0);
			varsLength++;
		} catch (Exception e) {}
	}

	public void putStringBytes(String v)
	{
		try {
			byte[] b = v.getBytes("UTF-8");
			writeByte(EXTERN_TYPE_STRING_BYTES);
			writeInt(b.length);
			write(b);
			writeByte(0);
			varsLength++;
		} catch (Exception e) {}
	}

	public void setStatement(String v)
	{
		statement = v;
	}

	public void setActionType(int v)
	{
		actionType = v;
	}

	public synchronized void writeTo(OutputStream out) throws IOException {
		int len = position;
		switch (actionType) {
			case ACTION_TYPE_STATEMENT:
				position = 8;
				writeInt(varsLength);
				position = len;
				try {
					write(statement.getBytes("UTF-8"));
				} catch (Exception e) {}
				len = position;
				break;
			default:
				len = 8;
				break;
		}
		position = 0;
		writeInt(len - 4);
		writeInt(actionType);
		position = len;
		super.writeTo(out);
	}

	public synchronized void writeTo(Socket socket) throws IOException
	{
		long start = System.currentTimeMillis();
		writeTo(socket.getOutputStream());
		socket.getOutputStream().flush();
		InputStream in = socket.getInputStream();
		reset();
		position = 0;
		in.read(buf, 0, 4);
		int size = readInt() + 4;
		ensureCapacity(size);
		while (position < size) {
			position += in.read(buf, position, size - position);
		}
		initVars();
		long end = System.currentTimeMillis();
		totalTime = end - start;
	}

	public synchronized void reset() {
		position = 12;
		varsLength = 0;
		actionType = ACTION_TYPE_STATEMENT;
		varsMeta = null;
		statement = null;
	}

	private void initVars()
	{
		position = 4;
		status = readInt();
		actionType = readInt();
		state = readInt();
		varsLength = readInt();
		time = readLong();
		if (status != 0) {
			System.out.println("error status: " + status);
		}
		varsMeta = new int[varsLength * 3];
		int t = 0;
		for (int i = 0, j = 0; i < varsLength; i++) {
			t = read();
			varsMeta[j++] = t;
			varsMeta[j] = position;
			switch (t) {
				case EXTERN_TYPE_INT:
				case EXTERN_TYPE_LONG:
				case EXTERN_TYPE_SHORT:
					break;
				case EXTERN_TYPE_DOUBLE:
				case EXTERN_TYPE_FLOAT:
					t++;
					break;
				case EXTERN_TYPE_BYTE:
				case EXTERN_TYPE_BOOLEAN:
					t = 1;
					break;
				case EXTERN_TYPE_NULL:
					t = 0;
					break;
				default:
					t = readInt();
					position++;
					varsMeta[j] += 4;
					break;
			}
			j++;
			varsMeta[j++] = t;
			position += t;
		}
	}

	public int getLength(int index)
	{
		int length = 0;
		if (index < varsLength && null != varsMeta) {
			index = index * 3;
			length = varsMeta[index + 2];
		}
		return length;
	}

	public Object get(int index)
	{
		if (index < varsLength && null != varsMeta) {
			switch (varsMeta[index * 3]) {
				case EXTERN_TYPE_INT:
					return getInt(index);
				case EXTERN_TYPE_LONG:
					return getLong(index);
				case EXTERN_TYPE_DOUBLE:
					return getDouble(index);
				case EXTERN_TYPE_FLOAT:
					return getFloat(index);
				case EXTERN_TYPE_SHORT:
					return getShort(index);
				case EXTERN_TYPE_BYTE:
					return getByte(index);
				case EXTERN_TYPE_BOOLEAN:
					return getBoolean(index);
				case EXTERN_TYPE_NULL:
					return null;
				case EXTERN_TYPE_STRING:
				case EXTERN_TYPE_STRING_BYTES:
					return getString(index);
				default:
					return getBytes(index);
			}
		}
		return null;
	}

	public byte getByte(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTE) {
				position = varsMeta[++index];
				return readByte();
			}
		}
		return 0;
	}

	public int getUnsignedByte(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTE) {
				position = varsMeta[++index];
				return readUnsignedByte();
			}
		}
		return 0;
	}

	public byte[] getBytes(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTES) {
				position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0) {
					byte[] b = new byte[index];
					read(b);
					return b;
				}
			}
		}
		return null;
	}

	public byte[] getBytes(int index, int len)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTES) {
				position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0 && len > 0) {
					len = len > index ? index : len;
					byte[] b = new byte[len];
					read(b);
					return b;
				}
			}
		}
		return null;
	}

	public byte[] getBytes(int index, byte[] b)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTES) {
				position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0) {
					read(b);
					return b;
				}
			}
		}
		return null;
	}

	public byte[] getBytes(int index, byte[] b, int off, int len)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTES) {
				position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0 && len > 0) {
					read(b, off, len);
					return b;
				}
			}
		}
		return null;
	}

	public boolean getBoolean(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BOOLEAN) {
				position = varsMeta[++index];
				return readBoolean();
			}
		}
		return false;
	}

	public short getShort(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_SHORT) {
				position = varsMeta[++index];
				return readShort();
			}
		}
		return 0;
	}

	public int getUnsignedShort(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_SHORT) {
				position = varsMeta[++index];
				return readUnsignedShort();
			}
		}
		return 0;
	}

	public char getChar(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_SHORT) {
				position = varsMeta[++index];
				return readChar();
			}
		}
		return 0;
	}

	public int getInt(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_INT) {
				position = varsMeta[++index];
				return readInt();
			}
		}
		return 0;
	}

	public long getLong(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_LONG) {
				position = varsMeta[++index];
				return readLong();
			}
		}
		return 0;
	}

	public float getFloat(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_FLOAT) {
				position = varsMeta[++index];
				return readFloat();
			}
		}
		return 0;
	}

	public double getDouble(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_DOUBLE) {
				position = varsMeta[++index];
				return readDouble();
			}
		}
		return 0;
	}

	public String getString(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			switch (varsMeta[index]) {
				case EXTERN_TYPE_STRING:
				case EXTERN_TYPE_STRING_BYTES:
					position = varsMeta[++index];
					index = varsMeta[++index];
					if (index > 0) {
						byte[] b = new byte[index];
						read(b);
						try {
							return new String(b, "UTF-8");
						} catch (Exception e) {}
					}
					break;
			}
		}
		return null;
	}

	public int getMessageLength()
	{
		return position;
	}

	public int getStatus()
	{
		return status;
	}
	
	public int getState()
	{
		return state;
	}

	public int getActionType()
	{
		return actionType;
	}

	public int getVarsLength()
	{
		return varsLength;
	}

	public long getTime()
	{
		return time;
	}

	public double getTimeSecond()
	{
		return (double)time / 1000000000;
	}

	public String getTimeString()
	{
		return String.format("%1$.9f", (double)time / 1000000000);
	}

	public long getTotalTimeMillis()
	{
		return totalTime;
	}

	public void clear()
	{
		super.clear();
		varsMeta = null;
		statement = null;
	}

}