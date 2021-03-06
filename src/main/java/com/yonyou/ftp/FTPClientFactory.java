package com.yonyou.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FTP工厂  windows下ftp共享目录为d:/source,ftp服务路径的根为"/"
 * @author changming
 * @version v.0.1
 */
public class FTPClientFactory extends BasePooledObjectFactory<FTPClient> {
    private static Logger logger = LoggerFactory.getLogger(FTPClientFactory.class);
    
    private FTPConfig ftpConfig;
    
    private FTPClient client;
    
    /**
     * 
     * @param host 主机
     * @param port 端口
     * @param username 用户名
     * @param password 密码
     */
    public FTPClientFactory(String host, int port, String username, String password) throws Exception {

    	ftpConfig = new FTPConfig();
    	ftpConfig .setHost(host);
    	ftpConfig.setPort(port);
    	ftpConfig.setUsername(username);
    	ftpConfig.setPassword(password);
    	try {
			client = create();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    /**
     * 新建对象
     */
    @Override
    public FTPClient create() throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(ftpConfig.getClientTimeout());
        try {
            ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                logger.error("FTPServer 拒绝连接");
                return null;
            }
            boolean result = ftpClient.login(ftpConfig.getUsername(),ftpConfig.getPassword());
            if (!result) {
                logger.error("ftpClient登陆失败!");
                throw new Exception("ftpClient登陆失败! userName:"+ ftpConfig.getUsername() + " ; password:"
                        + ftpConfig.getPassword());
            }
            ftpClient.setFileType(ftpConfig.getTransferFileType());
            ftpClient.setBufferSize(ftpConfig.getBufferSize());
            ftpClient.setControlEncoding(ftpConfig.getEncoding());
            if (ftpConfig.getPassiveMode()) {
                ftpClient.enterLocalPassiveMode();
            }
            ftpClient.changeWorkingDirectory(ftpConfig.getWorkingDirectory());
        } catch (IOException e) {
            logger.error("FTP连接失败：", e);
        }
        return ftpClient;
    }

    @Override
    public PooledObject<FTPClient> wrap(FTPClient ftpClient) {
        return new DefaultPooledObject<FTPClient>(ftpClient);
    }

    /**
         * 销毁对象
     */
    @Override
    public void destroyObject(PooledObject<FTPClient> p) throws Exception {
        FTPClient ftpClient = p.getObject();
        ftpClient.logout();
        super.destroyObject(p);
    }

