package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import swift.net.lfs.LFSByteArray;
import swift.net.lfs.LFSConnection;

public class TestAPI {

	static private String FILE_NAME = "Music/Music";

	public TestAPI()
	{
		testAPI();
		testData();

		getKeyValue("习惯了自己过");
		indexOf("习惯了自己过");
	}

	private void testAPI()
	{
		System.out.println("\n TestAPI");
		Socket con = LFSConnection.getConnection();
		LFSByteArray lb = new LFSByteArray();

		try {
			lb.setStatement(""
					//print 只有在服务器开启打印模式的时候才有效
					+ "print(\"\\n*** test vars ***\");"
					+ "print(\"收到的字节长度：\", vars.messageLength);"
					+ "local vl = vars.length;"
					+ "print(\"变量数组的长度：\", vl);"

					+ "print(\"当前的状态：\", out:getState());"
					+ "out:setState(-2);"
					+ "print(\"当前的状态：\", out:getState());"

					+ "for i = 0, vl - 1 do;"
					+ "	print(\"索引为 \" .. i .. \" 的长度和内容：\", vars:getLength(i), vars[i]);"
					+ "	out:putString(vars[i]);"
					+ "end;"

					+ "out:putInt(123);"
					+ "out:putLong(567);"
					+ "out:putFloat(123.123);"
					+ "out:putDouble(567.567);"
					+ "out:putNull();"
					+ "out:putString(\"来一碗星星\");"
					+ "");

			lb.putString("习惯了自己过");
			lb.putString("我要夏天");
			lb.putString("Bad Blood");

			lb.writeTo(con);

			for (int i = 0, l = lb.getVarsLength(); i < l; i++) {
				System.out.println(i + ": " + lb.getLength(i) + "  " + lb.get(i));
			}

			System.out.println("全部用时：" + lb.getTotalTimeMillis() + " " + lb.getStatus() + " " + lb.getState());
			System.out.println("服务用时：" + lb.getTimeStringMillis());
		} catch (Exception e) {
			LFSConnection.close(con);
			e.printStackTrace();
		}
		lb.clear();
		LFSConnection.put(con);
	}

	private void testData()
	{
		System.out.println("\n TestData");
		Socket con = LFSConnection.getConnection();
		LFSByteArray lb = new LFSByteArray();

		try {
			lb.setStatement(""
					+ "local ik = LFS.IndexKey.new();"
					+ "local f = LFS.UniqueFile.new(vars[0]);"
					+ "local o;"
					+ "for i = 1, vars.length - 1 do "
					+ "	o = vars[i];"
					+ "	ik:setKey(o);"
					+ "	out:putInt(f:write(ik, o));"
					+ "end;"
					+ "");

			lb.putString(FILE_NAME);
			lb.putString("习惯了自己过");
			lb.putString("我要夏天");
			lb.putString("Bad Blood");

			lb.writeTo(con);

			for (int i = 0, l = lb.getVarsLength(); i < l; i++) {
				System.out.println("FILE ID: " + lb.get(i));
			}

			System.out.println("全部用时：" + lb.getTotalTimeMillis());
			System.out.println("服务用时：" + lb.getTimeStringMillis());
		} catch (Exception e) {
			LFSConnection.close(con);
			e.printStackTrace();
		}
		lb.clear();
		LFSConnection.put(con);
	}

	private void getKeyValue(String key)
	{
		getKeyValue(key, FILE_NAME);
	}

	private void getKeyValue(String key, String fileName)
	{
		System.out.println("\n GetKeyValue");
		Socket con = LFSConnection.getConnection();
		LFSByteArray lb = new LFSByteArray();

		try {
			lb.setStatement(""
					+ "local ik = LFS.IndexKey.new();"
					+ "local f = LFS.UniqueFile.new(vars[0]);"
					+ "ik:setKey(vars[1]);"
					+ "local id = f:hasIndex(ik);"
					+ "out:putLong(id);"
					+ "if (id > 0) then;"
					+ "	f:read(ik);"
					+ "	out:putString(buffer);"
					+ "else;"
					+ "	out:putNull();"
					+ "end;"
					+ "out:putInt(f:getTotalFile());"
					+ "");

			lb.putString(fileName);
			lb.putString(key);

			lb.writeTo(con);

			System.out.println("FILE: " + lb.getLong(0) + " " + lb.get(1));
			System.out.println("TotalFile: " + lb.get(2));
			System.out.println("TIME: " + lb.getTotalTimeMillis() + " " + lb.getTimeStringMillis());
		} catch (Exception e) {
			LFSConnection.close(con);
			e.printStackTrace();
		}
		lb.clear();
		LFSConnection.put(con);
	}

