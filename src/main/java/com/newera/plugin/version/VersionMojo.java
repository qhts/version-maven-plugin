package com.newera.plugin.version;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
/** 
 * 
 *  提供静态文件版本管理的插件
 */  
@Mojo(name="version")
public class VersionMojo extends AbstractMojo  {  
	
    /**
     * 扫描资源文件或目录
     */
    @Parameter
    private List<File> scanIncluds;

    /**
     * 扫描替换文件或目录
     */
    @Parameter(required=true)
    private List<File> replaceInputs;
    
    /**
     * 扫描文件扩展名
     */
    @Parameter(defaultValue="*.js,*.css,*.png,*.jpg,*.jpeg,*.gif,*.bmp")
    private String scans;
    
    /**
     * 替换文件扩展名
     */
    @Parameter(defaultValue="*.jsp,*.js,*.css")
    private String replaces;
    
    /**
     * key为资源文件名，value为最新的修改时间
     */
    private Map<String, Long> sources = new HashMap<String, Long>();  
    
    /**
     * 需要替换的文件
     */
    private List<Set<File>> replaceFiles = new ArrayList<Set<File>>();
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
    
    /**
     * 打版本后输出资源地址地址
     */
    @Parameter(required=true)
    private List<File> replaceOutputs;
    
  
    public void execute() throws MojoExecutionException {  
    	if(replaceOutputs.size()!=replaceInputs.size()){
    		getLog().error("替换文件和输出文件数量不匹配");
    		throw new MojoExecutionException("替换文件和输出文件数量不匹配");
    	}
    	//加载静态资源
    	for(int i=0;i<scanIncluds.size();i++){
    		File f = scanIncluds.get(i);
    		if(f.exists()){
    			initScanIncluds(f);
    		}
    	}
    	//加载需要替换的文件
    	for(int i=0;i<replaceInputs.size();i++){
    		File f = replaceInputs.get(i);
    		Set<File> sf = new HashSet<File>();
    		if(f.exists()){
    			initreplaceInputs(f,sf);
    		}
    		replaceFiles.add(sf);
    	}
    	//在待替换文件中寻找替换资源信息，进行替换
    	doReplace();
    }  
    
    /**
     * 在待替换文件中寻找替换资源信息，进行替换
     * @throws MojoExecutionException 
     */
	private void doReplace() throws MojoExecutionException  {
		for(int i=0;i<replaceFiles.size();i++){
			Iterator<File> it = replaceFiles.get(i).iterator();  
			File src = replaceInputs.get(i);
			File dest = replaceOutputs.get(i);
			while(it.hasNext()){
				File f = it.next();
				String child = f.getAbsolutePath().replace(src.getAbsolutePath(), "");
				BufferedReader fr = null;
				BufferedWriter fw = null;
				StringBuffer sb = new StringBuffer();
				String s = "";
				try {
					fr = new BufferedReader(new InputStreamReader(new FileInputStream(f),"utf8"));
					int b;
					char[] c = new char[40960];
					while((b=fr.read(c))!=-1){
						sb.append(c,0,b);
					}
					s = sb.toString();
					for(String key :sources.keySet()){
						String date = sdf.format(new Date(sources.get(key)));
						s = s.replaceAll("([>/\"']{1})"+key+"(\\?v=\\d{10}){0,1}(\\s*[\"')]{1}|[ ]+)", "$1"+key+"?v="+date+"$3");
					}
					File desFile = new File(dest,child);
					if(!desFile.getParentFile().exists()){
						desFile.getParentFile().mkdirs();
					}
					if(!desFile.exists()){
						desFile.createNewFile();
					}
					fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(desFile,false), "utf8"));
					fw.write(s);
					getLog().info(f.getName()+" ---> 版本替换完成");
				} catch (Exception e) {
					getLog().error(e.getMessage());
					throw new MojoExecutionException("操作文件异常",e);
				}finally{
					sb = null;
					if(fr!=null){
						try {
							fr.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						fr = null;
					}
					if(fw!=null){
						try {
							fw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						fw = null;
					}
				}
			}
			
		}
	}

	/**
     * 扫描符合扩展名的全部待替换文件
     */
    private void initreplaceInputs(File file,Set<File> sf) {
    	if(file.isDirectory()){
    		File[] fs = file.listFiles(new ExtendsFileFilter(replaces));
    		for(File f : fs){
    			initreplaceInputs(f,sf);
    		}
    	}else{
    		sf.add(file);
    		getLog().debug("扫到需要替换的文件【"+file.getName()+"】");
    	}
	}

	/**
     * 扫描符合扩展名的全部资源文件
     */
    private void initScanIncluds(File file){
    	//遍历获得所有资源文件
    	if(file.isDirectory()){
    		File[] fs = file.listFiles(new ExtendsFileFilter(scans));
    		for(File f : fs){
    			initScanIncluds(f);
    		}
    	}else{
    		Long stime = sources.get(file.getName());
    		Long ttime = (stime == null ? file.lastModified() : (stime > file.lastModified() ? stime : file.lastModified()));
    		getLog().debug("扫描到文件【"+file.getName()+"】--> 最后修改时间："+sdf.format(new Date(ttime)));
    		sources.put(file.getName(), ttime);
    	}
    }
    
    /**
     * 过滤扩展名的Filter
     * @author qiantao
     *
     */
    class ExtendsFileFilter implements FileFilter{
    	
    	private String[] patten;
    	
    	ExtendsFileFilter(String p){
    		patten = p.split(",");
    		for(int i=0;i<patten.length;i++){
    			if(patten[i].matches("\\*(\\..*)")){
    				patten[i] = patten[i].substring(1);
    			}else{
    				getLog().error("扩展名配置不正确，必须以【*.】开头");
    			}
    		}
    	}

		public boolean accept(File f) {
			if(f.isDirectory()){
				return true;
			}else{
				String filename = f.getName().toLowerCase();
				for(int i=0;i<patten.length;i++){
					if(filename.endsWith(patten[i])){
						return true;
					}
				}
				return false;
			}
		}
    }
    
}  