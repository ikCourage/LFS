package test;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

import swift.net.lfs.LFSByteArray;
import swift.net.lfs.LFSConnection;

/**
 * 这是一个操作图片的示例，很简单
 * 如果能部署到 cdn 就更 perfect
 */
public class TestImage {

	static private String FILE_NAME = "Album/Image";

	public TestImage()
	{
		long fileId = writeImage(-1);
		readImage(fileId);
	}

	private long writeImage(long fileId)
	{
		System.out.println("\n WriteImage");
		File file = new File("taylor-swift-red.jpg");
		//因为是图片文件，其足够小，所以一次性载入
		byte[] buffer = new byte[(int)file.length()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(buffer);
			in.close();

			//如果追求更快的速度，可以使用延迟写文件的方法
			//参见视频时的占位
			Socket con = LFSConnection.getConnection();
			LFSByteArray lb = new LFSByteArray();
			lb.putString(FILE_NAME);
			lb.putLong(fileId);
			lb.putBytes(buffer);

			lb.setStatement(""
					+ "local r = data_open(vars[0]);"
					+ "if (r == 1) then "
					//为何是 long?因为 Java 里的 int 是有符号的，有效位数只有 31 位
					//如果文件数量超过 2^31 的话，那么会发生什么
					//不过，其实这里可以不用这样做，可以将 data_write 的返回值判断是否是正整数
					//如果不是则写入状态错误如：setState(-1) 这样就可以使用 putInt 了，保证了数据的准确性
					//然后在返回中判断状态是否成功
					+ "	putLong(data_write(vars[1], vars[2], -1));"
					+ "end;"
					+ "");
			lb.writeTo(con);

			fileId = lb.getLong(0);
			System.out.println("File ID: " + fileId);
			System.out.println("File Length: " + file.length());

			System.out.println("TIME: " + lb.getTotalTimeMillis());
			System.out.println("TIME: " + lb.getTimeString());

			lb.clear();
			LFSConnection.put(con);
		} catch (Exception e) {
			e.printStackTrace();
		}
		buffer = null;
		file = null;
		return fileId;
	}

	private void readImage(long fileId)
	{
		if (fileId < 0) return;
		System.out.println("\n ReadImage");
		try {
			Socket con = LFSConnection.getConnection();
			LFSByteArray lb = new LFSByteArray();
			lb.putString(FILE_NAME);
			//why?数据不会丢失吗？
			//在 LFS 中不会
			lb.putInt((int)fileId);

			lb.setStatement(""
					+ "local r = data_open(vars[0]);"
					+ "if (r == 1) then "
					+ "	r = data_read(vars[1]);"
					+ "	if (r >= 0) then "
					+ "		local dataData = vars.dataData;"
					+ "		putBytes(dataData, -1);"
					+ "	end;"
					+ "end;"
					+ "");
			lb.writeTo(con);

			//此时可以将这个图片写入 http 的输出流（图片服务器）
			//或者本地文件中
			byte[] buffer = lb.getBytes(0);
			if (null != buffer) {
				System.out.println("File Length: " + buffer.length);
			}
			else {
				System.out.println("没有该图片……");
			}

			System.out.println("TIME: " + lb.getTotalTimeMillis());
			System.out.println("TIME: " + lb.getTimeString());

			lb.clear();
			LFSConnection.put(con);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}