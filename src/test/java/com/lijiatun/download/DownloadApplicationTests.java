package com.lijiatun.download;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

import com.lijiatun.download.lang.DownloadThread;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DownloadApplicationTests {
	private static Logger log = Logger.getLogger(DownloadApplicationTests.class);
	@Autowired
	private TaskExecutor taskExecutor;
	/**
	 * 每个线程下载的字节数
	 */
	private long unitSize = 1000 * 1024;
	private CloseableHttpClient httpClient;
	private Long starttimes;
	private Long endtimes;
	
	/**
	 * 启动多个线程下载文件
	 */
	@Test
	public void doDownload() throws IOException {
		starttimes = System.currentTimeMillis();
		System.out.println("测试开始....");
		System.out.println("初始化测试类....");
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(100);
		
		httpClient = HttpClients.custom().setConnectionManager(cm).build();
		String remoteFileUrl="http://img14.360buyimg.com/n1/s546x546_jfs/t4606/138/2279996319/219884/231f9023/58ede3d3Nff4c5a39.jpg";
		String localPath = "/Users/liguangwen/Downloads/";
		String fileName = new URL(remoteFileUrl).getFile();
		System.out.println("远程文件名称：" + fileName);
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length()).replace("%20", " ");
		System.out.println("本地文件名称：" + fileName);
		long fileSize = this.getRemoteFileSize(remoteFileUrl);
		Long threadCount = (fileSize / unitSize) + (fileSize % unitSize != 0 ? 1 : 0);
		long offset = 0;
		CountDownLatch end = new CountDownLatch(threadCount.intValue());
		if (fileSize <= unitSize) 
		{
			// 如果远程文件尺寸小于等于unitSize
			DownloadThread downloadThread = new DownloadThread(remoteFileUrl,localPath + fileName, offset, fileSize, end, httpClient);
			taskExecutor.execute(downloadThread);
		} 
		else 
		{
			// 如果远程文件尺寸大于unitSize
			for (int i = 1; i < threadCount; i++) 
			{
				DownloadThread downloadThread = new DownloadThread(remoteFileUrl, localPath + fileName, offset, unitSize, end, httpClient);
				taskExecutor.execute(downloadThread);
				offset = offset + unitSize;
			}
			if (fileSize % unitSize != 0) 
			{
				// 如果不能整除，则需要再创建一个线程下载剩余字节
				DownloadThread downloadThread = new DownloadThread(remoteFileUrl, localPath + fileName, offset,
						fileSize - unitSize * (threadCount - 1), end, httpClient);
				taskExecutor.execute(downloadThread);
			}
		}
		try 
		{
			end.await();
		} 
		catch (InterruptedException e) 
		{
			log.error("DownLoadManager exception msg:{}", e);
			e.printStackTrace();
		}
		log.info("下载完成！{" + localPath + fileName + "} ");
		endtimes = System.currentTimeMillis();
		System.out.println("测试结束!!");
		System.out.println("********************");
		System.out.println("下载总耗时:" + (endtimes - starttimes) / 1000 + "s");
		System.out.println("********************");
	
	}

	/**
	 * 获取远程文件尺寸
	 */

	private long getRemoteFileSize(String remoteFileUrl) throws IOException 
	{
		long fileSize = 0;
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(
				remoteFileUrl).openConnection();
		httpConnection.setRequestMethod("HEAD");
		int responseCode = httpConnection.getResponseCode();
		if (responseCode >= 400) 
		{
			log.debug("Web服务器响应错误!");
			return 0;
		}
		String sHeader;
		for (int i = 1;; i++) 
		{
			sHeader = httpConnection.getHeaderFieldKey(i);
			if (sHeader != null && sHeader.equals("Content-Length")) 
			{
				System.out.println("文件大小ContentLength:" + httpConnection.getContentLength());
				fileSize = Long.parseLong(httpConnection.getHeaderField(sHeader));
				break;
			}
		}
		return fileSize;
	}

}