	private void indexOf(String value)
	{
		indexOf(value, FILE_NAME);
	}

	private void indexOf(String value, String fileName)
	{
		System.out.println("\n IndexOf");
		Socket con = LFSConnection.getConnection();
		LFSByteArray lb = new LFSByteArray();

		try {
			lb.setStatement(""
					+ "local f = LFS.DataFile.new(vars[0]);"
					+ "local id = f:indexOf(vars[1]);"
					+ "out:putLong(id);"
					+ "if (id >= 0) then;"
					+ "	f:read(id);"
					+ "	out:putString(buffer);"
					+ "else;"
					+ "	out:putNull();"
					+ "end;"
					+ "out:putInt(f:getTotalFile());"
					+ "");

			lb.putString(FILE_NAME);
			lb.putString(value);

			lb.writeTo(con);

			System.out.println("FILE: " + lb.getLong(0) + " " + lb.get(1));
			System.out.println("TotalFile: " + lb.get(2));
			System.out.println("TIME: " + lb.getTotalTimeMillis() + " " + lb.getTimeStringMillis());
		} catch (Exception e) {
			LFSConnection.close(con);
			e.printStackTrace();
		}
		lb.clear();
		LFSConnection.put(con);
	}

	private void addAllWords()
	{
		System.out.println("\n AddAllWords");
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("WordDict.txt");
		if (is == null){
			System.out.println("Main Dictionary not found!!!");
		}

		Socket con = LFSConnection.getConnection();
		LFSByteArray lb = new LFSByteArray();

		try {
			lb.setStatement(""
					+ "local ik = LFS.IndexKey.new();"
					+ "local f = LFS.UniqueFile.new(vars[0]);"
					+ "local o;"
					+ "for i = 1, vars.length - 1 do "
					+ "	o = vars[i];"
					+ "	ik:setKey(o);"
					+ "	out:putInt(f:write(ik, o));"
					+ "end;"
					+ "");

			lb.putString("WordDict/WordDict");

			BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					theWord = theWord.trim().toLowerCase();
					lb.putString(theWord);
				}
			} while (theWord != null);

			lb.writeTo(con);

			//for (int i = 0, l = lb.getVarsLength(); i < l; i++) {
			//	System.out.println(lb.get(i));
			//}

			System.out.println("TIME: " + lb.getTotalTimeMillis());
			System.out.println("TIME: " + lb.getTimeStringMillis());

		} catch (Exception ioe) {
			LFSConnection.close(con);
			System.err.println("Main Dictionary loading exception.");
			ioe.printStackTrace();

		} finally {
			try {
				if (is != null) {
					is.close();
					is = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		lb.clear();
		LFSConnection.put(con);
	}

	private void loadAllWords()
	{
		Socket con = LFSConnection.getConnection();
		LFSByteArray lb = new LFSByteArray();

		try {
			lb.setStatement(""
					+ "local f = LFS.DataFile.new(vars[0]);"
					+ "for i = 1, f:getTotalFile() do;"
					+ "	if (f:read(i) >= 0) then;"
					+ "		out:putInt(i);"
					+ "		out:putString(buffer);"
					+ "	end;"
					+ "end;"
					+ "");

			lb.putString("WordDict/WordDict");

			lb.writeTo(con);

			//String theWord = null;
			//for (int i = 0, l = lb.getVarsLength(); i < l; i += 2) {
			//	theWord = lb.getString(i + 1);
			//	theWord = theWord.trim().toLowerCase();
			//	System.out.println(lb.getInt(i) + " " + theWord);
			//}

			System.out.println("TIME: " + lb.getTotalTimeMillis());
			System.out.println("TIME: " + lb.getTimeStringMillis());
		} catch (Exception ioe) {
			LFSConnection.close(con);
			System.err.println("Main Dictionary loading exception.");
			ioe.printStackTrace();
		}
		lb.clear();
		LFSConnection.put(con);
	}

}