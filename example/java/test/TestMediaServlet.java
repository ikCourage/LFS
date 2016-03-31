package test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import swift.net.lfs.LFS_Stream;
import swift.net.lfs.LFS_Stream.IReadStream;
import swift.net.lfs.LFS_Stream.ReadStreamEnum;

/**
 * 一个流媒体服务，既可以浏览图片，也可以播放视频
 * http://localhost/testmeida/123.jpg (事实上，后缀是忽略的，只是为了更像普通文件而已)
 */
@WebServlet("/testmeida/*")
public class TestMediaServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	static private String FILE_NAME = "TestMedia/Media";

	public TestImageServlet()
	{
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		//因为图片是不会变的，所以应该永久缓存才对（这里实现的是永久缓存）
		//当然，也可以根据时间检查缓存
		if (null == request.getHeader("If-None-Match")) {
			String path = request.getPathInfo();
			if (null != path) {
				//解析文件 ID，忽略后缀，只是为了更像普通文件而已
				path = path.substring(path.lastIndexOf("/") + 1).replaceAll("[^\\d]+.*", "");
				if (path.length() != 0) {
					try {
						long fileId = Long.parseLong(path);
						
						long start = 0;
						long end = 0;
						if (null != request.getParameter("start")) {
							start = Long.parseLong(request.getParameter("start"));
						}
						if (null != request.getParameter("end")) {
							end = Long.parseLong(request.getParameter("end"));
						}
						if (start <= 0 && end <= 0) {
							String range = request.getHeader("Range");
							if (null != range) {
								range = range.replaceAll("[^\\d\\-]", "");
								String[] ranges = range.split("-");
								if (ranges.length >= 1) {
									start = Long.parseLong(ranges[0]);
								}
								if (ranges.length >= 2) {
									end = Long.parseLong(ranges[1]);
								}
							}
						}
						if (start < 0) {
							start = 0;
						}
						if (end < 0) {
							end = 0;
						}
						end = end > start ? end - start + 1 : 0;

						IReadStream readStream = new IReadStream()
						{
							@Override
							public boolean init(long fileId, int size, long sizeTotal, long sizeTotalRead, long offset)
							{
								//response.setHeader("Content-Type", "video/mp4");
								//设置客户端缓存
								response.setHeader("Cache-Control", "max-age=604800");
								response.setIntHeader("Etag", 0);
								response.setContentLengthLong(sizeTotalRead);
								if (null != request.getHeader("Range")) {
									response.setHeader("Content-Range", "bytes " + offset + "-" + (sizeTotalRead + offset - 1) + "/" + sizeTotal);
									response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
								}
								else {
									response.setHeader("Accept-Ranges", "bytes");
								}
								return true;
							}

							@Override
							public boolean parseData(byte[] b, int bytesAvalibale, int size, long sizeTotal, long sizeTotalRead, long sizeTotalReaded, long offset)
							{
								try {
									response.getOutputStream().write(b, 0, bytesAvalibale);
									return true;
								} catch (Exception e) {}
								return false;
							}
						};

						switch ((int)LFS_Stream.readStream(FILE_NAME, fileId, readStream, start, end)) {
							case ReadStreamEnum.ERROR_SOCKET_BACK_VALUE:
								response.setStatus(HttpServletResponse.SC_NOT_FOUND);
								break;
							case ReadStreamEnum.ERROR_SOCKET_CLOSED:
								response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
								break;
							case ReadStreamEnum.ERROR_IREAD_STREAM_PARSE_DATA:
							case ReadStreamEnum.ERROR_IREAD_STREAM_INIT:
							case ReadStreamEnum.ERROR_FILE_ID:
							case ReadStreamEnum.ERROR_OUT_OF_BOUNDS:
								response.setStatus(HttpServletResponse.SC_FORBIDDEN);
								break;
						}
					} catch (NumberFormatException e) {
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					}
				}
				else {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				}
			}
			else {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			}
		}
		else {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}

}
