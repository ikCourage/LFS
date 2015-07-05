package test;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

import swift.net.lfs.LFSByteArray;
import swift.net.lfs.LFSConnection;

/**
 * 这是一个操作视频的示例
 * 适合用作视频切片，提供视频服务
 * 如果是音乐呢？还不是一样……
 * （描述的很多，其实内容很少，很简单）
 */
public class TestVideo {

	static private String FILE_NAME = "Album/Video";

	public TestVideo()
	{
		long fileId = writeVideo(-1);
		readVideo(fileId);
	}

	private long writeVideo(long fileId)
	{
		System.out.println("\n WriteVideo");
		File file = new File("Love Story.mp4");
		//因为是视频文件比较大，所以分多次写入
		//避免此连接占用过多内存，影响其他连接
		int size = 1024 * 1024 * 10;
		int offset = 0;
		byte[] buffer = new byte[size];
		int bytesOffset = 0;
		int bytesAvalibale = 0;
		try {
			FileInputStream in = new FileInputStream(file);

			Socket con = LFSConnection.getConnection();
			LFSByteArray lb = new LFSByteArray();

			long start = System.currentTimeMillis();

			lb.reset();
			lb.putString(FILE_NAME);
			lb.putLong(fileId);
			//因为视频比较大，所以先写入视频的全部大小，用来占位，此时传输字节很小，所以速度飞快
			//（根据偏移和大小，在最后面写入一个空的字节）
			lb.putBytes(buffer, 0, 1);
			//size（将要写入的字节）
			lb.putInt(1);
			//offset (size - 1) 在最后面写入一个空字节
			lb.putInt((int)file.length() - 1);

			lb.setStatement(""
					+ "local r = data_open(vars[0]);"
					+ "if (r == 1) then "
					+ "	local id = vars[1];"
					//参考下面的介绍
					+ "	id = data_write(id, vars[2], vars[3], vars[4], 0, 2);"
					+ "	putInt(id);"
					+ "end;"
					+ "");

			lb.writeTo(con);

			fileId = lb.getInt(0);

			while ((bytesAvalibale = in.available()) > 0) {
				if (bytesOffset + bytesAvalibale > size) {
					in.read(buffer, bytesOffset, size - bytesOffset);
					bytesOffset = 0;

					lb.reset();
					lb.putString(FILE_NAME);
					lb.putLong(fileId);
					lb.putBytes(buffer, 0, size);
					//为什么需要这两个？
					//因为这里写视频其实是一个更新文件的过程，所以需要指定偏移和字节大小
					lb.putInt(size);
					lb.putInt(offset);
					offset += size;

					lb.setStatement(""
							+ "local r = data_open(vars[0]);"
							+ "if (r == 1) then "
							+ "	local id = vars[1];"
							//data_write(file:long, buffer:Object, size:long, offset:long = 0, sizeBlockMin:int = 0, link:int = 0):long
							//link 默认是 0，表示先删除，然后写入新的内容（这其实也是一种更新方式）
							//为了保证在原来数据上进行更新，那么 link 应设置为 2，此时即是覆盖式写入
							//在这里做下说明吧：
							//0 广义上理解为第一次写入，2 理解为对 0 的内容进行修改（但第一次写入时依然可以使用 2，此处示例即是如此）
							//sizeBlockMin 一般在只用作文件存储服务时用不到，默认值为 0
							+ "	id = data_write(id, vars[2], vars[3], vars[4], 0, 2);"
							+ "	putInt(id);"
							+ "end;"
							+ "");

					lb.writeTo(con);
					System.out.println("time: " + lb.getTimeSecond());

					fileId = lb.getInt(0);
				}
				else {
					in.read(buffer, bytesOffset, bytesAvalibale);
					bytesOffset += bytesAvalibale;
				}
			}

			//写入最后剩余的数据（这部分数据小于缓冲大小，所以这里可能还需要写一次）
			if (bytesOffset > 0) {
				size = bytesOffset;

				lb.reset();
				lb.putString(FILE_NAME);
				lb.putLong(fileId);
				lb.putBytes(buffer, 0, size);
				lb.putInt(size);
				lb.putInt(offset);
				offset += size;

				lb.setStatement(""
						+ "local r = data_open(vars[0]);"
						+ "if (r == 1) then "
						+ "	local id = vars[1];"
						+ "	id = data_write(id, vars[2], vars[3], vars[4], 0, 2);"
						+ "	putInt(id);"
						+ "end;"
						+ "");

				lb.writeTo(con);
				System.out.println("time: " + lb.getTimeSecond());

				fileId = lb.getInt(0);
			}

			long end = System.currentTimeMillis();
			long time = end - start;

			in.close();

			System.out.println("File ID: " + fileId);
			System.out.println("File Length: " + file.length());

			//这里的速度可能没有预想的快，为什么？
			//这里有打开本地文件并读取的时间，和数据通信的时间
			//事实上每次写入的速度很快，在每次 writeTo 后看看时间是不是很快？
			System.out.println("TIME: " + time);

			lb.clear();
			LFSConnection.put(con);
		} catch (Exception e) {
			e.printStackTrace();
		}
		buffer = null;
		file = null;
		return fileId;
	}

	private void readVideo(long fileId)
	{
		if (fileId < 0) return;
		System.out.println("\n ReadVideo");
		try {
			//把视频也分片读取，这恰好适合视频切片
			int offset = 0;
			int size = 1024 * 1024 * 10;
			boolean bytesAvalibale = true;
			byte[] buffer;

			Socket con = LFSConnection.getConnection();
			LFSByteArray lb = new LFSByteArray();

			long start = System.currentTimeMillis();

			while (bytesAvalibale == true) {
				lb.reset();
				lb.putString(FILE_NAME);
				//why?数据不会丢失吗？
				//在 LFS 中不会
				lb.putInt((int)fileId);
				lb.putInt(offset);
				lb.putInt(size);

				lb.setStatement(""
						+ "local r = data_open(vars[0]);"
						+ "if (r == 1) then "
						+ "	local id = vars[1];"
						+ "	local offset = vars[2];"
						+ "	local size = vars[3];"
						//data_read(file:int, offset:long = 0, sizeMaxRead:int = 0):int
						//sizeMaxRead 即最大读取的大小
						+ "	r = data_read(id, offset, size);"
						+ "	if (r >= 0) then "
						+ "		local dataData = vars.dataData;"
						//返回是否还有可用字节
						//dataData.sizeTotal 是此文件的总大小
						//dataData.size 是读取的实际大小，可能比 sizeMaxRead 小
						+ "		putBoolean(dataData.sizeTotal > offset + dataData.size);"
						//最后返回实际读取的内容
						+ "		putBytes(dataData, -1);"
						+ "	end;"
						+ "end;"
						+ "");
				lb.writeTo(con);

				//此时可以将这个视频写入 http 或者其他连接（比如：VIP 服务）的输出流（视频服务器）
				//或者本地文件中
				//分片加载来节约带宽
				bytesAvalibale = lb.getBoolean(0);
				buffer = lb.getBytes(1);
				if (null != buffer) {
					offset += buffer.length;
					System.out.println("bytesAvalibale: " + bytesAvalibale + " length: " + buffer.length);
				}
				else {
					System.out.println("没有该视频……");
				}
			}

			long end = System.currentTimeMillis();
			long time = end - start;

			System.out.println("File Length: " + offset);
			System.out.println("TIME: " + time);

			lb.clear();
			LFSConnection.put(con);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}