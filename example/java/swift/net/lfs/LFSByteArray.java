package swift.net.lfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class LFSByteArray extends ByteArray {
	static private final int ACTION_TYPE_STATEMENT = 102;

	static private final int EXTERN_TYPE_BYTE = 1;
	static private final int EXTERN_TYPE_BOOLEAN = -2;
	static private final int EXTERN_TYPE_SHORT = 2;
	static private final int EXTERN_TYPE_INT = 4;
	static private final int EXTERN_TYPE_LONG = 8;
	static private final int EXTERN_TYPE_FLOAT = 3;
	static private final int EXTERN_TYPE_DOUBLE = 7;
	static private final int EXTERN_TYPE_STRING = -4;
	static private final int EXTERN_TYPE_STRING_BYTES = -5;
	//static private final int EXTERN_TYPE_UINT32 = -20;
	static private final int EXTERN_TYPE_BYTES = -3;
	static private final int EXTERN_TYPE_NULL = -1;

	protected ByteArray head;
	protected ByteArray vars;
	protected int[] varsMeta;
	protected int varsLength;
	/**
	 * 数据大小
	 */
	protected int messageLength;
	/**
	 * 状态
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
		head = new ByteArray(12);
		vars = new ByteArray();
		varsLength = 0;
		actionType = ACTION_TYPE_STATEMENT;
	}

	public void putByte(int v)
	{
		vars.writeByte(EXTERN_TYPE_BYTE);
		vars.writeByte(v);
		varsLength++;
	}

	public void putBytes(byte[] v)
	{
		vars.writeByte(EXTERN_TYPE_BYTES);
		vars.writeInt(v.length);
		vars.write(v);
		vars.writeByte(0);
		varsLength++;
	}

	public void putBytes(byte[] v, int off, int len)
	{
		vars.writeByte(EXTERN_TYPE_BYTES);
		vars.writeInt(len);
		vars.write(v, off, len);
		vars.writeByte(0);
		varsLength++;
	}

	public void putBoolean(boolean v)
	{
		vars.writeByte(EXTERN_TYPE_BOOLEAN);
		vars.writeBoolean(v);
		varsLength++;
	}

	public void putNull()
	{
		vars.writeByte(EXTERN_TYPE_NULL);
		varsLength++;
	}

	public void putShort(int v)
	{
		vars.writeByte(EXTERN_TYPE_SHORT);
		vars.writeShort(v);
		varsLength++;
	}

	public void putChar(int v)
	{
		vars.writeByte(EXTERN_TYPE_SHORT);
		vars.writeChar(v);
		varsLength++;
	}

	public void putInt(int v)
	{
		vars.writeByte(EXTERN_TYPE_INT);
		vars.writeInt(v);
		varsLength++;
	}

	public void putLong(long v)
	{
		vars.writeByte(EXTERN_TYPE_LONG);
		vars.writeLong(v);
		varsLength++;
	}

	public void putFloat(float v)
	{
		vars.writeByte(EXTERN_TYPE_FLOAT);
		vars.writeFloat(v);
		varsLength++;
	}

	public void putDouble(double v)
	{
		vars.writeByte(EXTERN_TYPE_DOUBLE);
		vars.writeDouble(v);
		varsLength++;
	}

	public void putString(String v)
	{
		try {
			byte[] b = v.getBytes("UTF-8");
			vars.writeByte(EXTERN_TYPE_STRING);
			vars.writeInt(b.length);
			vars.write(b);
			vars.writeByte(0);
			varsLength++;
		} catch (Exception e) {}
	}

	public void putStringBytes(String v)
	{
		try {
			byte[] b = v.getBytes("UTF-8");
			vars.writeByte(EXTERN_TYPE_STRING_BYTES);
			vars.writeInt(b.length);
			vars.write(b);
			vars.writeByte(0);
			varsLength++;
		} catch (Exception e) {}
	}

	public void setStatement(String v)
	{
		try {
			write(v.getBytes("UTF-8"));
		} catch (Exception e) {}
	}

	public void setActionType(int v)
	{
		actionType = v;
	}

	public synchronized void writeTo(OutputStream out) throws IOException {
		int len = 4 + 4 + vars.size() + size();
		head.reset();
		head.writeInt(len);
		head.writeInt(actionType);
		head.writeInt(varsLength);
		head.writeTo(out);
		vars.writeTo(out);
		super.writeTo(out);
	}

	public synchronized void writeTo(Socket socket) throws IOException
	{
		long start = System.currentTimeMillis();
		writeTo(socket.getOutputStream());
		socket.getOutputStream().flush();
		InputStream in = socket.getInputStream();
		messageLength = readInt(in);
		state = readInt(in);
		actionType = readInt(in);
		varsLength = readInt(in);
		time = readLong(in);
		int offset = 0;
		int size = messageLength - 20;
		byte[] b = new byte[size];
		while (offset < size) {
			offset += in.read(b, offset, size - offset);
		}
		initVars(b, varsLength);
		long end = System.currentTimeMillis();
		totalTime = end - start;
	}

	public synchronized void reset() {
		head.reset();
		vars.reset();
		varsLength = 0;
		actionType = ACTION_TYPE_STATEMENT;
		varsMeta = null;
		super.reset();
	}

	public void init(byte[] b)
	{
		reset();
		buf = b;
		position = b.length;
	}

	private void initVars(byte[] b, int length)
	{
		reset();
		vars.buf = b;
		vars.position = 0;
		varsLength = length;
		varsMeta = new int[length * 3];
		int t = 0;
		int position = 0;
		for (int i = 0, j = 0; i < length; i++) {
			vars.position = position;
			t = vars.read();
			varsMeta[j++] = t;
			varsMeta[j] = ++position;
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
				varsMeta[j] += 4;
				position += 5;
				t = vars.readInt();
				break;
			}
			j++;
			varsMeta[j++] = t;
			position += t;
		}
	}

	private int readInt(InputStream in) throws IOException
	{
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}

	private long readLong(InputStream in) throws IOException
	{
		in.read(readBuffer, 0, 8);
		return (((long)readBuffer[0] << 56) +
				((long)(readBuffer[1] & 255) << 48) +
				((long)(readBuffer[2] & 255) << 40) +
				((long)(readBuffer[3] & 255) << 32) +
				((long)(readBuffer[4] & 255) << 24) +
				((readBuffer[5] & 255) << 16) +
				((readBuffer[6] & 255) <<  8) +
				((readBuffer[7] & 255) <<  0));
	}

	/*private float readFloat(InputStream in) throws IOException
	{
		return Float.intBitsToFloat(readInt(in));
	}

	private double readDouble(InputStream in) throws IOException
	{
		return Double.longBitsToDouble(readLong(in));
	}*/

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
				vars.position = varsMeta[++index];
				return vars.readByte();
			}
		}
		return 0;
	}

	public int getUnsignedByte(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTE) {
				vars.position = varsMeta[++index];
				return vars.readUnsignedByte();
			}
		}
		return 0;
	}

	public byte[] getBytes(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_BYTES) {
				vars.position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0) {
					byte[] b = new byte[index];
					vars.read(b);
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
				vars.position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0 && len > 0) {
					len = len > index ? index : len;
					byte[] b = new byte[len];
					vars.read(b);
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
				vars.position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0) {
					vars.read(b);
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
				vars.position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0 && len > 0) {
					vars.read(b, off, len);
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
				vars.position = varsMeta[++index];
				return vars.readBoolean();
			}
		}
		return false;
	}

	public short getShort(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_SHORT) {
				vars.position = varsMeta[++index];
				return vars.readShort();
			}
		}
		return 0;
	}

	public int getUnsignedShort(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_SHORT) {
				vars.position = varsMeta[++index];
				return vars.readUnsignedShort();
			}
		}
		return 0;
	}

	public char getChar(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_SHORT) {
				vars.position = varsMeta[++index];
				return vars.readChar();
			}
		}
		return 0;
	}

	public int getInt(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_INT) {
				vars.position = varsMeta[++index];
				return vars.readInt();
			}
		}
		return 0;
	}

	public long getLong(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_LONG) {
				vars.position = varsMeta[++index];
				return vars.readLong();
			}
		}
		return 0;
	}

	public float getFloat(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_FLOAT) {
				vars.position = varsMeta[++index];
				return vars.readFloat();
			}
		}
		return 0;
	}

	public double getDouble(int index)
	{
		if (index < varsLength && null != varsMeta) {
			index *= 3;
			if (varsMeta[index] == EXTERN_TYPE_DOUBLE) {
				vars.position = varsMeta[++index];
				return vars.readDouble();
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
				vars.position = varsMeta[++index];
				index = varsMeta[++index];
				if (index > 0) {
					byte[] b = new byte[index];
					vars.read(b);
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
		return messageLength;
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
		if (null != head) {
			head.clear();
		}
		if (null != vars) {
			vars.clear();
			vars = null;
		}
		varsMeta = null;
	}

}