    /**
         * 验证对象
     */
    @Override
    public boolean validateObject(PooledObject<FTPClient> p) {
        FTPClient ftpClient = p.getObject();
        boolean connect = false;
        try {
            connect = ftpClient.sendNoOp();
            if(connect){                
                ftpClient.changeWorkingDirectory(ftpConfig.getWorkingDirectory());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connect;
    }
    
    /**
     * 上传文件 
     * @param path ftp服务端路径 必须以"/"结尾
     * @param sendfile 本地文件路径
     * @param fileName 上传后文件名
     * @throws ex 
     */
    public boolean sendFile( String path, String sendfile, String fileName ) throws Exception{
    	System.out.println("文件上传，处理前ftp路径完整名:"+path);
		System.out.println("文件上传，处理前本地路径完整名:"+sendfile);
		//处理路径中多余的/ Windows下无法解析
		path=trimPath(path);
		sendfile=trimPath(sendfile);
		System.out.println("文件上传，处理后ftp路径完整名:"+path);
		System.out.println("文件上传，处理后本地路径完整名:"+sendfile);
        long start = System.currentTimeMillis();
        InputStream inputStream = null;
        boolean flag = false;
        try {
            File file = new File(sendfile+fileName);
            if(!file.exists()) {
            	logger.error("***************************文件不存在**************************");
            	return false;
            }
            if (!client.changeWorkingDirectory(path)) {
            	//目录不存在循环创建
            	CreateDirecroty(path);
            }
            inputStream = new FileInputStream(file);
            logger.debug("***************************"+new Date()+"  开始上传");
            flag = client.storeFile(path +fileName , inputStream);
            flag = true;
            logger.debug("***************************上传"+flag+" 耗时："+(System.currentTimeMillis()-start));
        } catch (ConnectException e2) {
        	throw e2 ;
        }catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw e;
        }
        finally{
            if(client != null)  try {client.disconnect();} catch (IOException ioe) {}
        }
        return flag;
    }
    
    /**
	 * * 下载文件 *
	 * 
	 * @param pathname
	 *            FTP服务器文件目录 *
	 * @param filename
	 *            文件名称 *
	 * @param localpath
	 *            下载后的文件路径 *
	 * @return
     * @throws Exception 
	 */

	public boolean downLoadFile(String pathname, String filename, String localpath) throws Exception {
		System.out.println("文件下载，处理前ftp路径完整名:"+pathname);
		System.out.println("文件下载，处理前本地路径完整名:"+localpath);
		//处理路径中多余的/ Windows下无法解析
		pathname=trimPath(pathname);
		localpath=trimPath(localpath);
		System.out.println("文件下载，处理后ftp路径完整名:"+pathname);
		System.out.println("文件下载，处理后本地路径完整名:"+localpath);
		boolean flag = false;
		OutputStream os = null;
		try {
			System.out.println("开始下载文件");
			// 切换FTP目录
			boolean changeFlag = client.changeWorkingDirectory(pathname);
			System.err.println("changeFlag==" + changeFlag);
 
			client.enterLocalPassiveMode();
			client.setRemoteVerificationEnabled(false);
			// 查看有哪些文件夹 以确定切换的ftp路径正确
			FTPFile[] ftpFiles = client.listFiles();
			for (FTPFile file : ftpFiles) {
				System.out.println(file.getName());
				if (filename.equalsIgnoreCase(file.getName())) {
					File localFile = new File(localpath+ file.getName());
					os = new FileOutputStream(localFile);
					client.retrieveFile(file.getName(), os);
					os.close();
					//读取到文件则代表读取成功
					flag = true;
				}
			}
			System.out.println("下载文件成功");
		} catch (Exception e) {
			System.out.println("下载文件失败");
			e.printStackTrace();
			throw new Exception("文件下载失败");
		} finally {
			if (client.isConnected()) {
				try {
					client.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != os) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flag;

	}
	
		// 改变目录路径
		public boolean changeWorkingDirectory(String directory) {
			//处理路径中多余的/ Windows下无法解析
			directory=trimPath(directory);
			boolean flag = true;
			try {
				flag = client.changeWorkingDirectory(directory);
				if (flag) {
					System.out.println("进入文件夹" + directory + " 成功！");
	 
				} else {
					System.out.println("进入文件夹" + directory + " 失败！开始创建文件夹");
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			return flag;
		}

	
		// 判断ftp服务器文件夹是否存在
		public boolean existFile(String path) throws Exception  {
			path=trimPath(path);
			try {
				boolean flag = false;
				FTPFile[] files =client.listFiles(path);
	            if(files!=null&&files.length>0){
	                System.out.println("files size:"+files[0].getSize());
	                flag=true;
	            }
				return flag;
			} catch (ConnectException e2) {
	        	throw new Exception("FTP连接失败");
	        }catch (IOException e) {
				// TODO Auto-generated catch block
				throw new Exception(path+"获取该文件目录时发生错误");
			}
		}

	
		// 创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
		public boolean CreateDirecroty(String remote) throws Exception {
			boolean success = true;
			String directory = remote + "/";
			// 如果远程目录不存在，则递归创建远程服务器目录
			if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(new String(directory))) {
				int start = 0;
				int end = 0;
				if (directory.startsWith("/")) {
					start = 1;
				} else {
					start = 0;
				}
				end = directory.indexOf("/", start);
				String path = "";
				String paths = "";
				while (true) {
					String subDirectory = new String(remote.substring(start, end).getBytes("GBK"), "iso-8859-1");
					
					if(subDirectory==null||subDirectory.trim().equals("")) {
						start = end + 1;
						end = directory.indexOf("/", start);
						continue;
					}
					path = path + "/" + subDirectory;
					if (!existFile(path)) {
						if (makeDirectory(subDirectory)) {
							changeWorkingDirectory(subDirectory);
						} else {
							System.out.println("创建目录[" + subDirectory + "]失败");
							changeWorkingDirectory(subDirectory);
						}
					} else {
						changeWorkingDirectory(subDirectory);
					}
	 
					paths = paths + "/" + subDirectory;
					start = end + 1;
					end = directory.indexOf("/", start);
					// 检查所有目录是否创建完毕
					if (end <= start) {
						break;
					}
				}
			}
			return success;
		}
		
		// 创建目录
		public boolean makeDirectory(String dir) {
			boolean flag = true;
			try {
				flag = client.makeDirectory(dir);
				if (flag) {
					System.out.println("创建文件夹" + dir + " 成功！");
	 
				} else {
					System.out.println("创建文件夹" + dir + " 失败！");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return flag;
		}


		/**
		 * 逐行读取文件
		 * @param path
		 * @return
		 * @throws Exception 
			 */
		public List<String> readFile(String path) throws Exception{
			 List<String> list = new ArrayList<String>();
			  InputStream is = null; 
			  InputStreamReader input=null;
			 try {
				 is = client.retrieveFileStream(path);
				 input=new InputStreamReader(is);
				 BufferedReader reader = new BufferedReader(input);
				 String line="";
				 while ((line = reader.readLine()) != null) {
	                System.out.println(line);
	                list.add(line);
	            }
				 reader.close();
				 if(is!=null) {
					 is.close();
				 }
				 // 主动调用一次getReply()把接下来的226消费掉. 这样做是可以解决这个返回null问题
		         client.getReply();
			} catch (Exception e) {
				// TODO: handle exception
				throw new Exception();
			}
			 	return list;
			
		}
		
		/**
		 * 处理路径中多余的/
		 * @param path
		 * @return
		 */
		public static 	String  trimPath(String path) {
			String[] arr=path.split("/");
			StringBuffer sb=new  StringBuffer();
			//linux mac系统路径以/开头
			if(path.substring(0,1).equals("/")) {
				sb.append("/");
			}
			for (String string : arr) {
				if(string!=null&&!string.equals("")) {
					sb.append(string+"/");
				}
			}
			String str=sb.toString();
			//源路径非/结尾，为文件地址，结尾要去掉拼接的/
			if(!(path.substring(path.length()-1).equals("/"))) {
				str=str.substring(0, str.length()-1);
			}
			return str;
			
		}
		public static void main(String[] args) {
			try {
//				System.out.println(trimPath("///user///home/afa/"));
//				System.out.println(trimPath("d://12312/2342/12312/21312.txt"));
				System.out.println(new FTPClientFactory("172.20.10.7", 2121, "root", "root").existFile("/bus/201904/"));
	//			new FTPClientFactory("172.20.10.7", 2121, "root", "root").sendFile("/bus/201904/","/users/liuzemin/desktop/test/M113-DEPART113-FMP-20190422-001-I.DESC", "M113-DEPART113-FMP-20190422-001-I.DESC");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
}
